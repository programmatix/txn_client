/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.rundb;

/**
 * This interface doesn't do anything and exists merely for documentation.
 *
 * A Database entry is a class representing a table. It provides routines for
 * mapping internal structures to their normalized database form, and
 * retrieving them back from the database.
 */
public interface DBEntry {
  /**
   * Sets the database ID for this object. This is only called by the
   * {@link RunDB} class.
   */
  public void setDbId(long id);
}
