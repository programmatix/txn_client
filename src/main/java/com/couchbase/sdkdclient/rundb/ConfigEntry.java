/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.rundb;

import com.couchbase.sdkdclient.options.MultiOption;
import com.couchbase.sdkdclient.options.OptionInfo;
import com.couchbase.sdkdclient.options.OptionLookup;
import com.couchbase.sdkdclient.options.RawOption;
import com.couchbase.sdkdclient.options.RawOption.OptionAttribute;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A configuration entry wraps a single command line option. This is stored
 * into the database and then displayed upon analysis. This allows a test
 * to be repeated by simply analyzing its configuration and re-specifying
 * it via the commandline.
 */
@DatabaseTable(tableName="configuration")
public class ConfigEntry implements DBEntry {
  @DatabaseField
  String key;

  @DatabaseField
  String value;

  @DatabaseField
  String subsystem;

  @DatabaseField
  boolean wasSpecified;

  @DatabaseField(canBeNull=false, foreign=true)
  RunEntry parent;

  public ConfigEntry() {
  }

  public ConfigEntry(RunEntry runent, RawOption option, String name, String val) {
    parent = runent;
    key = name;
    subsystem = option.getSubsystem();
    if (val == null) {
      val = "";
    }

    value = val;
    wasSpecified = option.wasPassed();
  }

  public ConfigEntry(RunEntry runent, RawOption option, String name) {
    this(runent, option, name, option.getCurrentRawValue());

    if (option instanceof MultiOption) {
      throw new IllegalArgumentException(
              "Must use different constructor for multi option");
    }
  }

  /**
   * Gets the fully-qualified configuration key
   * @return The key
   */
  public String getKey() {
    return key;
  }

  /**
   * Gets the effective string value for this item
   * @return
   */
  public String getValue() {
    return value;
  }

  /**
   * Gets the logical subsystem for this item
   */
  public String getSubsystem() {
    return subsystem;
  }

  /**
   * Whether this item was specified by the user, or was unmodified from its
   * default setting
   * @return true if user specified, false if default.
   */
  public boolean wasPassed() {
    return wasSpecified;
  }

  public static List<ConfigEntry> createEntries(RunEntry runent, OptionLookup lu) {
    Collection<OptionInfo> coll = lu.getOptionsInfo();
    List<ConfigEntry> ll = new ArrayList<ConfigEntry>(coll.size());

    for (OptionInfo info : coll) {
      if (info.getOption().getAttribute(OptionAttribute.HIDDEN) && info.getOption().wasPassed() == false) {
        continue;
      }

      String name = info.getCanonicalName();
      name = name.replaceAll("^/", "").replaceAll("\\.", "_");
      if (name.equals("include")) {
        continue; // Skip included files..
      }

      if (info.getOption() instanceof MultiOption) {
        MultiOption option = (MultiOption) info.getOption();
        Collection<String> values = option.getRawValues();
        if (values.isEmpty()) {
          ll.add(new ConfigEntry(runent, option, name, null));
        } else {
          for (String val : values) {
            ll.add(new ConfigEntry(runent, option, name, val));
          }
        }
      } else {
        ll.add(new ConfigEntry(runent, info.getOption(), name));
      }
    }

    return ll;
  }

  @Override
  public void setDbId(long id) {
  }
}