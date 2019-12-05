/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.handle;

import com.couchbase.sdkdclient.cluster.CBCluster;

/**
 * Simple class for a 'HandleFactory' class
 *
 * @author mnunberg
 */
public class HandleFactory {

  private final HandleOptions handleOptions;
  private final CBCluster cluster;

  public HandleFactory( HandleOptions hopts, CBCluster cl) {
    handleOptions = hopts;
    cluster = cl;
  }

  public HandleOptions getHandleOptions() {
    return handleOptions;
  }


  public CBCluster getCluster() {
    return cluster;
  }

}