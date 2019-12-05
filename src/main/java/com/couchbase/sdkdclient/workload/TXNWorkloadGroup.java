/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.workload;

import com.couchbase.sdkdclient.options.OptionTree;


/**
 * A workload group that encapsulates a single {@link KeyValueWorkload}
 */
public class TXNWorkloadGroup extends WorkloadGroup {

  private final TXNWorkload wl = new TXNWorkload("txn");
  {
    System.out.println("After TXNWorkloadGroup:");
    addWorkload(wl);
  }


  @Override
  public OptionTree getOptionTree() {
    return wl.getOptionTree();
  }
}
