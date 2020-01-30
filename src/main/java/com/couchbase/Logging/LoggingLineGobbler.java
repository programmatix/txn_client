package com.couchbase.Logging;/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * This class extends {@link LineGobbler} by logging each message received
 * via a specific logger at a specific level. You will need to provide your
 * own logger instance as typically this will be for some long running stream
 * which is spawned by the logger's parent class.
 *
 * This logger also features options to filter 'spam' input, that is excessive
 * and repetitive log messages.
 */
public class LoggingLineGobbler extends LineGobbler {
  private final static Pattern excTracePattern = Pattern.compile("^\\s+at.*");
  private final static Logger logger = LogUtil.getLogger(LineGobbler.class);
  private final static List<Pattern> filterPatterns = new ArrayList<Pattern>();
  private volatile String prefix = null;

  static {
    InputStream strm = LineGobbler.class.getClassLoader().getResourceAsStream(
            "sdkd-log-filters.regexp");
    if (strm != null) {
      try {
        String bufs[] = IOUtils.toString(strm).split("\n");
        for (String line : bufs) {
          if (line.startsWith("#")) {
            continue;
          }
          Pattern ptrn = Pattern.compile(line);
          filterPatterns.add(ptrn);
        }
      } catch (Exception ex) {
        logger.error("Couldn't read regex definitions", ex);
      }
    } else {
      logger.warn("Couldn't get regex definitions");
    }
  }

  private boolean shouldFilter = true;
  private ArrayList<String> atBuf = new ArrayList<String>();
  private final Logger msgLogger;
  private final LogEntry.Level level;
  private final Map<String,Integer> loggedExceptions = new HashMap<String, Integer>();
  private int excIds = 0;

  /**
   * Create a new loging line gobbler.
   * @param stream The stream to gobble
   * @param msgLogger The logger to use for logging each line
   * @param level The level at which each line should be logged.
   */
  public LoggingLineGobbler(InputStream stream, Logger msgLogger, LogEntry.Level level) {
    super(stream);
    this.msgLogger = msgLogger;
    this.level = level;
  }

  /**
   * Whether heuristic filtering should be enabled.
   *
   * This will enable regular expression loading and duplicate filtering
   * @param enabled true if filtering should be enabled.
   */
  public void setShouldFilter(boolean enabled) {
    shouldFilter = enabled;
  }

  /**
   * Sets the text to be prepended before each line.
   * @param pfx
   */
  public void setLinePrefix(String pfx) {
    prefix = pfx;
  }

  private boolean doFilter(String line) {
    if (!shouldFilter) {
      return true;
    }
    for (Pattern ptrn : filterPatterns) {
      if (ptrn.matcher(line).find()) {
        // Hide this message:
        log("+++ Suppressed: " + ptrn.toString());
        return false;
      }
    }

    if (excTracePattern.matcher(line).matches()) {
      atBuf.add(line);
      return false;

    } else if (!atBuf.isEmpty()) {
      String excMsg = StringUtils.join(atBuf, "\n");

      if (!loggedExceptions.containsKey(excMsg)) {
        int excId = excIds++;
        logger.info("+++ Following exception has internal ID: {}", excIds++);
        loggedExceptions.put(excMsg, excId);
        log(excMsg);

      } else {
        log("+++ Received exception of ID " + loggedExceptions.get(excMsg));
      }

      atBuf.clear();
    }

    return true;
  }

  private void log(String ln) {
    switch (level) {
      case TRACE:
        msgLogger.trace(ln);
      case DEBUG:
        msgLogger.debug(ln);
        break;
      case INFO:
        msgLogger.info(ln);
        break;
      case WARN:
        msgLogger.warn(ln);
        break;

      case ERROR:
      case CRITICAL:
        msgLogger.error(ln);
        break;
    }
  }

  @Override
  protected void msg(String ln) {
    if (!doFilter(ln)) {
      return;
    }
    log(prefix == null ? ln : prefix + ':' +ln);
  }

  @Override
  protected void err(Throwable e) {
    logger.error("Stream Error", e);
  }

  @Override
  protected void flush() {
    if (!atBuf.isEmpty()) {
      log("+++ Flushing remaining exception trace buffer");
      log(StringUtils.join(atBuf, "\n"));
      atBuf.clear();
    }
  }
}
