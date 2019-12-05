/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.ssh;

/**
 *
 * @author mnunberg
 */
public interface SimpleCommand {
  public String getStderr();
  public String getStdout();
  public boolean isSuccess();
}