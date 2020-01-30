package com.couchbase.Couchbase.Nodes;/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

public final class NodeGroupList {
  private final Collection<NodeGroup> groups;
  private final URI assignmentUri;

  public NodeGroupList(JsonObject json) throws Exception {
    JsonElement e = json.get("uri");
    if (e == null || e.isJsonPrimitive() == false) {
      throw new Exception("Expected modification URI");
    }
    assignmentUri = URI.create(e.getAsString());

    e = json.get("groups");
    if (e == null || e.isJsonArray() == false) {
      throw new Exception("Expected 'groups'");
    }

    groups = new ArrayList<NodeGroup>();
    for (JsonElement groupElem : e.getAsJsonArray()) {
      if (groupElem.isJsonObject() == false) {
        throw new Exception("Expected object for group");
      }
      groups.add(new NodeGroup(groupElem.getAsJsonObject()));
    }
  }

  public URI getAssignmentUri() {
    return assignmentUri;
  }

  public Collection<NodeGroup> getGroups() {
    return groups;
  }

  public NodeGroup find(String name) {
    for (NodeGroup group : groups) {
      if (group.getName().equals(name)) {
        return group;
      }
    }
    return null;
  }

}
