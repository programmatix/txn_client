package com.couchbase.sdkdclient.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AppenderLevelFilter extends Filter<ILoggingEvent> {
  // User-provided configuration
  final Map<String,Level> config = new HashMap<String, Level>();

  // Fast lookup cache
  final Map<String,Level> levelCache = new HashMap<String, Level>();

  // We mainly read the configuration
  final ReadWriteLock rwLock = new ReentrantReadWriteLock();

  // Default level
  private Level rootLevel = Level.INFO;

  /**
   * Creates an entry for a logger name which does not yet exist. This will
   * add an entry in the cache. The exclusive lock must be held.
   *
   * @param name The logger to look up
   * @return The minimum allowable level.
   */
  private Level handleNewLogger(String name) {
    String curMatch = null;
    for (String nn : levelCache.keySet()) {
      if (!name.startsWith(nn)) {
        continue;
      }
      if (curMatch == null) {
        curMatch = nn;
        continue;
      }

      if (curMatch.length() < nn.length()) {
        curMatch = nn;
      }
    }

    if (curMatch == null) {
//      System.err.println("Couldn't get match for " + name);
      levelCache.put(name, rootLevel);
    } else {
//      System.err.printf("Found match: %s => %s%n", name, curMatch);
      levelCache.put(name, levelCache.get(curMatch));
    }

    return levelCache.get(name);
  }

  /**
   * Set the default level for all loggers which are not matched by the
   * current logger. This is helpful if a level for the root logger has
   * not been defined.
   * @param lvl The minimum default level
   */
  public void setDefaultLevel(Level lvl) {
    rootLevel = lvl;
  }

  /**
   * Sets the level for the given logger with the provided name.
   * @param name The logger name for which to set the level
   * @param ll The minimum level to use.
   */
  public final void setLevel(String name, Level ll) {
//    System.err.printf("Setting level for '%s'%n", name);
    rwLock.writeLock().lock();
    try {
      levelCache.clear();
      config.put(name, ll);
      levelCache.putAll(config);
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  private static FilterReply doDecideLevel(ILoggingEvent ev, Level min) {
    if (ev.getLevel().isGreaterOrEqual(min)) {
      return FilterReply.NEUTRAL;
    }
    return FilterReply.DENY;
  }

  @Override
  public final FilterReply decide(ILoggingEvent event) {
    boolean hasReadLock = true;
    rwLock.readLock().lock();

    try {
      Level level = levelCache.get(event.getLoggerName());
      if (level == null) {
        rwLock.readLock().unlock();
        hasReadLock = false;
        rwLock.writeLock().lock();
        try {
          level = handleNewLogger(event.getLoggerName());
          return doDecideLevel(event, level);
        } finally {
          rwLock.writeLock().unlock();
        }

      }
      return doDecideLevel(event, level);
    } finally {

      if (hasReadLock) {
        rwLock.readLock().unlock();
      }
    }
  }
}
