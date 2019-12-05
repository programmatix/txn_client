/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.context;

/**
 *
 * @author mnunberg
 */
public enum HarnessError {

  /**
   * Problem in sending/receiving data from Driver
   */
  DRIVER,
  /**
   * Problem manipulating the cluster
   */
  CLUSTER,
  /**
   * Problem in scenario
   */
  SCENARIO,
  /**
   * Problem instantiating the workload
   */
  WORKLOAD,
  /**
   * Problem reading a user-provided configuration parameter
   */
  CONFIG,
  /**
   * Problem interacting with the log database
   */
  DB,
  /**
   * Generic Error
   */
  GENERIC
}
