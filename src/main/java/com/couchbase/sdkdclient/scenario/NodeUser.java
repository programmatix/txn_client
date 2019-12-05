/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.scenario;

/**
 * Common interface for all actions which manipulate nodes.
 */
public interface NodeUser {

  /**
   * Gets the number of nodes to be manipulated by this action
   * @return The number of nodes requested
   */
  public int getNumNodes();


  /**
   * Whether the <i>EPT</i> node should be included among the nodes.
   * Note that even if this is false, the <i>EPT</i> may still be used
   * if the cluster does not have enough non-EPT nodes to satisfy the number
   * required by {@link #getNumNodes() }
   * @return true if the EPT must be used, false otherwise.
   */
  public boolean shouldUseEpt();
}
