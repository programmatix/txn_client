/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.cluster;

import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.util.CallOnceSentinel;
import org.slf4j.Logger;

import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.EnumSet;
import java.util.Collections;

public class NodelistBuilder {
    public enum GroupPolicy {
        SEPARATE,
        TOGETHER
    }

    private final Logger logger = LogUtil.getLogger(NodelistBuilder.class);
    private final Set<NodeHost> allNodes = new HashSet<NodeHost>();
    private boolean built = false;
    private final Nodelist innerList;
    private String upgradeVersion = "";
    private Collection<NodeHost> reservedActive = null;
    private final GroupPolicy groupPolicy;

    public Collection<NodeHost> reserveActive(int count, boolean includeMaster) {
        reservedActive = innerList.getActiveSubset(count, includeMaster);
        return reservedActive;
    }

    public Collection<NodeHost> reserveForRemoval(int count, boolean includeMaster, Boolean isRebalanceSwap, String services) {

        List<NodeHost> ret = new ArrayList<NodeHost>();

        if (upgradeVersion != "" && isRebalanceSwap) {
            for(NodeHost nn:innerList.getAll()) {
                if (nn.getState().containsAll(EnumSet.of(NodeHost.State.ACTIVE))) {
                    ret.add(nn);
                    logger.debug("candidate to remove for rebalance: {}", nn.asUri());
                }
            }
            try{
                ret = ret.subList(0, count);
            } catch(InsufficientNodesException ex) {
                throw  new InsufficientNodesException("Not enough nodes for upgrade");
            }
            if (ret.size() == count) {
                logger.debug("nodes reserved to remove {}", ret);
                return ret;
            } else {
                logger.debug("ret size {} count {}", ret, count);
                throw new InsufficientNodesException("Not enough nodes for upgrade");
            }
        }

        Collection<NodeHost> nodes = innerList.getActiveSubset(includeMaster);
        ArrayList<NodeHost> mutableNodes = new ArrayList<NodeHost>(nodes);


        if (services.indexOf("n1ql") != -1 ) {
          mutableNodes.retainAll(innerList.getN1QLNodes());
        }

        if (services.indexOf("index") != -1 ) {
          mutableNodes.retainAll(innerList.getSecondaryindexNodes());
        }

        if (services.indexOf("fts") != -1) {
            mutableNodes.retainAll(innerList.getFTSNodes());
        }

        if (services.indexOf("cbas") != -1) {
            mutableNodes.retainAll(innerList.getAnalyticsNodes());
        }

        if (groupPolicy == GroupPolicy.SEPARATE) {
            innerList.hintDifferentGroups(nodes);
        } else {
            innerList.hintSameGroup(nodes);
        }
        if (mutableNodes.size() < count) {
          throw new InsufficientNodesException("Not enough nodes for removal,mutable:"+mutableNodes.size()+",count:"+count);
        }

        nodes = mutableNodes.subList(0, count);

        // for upgrade case nodes.size() == innerList.getActive().size()
        /*if (nodes.size() == innerList.getActive().size()) {
          throw new InsufficientNodesException("No active nodes will remain");
        }*/
        logger.debug("ret {} {}", nodes, count);
        return nodes;
    }

    private final CallOnceSentinel co_Free = new CallOnceSentinel();

    public Collection<NodeHost> reserveFree(int count, Boolean isRebalanceIn, String services) {
        co_Free.check();
        List<NodeHost> ret = new ArrayList<NodeHost>();

        //upgrade tests don't factor in services
        if (upgradeVersion != "" && isRebalanceIn) {
            for(NodeHost nn:innerList.getAll()) {
                logger.debug("{} {}", nn.getHostname(), nn.getState().toString());
                if (nn.getState().containsAll(EnumSet.of(NodeHost.State.UPGRADE, NodeHost.State.FREE))) {
                    ret.add(nn);
                    logger.debug("candidate to add for rebalance: {}", nn.asUri());
                }
            }
            try {
                ret = ret.subList(0, count);
            } catch(IndexOutOfBoundsException ex) {
                throw  new InsufficientNodesException("Not enough free nodes for upgrade");
            }
            if (ret.size() == count) {
                logger.debug("nodes reserved to add {}", ret);
                return ret;
            } else {
                logger.debug("ret size {} count {}", ret, count);
                throw new InsufficientNodesException("Not enough nodes for upgrade");
            }
        }
        if (services.indexOf("n1ql") != -1 ) {
           ret = innerList.getN1QLNodes();
        }
        if (services.indexOf("fts") != -1) {
            ret = innerList.getFTSNodes();
        }
        if (services.indexOf("index") != -1) {
          ret = innerList.getSecondaryindexNodes();
        }
        if (services.indexOf("cbas") != -1) {
            ret = innerList.getAnalyticsNodes();
        }
        if (services == ""){
            ret = innerList.getAll();
        }
        logger.info("ret {}", ret);
        if (reservedActive !=  null) {
          ret.removeAll(reservedActive);
        }

        if (ret.size()-1 >= count) {
            ret.remove(innerList.getMaster());
        } else {
            logger.warn("Master node will be free. This may cause cluster issues");
        }
        try {
          ret = ret.subList(0, count);
        } catch (IndexOutOfBoundsException ex) {
            throw new InsufficientNodesException("Not enough free nodes");
        }

        if (ret.size() >= innerList.size()) {
            throw new InsufficientNodesException("At least one node must be active");
        }

        innerList.remove(ret);

        if (groupPolicy == GroupPolicy.SEPARATE) {
            innerList.hintDifferentGroups(ret);
        } else {
            innerList.hintSameGroup(ret);
        }
        logger.info("ret {}", ret);

        return ret;
    }


    public NodelistBuilder(Collection<NodeHost> initialNodes, int numGroups, GroupPolicy groupPolicy, String upgradeVersion) {
        allNodes.addAll(initialNodes);
        this.groupPolicy = groupPolicy;
        this.upgradeVersion = upgradeVersion;

        if (allNodes.isEmpty()) {
            throw new IllegalArgumentException("Node list cannot be empty");
        }
        innerList = new Nodelist(new ArrayList<NodeHost>(initialNodes), allNodes.size(), numGroups, upgradeVersion);
    }

    public NodelistBuilder(Collection<NodeHost> initialNodes) {
        this(initialNodes, 1, GroupPolicy.SEPARATE, "");
    }

    public int getTotalSize() {
        return allNodes.size();
    }

    Nodelist build() {
        if (built) {
            throw new IllegalStateException("Nodelist already built");
        }

        built = true;
        innerList.maybeSwitchMaster();
        innerList.partitionGroups();
        return innerList;
    }
}
