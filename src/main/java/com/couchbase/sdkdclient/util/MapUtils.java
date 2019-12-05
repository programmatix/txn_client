/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;


public class MapUtils {
  /**
   * This static method is here to replace a common idiom I find myself doing
   * for populating a map with values which are lists.
   *
   * If the collection is present, then the item will be added to the collection.
   * otherwise the {@code defl} callable is invoked and the return value is
   * used as the initial value.
   *
   * @param <T> The key type for the map
   * @param <U> The value type for the map
   * @param map The actual map
   * @param key The key under which the collection should exist
   * @param value The value to be inserted into the collection.
   * @param defl The callable to invoke if the collection is not found.
   */
  static public <T,U> void addToValue(Map<T, Collection<U>> map, T key, U value, Callable<? extends Collection<U>> defl) {
    Collection<U> coll = map.get(key);

    if (coll == null) {
      try {
        coll = defl.call();
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
      if (coll == null) {
        throw new IllegalArgumentException("Expected returned collection. Got null");
      }
      map.put(key, coll);
    }
    coll.add(value);
  }

  /**
   * Like {@link #addToValue(Map, Object, Object, Callable) }
   * but uses an array list factory.
   * @param <T>
   * @param <U>
   * @param map
   * @param key
   * @param value
   */
  static public <T, U> void addToValue(Map<T, Collection<U>> map, T key, U value) {
    addToValue(map, key, value, new Callable<Collection<U>>() {
      @Override
      public ArrayList<U> call() {
        return new ArrayList<U>();
      }
    });
  }
}
