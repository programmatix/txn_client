/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.cluster;

import com.couchbase.cbadmin.assets.Bucket;
import com.couchbase.cbadmin.assets.Node;
import com.couchbase.cbadmin.assets.NodeGroup;
import com.couchbase.cbadmin.assets.NodeGroupList;
import com.couchbase.cbadmin.client.BucketConfig;
import com.couchbase.cbadmin.client.ClusterConfig;
import com.couchbase.cbadmin.client.ConnectionInfo;
import com.couchbase.cbadmin.client.CouchbaseAdmin;
import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.installer.ClusterInstaller;
import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.handle.HandleOptions;
import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.scenario.Scenario;
import com.couchbase.sdkdclient.ssh.SimpleCommand;
import com.couchbase.sdkdclient.util.Configured;
import com.couchbase.sdkdclient.util.MapUtils;
import com.couchbase.sdkdclient.util.NetworkIO;
import com.couchbase.sdkdclient.util.NoNetworkIO;
import com.couchbase.sdkdclient.util.Retryer;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import org.slf4j.Logger;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class represents a Couchbase cluster which will be set up and
 * possibly manipulated during the test. A cluster is a collection of one
 * or more nodes.
 *
 * Initially, the cluster nodes are not necessarily initialized (that is,
 * they may be in an invalid state or newly created. This class provides
 * the routines needed to set it up.
 *
 * Many of the routines in this class employ the use of the Couchbase
 * REST interface, which is interfaced with in Java using the
 * {@link CouchbaseAdmin} class.
 *
 * Additionally, this class provides methods for accessing the various
 * nodes in the cluster.
 *
 * Typically a {@link Scenario} will manipulate the cluster by e.g.
 * rebalancing, failing over, or adding nodes.
 *
 */
public class CBCluster {
  static public abstract class RestRetryer extends Retryer<RestApiException> {
    RestRetryer(int seconds) {
      super(seconds * 1000, 500, RestApiException.class);
    }

    @Override
    protected void handleError(RestApiException caught) throws RestApiException {
      if (caught.getStatusLine().getStatusCode() >= 500) {
        call();
      } else if (caught.getStatusLine().getStatusCode() == 409) {
        logger.error("N1QL Index was not deleted from previous run");
        return;
      } else {
        throw HarnessException.create(HarnessError.CLUSTER, caught);
      }
    }
  }

  final private ClusterInstaller installer;
  final private CBBucketOptions bucketOptions;
  final private CBClusterOptions clusterOptions;
  final private Nodelist nodelist;
  final private CBBucket mainBucket;
  final private static Logger logger = LogUtil.getLogger(CBCluster.class);

  public String ClusterCertificate;
  public String IndexType;


  public CBCluster(Configured<ClusterBuilder> configuredBuilder) {
    ClusterBuilder builder = configuredBuilder.get();
    bucketOptions = builder.getBucketOptions();
    clusterOptions = builder.getClusterOptions();
    nodelist = builder.getNodelistBuilder().build();
    mainBucket = new CBBucket(bucketOptions.getName(), bucketOptions.getPassword());
    if (clusterOptions.getUpgradeVersion() != "") {
      installer = builder.buildInstaller(true);
    } else {
      installer = builder.buildInstaller(false);
    }
    IndexType = clusterOptions.getN1QLIndexType();
  }


  /**
   * Clears a single cluster and leaves all its nodes in an uninitialized
   * state.
   * @param nodes
   * @throws RestApiException
   * @throws ClusterException
   */
  @NetworkIO
  private void clearSingleCluster(Collection<NodeHost> nodes)
          throws RestApiException, ClusterException {
    NodeHost master = null;
    for (NodeHost node : nodes) {
      Node.Membership mStatus = node.getAdmin().getAsNode().getMembership();
      if (mStatus == Node.Membership.ACTIVE) {
        logger.trace("Selected node {} as master for resetting cluster",
                     node.getKey());
        master = node;
        break;
      }
    }

    if (master == null) {
      // No master node. Is this possible?
      throw new ClusterException("Found cluster with no active nodes");
    }

    master.ensureNObject();

    for (Bucket bkt : master.getAdmin().getBuckets().values()) {
      logger.trace("Deleting existing bucket {}", bkt);
      for (int i = 0; i < 2; i++) {
        try {
          master.getAdmin().deleteBucket(bkt.getName());
          break;
        } catch (RestApiException ex) {
          logger.warn("Error while trying to delete bucket. Trying again", ex);
        }
      }
    }

    for (Node nn : master.getAdmin().getNodes()) {
      if (nn.equals(master.getNObject())) {
        continue;
      }
      logger.debug("Failing over existing and ejecting node {}", nn);
      final NodeHost m = master;
      final Node n = nn;

      if (nn.getMembership() != Node.Membership.INACTIVE_ADDED) {
        new RestRetryer(clusterOptions.getRestTimeout()) {
          @Override
          protected boolean tryOnce() throws RestApiException {
            m.getAdmin().failoverNode(n);
            return true;
          }
        }.call();
      }
      try {
        Thread.sleep(1000);
      } catch (Exception e) {

      }
      new RestRetryer(clusterOptions.getRestTimeout()) {
        @Override
        protected boolean tryOnce() throws RestApiException {
          m.getAdmin().ejectNode(n);
          return true;
        }
      }.call();
    }
    // reset master node

    String cmd = "curl -d 'gen_server:cast(ns_cluster, leave).' -u ";
    cmd += clusterOptions.getRestLogin().getUsername() + ":" + clusterOptions.getRestLogin().getPassword();
    cmd += " http://localhost:8091/diag/eval";
    try {
      Future<SimpleCommand> ft = RemoteCommands.runCommand(master.createSSH(), cmd);
      SimpleCommand res = ft.get();
      if (!res.isSuccess()) {
        logger.warn("Command {} failed. {}, {}", cmd, res.getStderr(), res.getStdout());
      }
    } catch (Exception ex) {
        logger.error("Exception :{}", ex);
    }
  }

  @NetworkIO
  private void resetClusterNodes() throws RestApiException, ClusterException {
    // First, discover the cluster
    Map<String,Collection<NodeHost>> clusterNodes = new HashMap<String, Collection<NodeHost>>();

    final AtomicReference<ConnectionInfo> refInfo = new AtomicReference<ConnectionInfo>();
    for (final NodeHost node : nodelist.getAll()) {
      new RestRetryer(clusterOptions.getRestTimeout()) {
        @Override
        protected boolean tryOnce() throws RestApiException {
          refInfo.set(node.getAdmin().getInfo());
          return true;
        }
      }.call();

      ConnectionInfo info = refInfo.get();

      if (!info.hasCluster()) {
        logger.trace("Not resetting {}. No cluster", node);
        continue;
      }

      MapUtils.addToValue(clusterNodes, info.getClusterIdentifier(), node);
      logger.trace("Node {} is a member of cluster {}",
                   node.getKey(), info.getClusterIdentifier());

    }

    for (Collection<NodeHost> llCluster : clusterNodes.values()) {
      clearSingleCluster(llCluster);
    }
  }

  /** Waits until all nodes are ready for a bucket(s) */
  @NetworkIO
  private void waitForBucketReady() throws RestApiException {
    new RestRetryer(clusterOptions.getRestTimeout() * 1000) {
      @Override
      protected boolean tryOnce() throws RestApiException {
        boolean allHealthy = true;
        List<Node> nodes = getAdmin().getNodes();

        for (Node nn : nodes) {
          if (nn.getStatus() != Node.Status.HEALTHY) {
            logger.trace("Not all nodes healthy: {} is {}",
                         nn.getRestUrl(),
                         nn.getStatus());
            allHealthy = false;
          }
        }
        if (allHealthy) {
          return true;
        }
        return false;
      }
    }.call();

    logger.info("Bucket creation done");
  }

  @NetworkIO
  private String setupMainBucket(CouchbaseAdmin adm) throws RestApiException {
    BucketConfig ourBucket = bucketOptions.buildMainBucketOptions();
    logger.info("Creating bucket {}", ourBucket.name);
    adm.createBucket(ourBucket);
    logger.info("Bucket creation submitted");
    return ourBucket.name;
  }

  @NetworkIO
  private void setupSecondaryBucket(CouchbaseAdmin adm) throws RestApiException {
    BucketConfig ourBucket = bucketOptions.buildMainBucketOptions();
    BucketConfig bConfig = new BucketConfig("bucket1");
    bConfig.bucketType = Bucket.BucketType.COUCHBASE;
    bConfig.ramQuotaMB = 100;
    adm.createBucket(bConfig);
  }

  static private String makeGroupName(int ix) {
    return "TEST_GROUP_" + ix;
  }

  @NetworkIO
  private void setupServerGroups(CouchbaseAdmin adm) throws RestApiException {
    // If we have a number of groups to use, let's do it now.
    if (nodelist.getNumGroups() < 2) {
      logger.debug("Not creating any groups");
      return;
    }

    // Clear existing groups..
    final NodeGroupList nlg = adm.getGroupList();
    final Map<NodeGroup, Collection<Node>> groupConfig = new HashMap<NodeGroup, Collection<Node>>();
    final NodeGroup[] groupObjs = new NodeGroup[nodelist.getNumGroups()];
    final Set<String> usedGroups = new HashSet<String>();

    for (int i = 0; i < nodelist.getNumGroups(); i++) {
      String groupName = makeGroupName(i);
      NodeGroup curObj = nlg.find(groupName);
      if (curObj == null) {
        logger.debug("Group {} does not yet exist. Creating", groupName);
        adm.addGroup(groupName);
        curObj = adm.findGroup(groupName);
      }
      groupObjs[i] = curObj;
    }

    for (NodeHost nn : nodelist.getActive()) {
      int ix = nodelist.getGroupForNode(nn);
      nn.ensureNObject();
      Node nodeObject = nn.getNObject();
      NodeGroup curGroup = groupObjs[ix];
      logger.debug("Adding {} to group {}", nodeObject, curGroup);

      MapUtils.addToValue(groupConfig, curGroup, nodeObject);
      usedGroups.add(curGroup.getName());
    }

    logger.debug("Will apply group configuration...");
    adm.allocateGroups(groupConfig, null);

    logger.debug("Removing unused groups..");
    for (NodeGroup group : nlg.getGroups()) {
      if (!usedGroups.contains(group.getName())) {
        adm.deleteGroup(group);
      }
    }
  }

  @NoNetworkIO
  private void setupIndexStorageMode(final String mode) throws RestApiException, ClusterException {
    final CouchbaseAdmin adm = getAdmin();
    logger.debug("Provisioning initial node {}", adm);
    new RestRetryer(clusterOptions.getRestTimeout()) {
      @Override
      protected boolean tryOnce() throws RestApiException {
        adm.changeIndexerSetting("storageMode", mode);
        logger.trace("Provisioning done");
        return true;
      }
    }.call();
  }

  @NetworkIO
  public void createAnalyticsDataSet(final String bucketName, boolean retry) throws RestApiException {
    List<NodeHost> analyticsNodes = nodelist.getAnalyticsNodes();
    NodeHost analyticsNode = null;
    for (NodeHost node: analyticsNodes) {
      if (getActiveNodes().contains(node)) {
        analyticsNode = node;
      }
    }
    if (analyticsNode == null) {
      logger.debug("no active analytics node found");
      return;
    }
    final CouchbaseAdmin adm = analyticsNode.getAdmin();
    if (retry) {
      new RestRetryer(clusterOptions.getRestTimeout()) {
        @Override
        protected boolean tryOnce() throws RestApiException {
          adm.createAnalyticsDataSet(bucketName);
          logger.trace("Creating analytics data set done");
          return true;
        }
      }.call();
    } else {
      adm.createAnalyticsDataSet(bucketName);
      logger.trace("Creating analytics data set done");
    }
  }


  @NetworkIO
  public void connectLocalAnalyticsDataSet(boolean retry) throws RestApiException {
    List<NodeHost> analyticsNodes = nodelist.getAnalyticsNodes();
    NodeHost analyticsNode = null;
    for (NodeHost node: analyticsNodes) {
      if (getActiveNodes().contains(node)) {
        analyticsNode = node;
      }
    }
    if (analyticsNode == null) {
      logger.debug("no active analytics node found");
      return;
    }
    final CouchbaseAdmin adm = analyticsNode.getAdmin();
    if (retry) {
      new RestRetryer(clusterOptions.getRestTimeout()) {
        @Override
        protected boolean tryOnce() throws RestApiException {
          adm.connectLocalAnalyticsDataSet();
          logger.trace("Connecting analytics data set done");
          return true;
        }
      }.call();
    } else {
      adm.connectLocalAnalyticsDataSet();
      logger.trace("Connecting analytics data set done");
    }
  }

  @NetworkIO
  public void createN1QLIndex(final String bucketName, String[] params, final String targetNode, boolean retry) throws RestApiException, ClusterException, InsufficientNodesException {
    List<NodeHost> n1QLNodes =  nodelist.getN1QLNodes();
    NodeHost n1qlNode = null;
    for (NodeHost node:n1QLNodes) {
      if (getActiveNodes().contains(node)) {
        n1qlNode = node;
      }
    }
    if (n1qlNode == null) {
      logger.debug("No active n1ql node found");
      return;
    }

    final CouchbaseAdmin adm = n1qlNode.getAdmin();
    final String[] n1qlParams = params;
    Random rand = new Random();

    final String indexName = clusterOptions.getN1QLIndexName()+Integer.toString(rand.nextInt(1000) + 1);
    if (retry) {
      new RestRetryer(clusterOptions.getRestTimeout()) {
        @Override
        protected boolean tryOnce() throws RestApiException {
          // create secondary index
          adm.setupN1QLIndex(indexName, IndexType, bucketName, n1qlParams, targetNode);
          logger.trace("Creating N1QL index done");
          return true;
        }
      }.call();
    } else {
      adm.setupN1QLIndex(indexName, IndexType, bucketName, n1qlParams, targetNode);
      logger.trace("Creating N1QL index done");
    }
  }

  @NetworkIO
  private void createFTSIndex(final String bucketName) throws RestApiException, ClusterException, InsufficientNodesException {
    List<NodeHost> ftsNodes =  nodelist.getFTSNodes();
    NodeHost ftsNode = null;
    for (NodeHost node:ftsNodes) {
      if (getActiveNodes().contains(node)) {
        ftsNode = node;
      }
    }
    if (ftsNode == null) {
      logger.debug("No active fts node found");
      return;
    }
    final CouchbaseAdmin adm = ftsNode.getAdmin();
    final String indexName = clusterOptions.getFtsIndexName();
    new RestRetryer(clusterOptions.getRestTimeout()) {
      @Override
      protected boolean tryOnce() throws RestApiException {
        adm.setupFTSIndex(indexName, bucketName);
        logger.trace("Creating FTS index done");
        return true;
      }
    }.call();


  }

  @NetworkIO
  private void setupNewCluster() throws RestApiException, ClusterException, InsufficientNodesException {
    final CouchbaseAdmin adm = getAdmin();
    final ClusterConfig clConfig = new ClusterConfig();

    logger.debug("setup service of initial node {}", adm);
    try {
      new RestRetryer(3) {
        @Override
        protected boolean tryOnce() throws RestApiException {
          adm.setupInitialService(nodelist.getMaster().getServices());
          logger.trace("Initial service added");
          return true;
        }
      }.call();
    } catch (Exception e) {
      logger.debug("provision is done in previous test");
    }

    clConfig.memoryQuota = clusterOptions.getNodeQuota();
    logger.debug("Provisioning initial node {}", adm);
    new RestRetryer(clusterOptions.getRestTimeout()) {
      @Override
      protected boolean tryOnce() throws RestApiException {
        adm.initNewCluster(clConfig);
        logger.trace("Provisioning done");
        return true;
      }
    }.call();

    // set hostname to master node
    String nodeHostname = String.valueOf(nodelist.getMaster());
    if (nodeHostname.contains(".com")) {
      nodeHostname = nodeHostname.replace(":8091", "");
      nodeHostname = nodeHostname.replace("http://", "");
      adm.setupInitialHostname(nodeHostname);
      logger.debug("Set hostname to master node");
    }

    // create user
    adm.createUser("default", "password");
    if (!bucketOptions.getName().isEmpty() && !bucketOptions.getName().equals("default"))
      adm.createUser(bucketOptions.getName(), "password");

    JoinReadyPoller.poll(nodelist.getActiveAux(), clusterOptions.getRestTimeout());


    for (final NodeHost nn : nodelist.getActiveAux()) {
      new RestRetryer(clusterOptions.getRestTimeout()) {
        @Override
        protected boolean tryOnce() throws RestApiException, InsufficientNodesException {
          logger.info("Adding node {} with services {}", nn.getAdmin().getEntryPoint(), nn.getServices());
          adm.addNewNode(nn.getAdmin().getEntryPoint(), nn.getServices());
          return true;
        }
      }.call();
    }

    logger.info("All nodes added. Will rebalance");
    adm.rebalance();
    try {
      RebalanceWaiter.poll(adm).get();
    } catch (ExecutionException ex) {
      throw new ClusterException(ex);
    } catch (InterruptedException ex) {
      throw new ClusterException(ex);
    }

    if (clusterOptions.getSetStorageMode()) {
      setupIndexStorageMode("memory_optimized");
    } else {
      setupIndexStorageMode("forestdb");
    }
    setupServerGroups(adm);

    // Again, really make sure no buckets exist..
    for (Bucket bkt : adm.getBuckets().values()) {
      logger.warn("Still have bucket {}", bkt);
    }

    // Now, add the buckets.
    if (bucketOptions.shouldAddDefaultBucket() &&
            bucketOptions.getName().equals("default") == false) {

      logger.info("Setting up default bucket");
      BucketConfig bConfig = new BucketConfig("default");
      bConfig.bucketType = Bucket.BucketType.COUCHBASE;
      bConfig.ramQuotaMB = 256;
      adm.createBucket(bConfig);
    }

    String bucketName = setupMainBucket(adm);
    setupSecondaryBucket(adm); // To cover MB-26144, since it happens when multiple buckets are created on Spock node
    waitForBucketReady();


    if (clusterOptions.getUseMaxConn()) {
      adm.setClusterMaxConn(clusterOptions.getMaxConn());
    }

    for (NodeHost nn : nodelist.getActiveAux()) {
      nn.ensureNObject();
    }
    try {
      Thread.sleep(10 * 1000);
    } catch (InterruptedException ex) {

    }

    if (bucketOptions.buildMainBucketOptions().bucketType != Bucket.BucketType.MEMCACHED) {
      logger.info("Creating n1ql index");
      // run twice to assure at least 2 index nodes have the n1ql index
      createN1QLIndex(bucketName, clusterOptions.getn1qlFieldsToIndex().split(","), null, true);
      //createN1QLIndex(bucketName, clusterOptions.getn1qlFieldsToIndex().split(","));

      // due to huge files created by fts index in data/@fts, it blocks rebalance thus blocks subdoc test
      // onece we resolved this issue, then turn this back on
    /*logger.info("Creating fts index");
    createFTSIndex(bucketName);*/

      createAnalyticsDataSet(bucketName, true);
      connectLocalAnalyticsDataSet(true);

      logger.info("setting up auto failover = " + clusterOptions.isAutoFailoverEnabled() +
              " with timeout = " + clusterOptions.getAutoFailoverTimeout());
      adm.setupAutoFailover(clusterOptions.isAutoFailoverEnabled(),
              clusterOptions.getAutoFailoverTimeout());
    }
  }
  private void setupNodesSSH() throws ClusterException {
    ExecutorService svc = Executors.newCachedThreadPool();
    List<Future<Boolean>> taskList = new ArrayList<Future<Boolean>>();
    for (final NodeHost nn : nodelist.getAll()) {
      Future<Boolean> ft = svc.submit(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          nn.initSSH();
          return true;
        }
      });

      taskList.add(ft);
    }

    svc.shutdown();
    for (Future ft : taskList) {
      try {
        ft.get();
      } catch (Exception ex) {
        throw new ClusterException(ex);
      }
    }
  }

  /**
   * Starts the cluster.
   * @throws ClusterException
   */
  @NetworkIO
  public void startCluster() throws ClusterException {
    logger.info("Node {} is master now", nodelist.getMaster());
    if (clusterOptions.shouldNotInitialize()) {
      logger.warn("noinit specified. Not setting up cluster or creating buckets");
      return;
    }

    logger.debug("Stopping any existing rebalance operations..");

    if (installer != null) {
      if (!clusterOptions.shouldUseSSH()) {
        throw new ClusterException("Installation request but SSH is disabled");
      }

      try {
          if (!clusterOptions.getIsDockerCluster()) {
            installer.install(nodelist.getAll());
          }
      } catch (ExecutionException ex) {
        throw HarnessException.create(HarnessError.CLUSTER, ex);
      }

      // Ensure we can connect to the REST port
      try {
        new RestRetryer(clusterOptions.getRestTimeout()) {
          @Override
          protected boolean tryOnce() throws RestApiException {
            for (NodeHost nn : nodelist.getAll()) {
              nn.getAdmin().getInfo();
            }
            return true;
          }
        }.call();
      } catch (RestApiException ex) {
        throw HarnessException.create(HarnessError.CLUSTER, ex);
      }
    }

    for (NodeHost node : nodelist.getAll()) {
      try {
        node.getAdmin().stopRebalance();
      } catch (RestApiException ex) {
        //logger.debug("Stop rebalance failed", ex);
      }
    }

    try  {
      resetClusterNodes();
    } catch (RestApiException ex) {
      throw new ClusterException(ex);
    }

    if (clusterOptions.getIsWindowsCluster()) {
      for (NodeHost node : nodelist.getAll()) {
          node.isWindows = true;

      }
    }

    if (clusterOptions.getIsDockerCluster()) {
      for (NodeHost node : nodelist.getAll()) {
        node.isDocker = true;

      }
    }

    // Now we need to reset all the cluster nodes
    try {
      if (clusterOptions.shouldUseSSH()) {
        setupNodesSSH();
      }

      setupNewCluster();
      if (clusterOptions.getUpgradeVersion() != "") {
          try {
            installer.install(nodelist.getFree(), true, clusterOptions.getUpgradeVersion());
          } catch (Exception e) {
            logger.error("Install failed:", e);
          }

      }

    } catch (RestApiException ex) {
      throw new HarnessException(ex);
    }

  }

  @NoNetworkIO
  public HandleOptions createHandleOptions() {
    return new HandleOptions() {
      @Override
      public String getBucketName() {
        return mainBucket.getName();
      }

      @Override
      public String getBucketPassword() {
        return mainBucket.getPassword();
      }

      @Override
      public URI getEntryPoint() {
        NodeHost node = nodelist.getMaster();
        //this work around is to support the .NET client(refer NCBC-833)
        if (node != null) {
          return node.asUri();
        }
        return nodelist.getMaster().asUri();
      }

      @Override
      public List<URI> getAuxNodes() {
        if (!clusterOptions.shouldPassAuxNodes()) {
          return Collections.emptyList();
        }

        List<URI> ll = new ArrayList<URI>();
        for (NodeHost nn : nodelist.getActiveAux()) {
          ll.add(nn.asUri());
        }
        return ll;
      }

      @Override
      public boolean getUseSSL() {
        return clusterOptions.getUseSSL();
      }

      @Override
      public String getClusterCertificate() {
        try {
          if (clusterOptions.getUseSSL()){
            return getAdmin().getClusterCertificate();
          } else {
            return "";
          }
        } catch (RestApiException ex) {
          throw new ClusterException("Unable to get cluster's certificate");
        }

      }

      @Override
      public int getAutoFailoverSetting() {
        if (clusterOptions.isAutoFailoverEnabled())
          return clusterOptions.getAutoFailoverTimeout() * 1000;
        else
          return 0;
      }

    };
  }

  private static List<Node> mkNodeList(Collection<NodeHost> coll) throws RestApiException {
    List<Node> ll = new ArrayList<Node>();
    for (NodeHost nn : coll) {
      nn.ensureNObject();
      ll.add(nn.getNObject());
    }
    return ll;
  }

  private CouchbaseAdmin getAdmin() {
    return nodelist.getMaster().getAdmin();
  }


  /**
   * Adds nodes and rebalances the cluster, returning a Future object
   * @param nodes A list of nodes to add. The nodes must not be active
   * @return A future which can be waited on for rebalance completion.
   */
  public Future<Boolean> addAndRebalance(Collection<NodeHost> nodes, String services) throws RestApiException {
    logger.debug("Adding Nodes {}", nodes);
    addNodes(nodes, services);
    nodelist.getAdmin().rebalance();
    return RebalanceWaiter.poll(nodelist.getAdmin());
  }

  public String getNodeVersion(NodeHost node) throws RestApiException {
    logger.debug("Lookup installed server version of {}", node);
    List<Node> nodes = nodelist.getAdmin().getNodes();
    String version = "";
    for (Node currentNode: nodes) {
      String currenthost = currentNode.getNSOtpNode();
      String nodehost = node.getHostname();
      if (currentNode.getNSOtpNode().equals("ns_1@"+node.getHostname())) {
        version = currentNode.getClusterVersion();
        break;
      }
    }
    return version;
  }

  /**
   * Adds nodes the cluster.
   * @param nodes A list of nodes to add. The nodes must not be active
   * @throws RestApiException
   */
  public void addNodes(Collection<NodeHost> nodes, String services) throws RestApiException {
    logger.debug("Adding nodes {}", nodes);
    if (!nodelist.getFree().containsAll(nodes)) {
      throw new IllegalArgumentException("Nodes must all be free");
    }

    for (NodeHost nn : nodes) {
      getAdmin().addNewNode(nn.getAdmin(), services);
    }

    nodelist.activate(nodes);
  }

  /**
   * Remove and rebalance nodes from the cluster.
   * @param nodes Nodes to remove. The nodes must be active.
   * @return A future to wait on. When its {@code get()} method returns the
   * rebalance will have been completed.
   * @throws RestApiException
   */
  public Future<Boolean> removeAndRebalance(Collection<NodeHost> nodes)
          throws RestApiException {
    logger.debug("Removing Nodes {}", nodes);
    if (!nodelist.getActive().containsAll(nodes)) {
      throw new IllegalArgumentException("Not all nodes specified were active");
    }

    List<NodeHost> nextActive = nodelist.getNextActive(nodes);
    nodelist.maybeSwitchMaster(nextActive);

    List<Node> rbKnown = mkNodeList(nextActive);
    List<Node> rbEject = mkNodeList(nodes);

    // They're still part of the cluster..
    rbKnown.addAll(rbEject);

    nodelist.getAdmin().rebalance(rbKnown, null, rbEject);
    nodelist.remove(nodes);
    return RebalanceWaiter.poll(nodelist.getMaster().getAdmin());
  }

  /**
   * Swap nodes from the cluster.
   * @param toAdd The nodes to add to the cluster. These nodes must be free
   * @param toRemove The nodes to remove from the cluster. These nodes must be
   * active.
   * @return A future which can be waited upon for rebalance.
   * @throws RestApiException
   */
  public Future<Boolean> swapAndRebalance(
          Collection<NodeHost> toAdd,
          Collection<NodeHost> toRemove,
          String services) throws RestApiException {

    if (!nodelist.getFree().containsAll(toAdd)) {
      throw new IllegalArgumentException("toAdd must only contain free nodes");
    }

    if (!nodelist.getActive().containsAll(toRemove)) {
      throw new IllegalArgumentException("toRemove must only contain active nodes");
    }

    // Also sets the active nodes we care about.
    addNodes(toAdd, services);

    List<NodeHost> nextActive = nodelist.getNextActive(toRemove);
    nodelist.maybeSwitchMaster(nextActive);

    List<Node> rbKnown = mkNodeList(nextActive);
    List<Node> rbRemove = mkNodeList(toRemove);
    rbKnown.addAll(rbRemove);

    nodelist.remove(toRemove);
    nodelist.getAdmin().rebalance(rbKnown, null, rbRemove);
    return RebalanceWaiter.poll(nodelist.getAdmin());
  }

  /**
   * Fail over nodes from the cluster
   * @param toFailover A collection of nodes to fail over. The nodes must be active.
   * @throws RestApiException
   */
  public void failoverNodes(Collection<NodeHost> toFailover) throws RestApiException {
    if (!nodelist.getActive().containsAll(toFailover)) {
      throw new IllegalArgumentException(
              "toFailover must only contain active nodes");
    }

    nodelist.switchMasterFromFailedNodes(toFailover);

    List<Node> foList = mkNodeList(toFailover);
    for (Node nn : foList) {
      logger.info("Failing over {}", nn);
      nodelist.getAdmin().failoverNode(nn);
    }

    nodelist.fail(toFailover);
  }

  public void ejectNodes(Collection<NodeHost> toEject) throws RestApiException {
    for (NodeHost nn : toEject) {
      if (!nn.getState().contains(NodeHost.State.FAILED_OVER)) {
        throw new IllegalArgumentException("Node "+ nn + " is not failed over");
      }
    }

    List<Node> ejList = mkNodeList(toEject);

    for (Node nn : ejList) {
      nodelist.getAdmin().ejectNode(nn);
    }

    nodelist.remove(toEject);
  }

  public void reAddNodes(Collection<NodeHost> toReadd) throws RestApiException {
    for (NodeHost nn : toReadd) {
      if (!nn.getState().contains(NodeHost.State.FAILED_OVER)) {
        throw new IllegalArgumentException("Node is not failed over");
      }
    }

    List<Node> raList = mkNodeList(toReadd);
    for (Node nn : raList) {
      nodelist.getAdmin().readdNode(nn);
    }

    nodelist.activate(toReadd);
  }

  public Future<Boolean> rebalanceCluster() throws RestApiException {
    getAdmin().rebalance();
    return RebalanceWaiter.poll(getAdmin());
  }


  // <editor-fold desc="Nodelist Proxies">
  public NodeHost getMasterNode() {
    return nodelist.getMaster();
  }

  public Collection<NodeHost> getAllNodes() {
    return nodelist.getAll();
  }

  public Collection<NodeHost> getActiveNodes() {
    return nodelist.getActive();
  }

  public Collection<NodeHost> getActiveAuxNodes() {
    return nodelist.getActiveAux();
  }

  public Collection<NodeHost> getFreeNodes() {
    return nodelist.getFree();
  }

  public Collection<NodeHost> getN1QLNodes() { return nodelist.getN1QLNodes(); }

  public Collection<NodeHost> getActiveSubset(int count, boolean includeMaster) {
    return nodelist.getActiveSubset(count, includeMaster);
  }

  public void updateBadNodes(Collection<NodeHost> badnodes, boolean ensureMaster) {
    nodelist.updateBadNodes(badnodes, ensureMaster);
  }

  // </editor-fold>


  @NoNetworkIO
  public CBBucket getMainBucket() {
    return mainBucket;
  }

  @NetworkIO
  public String getClusterVersion() throws RestApiException {
    return getAdmin().getInfo().getVersion();
  }

  @NoNetworkIO
  public String getFTSIndexName() {
    return clusterOptions.getFtsIndexName();
  }

  @NoNetworkIO
  public String getN1QLIndexName() {
    return clusterOptions.getN1QLIndexName();
  }

  @NoNetworkIO
  public Boolean checkQueryDistribution() { return clusterOptions.checkQueryDistribution(); }

  @NetworkIO
  public void getLogs() throws Exception {
    nodelist.getAdmin().startCBCollectInfo();
    Future<Boolean> ft = LogCollectionWaiter.poll(nodelist.getAdmin());
    ft.get();
    List<NodeHost> nodes = nodelist.getAll();
    for(Entry<String,String> entry:nodelist.getAdmin().logPaths.entrySet()) {
      for(NodeHost node: nodes) {
        if (entry.getKey().compareToIgnoreCase(node.host) == 0) {
          RemoteCommands.downloadLog(node.getSSH(), node.host, entry.getValue());
        }
      }
      if (entry.getKey().compareToIgnoreCase("127.0.0.1") == 0) {
        RemoteCommands.downloadLog(getMasterNode().getSSH(), getMasterNode().host, entry.getValue());
      }
    }
  }
}