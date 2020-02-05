package com.couchbase.Couchbase.Cluster;

import com.couchbase.Constants.Strings;
import com.couchbase.Constants.defaults;
import com.couchbase.Couchbase.Bucket.Bucket;
import com.couchbase.Couchbase.Bucket.BucketConfig;
import com.couchbase.Couchbase.Couchbase.CouchbaseAdmin;
import com.couchbase.Couchbase.Nodes.NodeHost;
import com.couchbase.Couchbase.Nodes.Nodelist;
import com.couchbase.Couchbase.Nodes.NodelistBuilder;
import com.couchbase.Couchbase.Utils.RebalanceWaiter;
import com.couchbase.Exceptions.*;
import com.couchbase.InputParameters.inputParameters;
import com.couchbase.Logging.LogUtil;
import com.couchbase.Utils.AliasLookup;
import com.couchbase.Utils.RemoteUtils.ConnectionInfo;
import com.couchbase.Utils.RemoteUtils.ServiceLogin;
import com.couchbase.Utils.Retryer;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class ClusterConfigure {
    final private static Logger logger = LogUtil.getLogger(ClusterConfigure.class);
    private inputParameters inputParameters;
    private NodelistBuilder nlb = null;
    private Nodelist nodelist;
    final private ArrayList<NodeHost> nodes = new ArrayList<NodeHost>();
    final private AliasLookup ourAliases = new AliasLookup();
    private ClusterConfigureUtils clusterconfigureutils;



    public ClusterConfigure(inputParameters inputParameters){
        this.inputParameters=inputParameters;
    }

    static public abstract class RestRetryer extends Retryer<RestApiException> {
        RestRetryer(int seconds) {
            super(seconds * 1000, 500, RestApiException.class);
        }

        @Override
        protected void handleError(RestApiException caught) throws RestApiException, InterruptedException {
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

    public String initializecluster(boolean ConfigureCluster){
        String seedNode =  configureCluster();

        if(ConfigureCluster){
            if (!inputParameters.getinstallcouchbase()) {
                {
                    if(!inputParameters.getinitializecluster()){
                        logger.warn("Initializing of cluster is set to false. Hence not configuring the cluster");
                        return seedNode ;
                    }
                }
            }
            clusterconfigureutils = new ClusterConfigureUtils(nodelist,inputParameters);
            // Ensure we can connect to the REST port
            testRestApiConnection();

            //Stop rebalance
            try  {
                stopRebalance();
            } catch (Exception ex) {
                throw new ClusterException(ex);
            }

            // Now we need to reset all the cluster nodes
            try  {
                resetClusterNodes();
            } catch (RestApiException ex) {
                throw new ClusterException(ex);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if (inputParameters.shouldUseSSH()) {
                    setupNodesSSH();
                }
                setupNewCluster();
            } catch (RestApiException | InterruptedException ex) {
                throw new HarnessException(ex);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            logger.info("ConfigureCluster is not asked not to perform. Hence returning empty");
        }
        return seedNode;
    }


    private String configureCluster(){
        try{
            for (String alias : defaults.ipalias) {
                List<String> aliases = Arrays.asList(alias.split("/"));
                ourAliases.associateAlias(aliases);
            }
            addNodesFromSpec();

            for (NodeHost nn : nodes) {
                nn.getAdmin().getAliasLookupCache().merge(ourAliases);
            }

            nlb = new NodelistBuilder(nodes, defaults.numGroups, inputParameters.getUpgradeVersion());
            this.nodelist= nlb.build();

        }catch(Exception e){
            logger.error("Exception during configuring cluster: "+e);
            System.exit(-1);
        }

        return nodelist.getMaster().host;
    }

    private void testRestApiConnection(){
        try {
            new RestRetryer(defaults.RestTimeout) {
                @Override
                protected boolean tryOnce() throws RestApiException {
                    for (NodeHost nn : nodelist.getAll()) {
                        nn.getAdmin().getInfo();
                        logger.debug(" nn.getAdmin().getInfo(): "+ nn.getAdmin().getInfo());
                    }
                    return true;
                }
            }.call();
        } catch (RestApiException ex) {
            throw HarnessException.create(HarnessError.CLUSTER, ex);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void  stopRebalance(){
        for (NodeHost node : nodelist.getAll()) {
            try {
                node.getAdmin().stopRebalance();
            } catch (RestApiException ex) {
                //logger.debug("Stop rebalance failed", ex);
            }
        }

    }

    /**
     * Adds nodes from the specification strings provided to the cluster
     * options.
     */
    private void addNodesFromSpec() {
        NodeHost nn;
        // Gets all the relevant options and handles them there.

        for (inputParameters.Host host : inputParameters.getClusterNodes()) {
            nn = NodeHost.fromSpec(host.ip+":",
                    getRestLogin(),
                    getUnixLogin(),
                    inputParameters.getclusterVersion(),
                    host.hostservices );
            logger.debug("Nodes order {}", nn.asUri());
            node(nn);
        }
    }

    /**
     * Adds a node to the cluster.
     * @param nn A node to add
     * @return The builder
     */
    public ClusterConfigure node(NodeHost nn) {
        if (nodes.contains(nn)) {
            logger.debug("Node {} already exists. Replacing", nn);
            nodes.remove(nn);
        }
        nodes.add(nn);
        logger.debug("Nodes collection {}", nodes);
        return this;
    }

    /**
     * @see #node(NodeHost)
     */
    public ClusterConfigure node(Collection<NodeHost> nn) {
        for (NodeHost node : nn) {
            node(node);
        }
        return this;
    }

    private void resetClusterNodes() throws Exception, ClusterException {
        // First, discover the cluster
        Map<String,Collection<NodeHost>> clusterNodes = new HashMap<String, Collection<NodeHost>>();
        final AtomicReference<ConnectionInfo> refInfo = new AtomicReference<ConnectionInfo>();
        for (final NodeHost node : nodelist.getAll()) {
            new ClusterConfigure.RestRetryer(defaults.RestTimeout) {
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

            ClusterConfigureUtils.addToValue(clusterNodes, info.getClusterIdentifier(), node);
            logger.trace("Node {} is a member of cluster {}",
                    node.getKey(), info.getClusterIdentifier());

        }
        for (Collection<NodeHost> llCluster : clusterNodes.values()) {
            clusterconfigureutils.clearSingleCluster(llCluster);
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


    private void setupNewCluster() throws Exception {
        final CouchbaseAdmin adm = nodelist.getMaster().getAdmin();

        logger.debug("setup service of initial node {}", adm);
        try {
            new ClusterConfigure.RestRetryer(3) {
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


        logger.debug("Provisioning initial node {}", adm);
        new ClusterConfigure.RestRetryer(defaults.RestTimeout) {
            @Override
            protected boolean tryOnce() throws RestApiException {
                adm.initNewCluster(inputParameters.getNodeQuota());
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
        if (!inputParameters.getbucketname().isEmpty() && !inputParameters.getbucketname().equals("default"))
            adm.createUser(inputParameters.getbucketname(), inputParameters.getbucketpassword());

        JoinReadyPoller.poll(nodelist.getActiveAux(), defaults.RestTimeout);


        for (final NodeHost nn : nodelist.getActiveAux()) {
            new ClusterConfigure.RestRetryer(defaults.RestTimeout) {
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

        if (inputParameters.getsetStorageMode()) {
            clusterconfigureutils.setupIndexStorageMode("memory_optimized");
        } else {
            clusterconfigureutils.setupIndexStorageMode("forestdb");
        }
        clusterconfigureutils.setupServerGroups(adm);

        // Again, really make sure no buckets exist..
        for (Bucket bkt : adm.getBuckets().values()) {
            logger.warn("Still have bucket {}", bkt);
        }

        // Now, add the buckets.
        if (inputParameters.getadddefaultbucket() &&
                inputParameters.getbucketname().equals("default") == false) {
            BucketConfig bConfig = new BucketConfig("default");
            bConfig.bucketType = Bucket.BucketType.COUCHBASE;
            bConfig.ramQuotaMB = 256;
            adm.createBucket(bConfig);
        }
        String bucketName = clusterconfigureutils.setupMainBucket(adm);
        clusterconfigureutils.setupSecondaryBucket(adm); // To cover MB-26144, since it happens when multiple buckets are created on Spock node
        clusterconfigureutils.waitForBucketReady();

        if (inputParameters.getusemaxconn()!=0) {
            adm.setClusterMaxConn(inputParameters.getusemaxconn());
        }

        for (NodeHost nn : nodelist.getActiveAux()) {
            nn.ensureNObject();
        }
        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException ex) {

        }

        if (!inputParameters.getbucketType().equals("MEMCACHED")) {


            logger.info("Creating n1ql index");
            // run twice to assure at least 2 index nodes have the n1ql index
            clusterconfigureutils.createN1QLIndex(bucketName, inputParameters.getn1qlFieldsToIndex().split(","), null, true);
            //createN1QLIndex(bucketName, clusterOptions.getn1qlFieldsToIndex().split(","));

            // due to huge files created by fts index in data/@fts, it blocks rebalance thus blocks subdoc test
            // onece we resolved this issue, then turn this back on
    /*logger.info("Creating fts index");
    createFTSIndex(bucketName);*/

            clusterconfigureutils.createAnalyticsDataSet(bucketName, true);
            clusterconfigureutils.connectLocalAnalyticsDataSet(true);

            logger.info("setting up auto failover = " + inputParameters.getenableAutoFailOver() +
                    " with timeout = " + inputParameters.getAutoFailoverTimeout());
            adm.setupAutoFailover(inputParameters.getenableAutoFailOver(),
                    inputParameters.getAutoFailoverTimeout());
        }
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
}