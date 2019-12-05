/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.workload;

import com.couchbase.sdkdclient.handle.HandleFactory;
import com.couchbase.sdkdclient.options.OptionConsumer;
import com.couchbase.sdkdclient.util.Configurable;
import com.couchbase.sdkdclient.util.PluginLoader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A group representing one or more workloads. Workload groups act as an
 * abstraction layer around a {@link Workload}.
 */
public abstract class WorkloadGroup implements OptionConsumer, Configurable {
  final Map<String, Workload> workloads = new HashMap<String, Workload>();
  private boolean configured = false;

  /**
   * Start each workload in the group. This should be called <i>after</i>
   * {@link #configure(com.couchbase.sdkdclient.handle.HandleFactory)}.
   * @throws IOException
   */
  public void start() throws IOException {
    for (Workload wl : workloads.values()) {
      wl.load();
    }
    for (Workload wl : workloads.values()) {
      wl.start();
    }
  }

  public void beginRebound() throws IOException {
    for (Workload wl : workloads.values()) {
      wl.beginRebound();
    }
  }
  /**
   * Prepares each workload, validating its options.
   */
  public void configure(HandleFactory hf) {
    configured = true;
    for (Workload wl : workloads.values()) {
      wl.configure(hf);

    }
  }

  @Deprecated
  @Override
  public void configure() {
    throw new UnsupportedOperationException("Use configure(HandleFactory(");
  }

  @Override
  public final boolean isConfigured() {
    return configured;
  }

  public void addWorkload(Workload wl) {
    workloads.put(wl.getName(), wl);
  }



  static final String WLPKG = "com.couchbase.sdkdclient.workload";
  public static WorkloadGroup findGroup(String name) {
    PluginLoader<WorkloadGroup> pl = new PluginLoader<WorkloadGroup>(WorkloadGroup.class, WLPKG);
    try {
      return pl.getPluginInstance(name);
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
  }
}
