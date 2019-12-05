/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster.actions;

import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.ClusterException;
import com.couchbase.sdkdclient.cluster.NodelistBuilder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A class representing a Rebalance action.
 * A rebalance here implies nodes being added or removed from the cluster.
 */
public interface RebalanceAction {
  /**
   * Starts the rebalance action.
   * @throws RestApiException
   * @throws ClusterException
   * @return A future object. 'get()' can be called on to check the progress
   */
  public Future<Boolean> start(CBCluster cluster) throws RestApiException, ClusterException, ExecutionException, InterruptedException;

  /**
   * Reverts the action done by @link{start}. Any nodes added with start()
   * will be removed, and any nodes removed will be re-added.
   *
   * @return A future which can be waited on for the rebalance operation to
   * complete.
   *
   * @throws RestApiException
   * @throws ClusterException
   */
  public Future<Boolean> undo(CBCluster cluster) throws RestApiException, ClusterException;

  /**
   * Subclasses should verify in this step that the configuration is
   * correct.
   * This should be called after the cluster nodes have been partitioned,
   * but may be called before the cluster is actually rebalanced.
   *
   * @throws ClusterException
   */
  void setup(NodelistBuilder nlb, RebalanceConfig config) throws ClusterException;
}