/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.rundb;
import com.couchbase.sdkdclient.context.RunContext;
import com.couchbase.sdkdclient.handle.Handle;
import com.couchbase.sdkdclient.workload.Workload;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.Collection;

/**
 * Entry representing a workload. A workload belongs to a single
 * {@link RunContext} and creates one or more {@link Handle} objects.
 */
@DatabaseTable(tableName="workloads")
public class WorkloadEntry implements DBEntry {

  @DatabaseField(canBeNull=false, foreign=true)
  RunEntry parent;

  @DatabaseField(generatedId=true)
  volatile Long Id;

  @DatabaseField(canBeNull=false)
  private String name;

  /**
   * Construct a new workload entry.
   * @param runent The parent entry for the {@link RunContext} which created
   * this workload.
   * @param wl The workload to add.
   */
  public WorkloadEntry(RunEntry runent, Workload wl) {
    parent = runent;
    name = wl.getName();
  }

  private WorkloadEntry() {
  }

  /**
   * Gets the parent
   * @return the parent entry.
   */
  public RunEntry getRunEntry() {
    return parent;
  }

  @ForeignCollectionField
  ForeignCollection<HandleEntry> handles;

  @Override
  public String toString() {
    return name;
  }

  /**
   * Get the workload name.
   * @return The workload name.
   */
  public String getName() {
    return name;
  }

  /**
   * Get all the handles created by this workload
   * @return A collection of handles which belong to this workload.
   */
  public Collection<HandleEntry> getHandles() {
    return handles;
  }

  @Override
  public void setDbId(long nn) {
    Id = nn;
  }
}