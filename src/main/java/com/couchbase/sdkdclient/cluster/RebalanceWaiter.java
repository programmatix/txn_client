/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster;

import com.couchbase.cbadmin.client.CouchbaseAdmin;
import com.couchbase.cbadmin.client.RebalanceInfo;
import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.logging.LogUtil;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;

/**
 *
 * @author mnunberg
 */
public class RebalanceWaiter implements Callable<Boolean> {
  final static Logger logger = LogUtil.getLogger(RebalanceWaiter.class);
  final CouchbaseAdmin admin;
  int pollInterval = 1;

  boolean sweepOnce() throws RestApiException, ClusterException {
    RebalanceInfo info = admin.getRebalanceStatus();

    if (info.isComplete()) {
      if (info.isStopped()) {
        logger.error("Rebalance abnormally stopped");
      } else {
        logger.info("Rebalance complete");
      }
      return true;
    }

    logger.debug("Rebalance Progress: {}%", (int)info.getProgress());
    return false;
  }

  @Override
  public Boolean call() throws RestApiException, ClusterException {
    while (!sweepOnce()) {
      try {
        Thread.sleep(pollInterval * 1000);
      } catch (InterruptedException ex) {
        throw new ClusterException(ex);
      }
    }
    return true;
  }

  private RebalanceWaiter(CouchbaseAdmin adm) {
    admin = adm;
  }

  public static Future<Boolean> poll(CouchbaseAdmin adm) {
    RebalanceWaiter waiter = new RebalanceWaiter(adm);
    ExecutorService svc = Executors.newSingleThreadExecutor();
    Future<Boolean> ft = svc.submit(waiter);
    svc.shutdown();
    return ft;
  }
}