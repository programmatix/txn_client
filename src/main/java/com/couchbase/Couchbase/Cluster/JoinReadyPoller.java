package com.couchbase.Couchbase.Cluster;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import com.couchbase.Couchbase.Couchbase.CouchbaseAdmin;
import com.couchbase.Couchbase.Nodes.Node;
import com.couchbase.Couchbase.Nodes.NodeHost;
import com.couchbase.Exceptions.HarnessError;
import com.couchbase.Exceptions.HarnessException;
import com.couchbase.Exceptions.RestApiException;
import com.couchbase.Logging.LogUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Waits until the nodes specified are ready to be joined.
 * This is a workaround for MB-6060 and MB-7038
 */
public class JoinReadyPoller implements Callable<Boolean> {
  final static Logger logger = LogUtil.getLogger(JoinReadyPoller.class);
  final CouchbaseAdmin adm;
  final int seconds;

  @Override
  public Boolean call() throws RestApiException, InterruptedException {
    new ClusterConfigure.RestRetryer(seconds) {
      protected boolean tryOnce() throws RestApiException {
        Node n;
        try {
          n = new Node(adm.getJson(CouchbaseAdmin._P_NODES_SELF).getAsJsonObject());
          logger.trace(String.format("Compat version: 0x%x", n.getClusterCompatVersion()));
        } catch (IOException ex) {
          throw new RestApiException(ex);
        }

        if (n.getClusterCompatMajorVersion() > 0) {
          return true;
        }

        return false;
      }
    }.call();
    return true;
  }

  private JoinReadyPoller(CouchbaseAdmin admin, int seconds) {
    adm = admin;
    this.seconds = seconds;
  }

  public static void poll(Collection<NodeHost> nodes, int nSeconds)  {
    if (nodes.isEmpty()) {
      return;
    }
    List<CouchbaseAdmin> cNodes = new LinkedList<CouchbaseAdmin>();
    for (NodeHost nn : nodes) {
      cNodes.add(nn.getAdmin());
    }

    ExecutorService svc = Executors.newFixedThreadPool(cNodes.size());
    List<Future<Boolean>> futures = new LinkedList<Future<Boolean>>();

    for (CouchbaseAdmin adm : cNodes) {
      FutureTask<Boolean> task = new FutureTask<Boolean>(new JoinReadyPoller(adm, nSeconds));
      futures.add(task);
      svc.execute(task);
    }

    // Wait
    svc.shutdown();
    try {
      for (Future ft : futures) {
        ft.get();
      }
    } catch (ExecutionException ex) {
      throw HarnessException.create(HarnessError.CLUSTER, ex);
    } catch (InterruptedException ex) {
      throw HarnessException.create(HarnessError.CLUSTER, ex);
    }
  }
}