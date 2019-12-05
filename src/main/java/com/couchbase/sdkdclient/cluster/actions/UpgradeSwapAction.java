package com.couchbase.sdkdclient.cluster.actions;

import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.NodeHost;
import com.couchbase.sdkdclient.cluster.NodelistBuilder;
import com.couchbase.sdkdclient.logging.LogUtil;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by jaekwon.park on 6/8/17.
 */
public class UpgradeSwapAction implements RebalanceAction {
    List<NodeHost> toAdd = new ArrayList<NodeHost>();
    Collection<NodeHost> toRemove;
    String services;
    final private static Logger logger = LogUtil.getLogger(UpgradeSwapAction.class);

    public UpgradeSwapAction(String service) {
        this.services = service;
    }

    @Override
    public void setup(NodelistBuilder nlb, RebalanceConfig config) {
        toAdd.addAll(nlb.reserveFree(config.getNumNodes(), true, services));
        toRemove = nlb.reserveForRemoval(config.getNumNodes(), true, true, services);

    }

    @Override
    public Future<Boolean> start(CBCluster cluster) throws RestApiException, ExecutionException,InterruptedException  {

        if (toAdd.size() != toRemove.size()) {
            throw new IllegalArgumentException("Number of nodes to be added: " + toAdd.size() + " must " +
                    "be equal to number of nodes to be removed:" + toRemove.size());
        }

        List<NodeHost> nodesReservedForRemoval = new ArrayList<NodeHost>(toRemove);

        int ii = 0;
        for(NodeHost an:toAdd) {
            List<NodeHost> addNodeColl = Arrays.asList(an);
            ii++;
            for(NodeHost rn:nodesReservedForRemoval) {
                int oldVersion = Integer.valueOf(cluster.getNodeVersion(rn).split("\\.")[0]);
                logger.info("service="+rn.getServices()+", version="+oldVersion);

                List<NodeHost> removeNodeColl = Arrays.asList(rn);
                nodesReservedForRemoval.remove(rn);
                // copy services from node to be removed
                cluster.addAndRebalance(addNodeColl, rn.getServices()).get();
                logger.info("added "+an.getHostname()+" with service="+rn.getServices()+" version="+an.getVersion());
                // if index node is added and server version is prespock, copy index
                if (rn.getServices().contains("index") && oldVersion <= 4 && cluster.IndexType.equals("secondary")) {
                    logger.info("creating index to "+an.getHostname());
                    addN1qlIndex(cluster, an.getHostname());
                }

                if (toAdd.size() == ii) {
                    Future<Boolean> result = cluster.removeAndRebalance(removeNodeColl);
                    // if index node is added and server version is prespock and only primary index is created,
                    // create a primary index after removing prespock node
                    if (rn.getServices().contains("index") && oldVersion <= 4 && cluster.IndexType.equals("primary")) {
                        logger.info("creating index to " + an.getHostname());
                        addN1qlIndex(cluster, an.getHostname());
                    }
                    return result;
                } else {
                    cluster.removeAndRebalance(removeNodeColl).get();

                    // if index node is added and server version is prespock and only primary index is created,
                    // create a primary index after removing prespock node
                    if (rn.getServices().contains("index") && oldVersion <= 4 && cluster.IndexType.equals("primary")) {
                        logger.info("creating index to " + an.getHostname());
                        addN1qlIndex(cluster, an.getHostname());
                    }
                }
                Thread.sleep(1000);
                break;
            }
        }

        return null;
    }

    private void addN1qlIndex(CBCluster cluster, String targetNode) {
        try {
            // create index
            logger.info("Creating n1ql index");
            final String[] n1qlParams = {"tag,type"};
            cluster.createN1QLIndex("default", n1qlParams, targetNode, false);

            logger.info("Sleep 5 sec");
            Thread.sleep(5000);
        } catch (Exception e) {
            logger.info("Exception:"+e.toString());
        }
    }

    @Override
    public Future<Boolean> undo(CBCluster  cluster) throws RestApiException {
        return cluster.swapAndRebalance(toRemove, toAdd, this.services);
    }
}

