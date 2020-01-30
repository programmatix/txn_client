package com.couchbase.Couchbase.Bucket;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.google.gson.JsonObject;

/**
 *
 * @author mnunberg
 */
public class Bucket {
  public enum BucketType { COUCHBASE, MEMCACHED, EPHEMERAL }
  public enum AuthType {SASL, NONE }
  public enum EphemeralBucketEvictionPolicy {NOEVICTION, NRUEVICTION}
  public enum CompressionMode {OFF, PASSIVE, ACTIVE}
  private final String name;
  private final JsonObject rawJson;
  private final int replicas;
  private final BucketType type;

  public Bucket(JsonObject def) {
    rawJson = def;
    name = rawJson.get("name").getAsString();
    replicas = rawJson.get("replicaNumber").getAsInt();
    String sType = rawJson.get("bucketType").getAsString();

    if (sType.equals("membase") || sType.equals("couchbase")) {
      type = BucketType.COUCHBASE;
    } else if (sType.equals("ephemeral")) {
      type = BucketType.EPHEMERAL;
    } else {
      type = BucketType.MEMCACHED;
    }
  }

  public String getName() {
    return name;
  }

  public int getReplicaCount() {
    return replicas;
  }

  public JsonObject getRawJson() {
    return rawJson;
  }

  public BucketType getType() {
    return type;
  }

  @Override
  public String toString() {
    return String.format("Bucket type=%s Name=%s Replicas=%d", type, name, replicas);
  }
}
