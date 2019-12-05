/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster.actions;

import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.NodeHost;
import com.couchbase.sdkdclient.cluster.ClusterException;
import com.couchbase.sdkdclient.cluster.NodelistBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 *
 * @author mnunberg
 */
public class RebalanceInAction implements RebalanceAction {
  private List<NodeHost> addedNodes = null;
  String services;

  public RebalanceInAction(String services) {
    this.services = services;
  }
  @Override
  public void setup(NodelistBuilder nlb, RebalanceConfig config) {
    // EPT is the master node, but we don't care since we're just adding
    // nodes
    addedNodes = new ArrayList<NodeHost>(nlb.reserveFree(config.getNumNodes(), true, services));
  }

  @Override
  public Future<Boolean> start(CBCluster cluster) throws RestApiException, ClusterException {
    return cluster.addAndRebalance(addedNodes, this.services);
  }

  @Override
  public Future<Boolean> undo(CBCluster cluster) throws RestApiException, ClusterException {
    return cluster.removeAndRebalance(addedNodes);
  }
}
