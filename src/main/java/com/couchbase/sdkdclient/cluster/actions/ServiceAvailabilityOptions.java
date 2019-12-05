/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster.actions;

import com.couchbase.sdkdclient.cluster.actions.ServiceAvailabilityAction.ActionType;
import com.couchbase.sdkdclient.cluster.actions.ServiceAvailabilityAction.ProcessType;
import com.couchbase.sdkdclient.options.BoolOption;
import com.couchbase.sdkdclient.options.EnumOption;
import com.couchbase.sdkdclient.options.IntOption;
import com.couchbase.sdkdclient.options.OptBuilder;
import com.couchbase.sdkdclient.options.StringOption;
import com.couchbase.sdkdclient.scenario.NodeUser;



public class ServiceAvailabilityOptions implements NodeUser {
  private EnumOption<ProcessType> process =
          OptBuilder.start("name", ProcessType.class)
          .help("Name of service to affect")
          .required()
          .build();

  private EnumOption<ActionType> action =
          OptBuilder.start("action", ActionType.class)
          .help("Action to perform")
          .required()
          .build();

  private IntOption staggerDelay =
          OptBuilder.startInt("stagger")
          .help("Delay to sleep between which each service is modified")
          .argName("SECONDS")
          .defl("1")
          .build();

  private IntOption recoverDelay =
          OptBuilder.startInt("recover-delay")
          .help("Delay for a few seconds for server to recover")
          .argName("RECOVER")
          .defl("5")
          .build();

  private IntOption nodeCount =
          OptBuilder.startInt("count")
          .help("How many nodes to affect")
          .required()
          .build();

  private BoolOption useEpt =
          OptBuilder.startBool("ept")
          .help("Whether the EPT should be affected")
          .defl("false")
          .build();
  
  private StringOption sdkdclientIP =
          OptBuilder.startString("sdkdclientip")
          .help("SKDClient host ip address")
          .defl("localhost")
          .build();

  private BoolOption enabled =
          OptBuilder.startBool("enabled")
          .help("Enabled to run by default")
          .defl("true")
          .build();

  private IntOption queryPort =
          OptBuilder.startInt("query-port")
          .help("Query port the client connects to")
          .defl("8093")
          .build();

  /**
   * Gets the process type which should be affected in the service interruption
   * @return The process to be affected
   */
  public ProcessType getProcess() {
    return process.getValue();
  }

  /**
   * Gets the action which determines <i>how</i> the service returned by
   * {@link #getProcess() } will be affected.
   * @return The action to perform
   */
  public ActionType getAction() {
    return action.getValue();
  }

  /**
   * Indicates the delay for which to <i>stagger</i> actions between the nodes.
   *
   * When multiple nodes are selected for service interruption, this allows
   * the interruption to induce a delay between which each node is interrupted,
   * so e.g:
   *
   * <li>
   * <ul>Three nodes are selected for killing <code>memcached</code></ul>
   * <ul>Stagger delay is set to 100 (i.e. 100 seconds)</ul>
   * <ul>Node 1's memcached service is killed</ul>
   * <ul>Action sleeps for 100 seconds</ul>
   * <ul>Node 2's memcached service is killed</ul>
   * <ul>Action sleeps for 100 seconds</ul>
   * <ul>Node 3's memcached service is killed</ul>
   * </li>
   *
   * @return The stagger delay to use.
   */
  public int getStaggerDelay() {
    return staggerDelay.getValue();
  }

  public int getRecoverDelay() {
    return recoverDelay.getValue();
  }

  @Override
  public int getNumNodes() {
    return nodeCount.getValue();
  }

  @Override
  public boolean shouldUseEpt() {
    return useEpt.getValue();
  }
 
  public String getHostIp(){
	return sdkdclientIP.getValue();
  }

  public int getQueryPort() { return  queryPort.getValue(); }
}
