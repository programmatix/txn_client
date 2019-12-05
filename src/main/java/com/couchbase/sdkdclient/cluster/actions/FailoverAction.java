/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster.actions;

import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.ClusterBuilder;
import com.couchbase.sdkdclient.cluster.NodeHost;
import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.options.OptionConsumer;
import com.couchbase.sdkdclient.options.OptionDomains;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.options.OptionTreeBuilder;
import com.couchbase.sdkdclient.scenario.FailoverScenario;
import com.couchbase.sdkdclient.scenario.Scenario;
import com.couchbase.sdkdclient.util.TimeUtils;
import org.slf4j.Logger;

import java.util.Collection;

/**
 * This class provides the implementation for failing over the cluster.
 * It allows the selection of one or more nodes to fail over, and also allows
 * the option of including the <i>EPT</i> node among those to be failed over.
 *
 * The action is first initialized by calling its {@link FailoverAction#setup}
 * method on a prepared cluster. At this stage it will select the nodes to fail
 * over and will throw an exception if there are insufficient nodes.
 *
 * Later on, the {@link Scenario} (or more specifically,
 * the {@link FailoverScenario} will invoke the {@link FailoverAction#change}
 * method which will actually perform the failover and any subsequent actions.
 */
public class FailoverAction implements OptionConsumer {
  protected final FailoverOptions options = new FailoverOptions();
  protected Collection<NodeHost> failedOver;
  protected Logger logger = LogUtil.getLogger(FailoverAction.class);

  @Override
  public OptionTree getOptionTree() {
    return new OptionTreeBuilder()
            .group("failover")
            .description("Options controlling failover")
            .source(options, FailoverOptions.class)
            .prefix(OptionDomains.FAILOVER)
            .build();
  }

  public void setup(ClusterBuilder clb) {
    failedOver = clb.getNodelistBuilder().reserveForRemoval(options.getNumNodes(),
                                                            options.shouldUseEpt(), false, options.getServices());
  }

  private void doNextAction(CBCluster cl) throws RestApiException, HarnessException {
    switch (options.getNextAction()) {
      case FO_EJECT_REBALANCE:
      case FO_EJECT:
        logger.info("Ejecting nodes after failover");
        cl.ejectNodes(failedOver);
        break;

      case FO_READD_REBALANCE:
      case FO_READD:
        logger.info("Readding nodes after failover");
        cl.reAddNodes(failedOver);
        break;

      case FO_REBALANCE:
        break; // Next pass

      case FO_NOACTION:
        logger.warn("No action requested after failed over. " +
                    "Cluster will be imbalanced");
        break;
    }

    switch (options.getNextAction()) {
      case FO_EJECT_REBALANCE:
      case FO_READD_REBALANCE:
      case FO_REBALANCE:
        logger.info("Rebalancing nodes after failover");
        try {
          cl.rebalanceCluster().get();
        } catch (Exception ex) {
          throw HarnessException.create(HarnessError.CLUSTER, ex);
        }
        break;

      default:
        break;
    }
  }

  public void change(CBCluster cl) throws HarnessException {
    try {
      cl.failoverNodes(failedOver);

      if (options.getNextAction() != FailoverOptions.NextAction.FO_NOACTION) {
        if (options.getGraceInterval() > 0) {
          logger.info("Nodes failed over. Requested sleep for {} seconds",
                      options.getGraceInterval());
          TimeUtils.sleepSeconds(options.getGraceInterval());
        }
        doNextAction(cl);
      }

    } catch (RestApiException ex) {
      throw new HarnessException(HarnessError.CLUSTER, ex);
    }
  }

  public String getDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append("Fail over ").append(options.getNumNodes()).append(" nodes");
    if (options.shouldUseEpt()) {
      sb.append(" (including the EPT)");
    }
    sb.append(". ");

    if (options.getNextAction() != FailoverOptions.NextAction.FO_NOACTION) {
      int sleepInterval = options.getGraceInterval();
      if (sleepInterval > 0) {
        sb.append("Then sleep for ").append(sleepInterval).append(" seconds and ");
      }

      switch (options.getNextAction()) {
        case FO_EJECT:
          sb.append("eject the nodes from the cluster");
          break;
        case FO_EJECT_REBALANCE:
          sb.append("eject the nodes from the cluster and rebalance");
          break;
        case FO_READD:
          sb.append("readd the nodes to the cluster");
          break;
        case FO_READD_REBALANCE:
          sb.append("readd the nodes to the cluster and rebalance");
          break;
        case FO_REBALANCE:
          sb.append("rebalance");
          break;
      }
    }
    return sb.toString();
  }
}