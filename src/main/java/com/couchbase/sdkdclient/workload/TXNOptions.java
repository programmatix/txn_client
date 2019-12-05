/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.workload;

import com.couchbase.sdkdclient.options.*;

public class TXNOptions implements OptionConsumer, WorkloadOptions {
  private IntOption nodes_init =
          OptBuilder.startInt("nodes_init")
          .help("NUMBER OF NODES OF CLUSTER")
          .defl("1")
          .build();

  private IntOption replicas =
          OptBuilder.startInt("replicas")
                  .help("Number of replicas of bucket")
                  .defl("1")
                  .build();

  private BoolOption transaction_commit
          = OptBuilder.startBool("transaction_commit")
          .help("Commit after transaction")
          .defl("true")
          .build();

  private StringOption op_type
          = OptBuilder.startString("op_type")
          .help("Commit after transaction")
          .defl("create")
          .build();

  private StringOption GROUP
          = OptBuilder.startString("GROUP")
          .help("GROUP")
          .defl("P0")
          .build();

  private StringOption OS_type
          = OptBuilder.startString("OS_type")
          .help("OS_type")
          .defl("windows")
          .build();

  private IntOption nthreads
          = OptBuilder.startInt("nthreads")
          .help("Threads to run")
          .defl("10")
          .build();

  private IntOption timeres
          = OptBuilder.startInt("timeres")
          .help("Time resolution output")
          .defl("1")
          .build();

  private IntOption totaldocs
          = OptBuilder.startInt("totaldocs")
          .help("Total documents to be inserted")
          .defl("100")
          .build();

  private IntOption batchsize
          = OptBuilder.startInt("batchsize")
          .help("Documents to be inserted per each txn")
          .defl("10")
          .build();



  private IntOption poolsize = OptBuilder.startInt("reqpool_size")
          .help("Thread pool size for sending requests. This is internal to " +
                  "the SDKD client")
          .defl("10")
          .build();

    private IntOption durability = OptBuilder.startInt("durability")
            .help("Durability parameter for the txn")
            .defl("1")
            .build();

  private IntOption timeout = OptBuilder.startInt("timeout")
          .help("timeout parameter for the txn")
          .defl("10")
          .build();



  @Override
  public OptionTree getOptionTree() {
    return new OptionTreeBuilder()
            .group("views")
            .description("Option controlling the views workload")
            .prefix(OptionDomains.TXN)
            .source(this, TXNOptions.class)
            .build();
  }

  public String getOsType() {
    return OS_type.getValue();
  }

  public String getGroup() {
    return GROUP.getValue();
  }

  public String getOpType() {
    return op_type.getValue();
  }

  public Integer getdurability() {
    return durability.getValue();
  }

  public boolean gettransaction_commit() {
    return transaction_commit.getValue();
  }

  public int gettotaldocs() {
    return totaldocs.getValue();
  }

  public int getbatchsize() {
    return batchsize.getValue();
  }


  public int getreplicas() {
    return replicas.getValue();
  }

  public int getnodes_init() {
    return nodes_init.getValue();
  }

  public int getNumThreads() {
    return nthreads.getValue();
  }


  @Override
  public int getThreadpoolSize() {
    return poolsize.getValue();
  }

  @Override
  public int getTimeResolution() {
    return timeres.getValue();
  }


}
