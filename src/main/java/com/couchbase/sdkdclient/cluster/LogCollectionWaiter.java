package com.couchbase.sdkdclient.cluster;

import com.couchbase.cbadmin.client.CouchbaseAdmin;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

/**
 * author subhashni
 */
public class LogCollectionWaiter implements Callable<Boolean> {
  private CouchbaseAdmin admin = null;
  private int pollInterval = 5;

  private Boolean isComplete() throws Exception {
    if (admin.getCBCollectionStatus() == null) {
      return false;
    }
    if (admin.getCBCollectionStatus().compareToIgnoreCase("completed") == 0) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Boolean call() throws Exception {
    while (!isComplete()) {
      try {
        Thread.sleep(pollInterval * 1000);
      } catch (InterruptedException ex) {
        throw new ClusterException(ex);
      }

    }


    return true;
  }

  private LogCollectionWaiter(CouchbaseAdmin adm) {
    this.admin = adm;
  }


  public static Future<Boolean> poll(CouchbaseAdmin adm) {
    LogCollectionWaiter waiter = new LogCollectionWaiter(adm);
    ExecutorService  svc = Executors.newFixedThreadPool(1);
    Future<Boolean> future = svc.submit(waiter);
    svc.shutdown();
    return future;
  }
}
