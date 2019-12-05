/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.cbadmin.client;

import com.couchbase.cbadmin.assets.Bucket;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mnunberg
 */
public class BucketConfig {
  public final String name;
  private String password;
  public Bucket.BucketType bucketType;
  public Bucket.EphemeralBucketEvictionPolicy evictionPolicy;

  private Bucket.AuthType authType = Bucket.AuthType.SASL;
  private int proxyPort = 0;
  public int ramQuotaMB;
  public int replicaCount = 0;
  public boolean shouldIndexReplicas = false;

  public BucketConfig(String bktname) {
    name = bktname;
  }

  /**
   * Indicate the auth type is SASL auth.
   */
  public void setSaslAuth() {
    proxyPort = 0;
    authType = Bucket.AuthType.SASL;
  }

  /**
   * Indicate SASL auth should not be used.
   * This also enforces a proxy port for raw text access
   * @param txtProxyPort The port for plain text access
   */
  public void setNoAuth(int txtProxyPort) {
    proxyPort = txtProxyPort;
    authType = Bucket.AuthType.NONE;
  }

  /**
   * Sets the password for this bucket. Also sets SASL auth
   * @param passwd The SASL password.
   */
  public void setSaslPassword(String passwd) {
    setSaslAuth();
    password = passwd;
  }

  public Map<String,String> makeParams() {
    Map<String,String> params = new HashMap<String,String>();
    params.put("name", name);
    params.put("ramQuotaMB", "" + ramQuotaMB);
    if (bucketType != Bucket.BucketType.EPHEMERAL)
      params.put("replicaIndex", shouldIndexReplicas ? "1" : "0");
    if (bucketType != Bucket.BucketType.MEMCACHED)
      params.put("replicaNumber", "" + replicaCount);

    if (authType == Bucket.AuthType.SASL ||
            (password != null && password.isEmpty() == false)) {
      params.put("authType", "sasl");
      if (password == null ) {
        params.put("saslPassword", "");
      } else {
        params.put("saslPassword", password);
      }
    } else {
      params.put("authType", "none");
      params.put("proxyPort", "" + proxyPort);
    }

    switch (bucketType) {
      case COUCHBASE:
        params.put("bucketType", "membase");
        break;
      case EPHEMERAL:
        params.put("bucketType", "ephemeral");
        break;
      case MEMCACHED:
        params.put("bucketType", "memcached");
        break;
      default:
        throw new IllegalArgumentException("Incorrect bucket type");
    }

    if (bucketType == Bucket.BucketType.EPHEMERAL) {
      switch (evictionPolicy) {
        case NRUEVICTION:
          params.put("'evictionPolicy", "nruEviction");
          break;
        case NOEVICTION:
          params.put("evictionPolicy", "noEviction");
      }
    }

    return params;
  }
}