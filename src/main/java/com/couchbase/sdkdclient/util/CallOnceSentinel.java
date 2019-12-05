/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.util;

public class CallOnceSentinel {
  private StackTraceElement[] stack = null;
  public CallOnceSentinel() {}

  // Check that this function was called only once.
  public synchronized void check() {
    if (stack == null) {
      stack = Thread.currentThread().getStackTrace();
    } else {
      IllegalStateException ex =
              new IllegalStateException("This is the stack of the previous call");
      ex.setStackTrace(stack);
      throw ex;
    }
  }

  public boolean wasCalled() {
    return stack != null;
  }
}