/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.scenario;

import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.ClusterBuilder;
import com.couchbase.sdkdclient.cluster.actions.ServiceAvailabilityAction;
import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.options.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;


/**
 * This scenario affects the server processes directly. Unlike the
 * {@link RebalanceScenario} and {@link FailoverScenario}, this scenario does
 * not use the REST API to induce topology changes as its primary goal, rather
 * it will directly manipulate the server processes.
 *
 * This scenario can be used to simulate absolute failures in real world
 * deployments when processes crash or hang.
 *
 * See {@link ServiceAvailabilityAction} for the implementation of the
 * service failures.
 *
 * <b>This scenario typically requires SSH access to each node of the cluster</b>
 */
public class ServiceFailureScenario extends PhasedScenario {
  protected final ServiceAvailabilityAction svcAction =
          new ServiceAvailabilityAction();

  // Now let's make our own options
  class Options {
    IntOption foDelay = OptBuilder.startInt("failover-delay")
            .help("How long to wait before failover, after action takes place")
            .defl("-1")
            .build();

    IntOption restoreDelay = OptBuilder.startInt("restore-delay")
            .help("How long to wait until service is restored. A value of " +
                  "-1 means the service is never restored")
            .defl("30")
            .build();

    IntOption readdDelay = OptBuilder.startInt("readd-delay")
            .help("How long to wait before re-adding the node. " +
                  "This option only makes sense if failover was enabled")
            .defl("-1")
            .build();
  }

  private Options options = new Options();

  @Override
  public OptionTree getOptionTree() {
    OptionTree ret = new OptionTree();
    OptionTree svctree = svcAction.getOptionTree();
    for (RawOption opt : OptionTree.extract(options, Options.class)) {
      svctree.addOption(opt);
    }
   // System.out.println("SVCTree options:");
    //printoptions(svctree.getAllOptionsInfo());
    ret.addChild(svctree);
    //System.out.println("Super options:");
    //printoptions(super.getOptionTree().getAllOptionsInfo());
    //System.out.println("DONE DONE DONE");

    ret.addChild(super.getOptionTree());
    return ret;
  }

  @Override
  public void doConfigure(ClusterBuilder clb) {
    svcAction.setup(clb);
    svcAction.prepare();
    if (!clb.getClusterOptions().shouldUseSSH()) {
      throw new HarnessException(HarnessError.CONFIG, "Cannot use this scenario without SSH support");
    }

    int requiredActive = svcAction.getRequiredActiveCount();
    int total = clb.getNodelistBuilder().getTotalSize();

    if (options.foDelay.getValue() >= 0) {
      if (requiredActive == total) {
        throw new IllegalArgumentException(
                "Cannot failover when all nodes are down");
      }
    }
  }

  /**
   * Checks whether an option is enabled, and if it has a delay. If it has
   * a delay, we sleep here. Returns true or false if we should proceed with
   * the action.
   * @param o
   * @return
   */
  private boolean sleepAndDo(IntOption o) {
    if (o.getValue() < 0) {
      return false;
    }

    if (o.getValue() == 0) {
      // enabled, but no delay.
      return true;
    }

    logger.info("Sleeping {} seconds", o.getValue());
    try {
      Thread.sleep(o.getValue() * 1000);
    } catch (InterruptedException exc) {
      throw HarnessException.create(HarnessError.GENERIC, exc);
    }
    return true;
  }

  @Override
  protected void executeChange(CBCluster cluster) throws HarnessException {
    try {
      svcAction.turnOff();
      cluster.updateBadNodes(svcAction.getNodes(), false);
      boolean isFailedOver = false;

      // Maybe failover?
      if (sleepAndDo(options.foDelay)) {
        isFailedOver = true;
        logger.info("Failing over nodes after service failure");
        cluster.failoverNodes(svcAction.getNodes());
      }

      // Maybe restore?
      if (sleepAndDo(options.restoreDelay)) {
        logger.info("Turning on services for nodes");
        svcAction.turnOn();

        // Re-Add the nodes?
        if (isFailedOver && sleepAndDo(options.readdDelay)) {
          logger.info("Readding failed over nodes to the cluster");
          cluster.reAddNodes(svcAction.getNodes());
        }
      }

    } catch (IOException exc) {
      throw HarnessException.create(HarnessError.CLUSTER, exc);
    } catch (RestApiException exc) {
      throw new HarnessException(HarnessError.CLUSTER, exc);
    }
  }

  private boolean appendSleepDescription(IntOption opt, String action, StringBuilder sb) {
    int val = opt.getValue();
    if (val < 0) {
      return false;
    }
    if (val > 0) {
      sb.append(" sleep for ").append(val).append(" seconds and ");
    }
    sb.append(action).append(" the nodes.");
    return true;
  }

  @Override
  protected String getChangeDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append(svcAction.getFailureDescription());
    appendSleepDescription(options.foDelay, "failover", sb);
    if (appendSleepDescription(options.restoreDelay, "restore", sb)) {
      sb.append(" Restoring is peformed by: ").append(svcAction.getActivationDescription());
    }
    appendSleepDescription(options.readdDelay, "readd", sb);
    return sb.toString();
  }


  public static void  printoptions(Collection<OptionInfo> optionsInfo ){
    Iterator<OptionInfo> itr = optionsInfo.iterator();
    while(itr.hasNext()){
      OptionInfo op= itr.next();
      System.out.println("Option Info Name: "+op.getOption().getName());
      System.out.println("Option Info Value: "+op.getOption().getCurrentRawValue());

    }
  }
}
