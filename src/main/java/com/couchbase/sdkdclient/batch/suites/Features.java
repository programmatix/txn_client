/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.batch.suites;

/**
 * In addition to the base definitions, a test may also have some additional
 * features which control how a bucket is created or a workload is executed.
 */
public enum Features {

  /**
   * The test should be executed with the {@link com.couchbase.sdkdclient.workload.HybridWorkloadGroup} rather
   * than the default {@link com.couchbase.sdkdclient.workload.GetSetWorkloadGroup}
   */
  HYBRID(0x01),

  /**
   * The bucket should require a password
   */
  SASL(0x02),

  /**
   * The bucket should be a memcached bucket.
   */
  MEMD(0x04),

  /**
   * The test should execute spatial workload group
   */
  SPATIAL(0x08),

  /**
  * The test should execute n1ql workload group
  */
  N1QL(0x20),

  /**
   * The test should execute view workload group
   */
  VIEW(0x10),

  /**
   * The test should execute n1ql workload group
   */
  N1QLHYBRID(0x40),

  /**
   * The test should execute dcp streaming
   */
  DCP(0x80),
  /**
   * The test should execute subdoc workload group
   */
  SUBDOC(0x100),

    /**
     * The test should execute FTS workload group
     */
    FTS(0x200),

  /**
   * The test should execute Analytics workload group
   */
  CBAS(0x400),

  /**
   * The bucket should be a ephemeral bucket.
   */
  EPHM(0x800),

  /**
   * The test should be transactions
   */
  TXN(0x1000)
  ;

  final int value;

  Features(int val) {
    value = val;
  }

  /**
   * Checks whether a given integer contains this feature.
   * e.g. {@code Features.SASL.is(val); }
   * @param val The value to check
   * @return True if the value has this feature, false otherwise
   */
  boolean is(int val) {
    return (val & value) != 0;
  }

  /**
   * Return a stringified version of this featureset. This concatenates
   * the string version of each constant found to match {@code vv}
   * @param vv the featureset to match against.
   * @return The string describing the featureset.
   */
  public static String makeName(int vv) {
    StringBuilder sb = new StringBuilder();
    if (TXN.is(vv)) {
      sb.append("TXN");
    }
    if (HYBRID.is(vv)) {
      sb.append("HYBRID");
    }
    if (SASL.is(vv)) {
      sb.append("SASL");
    }
    if (MEMD.is(vv)) {
      sb.append("MEMD");
    }
    if (SPATIAL.is(vv)) {
      sb.append("SPATIAL");
    }
    if (N1QL.is(vv)) {
        sb.append("N1QL");
    }
    if (N1QLHYBRID.is(vv)) {
      sb.append("N1QLHYBRID");
    }
    if (DCP.is(vv)) {
      sb.append("DCP");
    }
    if (SUBDOC.is(vv)) {
      sb.append("SUBDOC");
    }
    if (FTS.is(vv)) {
      sb.append("FTS");
    }
    if (CBAS.is(vv)) {
      sb.append("CBAS");
    }
    if (EPHM.is(vv)) {
      sb.append("EPHM");
    }

    String s = sb.toString();
    if (s.isEmpty()) {
      return "KV";
    }
    return s;
  }
}
