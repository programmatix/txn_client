/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.context;

import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.sdkdclient.cluster.CBCluster;
import com.couchbase.sdkdclient.cluster.ClusterBuilder;

import com.couchbase.sdkdclient.handle.HandleFactory;
import com.couchbase.sdkdclient.options.OptionInfo;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.rundb.MiscEntry;
import com.couchbase.sdkdclient.rundb.RunDB;
import com.couchbase.sdkdclient.rundb.RunEntry;
import com.couchbase.sdkdclient.scenario.Scenario;

import com.couchbase.sdkdclient.util.Configured;
import com.couchbase.sdkdclient.util.Unconfigured;
import com.couchbase.sdkdclient.workload.WorkloadGroup;
import com.couchbase.sdkdclient.batch.BRun;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.couchbase.sdkdclient.options.RawOption;


import java.util.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import java.sql.Timestamp;

public class RunContext {
  protected final CBCluster cluster;
  protected final WorkloadGroup wlGroup;
  protected final Scenario scenario;
  protected final String brunCmd;
  protected RunDB database;
  protected RunEntry runEntry;

  static Logger logger = LoggerFactory.getLogger(RunContext.class);

  public RunContext(RunDB db,
                    Configured<ClusterBuilder> clb,
                    Configured<Scenario> sc,
                    Unconfigured<WorkloadGroup> wg) {
    database = db;
    scenario = sc.get();
    brunCmd = BRun.getCurrStesterCmd();
    wlGroup = wg.get();
    cluster = new CBCluster(clb);
    runEntry = db.getEntry();
  }

  private static void waitFuture(Future f, HarnessError err) {
    try {
      f.get();
    } catch (InterruptedException ex) {
      throw HarnessException.create(err, ex);
    } catch (ExecutionException ex) {
      throw HarnessException.create(err, ex);
    }
  }



  /**
   * Sets up the cluster, stars the scenario, and outputs the results somewhere.
   */
  public void run() throws HarnessException {
    try {
      Future ftDriver;
      Future ftCluster;
      ExecutorService svc = Executors.newFixedThreadPool(2);

      logger.info("Starting cluster and driver");
      ftDriver = svc.submit(new Callable() {
        @Override
        public Object call() throws Exception {
          logger.info("Trying to start Driver");
        //  driver.start();
          logger.info("Driver started");
         // Map<String, Object> driverInfo = driver.getInfo();
         /// database.addProperty(MiscEntry.K_SDKDINFO, new Gson().toJson(driverInfo));
          logger.info("Driver1");

          Map<String, Object> TestInfo = new HashMap<String, Object>();
          if (scenario.getClass().toString().contains("ServiceFailureScenario")) {
            TestInfo.put("Scenario", "ServiceFailureScenario");
            OptionTree SvcFailureOptTree = null;
            Object[] optTreeArr = scenario.getOptionTree().getChildren().toArray();
            for (Object optTree : optTreeArr) {
              if (((OptionTree) optTree).getDescription().contains("service")) {
                SvcFailureOptTree = (OptionTree) optTree;
              }
            }
            RawOption option = SvcFailureOptTree.find("count");
            TestInfo.put("ServiceFailureCount", option.getCurrentRawValue());
          }

          if (scenario.getClass().toString().contains("RebalanceScenario")) {
            TestInfo.put("Scenario", "RebalanceScenario");
            OptionTree RebalanceOptTree = null;
            Object[] optTreeArr = scenario.getOptionTree().getChildren().toArray();
            for (Object optTree : optTreeArr) {
              if (((OptionTree) optTree).getDescription().contains("rebalance")) {
                RebalanceOptTree = (OptionTree) optTree;
              }
            }
            RawOption option = RebalanceOptTree.find("mode");
            TestInfo.put("RebalanceMode", option.getCurrentRawValue());
            option = RebalanceOptTree.find("count");
            TestInfo.put("RebalanceCount", Integer.getInteger(option.getCurrentRawValue()));
            option = RebalanceOptTree.find("services");
            TestInfo.put("RebalanceServices", option.getCurrentRawValue());

          }

          if (scenario.getClass().toString().contains("FailoverScenario") &&
                  !scenario.getClass().toString().contains("IndexFailoverScenario")) {
            TestInfo.put("Scenario", "FailoverScenario");
            OptionTree FailoverOptTree = null;
            Object[] optTreeArr = scenario.getOptionTree().getChildren().toArray();
            for (Object optTree : optTreeArr) {
              if (((OptionTree) optTree).getDescription().contains("failover")) {
                FailoverOptTree = (OptionTree) optTree;
              }
            }
            RawOption option = FailoverOptTree.find("count");
            TestInfo.put("FailoverCount", option.getCurrentRawValue());
            option = FailoverOptTree.find("services");
            TestInfo.put("FailoverServices", option.getCurrentRawValue());
          }


          RawOption option = wlGroup.getOptionTree().find("batchsize");
          TestInfo.put("BatchSize", option != null ? option.getCurrentRawValue() : "1");
          option = wlGroup.getOptionTree().find("opmode");
          if (option != null) {
            TestInfo.put("OpMode", option.getCurrentRawValue());
          }

          String workloadClass = wlGroup.getClass().toString();
          String workloadGroup = workloadClass.substring(workloadClass.lastIndexOf(".") + 1, workloadClass.length());

          Collection<OptionInfo> coll = wlGroup.getOptionTree().getAllOptionsInfo();
          Iterator<OptionInfo> it = coll.iterator();

          while (it.hasNext()) {
            OptionInfo opt = it.next();
            TestInfo.put(opt.getOption().getName().toLowerCase().replace(".", ""), opt.getOption().getCurrentRawValue());
          }
          String workload;
          if (workloadGroup.compareToIgnoreCase("GetSetWorkloadGroup") == 0) {
            workload = "KV";
          } else if (workloadGroup.compareToIgnoreCase("HybridWorkloadGroup") == 0) {
            workload = "KV+VIEW";
          } else if (workloadGroup.compareToIgnoreCase("N1QLWorkloadGroup") == 0) {
            workload = "N1QL";
          } else if (workloadGroup.compareToIgnoreCase("ViewWorkloadGroup") == 0) {
            workload = "VIEW";
          } else if (workloadGroup.compareToIgnoreCase("N1QLHybridWorkloadGroup") == 0) {
            workload = "KV+N1QL";
          } else if (workloadGroup.compareToIgnoreCase("SpatialWorkloadGroup") == 0) {
            workload = "SPATIAL";
          } else if (workloadGroup.compareToIgnoreCase("FTSWorkloadGroup") == 0) {
            workload = "FTS";
          } else if (workloadGroup.compareToIgnoreCase("SubdocWorkloadGroup") == 0) {
            workload = "SUBDOC";
          } else if (workloadGroup.compareToIgnoreCase("AnalyticsWorkloadGroup") == 0) {
            workload = "CBAS";
          } else if (workloadGroup.compareToIgnoreCase("EphemeralWorkloadGroup") == 0) {
            workload = "EPHM";
          } else {
            workload = "OTHER";
          }
          TestInfo.put("Workload", workload);
        //  TestInfo.put("SDK", driverInfo.get("SDK"));
         // TestInfo.put("SDKVersion", driverInfo.get("SDKVersion"));
          //TestInfo.put("SDKChangeset", driverInfo.get("CHANGESET"));
          //TestInfo.put("SDKCoreChangeset", driverInfo.get("CORECHANGESET"));
          database.addProperty(MiscEntry.K_TESTINFO, new Gson().toJson(TestInfo));
          return null;
        }
      });

      database.addPropertyQuietly(MiscEntry.K_BEGINTIME, String.valueOf(((new Timestamp(System.currentTimeMillis()/1000).getTime()))));

      ftCluster = svc.submit(new Callable() {
        @Override
        public Object call() throws Exception {
          cluster.startCluster();
          return null;
        }
      });


      waitFuture(ftDriver, HarnessError.DRIVER);
      System.out.println("Await for driver is over");
      waitFuture(ftCluster, HarnessError.CLUSTER);
      logger.info("Driver and cluster initialized");

      HandleFactory hf = new HandleFactory(cluster.createHandleOptions(), cluster);
      wlGroup.configure(hf);

      //logger.info(scenario.getConfiguredDescription());
      database.addPropertyQuietly(MiscEntry.K_SCDESCRIPTION,
              //"THIS IS MY DESCRIPTION VALUE");
              scenario.getConfiguredDescription());

      database.addPropertyQuietly(MiscEntry.K_SCRUNCMD,
              brunCmd);
              //"Run Command Value");





      try {
        String clVersion = cluster.getClusterVersion();
        database.addProperty(MiscEntry.K_CLINFO, clVersion);
      } catch (RestApiException ex) {
        logger.warn("Couldn't get cluster version", ex);
      } catch (SQLException ex) {
        logger.error("Couldn't write cluster version to DB", ex);
      }

/*
      final DriverPoller dPoller;
      try {
        dPoller = driver.getPoller();
      } catch (IOException ex) {
        throw new HarnessException(HarnessError.DRIVER, ex);
      }
*/
      CompletionService<Void> compsvc = new ExecutorCompletionService<Void>(svc);
      final AtomicBoolean scenarioReady = new AtomicBoolean(false);

      compsvc.submit(new Callable<Void>() {
        @Override
        public Void call() {
          logger.info("Running scenario..");
          try {
            scenario.run(cluster, wlGroup);
          } catch (Exception ex) {
            throw HarnessException.create(HarnessError.SCENARIO, ex);
          } finally {
            scenarioReady.set(true);
            /*
            if (dPoller != null) {
              dPoller.cancel();
            }

             */
          }
          return null;
        }
      });

/*
      compsvc.submit(new Callable<Void>() {
        @Override
        public Void call() {
          if (dPoller == null) {
            return null; // Not supported..
          }

         DriverPoller.Status status = dPoller.waitExit();
          if (!scenarioReady.get()) {
            if (status.isOk()) {
              throw new HarnessException(HarnessError.DRIVER, "Driver crashed");
            } else {
              throw new HarnessException(HarnessError.DRIVER, status.getCause());
            }
          }
          return null;
        }
      });
*/
      svc.shutdown();

      for (int i = 0; i < 2; i++) {
        try {
          Future f = compsvc.take();
          f.get();
        } catch (ExecutionException ex) {
          throw HarnessException.create(HarnessError.GENERIC, ex);
        } catch (InterruptedException ex) {
          throw HarnessException.create(HarnessError.GENERIC, ex);
        }
      }

      database.addPropertyQuietly(MiscEntry.K_ENDTIME, String.valueOf(((new Timestamp(System.currentTimeMillis()/1000).getTime()))));
    } catch (HarnessException ex) {
      logger.debug("Caught exception in run {}", ex);
      try {
        cluster.getLogs();
      } catch (Exception logex) {
        throw new HarnessException(HarnessError.CLUSTER, logex);
      }
      throw ex;
    }
  }
}
