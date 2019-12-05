/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.cbadmin.assets;

import com.google.gson.JsonObject;

/**
 *
 * @author mnunberg
 */
public interface ClusterAsset {
  public JsonObject getRawJson();
}
