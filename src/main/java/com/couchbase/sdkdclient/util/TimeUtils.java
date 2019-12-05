/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.util;

import com.couchbase.sdkdclient.logging.LogUtil;

public class TimeUtils {
  private static org.slf4j.Logger logger = LogUtil.getLogger(TimeUtils.class);
  private TimeUtils() {}

  /**
   * Gets the current time in seconds
   * @return The unix epoch in seconds
   */
  public static int time() {
    return (int) (System.currentTimeMillis() / 1000);
  }

  public static void sleepSeconds(int seconds) {
    sleepMillis(seconds * 1000);
  }

  public static void sleepMillis(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      //
      logger.error("While sleeping", ex);
    }
  }
}
