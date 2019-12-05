/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster.actions;

import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.*;
import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.ssh.SimpleCommand;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * @author akshata.trivedi
 */
public class RebalanceRefreshAction implements RebalanceAction {
  String services;
  Boolean ignoreRebalance;

  private ArrayList<NodeHost> toRefresh = new ArrayList<NodeHost>();
  private final static Logger logger = LogUtil.getLogger(RebalanceRefreshAction.class);

  public RebalanceRefreshAction(String service, Boolean ignoreRebalance) {
      this.services = service;
      this.ignoreRebalance = ignoreRebalance;
  }

  @Override
  public void setup(NodelistBuilder nlb, RebalanceConfig config) {
    toRefresh.addAll(nlb.reserveForRemoval(config.getNumNodes(), true, false, services));
  }

  @Override
  public Future<Boolean> start(CBCluster cluster) throws RestApiException, ExecutionException,InterruptedException {
    return RefreshWaiter.poll(cluster, toRefresh, services, ignoreRebalance);
  }

  @Override
  public Future<Boolean> undo(CBCluster  cluster) throws RestApiException {
    return null;
  }

  public static class RefreshWaiter implements Callable<Boolean> {
    final CBCluster cluster;
    final List<NodeHost> nodesReservedForRefresh;
    String services;
    Boolean ignoreRebalance;

    private void runCommand(String cmd, NodeHost nn) throws IOException {
      Future<SimpleCommand> ft = RemoteCommands.runCommand(nn.getSSH(), cmd);
      try {
        SimpleCommand res = ft.get();
        if (!res.isSuccess()) {
          logger.warn("Command {} failed. {}, {}",
                  cmd, res.getStderr(), res.getStdout());
        }
      } catch (Exception ex) {
        throw new IOException(ex);
      }
    }

    @Override
    public Boolean call() throws RestApiException, ClusterException, ExecutionException, InterruptedException {
      for(NodeHost nn:nodesReservedForRefresh) {
        if (!ignoreRebalance) {
          cluster.removeAndRebalance(Arrays.asList(nn)).get();
        }

        try {
          runCommand("service couchbase-server restart", nn);
          logger.info("Waiting 30 seconds for server to startup: " + nn.getHostname());
          Thread.sleep(30000);
        } catch (IOException ex) {
          throw  new ExecutionException(ex);
        }

        if (!ignoreRebalance) {
          cluster.addAndRebalance(Arrays.asList(nn), services).get();
        }
      }
      return true;
    }

    private RefreshWaiter(CBCluster cluster, List<NodeHost> nodesReservedForRefresh, String services,
                          Boolean ignoreRebalance) {
      this.cluster = cluster;
      this.nodesReservedForRefresh = nodesReservedForRefresh;
      this.services = services;
      this.ignoreRebalance = ignoreRebalance;
    }

    public static Future<Boolean> poll(CBCluster cluster, List<NodeHost> nodesReservedForRefresh, String services, Boolean ignoreRebalance) {
      RefreshWaiter waiter = new RefreshWaiter(cluster, nodesReservedForRefresh, services, ignoreRebalance);
      ExecutorService svc = Executors.newSingleThreadExecutor();
      Future<Boolean> ft = svc.submit(waiter);
      svc.shutdown();
      return ft;
    }
  }

}



