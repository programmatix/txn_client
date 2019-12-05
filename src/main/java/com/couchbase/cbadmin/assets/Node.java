/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.cbadmin.assets;

import com.couchbase.cbadmin.client.CouchbaseAdmin;
import com.couchbase.cbadmin.client.RestApiException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author mnunberg
 */
public class Node implements ClusterAsset {
  public enum Membership { ACTIVE, INACTIVE_ADDED, INACTIVE_FAILED, UNKNOWN }
  public enum Status { HEALTHY, UNHEALTHY, WARMUP, UNKNOWN }

  private CouchbaseAdmin selfClient;
  private URL n1qlUrl;
  private URL couchUrl;
  private URL restUrl;
  private URL ftsUrl;
  private URL cbasUrl;
  private JsonObject rawJson;
  private String versionString;
  private String NSOtpNode;
  private int clusterCompatVersion = 0;
  private Membership membership = Membership.UNKNOWN;
  private Status status = Status.UNKNOWN;

  public URL getCouchUrl() {
    return couchUrl;
  }

  public URL getN1QLUrl() {
    return n1qlUrl;
  }

  public URL getFTSUrl() {
    return ftsUrl;
  }

  public URL getCBASUrl() {
    return cbasUrl;
  }

  public URL getRestUrl() {
    return restUrl;
  }

  public String getNSOtpNode() {
    return NSOtpNode;
  }

  public String getClusterVersion() {
    return versionString;
  }

  public int getClusterCompatMajorVersion() {
    if (clusterCompatVersion == 1) {
      return Cluster.COMPAT_1x;
    } else {
      return Cluster.COMPAT_20;
    }
  }

  public int getClusterCompatVersion() {
    return clusterCompatVersion;
  }

  public Membership getMembership() {
    return membership;
  }

  public Status getStatus() {
    return status;
  }

  /**
   * Checks to see that the node is functioning OK by examining some of its
   * status variables.
   * @return
   */
  public boolean isOk() {
    if (membership != Membership.ACTIVE) {
      return false;
    }
    if (status != Status.HEALTHY) {
      return false;
    }
    return true;
  }

  @Override
  public JsonObject getRawJson() {
    return rawJson;
  }

  public Node(JsonObject def) throws RestApiException {
    JsonElement eTmp;
    String sTmp;
    eTmp = def.get("hostname");
    if (eTmp == null) {
      throw new RestApiException("Expected 'hostname'", def);
    }
    sTmp = eTmp.getAsString();

    try {
      restUrl = new URL("http://" + sTmp + "/");
    } catch (MalformedURLException ex) {
      throw new RuntimeException(ex);
    }

    eTmp = def.get("couchApiBase");
    if (eTmp != null) {
      try {
        sTmp = eTmp.getAsString();
        couchUrl = new URL(sTmp);
      } catch (MalformedURLException ex) {
        throw new RuntimeException(ex);
      }
    }

    eTmp = def.get("version");
    if (eTmp == null) {
      throw new RestApiException("Expected 'version' in nodes JSON", def);
    }
    versionString = eTmp.getAsString();

    eTmp = def.get("otpNode");
    if (eTmp == null) {
      throw new RestApiException("Expected 'otpNode'", def);
    }
    NSOtpNode = eTmp.getAsString();

    eTmp = def.get("clusterCompatibility");
    if (eTmp != null) {
      clusterCompatVersion = eTmp.getAsInt();
    }

    eTmp = def.get("clusterMembership");
    if (eTmp != null) {
      sTmp = eTmp.getAsString();

      if (sTmp.equals("active")) {
        membership = Membership.ACTIVE;
      } else if (sTmp.equals("inactiveAdded")) {
        membership = Membership.INACTIVE_ADDED;

      } else if (sTmp.equals("inactiveFailed")) {
        membership = Membership.INACTIVE_FAILED;
      }
    }

    eTmp = def.get("status");
    if (eTmp != null) {
      sTmp = eTmp.getAsString();
      if (sTmp.equals("healthy")) {
        status = Status.HEALTHY;
      } else if (sTmp.equals("unhealthy")) {
        status = Status.UNHEALTHY;
      } else if (sTmp.equals("warmup")) {
        status = Status.WARMUP;
      }
    }
  }

  public CouchbaseAdmin getOwnClient(CouchbaseAdmin template) {
    if (selfClient == null) {
      selfClient = template.copyForHost(restUrl, n1qlUrl, ftsUrl, cbasUrl);
    }

    return selfClient;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
            .append(NSOtpNode)
            .append(restUrl)
            .toHashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if (! (other instanceof Node)) {
      return false;
    }

    Node nOther = (Node) other;
    return new EqualsBuilder()
            .append(NSOtpNode, nOther.NSOtpNode)
            .append(restUrl, nOther.restUrl)
            .isEquals();
  }

  @Override
  public String toString() {
    return "<URI:"+restUrl.getAuthority()+","+NSOtpNode+">";
  }
}
