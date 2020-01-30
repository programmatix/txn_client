package com.couchbase.Couchbase.Nodes;/*
 * Copyright (c) 2013 Couchbase, Inc.
 */


import com.couchbase.Couchbase.Couchbase.CouchbaseAdmin;
import com.couchbase.Logging.LogUtil;
import org.slf4j.Logger;
import java.util.*;

public final class Nodelist {
    final private Logger logger = LogUtil.getLogger(Nodelist.class);
    final private List<NodeHost> allNodes = new ArrayList<NodeHost>();
    final private List<NodeHost> activeNodes = new LinkedList<NodeHost>();
    final private List<NodeHost> n1qlNodes = new ArrayList<NodeHost>();
    final private List<NodeHost> secondaryindexNodes = new ArrayList<NodeHost>();
    final private List<NodeHost> ftsNodes = new ArrayList<NodeHost>();
    final private List<NodeHost> analyticsNodes = new ArrayList<NodeHost>();
    final private int numGroups;
    final private String upgradeVersion;

    final private Map<NodeHost, Integer> groupMap = new HashMap<NodeHost, Integer>();
    final private Set<NodeHost> hintedSeparateNodes = new HashSet<NodeHost>();
    final private Set<NodeHost> hintedSameNodes = new HashSet<NodeHost>();
    private volatile boolean partitioned = false;
    private NodeHost master;

    /**
     * Gets the master node. This is the node that will (under
     * normal circumstances) not be removed.
     * @return The current master node.
     */
    public NodeHost getMaster() {
        return master;
    }

    /**
     * Get all the nodes we were configured with
     * @return All nodes
     */
    public List<NodeHost> getAll() {
        return new ArrayList<NodeHost>(allNodes);
    }

    /**
     * Get all the nodes *except* the master.
     *
     * These are the auxilliary nodes. Note that the cluster doesn't have an
     * idea of master or auxiliary (as far as public topology is concerned).
     * We only use master here to refer to the node we use for REST API
     * interaction. All non-master nodes
     * @return All nodes except the master.
     */
    public Collection<NodeHost> getAllAux() {
        List<NodeHost> ret = new ArrayList<NodeHost>(allNodes);
        ret.remove(master);
        return ret;
    }

    /**
     * Get all the active nodes in the cluster
     * @return Collection of active nodes in the cluster
     */
    public Collection<NodeHost> getActive() {
        return Collections.unmodifiableCollection(activeNodes);
    }

    /**
     * Get all the non-master active nodes in the cluster
     * @return Collection of nodes
     */
    public Collection<NodeHost> getActiveAux() {
        List<NodeHost> ret = new ArrayList<NodeHost>(activeNodes);
        ret.remove(master);
        logger.debug("master {}", master);
        return ret;
    }

    /**
     * Get all non-master active nodes with passed in service enabled
     * @param service enabled on the node
     * @return Collection of nodes
     */
    public Collection<NodeHost> getActiveAux(String service) {
        List<NodeHost> ret = new ArrayList<NodeHost>(activeNodes);
        ret.remove(master);
        for (NodeHost nodeHost:ret) {
            if (nodeHost.getServices().indexOf(service) == -1) {
                ret.remove(nodeHost);
            }
        }
        return ret;
    }

    /**
     * Get a list of nodes which are not part of a cluster
     * @return A collection of nodes
     */
    public Collection<NodeHost> getFree() {
        List<NodeHost> ll = new ArrayList<NodeHost>(allNodes);
        ll.removeAll(activeNodes);
        return ll;
    }

    /** Returns a list of nodes reserved for upgrade
     *  @return Collection of nodes
     */
    public Collection<NodeHost> getUpgrade() {
        List<NodeHost> ll = null;
        for (NodeHost nn:allNodes) {
            if (nn.getState().containsAll(EnumSet.of(NodeHost.State.UPGRADE, NodeHost.State.ACTIVE))) {
                ll.add(nn);
            }
        }
        return ll;
    }

    void activate(Collection<NodeHost> nodes) {
        for (NodeHost nn : nodes) {
            activeNodes.add(nn);
            if (nn.getState().contains(NodeHost.State.UPGRADE)) {
                nn.setState(EnumSet.of(NodeHost.State.UPGRADE, NodeHost.State.ACTIVE));
            } else {
                nn.setState(EnumSet.of(NodeHost.State.ACTIVE));
            }
        }
    }

    void remove(Collection<NodeHost> nodes) {
        activeNodes.removeAll(nodes);
        for (NodeHost nn : nodes) {
            if (nn.getState().contains(NodeHost.State.UPGRADE)) {
                nn.setState(EnumSet.of(NodeHost.State.UPGRADE, NodeHost.State.FREE));
            } else {
                nn.setState(EnumSet.of(NodeHost.State.FREE));
            }
        }
    }

    void fail(Collection<NodeHost> nodes) {
        activeNodes.removeAll(nodes);
        for (NodeHost nn : nodes) {
            if (nn.getState().contains(NodeHost.State.UPGRADE)) {
                nn.setState(EnumSet.of(NodeHost.State.UPGRADE, NodeHost.State.FAILED_OVER));
            } else {
                nn.setState(EnumSet.of(NodeHost.State.FAILED_OVER));
            }
        }
    }

    void maybeSwitchMaster(List<NodeHost> nextActive) {
        if (nextActive.isEmpty()) {
            throw new IllegalStateException("No active nodes remain");
        }
        if (!nextActive.contains(master)) {
            logger.debug("Master node being removed from cluster. Switching");
            master = nextActive.get(0);
        }
    }

    void maybeSwitchMaster() {
        maybeSwitchMaster(activeNodes);
    }


    void switchMasterFromFailedNodes(Collection<NodeHost> nodes) {
        maybeSwitchMaster(getNextActive(nodes));
    }

    /**
     * Get a list of the remaining active nodes, excluding the ones specified
     * in the 'omitlist'
     * @param omitList A list of nodes to not be used in consideration.
     * @return
     */
    List<NodeHost> getNextActive(Collection<NodeHost> omitList) {
        ArrayList<NodeHost> ll = new ArrayList<NodeHost>(getActive());
        ll.removeAll(omitList);
        return ll;
    }

    /**
     * Notify the cluster that the specific nodes listed should no longer
     * be used for REST manipulation. This operation does not modify
     * anything except selection of the master nodes
     * @param badnodes Nodes which should be considered bad
     * @param ensureMaster Whether a master must remain. If true, then
     * an IllegalStateException will be raised if no node remains 'good'
     */
    public void updateBadNodes(Collection<NodeHost> badnodes, boolean ensureMaster) {
        try {
            switchMasterFromFailedNodes(badnodes);
        } catch (IllegalStateException ex) {
            if (!ensureMaster) {
                logger.info("No master remains", ex);
            } else {
                throw ex;
            }
        }
    }

    /**
     * Convenience method to get the administrative connection. Equivalent to
     * {@code  getMaster().getAdmin() }
     * @return The administrative client.
     */
    public CouchbaseAdmin getAdmin() {
        return master.getAdmin();
    }

    /**
     * Gets a subset of the active nodes
     * @param count The number of nodes to return
     * @param includeMaster Whether to include the master node in the subset. If
     * there are not enough non-master nodes, the master will be added and a
     * warning will be logged.
     * @return The list of nodes, whose size will be @c{count}
     * @throws IllegalStateException If there are not enough nodes.
     */
    public Collection<NodeHost> getActiveSubset(int count, boolean includeMaster) {
        List<NodeHost> nodes = new ArrayList<NodeHost>(count);
        if (includeMaster) {
            nodes.add(master);
            if (nodes.size() < count) {
                nodes.addAll(getActiveAux());
            }
        } else {
            nodes.addAll(getActiveAux());
            if (nodes.size() < count) {
                logger.warn("EPT not requested, but adding anyway");
                nodes.add(master);
            }
        }

        if (nodes.size() < count) {
            throw new IllegalStateException(
                    String.format("Not enough nodes. Want=%d, Have=%d",
                            count, nodes.size()));
        }

        return Collections.unmodifiableCollection(nodes.subList(0, count));
    }


    public Collection<NodeHost> getActiveSubset(boolean includeMaster) {
        List<NodeHost> nodes = new ArrayList<NodeHost>();
        if (includeMaster) {
            nodes.add(master);
        }
        nodes.addAll(getActiveAux());
        return Collections.unmodifiableCollection(nodes);
    }

    private void ensureNotPartitioned() {
        if (partitioned) {
            throw new IllegalStateException("Groups already partitioned");
        }
    }

    /**
     * Assign different groups for a set of nodes
     *
     * This method will attempt to configure the groups so that the nodes listed
     * in {@code nodes} all end up in separate groups. Note that if the number
     * of configured groups is not large enough to contain all the nodes, a best
     * effort will still be made.
     *
     * @param nodes The nodes to operate on.
     */
    void hintDifferentGroups(Collection<NodeHost> nodes) {
        ensureNotPartitioned();
        hintedSeparateNodes.addAll(nodes);
    }

    /**
     * Ensure that all nodes provided are contained within the same group
     *
     * This is the inverse of {@link #hintDifferentGroups(java.util.Collection)}
     * and ensures that all the nodes within {@code nodes} are in the same group.
     * @param nodes The nodes to group together
     */
    void hintSameGroup(Collection<NodeHost> nodes) {
        ensureNotPartitioned();
        hintedSameNodes.addAll(nodes);
    }

    /**
     * Assigns the relevant groups to the nodes.
     */
    void partitionGroups() {
        if (partitioned) {
            return;
        }

        partitioned = true;
        int curGroup = 0;

        // Figure out how many groups we want..
        for (NodeHost nn : hintedSeparateNodes) {
            groupMap.put(nn, curGroup);
            curGroup = (curGroup + 1) % numGroups;
        }

        for (NodeHost nn : hintedSameNodes) {
            if (groupMap.containsKey(nn)) {
                // GRR.. we should warn here?
                continue;
            }

            groupMap.put(nn, 0);
        }

        // Now go through all the other nodes which aren't part of any special
        // list.
        curGroup = 0;
        for (NodeHost nn : allNodes) {
            if (groupMap.containsKey(nn)) {
                continue;
            }

            groupMap.put(nn, curGroup);
            curGroup =  (curGroup + 1) % numGroups;
        }

        partitioned = true;
    }

    /**
     * Get the number of 'groups' this list is configured with.
     * @return The number of groups.
     */
    public int getNumGroups() {
        return numGroups;
    }

    /**
     * Get the group for the specific node.
     *
     * If the list is not yet partitioned into groups, it is done so on demand.
     * @param node The node to look up.
     *
     * @return An integer within the range of {@code 0 <= X < numGroups}. The
     * intended use is to append this integer to some kind of external mechanism.
     */
    public int getGroupForNode(NodeHost node) {
        partitionGroups();
        return groupMap.get(node);
    }

    public String getUpgradeVersion() { return upgradeVersion; }

    Nodelist(List<NodeHost> collNodes, int numActive, int numGroups, String upgradeVersion) {
        allNodes.addAll(collNodes);
        //activeNodes.addAll(collNodes.subList(0, numActive));

        int i = 0;

        for (NodeHost nn : collNodes) {
            logger.debug("Node order {}", nn.asUri());
            logger.debug("Upgrade version {}", upgradeVersion);
            if (upgradeVersion != "" && (
                    nn.getVersion().equalsIgnoreCase(upgradeVersion) || nn.getVersion().equalsIgnoreCase("UPGRADE"))) {
                // if Upgrade version is not specified, use upgradeVersion option value
                logger.debug("version {}", nn.getVersion());
                nn.setState(EnumSet.of(NodeHost.State.UPGRADE, NodeHost.State.FREE));
            } else {
                if (i < numActive) {
                    activeNodes.add(nn);
                    nn.setState(EnumSet.of(NodeHost.State.ACTIVE));
                    i++;
                }
                else {
                    nn.setState(EnumSet.of(NodeHost.State.FREE));
                }
            }
            if (nn.getServices().indexOf("n1ql") !=  -1) {
              n1qlNodes.add(nn);
            }
            if (nn.getServices().indexOf("index") != -1){
              secondaryindexNodes.add(nn);
            }
            if (nn.getServices().indexOf("fts") !=  -1) {
                ftsNodes.add(nn);
            }
            if (nn.getServices().indexOf("cbas") != -1) {
                analyticsNodes.add(nn);
            }
        }
        if (upgradeVersion != ""){
            if (activeNodes.size() == 0) {
                throw new IllegalArgumentException("No nodes for upgrade");
            }
        }


        logger.debug("Number of active nodes {}", activeNodes.size());
        this.master = activeNodes.get(0);
        this.numGroups = Math.min(numGroups, allNodes.size());
        this.upgradeVersion = upgradeVersion;
    }

    public int size() {
        return allNodes.size();
    }

    public List<NodeHost> getN1QLNodes() { return n1qlNodes; }

    public List<NodeHost> getSecondaryindexNodes() { return secondaryindexNodes; }

    public List<NodeHost> getFTSNodes() { return ftsNodes;}

    public List<NodeHost> getAnalyticsNodes() { return analyticsNodes; }
}
