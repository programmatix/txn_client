/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.rundb;

import com.couchbase.sdkdclient.handle.Handle;
import com.couchbase.sdkdclient.workload.Workload;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Entry wrapping a {@link Handle}.
 * A handle must belong to a single {@link Workload} which is represented
 * as a {@link WorkloadEntry}.
 */
@DatabaseTable(tableName="handles")
public class HandleEntry implements DBEntry {
  // Field Names
  public static final String FLD_WORKLOAD = "workload";


  @DatabaseField(id=true)
  private volatile long Id;

  @DatabaseField(canBeNull=false, foreign=true, columnName = FLD_WORKLOAD)
  private WorkloadEntry workload = null;

  @DatabaseField(canBeNull=false)
  private int handleId = -1;




  /**
   * Constructs a new object.
   * @param h the handle to wrap
   * @param wl The entry for the {@link Workload} which created this handle.
   */
  public HandleEntry(Handle h, WorkloadEntry wl) {
    workload = wl;
    handleId = h.getId();
  }

  /**
   * No-arg constructor for ORM. Do not use
   */
  private HandleEntry() {
  }

  @Override
  public String toString() {
    return "HID:" + handleId;
  }


  public long getId() {
    return Id;
  }

  @Override
  public void setDbId(long idnum) {
    Id = idnum;
  }

}