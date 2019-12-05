/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.scenario;

import com.couchbase.sdkdclient.context.RunContext;

/**
 * This defines the <i>Phase Listener</i> interface for scenarios, and allows
 * other classes to detect when a phase has started or ended.
 *
 * Currently this is employed by {@link RunContext} to log phase ranges to
 * the database.
 */
public interface ScenarioListener {

  /**
   * Called when the <i>RAMP</i> phase begins. At this point, the workload
   * has just started and no cluster manipulation has yet taken place.
   * @param sc The scenario
   */
  public void onRamp(Scenario sc);

  /**
   * Called when the cluster change is about to take place. This implicitly
   * marks the end of the <i>RAMP</i> phase and explicitly marks the
   * beginning of the <i>CHANGE</i> phase.
   * @param sc The scenario
   */
  public void onChange(Scenario sc);

  /**
   * Called when the cluster change has been completed. When this method is
   * called, it is assumed that the cluster is in a steady, stable state and
   * that no subsequent topology or network changes will take place. This
   * implicitly marks the end of the <i>CHANGE</i> phase and explicitly
   * marks the beginning of the <i>REBOUND</i> phase.
   * @param sc
   */
  public void onRebound(Scenario sc);

  /**
   * Called when the scenario (and thus the test) is complete. This implicitly
   * marks the end of the <i>REBOUND</i> phase.
   * @param sc
   */
  public void onDone(Scenario sc);
}
