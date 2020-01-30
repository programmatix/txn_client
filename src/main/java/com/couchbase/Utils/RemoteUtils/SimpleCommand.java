package com.couchbase.Utils.RemoteUtils;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author mnunberg
 */
public interface SimpleCommand {
  public String getStderr();
  public String getStdout();
  public boolean isSuccess();
}