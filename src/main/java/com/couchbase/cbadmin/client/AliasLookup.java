/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.cbadmin.client;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * On hosts with more than a single IP, the cluster will sometimes use a
 * different IP. While the cluster doesn't have problems communicating with
 * the nodes, user-facing IPs may indeed do so.
 */
public class AliasLookup {
  private final Map<String,Set<String>> dict = new HashMap<String, Set<String>>();
  private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  private final Logger logger = LoggerFactory.getLogger(AliasLookup.class);

  public Collection<String> getForAlias(String alias) {
    Set<String> s;
    InetAddress addr = null;
    try {
      addr = InetAddress.getByName(alias);
    } catch (Exception e) {
        logger.warn("Exception:{}", e.getMessage());
    }
    if (addr != null) {
      alias = addr.getHostAddress();
    }

    rwLock.readLock().lock();

    try {
      s = dict.get(alias);
    } finally {
      rwLock.readLock().unlock();
    }

    if (s == null) {
      s = new HashSet<String>();
      s.add(alias);
    }
    return s;
  }

  public void associateAlias(Collection<String> aliases) {
    rwLock.writeLock().lock();
    try {
      for (String alias : aliases) {
        Set<String> s = dict.get(alias);
        if (s == null) {
          s = new HashSet<String>();
          dict.put(alias, s);
        }

        s.add(alias);
        for (String other : aliases) {
          s.add(other);
        }
      }
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  public void associateAlias(String a1, String a2) {
    List<String> s = new ArrayList<String>(2);
    s.add(a1);
    s.add(a2);
    associateAlias(s);
  }

  public void merge(AliasLookup other) {
    other.rwLock.readLock().lock();
    try {
      for (Set<String> s : other.dict.values()) {
        associateAlias(s);
      }
    } finally {
      other.rwLock.readLock().unlock();
    }
  }
}