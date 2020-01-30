package com.couchbase.Exceptions;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



public class ClusterException extends HarnessException {
  public ClusterException(String msg) {
    super(HarnessError.CLUSTER, msg);
  }

  public ClusterException(Throwable e) {
    super(e);
  }
}
