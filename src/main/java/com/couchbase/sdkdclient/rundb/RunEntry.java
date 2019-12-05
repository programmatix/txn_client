package com.couchbase.sdkdclient.rundb;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Root entry representing a single run.
 */
@DatabaseTable(tableName="run_entries")
public class RunEntry implements DBEntry {
  @DatabaseField(generatedId=true)
  private volatile UUID Id;

  public RunEntry() {
  }

  @Override
  public String toString() {
    return Id.toString();
  }

  @ForeignCollectionField
  ForeignCollection<ConfigEntry> configEntries;

  @ForeignCollectionField
  ForeignCollection<WorkloadEntry> workloads;

  @ForeignCollectionField
  ForeignCollection<MiscEntry> properties;


  /**
   * Gets the configuration entries for this run.
   * @return A collection of entries.
   */
  public Collection<ConfigEntry> getConfiguration() {
    return configEntries;
  }

  /**
   * Gets a list of miscellaneous key-value properties which were stored
   * during this run.
   * @return
   */
  public Collection<MiscEntry> getProperties() {
    return properties;
  }

  /**
   * Gets a list of workloads created for this run
   * @return A collection of workloads.
   */
  public Collection<WorkloadEntry> getWorkloads() {
    return workloads;
  }


  @Override
  public void setDbId(long n) {
    //
  }
}