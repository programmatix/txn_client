/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster.actions;

import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.NodeHost;
import com.couchbase.sdkdclient.cluster.NodelistBuilder;
import com.couchbase.sdkdclient.context.HarnessException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author mnunberg
 */
public class RebalanceSwapAction implements RebalanceAction {
  List<NodeHost> toAdd = new ArrayList<NodeHost>();
  Collection<NodeHost> toRemove;
  String services;

  public RebalanceSwapAction(String service) {
      this.services = service;
  }

  @Override
  public void setup(NodelistBuilder nlb, RebalanceConfig config) {
    toAdd.addAll(nlb.reserveFree(config.getNumNodes(), true, services));
    toRemove = nlb.reserveForRemoval(config.getNumNodes(), config.shouldUseEpt(), true, services);

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
      cluster.addNodes(addNodeColl, this.services);
      ii++;
      for(NodeHost rn:nodesReservedForRemoval) {
        List<NodeHost> removeNodeColl = Arrays.asList(rn);
        nodesReservedForRemoval.remove(rn);
        if (toAdd.size() == ii) {
          return cluster.removeAndRebalance(removeNodeColl);
        } else {
          cluster.removeAndRebalance(removeNodeColl).get();
        }
        break;
      }
    }

    return null;
  }

  @Override
  public Future<Boolean> undo(CBCluster  cluster) throws RestApiException {
    return cluster.swapAndRebalance(toRemove, toAdd, this.services);
  }
}
