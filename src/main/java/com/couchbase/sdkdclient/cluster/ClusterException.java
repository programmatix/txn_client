/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster;

import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;

public class ClusterException extends HarnessException {
  public ClusterException(String msg) {
    super(HarnessError.CLUSTER, msg);
  }

  public ClusterException(Throwable e) {
    super(e);
  }
}
