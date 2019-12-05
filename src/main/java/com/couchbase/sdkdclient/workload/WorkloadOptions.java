/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.workload;

/**
 * Common options for all workloads.
 */
public interface WorkloadOptions {
  /**
   * @return the number of threads this workload should spawn.
   */
  public int getNumThreads();

  /**
   * Sets the resolution at which the <i>SDKD</i> will log timing information.
   * If set to 0 then timing statistics are disabled.
   *
   * Effectively this indicates the granularity at which timing information
   * will be returned.
   *
   * @return The time resolution, in seconds.
   */
  public int getTimeResolution();

  /**
   * Gets the size of the handle threadpool, or how many concurrent
   * handles should be messages/instantiated. This may increase performance
   * but may also cause instability if a specific SDKD is not designed to
   * handle concurrent instantiation.
   * @return The size of the threadpool.
   */
  public int getThreadpoolSize();
}