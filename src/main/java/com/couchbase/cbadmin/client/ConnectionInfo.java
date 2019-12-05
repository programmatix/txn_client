/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.cbadmin.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Basic information about a connection
 */
public class ConnectionInfo {
  private final String clusterVersion;
  private final boolean clusterActive;
  private final boolean adminCreds;
  private final String clusterId;

  public ConnectionInfo(JsonObject poolsJson) throws RestApiException {
    JsonElement eTmp;
    eTmp = poolsJson.get("implementationVersion");
    if (eTmp == null) {
      throw new RestApiException("implementationVersion missing", poolsJson);
    }
    clusterVersion = eTmp.getAsString();

    eTmp = poolsJson.get("pools");
    if (eTmp == null || eTmp.isJsonArray() == false) {
      throw new RestApiException("pools missing", poolsJson);
    }

    if (eTmp.getAsJsonArray().size() > 0) {
      clusterActive = true;
      eTmp = eTmp.getAsJsonArray().get(0);

      if (eTmp.isJsonObject() == false) {
        throw new RestApiException("Expected object in pools entry", eTmp);
      }

      eTmp = eTmp.getAsJsonObject().get("uri");
      if (eTmp == null || eTmp.isJsonPrimitive() == false) {
        throw new RestApiException("uri missing or malformed", eTmp);
      }

      clusterId = eTmp.getAsString();

    } else {
      clusterActive = false;
      clusterId = null;
    }

    eTmp = poolsJson.get("isAdminCreds");
    adminCreds = eTmp != null && eTmp.getAsBoolean();
  }

  /**
   * Returns the cluster version of this node
   * @return
   */
  public String getVersion() {
    return clusterVersion;
  }

  /**
   * Checks whether this node is part of a cluster
   * @return
   */
  public boolean hasCluster() {
    return clusterActive;
  }

  /**
   * Checks whether we're connected with admin credentials
   * @return
   */
  public boolean isAdmin() {
    return adminCreds;
  }

  /**
   * Gets a string uniquely identifying this cluster
   * @return The string, or null if not part of a cluster
   */
  public String getClusterIdentifier() {
    return clusterId;
  }
}
