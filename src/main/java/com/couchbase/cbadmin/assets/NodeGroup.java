/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.cbadmin.assets;

import com.couchbase.cbadmin.client.RestApiException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A node group (or in sever parlance, a {@code serverGroup}
 * is a group of nodes designated to reside in the same physical
 * domain.
 *
 * The NodeGroup object is the unit of data for dealing with groups
 * within the {@link com.couchbase.cbadmin.client.CouchbaseAdmin} API.
 */
public final class NodeGroup {
  static private class Deserialized {
    String name;
    String uri;
    JsonArray nodes;
  }

  static private URI getFromString(String s) {
    if (s == null) {
      throw new IllegalArgumentException("Field was null");
    }
    return URI.create(s);
  }

  final private String name;
  final private URI uri;
  final private List<Node> nodes = new ArrayList<Node>();

  public NodeGroup(JsonElement json) throws RestApiException {
    Deserialized obj = new Gson().fromJson(json, Deserialized.class);

    if ((this.name = obj.name) == null) {
      throw new IllegalArgumentException("Malformed JSON");
    }

    this.uri  = getFromString(obj.uri);
    if (obj.nodes == null) {
      throw new IllegalArgumentException("Node list was empty");
    }
    for (JsonElement e : obj.nodes) {
      nodes.add(new Node(e.getAsJsonObject()));
    }
  }

  public String getName() {
    return name;
  }

  public URI getUri() {
    return uri;
  }

  public Collection<Node> getNodes() {
    return nodes;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof NodeGroup)) {
      return false;
    }

    NodeGroup other = (NodeGroup) o;


    assert uri != null;
    assert other.uri != null;
    return uri.equals(other.uri);
  }

  @Override
  public int hashCode() {
    return uri != null ? uri.hashCode() : 0;
  }

  @Override
  public String toString() {
    return String.format("serverGroup '%s' => %s", name, uri);
  }
}
