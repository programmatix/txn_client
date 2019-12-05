/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.scenario;

import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.ClusterBuilder;
import com.couchbase.sdkdclient.cluster.actions.FailoverAction;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.options.OptionTree;

/**
 * This scenario will fail over one or more nodes and possibly readd them
 * back in and/or rebalance the cluster.
 *
 * @see FailoverAction for the implementation of the failover
 */
public class FailoverScenario extends PhasedScenario {
  protected final FailoverAction action = new FailoverAction();

  @Override
  public void doConfigure(ClusterBuilder clb) {
    action.setup(clb);
  }

  @Override
  protected void executeChange(CBCluster cluster) throws HarnessException {
    action.change(cluster);
  }

  @Override
  public OptionTree getOptionTree() {
    OptionTree tree = new OptionTree();
    tree.addChild(action.getOptionTree());
    tree.addChild(super.getOptionTree());
    return tree;
  }

  @Override
  protected String getChangeDescription() {
    return action.getDescription();
  }
}
