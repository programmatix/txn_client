package com.couchbase.Constants;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author mnunberg
 */
public enum Command {

  /**
   * Request a new HANDLE
   */
  NEWHANDLE("NEWHANDLE"),

  /**
   * Close an existing handle
   */
  CLOSEHANDLE("CLOSEHANDLE"),

  /**
   * Close the main (i.e. "control") connection to the SDKD. The SDKD may
   * exit after a {@code GOODBYE}
   */
  GOODBYE("GOODBYE"),

  /**
   * Cancel an ongoing command.
   */
  CANCEL("CANCEL"),

  /**
   * Retrieve information about the SDKD
   */
  INFO("INFO"),

  /**
   * Declare the SDKD's absolute TTL timer.
   */
  TTL("TTL"),

  /**
   * Upload logs
   */
  UPLOADLOGS("UPLOADLOGS"),
  /**
   * Create a new dataset. Don't use this.
   */
  @Deprecated
  NEWDATASET("NEWDATASET"),

  /**
   * Store the keys specified in the dataset
   */
  MC_DS_MUTATE_SET("MC_DS_MUTATE_SET"),

  /**
   * Append to the keys specified in the dataset.
   */
  MC_DS_MUTATE_APPEND("MC_DS_MUTATE_APPEND"),

  /**
   * Prepend to the keys specific in the dataset.
   */
  MC_DS_MUTATE_PREPEND("MC_DS_MUTATE_PREPEND"),
  MC_DS_MUTATE_REPLACE("MC_DS_MUTATE_REPLACE"),
  MC_DS_MUTATE_ADD("MC_DS_MUTATE_ADD"),

  /**
   * Get the items specified in this dataset
   */
  MC_DS_GET("MC_DS_GET"),
  /**
   * Delete the keys in this dataset
   */
  MC_DS_DELETE("MC_DS_DELETE"),
  /**
   * Touch the keys in this dataset
   */
  MC_DS_TOUCH("MC_DS_TOUCH"),
  /**
   * Get replica items for keys in this dataset
   */
  MC_DS_GETREPLICA("MC_DS_GETREPLICA"),
  /**
   * Get with lock  in the dataset
   */
  MC_DS_GETWITHLOCK("MC_DS_GETWITHLOCK"),
  /**
   * Increment key in the dataset
   */
  MC_DS_INCR("MC_DS_INCR"),
  /**
   * Decrement key in the datase
   */
  MC_DS_DECR("MC_DS_DECR"),
  /**
   * Verify keys in the dataset exists
   */
  MC_DS_EXISTS("MC_DS_EXISTS"),
  /**
   * unlock keys in the dataset
   */
  MC_DS_UNLOCK("MC_DS_UNLOCK"),
  /**
   * Endure keys in this dataset
   */
  MC_DS_ENDURE("MC_DS_ENDURE"),

  /**
   * Endure keys using seq no
   */
  MC_DS_ENDURESEQNO("MC_DS_ENDURESEQNO"),

  /**
   * Observe keys in this dataset
   */
  MC_DS_OBSERVE("MC_DS_OBSERVE"),
  /**
   * Verify key stats for the dataset
   */
  MC_DS_STATS("MC_DS_STATS"),

  /**
   * Pre-load documents in the cluster so that they can appear in the
   * specialized view.
   */
  CB_VIEW_LOAD("CB_VIEW_LOAD"),

  /**
   * Execute the query which shall yield documents loaded via
   * {@link #CB_VIEW_LOAD}
   */
  CB_VIEW_QUERY("CB_VIEW_QUERY"),

  /**
   * Load spatial dataset
   */
  CB_SPATIAL_LOAD("CB_SPATIAL_LOAD"),

  /**
   * Execute the bounding box query
   */
  CB_SPATIAL_QUERY("CB_SPATIAL_QUERY"),

  /**
   * Pre-load documents in the cluster so that they can appear in the
   * specialized n1ql.
   */
 CB_N1QL_LOAD("CB_N1QL_LOAD"),

  /**
   * Execute the n1ql query which shall yield documents loaded via
   * {@link #CB_N1QL_QUERY}
   */
 CB_N1QL_QUERY("CB_N1QL_QUERY"),

  /** create primary or secondary indexes on the bucket
   */
  CB_N1QL_CREATE_INDEX("CB_N1QL_CREATE_INDEX"),

  /** Load mutations for streaming dcp */
  CB_DCP_LOAD("CB_DCP_LOAD"),

  /** Stream dcp data */
  CB_DCP_STREAM("CB_DCP_STREAM"),

  /** Load json data for sub document change tests */
  MC_DS_SD_LOAD("MC_DS_SD_LOAD"),

  /** Run the json sub document changes */
  MC_DS_SD_RUN("MC_DS_SD_RUN"),

  /** FTS index creation */
  CB_FTS_LOAD("CB_FTS_LOAD"),

  /** FTS query */
  CB_FTS_QUERY("CB_FTS_QUERY"),

   /* Load json data for analytics workload */
   CB_AS_LOAD("CB_AS_LOAD"),

    /* Analytics queries */
    CB_AS_QUERY("CB_AS_QUERY"),

    /* Transaction Data Testing */
    TXN_BASIC_TEST("TXN_BASIC_TEST"),

    /* Transaction Create */
    TXN_CREATE("TXN_CREATE"),

    /* Transaction Load Data */
    TXN_LOAD_DATA("TXN_LOAD_DATA"),

    /* Transaction Data Update */
    TXN_DATA_UPDATE("TXN_DATA_UPDATE"),
  ;

  private final String value;

  Command(String val) {
    this.value = val;
  }

  @Override
  public final String toString() {
    return value;
  }
}
