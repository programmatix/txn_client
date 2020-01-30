package com.couchbase.Couchbase.Cluster;

import com.couchbase.Constants.Strings;
import com.couchbase.Constants.defaults;
import com.couchbase.Couchbase.Bucket.Bucket;
import com.couchbase.Couchbase.Bucket.BucketConfig;
import com.couchbase.Couchbase.Cluster.ClusterConfigure;
import com.couchbase.Couchbase.Couchbase.CouchbaseAdmin;
import com.couchbase.Couchbase.Nodes.*;
import com.couchbase.Exceptions.ClusterException;
import com.couchbase.Exceptions.InsufficientNodesException;
import com.couchbase.Exceptions.RestApiException;
import com.couchbase.InputParameters.inputParameters;
import com.couchbase.Logging.LogUtil;
import com.couchbase.Utils.RemoteUtils.RemoteCommands;
import com.couchbase.Utils.RemoteUtils.ServiceLogin;
import com.couchbase.Utils.RemoteUtils.SimpleCommand;
import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.*;

public class ClusterConfigureUtils {
    final private static Logger logger = LogUtil.getLogger(ClusterConfigure.class);

    private Nodelist nodelist;
    private inputParameters inputParameters;

    ClusterConfigureUtils(Nodelist nodeList, inputParameters inputParameters){
        this.nodelist=nodeList;
        this.inputParameters = inputParameters;
    }



    /**
     * Clears a single cluster and leaves all its nodes in an uninitialized
     * state.
     * @param nodes
     * @throws RestApiException
     * @throws ClusterException
     */
    public void clearSingleCluster(Collection<NodeHost> nodes)
            throws Exception {

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
            throw new Exception("Found cluster with no active nodes");
        }

        master.ensureNObject();

        for (Bucket bkt : master.getAdmin().getBuckets().values()) {
            logger.trace("Deleting existing bucket {}", bkt);
            for (int i = 0; i < 2; i++) {
                master.getAdmin().deleteBucket(bkt.getName());
                break;
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
                new ClusterConfigure.RestRetryer(defaults.RestTimeout) {
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
            new ClusterConfigure.RestRetryer(defaults.RestTimeout) {
                @Override
                protected boolean tryOnce() throws RestApiException {
                    m.getAdmin().ejectNode(n);
                    return true;
                }
            }.call();
        }
        // reset master node

        String cmd = "curl -d 'gen_server:cast(ns_cluster, leave).' -u ";
        cmd += getRestLogin().getUsername() + ":" + getRestLogin().getPassword();
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


    public void setupServerGroups(CouchbaseAdmin adm) throws Exception {
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

            addToValue(groupConfig, curGroup, nodeObject);
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

    public void setupIndexStorageMode(final String mode) throws RestApiException, InterruptedException {
        final CouchbaseAdmin adm = nodelist.getMaster().getAdmin();
        logger.debug("Provisioning initial node {}", adm);
        new ClusterConfigure.RestRetryer(defaults.RestTimeout) {
            @Override
            protected boolean tryOnce() throws RestApiException {
                adm.changeIndexerSetting("storageMode", mode);
                logger.trace("Provisioning done");
                return true;
            }
        }.call();
    }



     String setupMainBucket(CouchbaseAdmin adm) throws RestApiException {
        BucketConfig ourBucket = buildMainBucketOptions();
        adm.createBucket(ourBucket);
        return ourBucket.name;
    }

     void setupSecondaryBucket(CouchbaseAdmin adm) throws RestApiException {
        BucketConfig bConfig = new BucketConfig("bucket1");
        bConfig.bucketType = Bucket.BucketType.COUCHBASE;
        bConfig.ramQuotaMB = 100;
        adm.createBucket(bConfig);
    }

    /** Waits until all nodes are ready for a bucket(s) */
     void waitForBucketReady() throws RestApiException, InterruptedException {
        new ClusterConfigure.RestRetryer(defaults.RestTimeout) {
            @Override
            protected boolean tryOnce() throws RestApiException {
                boolean allHealthy = true;
                List<Node> nodes = nodelist.getMaster().getAdmin().getNodes();

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


    public void createN1QLIndex(final String bucketName, String[] params, final String targetNode, boolean retry) throws RestApiException, ClusterException, InsufficientNodesException, InterruptedException {
        List<NodeHost> n1QLNodes =  nodelist.getN1QLNodes();
        NodeHost n1qlNode = null;
        for (NodeHost node:n1QLNodes) {
            if (nodelist.getActive().contains(node)) {
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

        final String indexName = inputParameters.getn1qlIndexName()+Integer.toString(rand.nextInt(1000) + 1);
        if (retry) {
            new ClusterConfigure.RestRetryer(defaults.RestTimeout) {
                @Override
                protected boolean tryOnce() throws RestApiException {
                    // create secondary index
                    adm.setupN1QLIndex(indexName, inputParameters.getn1qlIndexType(), bucketName, n1qlParams, targetNode);
                    logger.trace("Creating N1QL index done");
                    return true;
                }
            }.call();
        } else {
            adm.setupN1QLIndex(indexName, inputParameters.getn1qlIndexType(), bucketName, n1qlParams, targetNode);
            logger.trace("Creating N1QL index done");
        }
    }


    public void createAnalyticsDataSet(final String bucketName, boolean retry) throws RestApiException, InterruptedException {
        List<NodeHost> analyticsNodes = nodelist.getAnalyticsNodes();
        NodeHost analyticsNode = null;
        for (NodeHost node: analyticsNodes) {
            if (nodelist.getActive().contains(node)) {
                analyticsNode = node;
            }
        }
        if (analyticsNode == null) {
            logger.debug("no active analytics node found");
            return;
        }
        final CouchbaseAdmin adm = analyticsNode.getAdmin();
        if (retry) {
            new ClusterConfigure.RestRetryer(defaults.RestTimeout) {
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

    public void connectLocalAnalyticsDataSet(boolean retry) throws RestApiException, InterruptedException {
        List<NodeHost> analyticsNodes = nodelist.getAnalyticsNodes();
        NodeHost analyticsNode = null;
        for (NodeHost node: analyticsNodes) {
            if (nodelist.getActive().contains(node)) {
                analyticsNode = node;
            }
        }
        if (analyticsNode == null) {
            logger.debug("no active analytics node found");
            return;
        }
        final CouchbaseAdmin adm = analyticsNode.getAdmin();
        if (retry) {
            new ClusterConfigure.RestRetryer(defaults.RestTimeout) {
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

    static private String makeGroupName(int ix) {
        return "TEST_GROUP_" + ix;
    }



    public BucketConfig buildMainBucketOptions() {
        BucketConfig bconf = new BucketConfig(inputParameters.getbucketname());
        bconf.bucketType = bconf.getbucketType(inputParameters.getbucketType());
        bconf.ramQuotaMB = inputParameters.getbucketRamSize();
        bconf.replicaCount = inputParameters.getbucketReplicaCount();
        bconf.evictionPolicy = bconf.getevictionPolicy(inputParameters.getbucketEphemeralEvictionPolicy());
        String passwd = inputParameters.getbucketSaslpassword();
        if (passwd != null && passwd.length() > 0) {
            bconf.setSaslPassword(passwd);
        }
        return bconf;
    }

    public ServiceLogin getRestLogin() {
        return new ServiceLogin(Strings.ADMIN_USER,
                Strings.PASSWORD,
                -1);
    }

    public ServiceLogin getUnixLogin() {
        return new ServiceLogin(inputParameters.getsshusername(),
                inputParameters.getsshpassword(),
                -1);
    }

    static public <T,U> void addToValue(Map<T, Collection<U>> map, T key, U value, Callable<? extends Collection<U>> defl) {
        Collection<U> coll = map.get(key);

        if (coll == null) {
            try {
                coll = defl.call();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            if (coll == null) {
                throw new IllegalArgumentException("Expected returned collection. Got null");
            }
            map.put(key, coll);
        }
        coll.add(value);
    }

    /**
     * Like {@link #addToValue(Map, Object, Object, Callable) }
     * but uses an array list factory.
     * @param <T>
     * @param <U>
     * @param map
     * @param key
     * @param value
     */
    static public <T, U> void addToValue(Map<T, Collection<U>> map, T key, U value) {
        addToValue(map, key, value, new Callable<Collection<U>>() {
            @Override
            public ArrayList<U> call() {
                return new ArrayList<U>();
            }
        });
    }
}
