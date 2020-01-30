package com.couchbase.Logging;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.color.HighlightingCompositeConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;

/**
 *
 * @author mnunberg
 */
public class Highlighter extends HighlightingCompositeConverter {
  public static boolean ENABLED = true;
  public static void setEnabled(boolean enabled) {
    ENABLED = enabled;
  }

  @Override
  protected String getForegroundColorCode(ILoggingEvent event) {
    if (!ENABLED) {
      return ANSIConstants.DEFAULT_FG;
    }

    Level level = event.getLevel();
    switch (level.toInt()) {
      case Level.ERROR_INT:
        return ANSIConstants.BOLD+ANSIConstants.RED_FG;
      case Level.WARN_INT:
        return ANSIConstants.YELLOW_FG;
      case Level.INFO_INT:
        return ANSIConstants.DEFAULT_FG;
      case Level.DEBUG_INT:
        return ANSIConstants.BLUE_FG;
      case Level.TRACE_INT:
        return ANSIConstants.BLUE_FG;
      default:
        return ANSIConstants.DEFAULT_FG;
    }
  }
}
