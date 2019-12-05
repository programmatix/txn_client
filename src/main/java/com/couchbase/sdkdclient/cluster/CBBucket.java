/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster;

/**
 *
 * @author mnunberg
 */
public class CBBucket {
  private String name;
  private String saslPassword;

  public CBBucket(String bktname, String bktpass) {
    name = bktname;
    saslPassword = bktpass;
  }

  public String getName() {
    return name;
  }

  public String getPassword() {
    return saslPassword;
  }
}
