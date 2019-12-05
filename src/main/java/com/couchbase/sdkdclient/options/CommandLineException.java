/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

/**
 *
 * @author mnunberg
 */
public class CommandLineException extends Exception {

  public CommandLineException(String msg) {
    super(msg);
  }
  public CommandLineException(String optStr, String msg) {
    super(String.format("%s: %s", optStr, msg));
  }
}
