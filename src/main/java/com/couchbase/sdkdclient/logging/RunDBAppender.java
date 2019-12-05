/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.couchbase.sdkdclient.rundb.LogEntry;
import com.couchbase.sdkdclient.rundb.RunDB;
import com.couchbase.sdkdclient.util.CbTime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RunDBAppender extends UnsynchronizedAppenderBase<ILoggingEvent>{
  static private RunDB database;
  static private boolean enabled = true;

  static final private List<LogEntry> pending = new ArrayList<LogEntry>();

  public static void setDatabase(RunDB db) {
    database = db;
    for (LogEntry ent : pending) {
      try {
        db.addLogEntry(ent);
      } catch (SQLException ex) {
        // Do nothing?
      }
    }
    pending.clear();
  }

  public static void disable() {
    enabled = false;
  }

  static LogEntry.Level levelMap(ILoggingEvent e) {
    switch (e.getLevel().levelInt) {
      case Level.TRACE_INT:
        return LogEntry.Level.TRACE;
      case Level.DEBUG_INT:
        return LogEntry.Level.DEBUG;
      case Level.INFO_INT:
        return LogEntry.Level.INFO;
      case Level.WARN_INT:
        return LogEntry.Level.WARN;
      case Level.ERROR_INT:
        return LogEntry.Level.ERROR;
      default:
        return LogEntry.Level.INFO;
    }
  }

  @Override
  public void append(ILoggingEvent e) {
    if (!enabled) {
      return;
    }

    LogEntry entry = new LogEntry(
            levelMap(e),
            CbTime.fromMillis(e.getTimeStamp()),
            e.getLoggerName(),
            e.getFormattedMessage());
    if (database == null) {
      pending.add(entry);
    } else {
      try {
        database.addLogEntry(entry);
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
