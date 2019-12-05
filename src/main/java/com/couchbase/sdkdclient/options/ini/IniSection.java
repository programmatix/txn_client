/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options.ini;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author mnunberg
 */
public class IniSection {
  final Collection<Entry<String,String>> kvPairs =
          new ArrayList<Entry<String, String>>();

  final Map<String,Collection<String>> quickMap =
          new HashMap<String, Collection<String>>();

  public Collection<Entry<String,String>> entrySet() {
    return kvPairs;
  }

  void seal() {
    for (Entry<String,String> ent : kvPairs) {
      Collection<String> coll = quickMap.get(ent.getKey());
      if (coll == null) {
        coll = new ArrayList<String>();
        quickMap.put(ent.getKey(), coll);
      }
      coll.add(ent.getValue());
    }
  }

  public String get(String k, String defl) {
    Collection<String> coll = quickMap.get(k);
    if (coll != null) {
      return coll.iterator().next();
    } else {
      return defl;
    }
  }

  public int getInt(String k, int defl) {
    String s = get(k, null);
    if (s == null) {
      return defl;
    }
    return Integer.parseInt(s);
  }
}
