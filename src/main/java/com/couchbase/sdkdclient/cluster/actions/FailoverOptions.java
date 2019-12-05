/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster.actions;

import com.couchbase.sdkdclient.options.*;
import com.couchbase.sdkdclient.scenario.FailoverScenario;
import com.couchbase.sdkdclient.scenario.NodeUser;

/**
 * Options for {@link FailoverAction} and {@link FailoverScenario}.
 */
public class FailoverOptions implements NodeUser {
  public enum NextAction {

    /**
     * Failover the nodes, and then rebalance the cluster.
     */
    FO_REBALANCE,

    /**
     * Failover the nodes and then eject them from the cluster.
     */
    FO_EJECT,

    /**
     * Failover the nodes, eject them from the cluster, and then rebalance
     * the cluster.
     */
    FO_EJECT_REBALANCE,

    /**
     * Don't do anything.
     */
    FO_NOACTION,

    /**
     * Failover the nodes, and then add them back to the cluster
     */
    FO_READD,

    /**
     * Failover the nodes, add them back to the cluster, and then rebalance.
     */
    FO_READD_REBALANCE
  };

  private EnumOption<NextAction> nextAction =
          OptBuilder.start("next-action", NextAction.class)
          .alias("action")
          .help("Action to perform after failover")
          .defl("FO_REBALANCE")
          .build();


  private IntOption count =
          OptBuilder.startInt("count")
          .help("How many nodes to fail over")
          .defl("2")
          .build();

  private BoolOption ept =
          OptBuilder.startBool("ept")
          .help("If the EPT node should be failed over")
          .defl("false")
          .build();


  private IntOption nextGrace =
          OptBuilder.startInt("next-delay")
          .help("Time to sleep between failover and next action")
          .defl("0")
          .alias("action_delay")
          .build();

  private StringOption services =
          OptBuilder.<StringOption>start(new StringOption("services"))
                  .help("Which service to failover")
                  .defl("kv")
                  .build();

  /**
   * Gets the action to be performed for failover
   * @return The action constant
   */
  public NextAction getNextAction() {
    return nextAction.getValue();
  }

  @Override
  public int getNumNodes() {
    return count.getValue();
  }

  @Override
  public boolean shouldUseEpt() {
    return ept.getValue();
  }

  /**
   * Indicates the time to sleep between an action with multiple stages. In this
   * case, it indicates how long to wait between each stage. For example,
   * in the case of {@link NextAction#FO_EJECT_REBALANCE} it indicates how
   * long to wait between ejecting the nodes and rebalancing the cluster.
   *
   * @return How long to wait between each stage, in seconds.
   */
  public int getGraceInterval() {
    return nextGrace.getValue();
  }

  public String getServices() { return services.getValue(); }
}