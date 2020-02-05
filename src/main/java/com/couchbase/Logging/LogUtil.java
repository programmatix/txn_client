package com.couchbase.Logging;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to manage logging contexts and levels
 */
public class LogUtil {
  static public class LogProducer {
    private final String alias;
    private final String description;
    private final String loggerName;

    public LogProducer(String shortname, String desc, String logname) {
      alias = shortname;
      description = desc;
      loggerName = logname;
    }

    public String getAlias() {
      return alias;
    }

    public String getDescription() {
      return description;
    }

    public String getLoggerName() {
      return loggerName;
    }
  }

  public static final Map<String,LogProducer> logProducers =
          new HashMap<String, LogProducer>();

  // Common prefixes for loggers
  public final static String PKG = "com.couchbase";
  public final static String CONSTANTS = PKG + "." + "Constants";
  public final static String COUCHBASE = PKG + "." + "Couchbase";
  public final static String EXCEPTIONS = PKG + "." + "Exceptions";
  public final static String INPUTPARAMETERS = PKG + "." + "InputParameters";
  public final static String TESTS = PKG + "." + "Tests";
  public final static String UTILS = PKG + "." + "Utils";
  private final static Logger logger = LoggerFactory.getLogger(LogUtil.class);

  static {
    LoggingOptions.init();
    logProducers.put("CONSTANTS", new LogProducer("CONSTANTS", "CONSTANTS", CONSTANTS));
    logProducers.put("COUCHBASE", new LogProducer("COUCHBASE", "Install and config CB", COUCHBASE));
    logProducers.put("EXCEPTIONS", new LogProducer("EXCEPTIONS", "EXCEPTIONS", EXCEPTIONS));
    logProducers.put("INPUTPARAMETERS", new LogProducer("INPUTPARAMETERS", "INPUTPARAMETERS", INPUTPARAMETERS));
    logProducers.put("TESTS", new LogProducer("TESTS", "TESTS Logging", TESTS));
    logProducers.put("UTILS", new LogProducer("UTILS", "UTILS", UTILS));
  }


  /**
   * Gets a logger
   * @param cls The class which will use the logger
   * @param shortname An alias for the logger
   * @param desc Description for the logger
   * @return a Logger instance
   */
  public static Logger getLogger(Class cls, String shortname, String desc) {
    synchronized (logProducers) {
      LogProducer lp = new LogProducer(shortname, desc, cls.getCanonicalName());
      if (shortname != null) {
        logProducers.put(shortname, lp);
      }
    }
    return LoggerFactory.getLogger(cls);
  }

  public static Logger getLogger(String clsName) {
    String longestString = null;

    synchronized (logProducers) {
      for (String curPrefix : logProducers.keySet()) {
        if (!clsName.startsWith(curPrefix)) {
          continue;
        }
        if (longestString == null) {
          longestString = curPrefix;
          continue;
        }
        if (curPrefix.length() > longestString.length()) {
          longestString = curPrefix;
        }
      }

      if (longestString == null) {
        longestString = clsName;
        logProducers.put(longestString, new LogProducer(clsName, null, clsName));
      }
    }
    return LoggerFactory.getLogger(longestString);
  }

  public static Logger getLogger(Class cls) {
    return getLogger(cls.getCanonicalName());
  }

  public static void setLevelFromSpec(String spec) {
    String[] kv = spec.split(":");
    if (kv.length != 2) {
      throw new IllegalArgumentException("Spec should be sys:level. Got " + spec);
    }

    String name = kv[0];
    String level = kv[1];


    if (name.equals("all")) {

      logger.debug("Setting name to all");
      LoggingOptions.setOutputLevel(PKG, level);
    } else if (name.equals("spam")) {
      for (LogProducer lp : logProducers.values()) {
        LoggingOptions.setOutputLevel(lp.getLoggerName(), level);
      }

    } else if (logProducers.containsKey(name)) {
      LogProducer lp = logProducers.get(name);
      LoggingOptions.setOutputLevel(lp.getLoggerName(), level);
    } else {
      logger.warn("No alias for {}. Assuming classname", name);
      LoggingOptions.setOutputLevel(name, level);
    }
   // System.exit(-1);
  }

  public static String getLoggersHelp() {
    StringBuilder sb = new StringBuilder();
    sb.append("The following is a comprehensive list of all sdkdclient loggers\n\n");
    for (LogProducer lp : logProducers.values()) {
      sb.append(lp.getAlias());
      sb.append("\n");
      sb.append("  ").append("Path: ").append(lp.getLoggerName());
      sb.append("\n");
      if (lp.getDescription() != null) {
        sb.append("  ").append("Description: ").append(lp.getDescription());
        sb.append("\n");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  /**
   * Adds a {@code --debug, -d} option to the command line. This will doConfigure
   * logging
   * @param parser
   */
  static final RawOption debugOption = new RawOption("debug", "Debugging options") {
    @Override
    public void parse(String input) {
      if (input == null || input.isEmpty()) {
        return;
      }
      setLevelFromSpec(input);
    }
  };

  static {
    debugOption.setDescription(
            "Logging levels to set. Specify this option multiple "+
            "times in the format of `-d <prefix>:<level>` where `prefix` " +
            "is a logging prefix and level is the minimum severity level to output");
    debugOption.addShortAlias("d");
  }



  private LogUtil() {}
}