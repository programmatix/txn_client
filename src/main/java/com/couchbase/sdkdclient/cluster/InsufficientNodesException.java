/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.cluster;

public class InsufficientNodesException extends IllegalStateException {
  public InsufficientNodesException(String msg) {
    super(msg);
  }
}
