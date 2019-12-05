/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.scenario;

import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.util.NetworkIO;
import com.couchbase.sdkdclient.workload.WorkloadGroup;

import java.io.IOException;

/**
 * Abstract class for a {@link Scenario} which manipulates the cluster.
 * Phased scenarios are divided into three phases:
 * <table>
 *
 * <tr>
 * <td>Ramp</td>
 * <td>Frame of time before the cluster is manipulated. This is typically used
 * to establish a baseline metric.</td>
 * </tr>
 *
 * <tr>
 * <td>Change</td>
 * <td>Frame of time during which the cluster is manipulated. This is the time
 * at which the SDKD and the underlying SDK are most likely to experience errors
 * due to cluster connections being dropped or topology changes taking place.
 * Depending on the topology change taking place, the errors visible may either
 * be many or few</td>
 * </tr>
 *
 * <tr>
 * <td>Rebound</td>
 * <td>Frame of time <i>after</i> the cluster manipulation has taken place. At
 * this time, the client is expected to stabilize and eventually return to
 * a state similar to that of the <i>ramp</i> phase.
 * </td>
 * </tr>
 * </table>
 *
 *
 * Subclasses are expected to implement the {@link #executeChange(com.couchbase.sdkdclient.cluster.CBCluster)} method
 * which will contain the actual code to change the cluster.
 */
public abstract class PhasedScenario extends Scenario {
  private final PhaseOptions options = new PhaseOptions();

  @Override
  public OptionTree getOptionTree() {
    return options.getOptionTree();
  }

  private void doSleep(int seconds) {
    if (!options.shouldSleep()) {
      logger.info("Not sleeping because --change-only specified");
      return;
    }
    try {
      Thread.sleep(seconds * 1000);
    } catch (InterruptedException ex) {
      throw HarnessException.create(HarnessError.SCENARIO, ex);
    }

  }

  @Override
  public final void run(CBCluster cluster, WorkloadGroup wlGroup) throws HarnessException {
    //logger.info("Starting RAMP phase");
    if (listener != null) {
      listener.onRamp(this);
    }

    /* pre-execute some steps before starting workload */
    preExecPhase(cluster);

    try {
      wlGroup.start();
    } catch (Exception ex) {
      throw HarnessException.create(HarnessError.WORKLOAD, ex);
    }
    logger.info("Exiting here since situations testing will be handled later");
    System.exit(-1);
    logger.info("RAMP phase started. Waiting for {} seconds", options.getRampSleep());
    doSleep(options.getRampSleep());

    logger.info("Starting CHANGE phase");
    if (listener != null) {
      listener.onChange(this);
    }

    if (!options.shouldChange()) {
      logger.warn("no-change specified. Not changing");
    } else {
      executeChange(cluster);
    }

    logger.info("CHANGE phase done");

    try {
      wlGroup.beginRebound();
    } catch (IOException ex) {
      throw HarnessException.create(HarnessError.WORKLOAD, ex);
    }

    if (listener != null) {
      listener.onRebound(this);
    }

    logger.info("Starting REBOUND for {} seconds", options.getReboundSleep());
    doSleep(options.getReboundSleep());
    logger.info("REBOUND done. Will collect results");

    if (listener != null) {
      listener.onDone(this);
    }


    logger.info("Results collected. DONE");


  }

  /**
   * Performs the cluster change. This method should only return once
   * the change has been completed
   * @throws HarnessException
   */
  @NetworkIO
  abstract protected void executeChange(CBCluster cluster) throws HarnessException;
  abstract protected String getChangeDescription();

  final static private String descriptionFormat =
     "Ramp for %d seconds. Cluster modification: %s. Rebound for %d seconds.%s";

  @Override
  public String getConfiguredDescription() {
    String specialOptions = "";
    if (!options.shouldChange()) {
      specialOptions += " [NO CHANGE] ";
    }
    if (!options.shouldSleep()) {
      specialOptions += " [NO SLEEP] ";
    }

    return String.format(descriptionFormat,
                         options.getRampSleep(),
                         getChangeDescription(),
                         options.getReboundSleep(),
                         specialOptions);
  }

  protected void preExecPhase(CBCluster cluster) throws HarnessException {}
}
