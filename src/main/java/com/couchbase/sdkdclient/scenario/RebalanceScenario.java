/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.scenario;

import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.ClusterBuilder;
import com.couchbase.sdkdclient.cluster.actions.RebalanceAction;
import com.couchbase.sdkdclient.cluster.actions.RebalanceActionFactory;
import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.util.NetworkIO;

import java.util.concurrent.ExecutionException;

/**
 * This scenario adds or removes nodes from the cluster and then rebalances
 * it.
 *
 * It relies on the {@link RebalanceActionFactory} class to parse the
 * options and decide on the action, and on the {@link RebalanceAction}
 * class to perform the actual changes.
 */
public class RebalanceScenario extends PhasedScenario {
  protected RebalanceActionFactory rbOptions = new RebalanceActionFactory();
  protected RebalanceAction rbAction;

  @Override
  @NetworkIO
  protected void doConfigure(ClusterBuilder clb) throws HarnessException {
    rbAction = rbOptions.createAction();
    rbAction.setup(clb.getNodelistBuilder(), rbOptions);
  }

  @Override
  protected void executeChange(CBCluster cluster) throws HarnessException {
    try {
      rbAction.start(cluster).get();
    } catch (RestApiException ex) {
      throw new HarnessException(HarnessError.CLUSTER, ex);
    } catch (ExecutionException ex) {
      throw HarnessException.create(HarnessError.CLUSTER, ex);
    } catch (InterruptedException ex) {
      throw HarnessException.create(HarnessError.CLUSTER, ex);
    }
  }

  @Override
  public OptionTree getOptionTree() {
    OptionTree tree = new OptionTree();
    tree.addChild(rbOptions.getOptionTree());
    tree.addChild(super.getOptionTree());
    return tree;
  }

  @Override
  protected String getChangeDescription() {
    return rbOptions.getDescription();
  }
}