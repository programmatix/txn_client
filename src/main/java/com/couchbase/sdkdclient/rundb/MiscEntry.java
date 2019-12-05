/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.rundb;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.Map.Entry;

/**
 * Miscellaneous entry which can be used to log various properties which
 * otherwise have no structural relationship to other classes.
 */
@DatabaseTable(tableName = "misc")
public class MiscEntry implements Entry<String, String>, DBEntry {

  /**
   * Key for the commandline string.
   */
  public static final String K_CMDLINE = "verbatim_commandline";

  /**
   * Key for the SDKD response to the initial <i>INFO</i> request
   */
  public static final String K_SDKDINFO = "sdkd_info";

  /**
   * Key for the SDK info
   */
  public static final String K_TESTINFO = "test_info";

  /**
   * Key for the cluster version.
   */
  public static final String K_CLINFO = "cluster_info";

  /**
   * Key for the internal database version.
   */
  public static final String K_DBVERSION = "log_version";

  /**
   * Key for scenario descrption
   */
  public static final String K_SCDESCRIPTION = "scenario_description";

  /**
   * Key for Run Command
   */
  public static final String K_SCRUNCMD = "runcmd";

  public static final String K_STVERSION = "stester_version";

  public static final String K_BEGINTIME = "begin";
  public static final String K_ENDTIME   = "end";

  /**
   * Key for the log url
   */
  public static final String K_LOGURL = "log_url";
  /**
   * ID For test within suite.
   */
  public static final String K_SUITEID = "suite_test_id";

  @DatabaseField(foreign = true, uniqueCombo = true)
  RunEntry parent;

  @DatabaseField(uniqueCombo = true)
  String key;

  @DatabaseField(dataType = DataType.LONG_STRING)
  String value;

  private MiscEntry() {
  }

  /**
   * Construct a new entry.
   * @param ent The parent entry
   * @param k The key for this property
   * @param v The value for this property
   */
  public MiscEntry(RunEntry ent, String k, String v) {
    parent = ent;
    key = k;
    value = v;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public String setValue(String value) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void setDbId(long nn) {

  }
}
