package com.couchbase.Logging;/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.filter.Filter;
import com.couchbase.Logging.AppenderLevelFilter;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Configures logging options. This is separate from the LogUtil
 * class, as this depends on a specific implementation.
 */
class LoggingOptions {
  public static final int LVL_TRACE = Level.TRACE_INT;
  public static final int LVL_DEBUG = Level.DEBUG_INT;
  public static final int LVL_INFO = Level.INFO_INT;
  public static final int LVL_WARN = Level.WARN_INT;
  public static final int LVL_ERROR = Level.ERROR_INT;

  private final static LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
  private final static AppenderLevelFilter levelFilter;
  private final static ConsoleAppender<ILoggingEvent> stdoutAppender;


  static {
    Appender<ILoggingEvent> cand = null;
    Logger rootLogger = ctx.getLogger("ROOT");
    Iterator<Appender<ILoggingEvent>> iter = rootLogger.iteratorForAppenders();

    while (iter.hasNext()) {
      Appender<ILoggingEvent> cur = iter.next();
      if (cur.getName().equals("STDOUT")) {
        cand = cur;
        break;
      }
    }


    Filter candFilt = null;
    if (cand == null) {
      System.err.println("Couldn't get STDOUT appender");
      levelFilter = null;
      stdoutAppender = null;
    } else {
      stdoutAppender = (ConsoleAppender<ILoggingEvent>)cand;
      for (Filter filter : cand.getCopyOfAttachedFiltersList()) {
        if (filter.getName() == null) {
          continue;
        }
        if (filter.getName().equals("STDOUT_FILTER")) {
          candFilt = filter;
          break;
        }
      }
      if (candFilt == null) {
        System.err.println("Couldn't get STDOUT_FILTER filter");
        levelFilter = null;
      } else {
        levelFilter = (AppenderLevelFilter)candFilt;
      }
    }
  }

  /**
   * Controls the output level to the console for a given logger
   * @param name The logger to limit
   * @param level The minimum output level to use.
   */
  public static void setOutputLevel(String name, int level) {
    Level lvl = Level.toLevel(level);
    if (levelFilter != null) {
      levelFilter.setLevel(name, lvl);
    } else {
      System.err.printf("%s: Level Filter is null%n", name);
    }
  }

  /**
   * @see #setOutputLevel(String, int)
   * @param name
   * @param level
   */
  public static void setOutputLevel(String name, String level) {
    Level lvl = Level.valueOf(level);
    setOutputLevel(name, lvl.levelInt);
  }

  /**
   * Set the <b>logger</b> level. Unlike {@link #setOutputLevel(String, int)}
   * this controls the logger itself and thus messages filtered here will
   * <b>not</b> be sent to the database
   * @param name The logger to filter
   * @param lvlstr A string for the level, e.g. {@code TRACE}, {@code DEBUG} etc.
   */
  public static void setLoggerLevel(String name, int lvlstr) {
    Level lvl = Level.toLevel(lvlstr);
    Logger logger = ctx.getLogger(name);
    logger.setLevel(lvl);
  }

  /**
   * Enable or disable colors. This should be called early on in order to ensure
   * it can be applied
   * @param enabled true if it should be enabled, false otherwise.
   */
  public static void setColorsEnabled(boolean enabled) {
    if (stdoutAppender == null) {
      return;
    }

    stdoutAppender.setWithJansi(enabled);
    Highlighter.setEnabled(enabled);
    // Might be a bit of a hack?
    stdoutAppender.start();
  }

  static void init() {
    // Do nothing, just let it be imported
  }

  private LoggingOptions() {}
}
