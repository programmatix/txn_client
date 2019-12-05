/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.workload;

import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.handle.Handle;
import com.couchbase.sdkdclient.handle.HandleFactory;
import com.couchbase.sdkdclient.logging.LogUtil;

import com.couchbase.sdkdclient.util.Configurable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A workload is the configuration behind the SDKD commands. It creates
 * SDKD handles and instructs them on the type and frequency of the operations
 * to perform.
 *
 * A workload is contained under a {@link WorkloadGroup} to allow abstraction
 * over the specifics of the SDKD commands and configuration, and also to allow
 * multiple workloads to operate under a single group.
 */
public abstract class Workload implements Configurable {

  final static public String NAME_KV = "mc";
  final static public String NAME_EPHM = "ephm";
  final static public String NAME_CB = "cb";
  final static public String NAME_HT = "ht";
  final static public String NAME_SPATIAL = "spatial";
  final static public String NAME_N1QL = "n1ql";
  final static public String NAME_SUBDOC = "sd";
  final static public String NAME_FTS = "fts";
  final static public String NAME_CBAS = "cbas";

  private final String name;
  protected final Logger logger = LogUtil.getLogger(Workload.class);
  protected HandleFactory handleMaker;
  private final List<Handle> allHandles = new ArrayList<Handle>();
  private final Object lsnLock = new Object();
  private final Object llLock = new Object();
  private boolean configured = false;

  /**
   * Create a single workload. A workload acts as a single measurable
   * performance unit
   *
   * @param wlname The unique ID by which the workload will be known in its
   * group
   */
  public Workload(String wlname) {
    name = wlname;
  }



  public void configure(HandleFactory hf) {
    handleMaker = hf;
    configured = true;
  }

  @Deprecated
  @Override
  public void configure() {
    throw new UnsupportedOperationException("Use configure(HandleFactory)");
  }

  @Override
  public boolean isConfigured() {
    return configured;
  }

  public String getName() {
    return name;
  }



  public abstract void start() throws IOException;

  public abstract void beginRebound() throws IOException;

  public void load() throws IOException {

  }



}