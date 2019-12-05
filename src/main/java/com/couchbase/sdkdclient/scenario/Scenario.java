/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.scenario;

import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.ClusterBuilder;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.options.OptionConsumer;
import com.couchbase.sdkdclient.util.*;
import com.couchbase.sdkdclient.workload.WorkloadGroup;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * A scenario defines the interaction between a {@link WorkloadGroup} and
 * a {@link CBCluster}. In truth, a scenario is free to do anything, and is
 * the main sequence flow behind the test.
 *
 * See subclasses for more details about each scenario. Particularly, see
 * the {@link PhasedScenario} class for more information.
 */
public abstract class Scenario implements OptionConsumer, Configurable {
  protected ScenarioListener listener = null;
  protected static Logger logger = LogUtil.getLogger(Scenario.class);
  private boolean configured = false;

  /**
   * @param cluster
   * @param wg
   * @throws IOException
   */
  public abstract void run(CBCluster cluster, WorkloadGroup wg) throws IOException;

  /**
   * Gets a human readable description based on the configured scenario.
   * @return A description of the scenario.
   */
  public abstract String getConfiguredDescription();


  public void configure(Configured<ClusterBuilder> clb) {
    doConfigure(clb.get());
    configured = true;
  }

  @Override
  public boolean isConfigured() {
    return configured;
  }

  @Deprecated
  @Override
  public void configure() {
    throw new UnsupportedOperationException("Use configure(ClusterBuilder)");
  }

  /**
   * Sets up the cluster itself
   */
  @NoNetworkIO
  protected abstract void doConfigure(ClusterBuilder clb) throws HarnessException;

  public void setListener(ScenarioListener lsn) {
    listener = lsn;
  }

  private static final String SCPKG = "com.couchbase.sdkdclient.scenario";


  public static Scenario load(String name) {
    PluginLoader<Scenario> pl = new PluginLoader<Scenario>(Scenario.class, SCPKG);
    pl.addAlias("rebalance.once", RebalanceScenario.class);
    pl.addAlias("rebalance", RebalanceScenario.class);
    pl.addAlias("upgrade", UpgradeScenario.class);
    pl.addAlias("base.raw", BasicScenario.class);
    pl.addAlias("basic", BasicScenario.class);
    pl.addAlias("default", BasicScenario.class);
    pl.addAlias("passthrough", BasicScenario.class);
    pl.addAlias("failover.once", FailoverScenario.class);
    pl.addAlias("failover", FailoverScenario.class);
    pl.addAlias("svc", ServiceFailureScenario.class);
    pl.addAlias("servicefailure", ServiceFailureScenario.class);
    pl.addAlias("indexfailover", IndexFailoverScenario.class);

    try {
      return pl.getPluginInstance(name);
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
  }

}