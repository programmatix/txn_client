/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster.actions;

import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.NodeHost;
import com.couchbase.sdkdclient.cluster.NodelistBuilder;

import java.util.Collection;
import java.util.concurrent.Future;

/**
 *
 * @author mnunberg
 */
public class RebalanceOutAction implements RebalanceAction {
  private Collection<NodeHost> removedNodes;

  String services;

  public RebalanceOutAction(String service) {
    this.services = service;
  }

  @Override
  public void setup(NodelistBuilder nlb, RebalanceConfig config) {
    removedNodes = nlb.reserveForRemoval(config.getNumNodes(), config.shouldUseEpt(), false, services);
  }

  @Override
  public Future<Boolean> start(CBCluster cluster) throws RestApiException {

    return cluster.removeAndRebalance(removedNodes);
  }

  @Override
  public Future<Boolean> undo(CBCluster cluster) throws RestApiException {
    return cluster.addAndRebalance(removedNodes, services);
  }
}