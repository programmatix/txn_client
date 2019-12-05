/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.rundb;

import com.couchbase.sdkdclient.util.CbTime;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName="logEntries")
public class LogEntry implements DBEntry {
  public static final String FLD_TIMESTAMP = "timestamp";
  public enum Level {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    CRITICAL(5),
    ANY(-1);

    private final int val;

    Level(int num) {
      val = num;
    }

    public int getCode() {
      return val;
    }
  }

  @DatabaseField
  Level level;

  @DatabaseField
  String subsys;

  @DatabaseField(dataType = DataType.LONG_STRING)
  String message;

  @DatabaseField(columnName=FLD_TIMESTAMP)
  long msTimestamp;

  /**
   * Do not use. 0-arg constructor for ORMLite.
   */
  LogEntry() { }

  public LogEntry(Level lvl, CbTime ts, String sys, String msg) {
    level = lvl;
    message = msg;
    msTimestamp = ts.getEpochMillis();
    subsys = sys;
  }

  /**
   * Get the level for this log message
   * @return
   */
  public Level getLevel() {
    return level;
  }

  /**
   * @return the logger name for this message.
   */
  public String getSubsys() {
    return subsys;
  }

  /**
   * @return The actual content of this message
   */
  public String getMessage() {
    return message;
  }

  /**
   * @return The time at which this message was logged.
   */
  public CbTime getTimestamp() {
    return CbTime.fromMillis(msTimestamp);
  }

  @Override
  public void setDbId(long n) {
    // No ID here
  }
}