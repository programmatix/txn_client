/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.stester;

import com.couchbase.sdkdclient.cluster.ClusterBuilder;
import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.context.RunContext;
import com.couchbase.sdkdclient.logging.RunDBAppender;
import com.couchbase.sdkdclient.options.OptionConsumer;
import com.couchbase.sdkdclient.options.OptionInfo;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.options.OptionUtils;
import com.couchbase.sdkdclient.rundb.RunDB;
import com.couchbase.sdkdclient.util.Configurable;
import com.couchbase.sdkdclient.util.Configured;
import com.couchbase.sdkdclient.util.Unconfigured;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * This class is used to create a {@link RunContext}.
 * It gets into the more hairy parts of the configuration processing such
 * as parsing and sealing the plugins' sub-options.
 */
public class RunContextBuilder implements Configurable, OptionConsumer {
  private final ClusterBuilder clusterBuilder = new ClusterBuilder();
  private RunDB database = null;
  private boolean configured = false;
  private final HarnessBuilder stBuilder;
  private final List<OptionTree> trees;

  /**
   * Create a new object.
   *
   * @param hb A configured {@link HarnessBuilder}
   * object. This will be used to retrieve the selected plugins' sub-options
   * and build the database connection.
   *
   * @throws HarnessException
   */
  public RunContextBuilder(Configured<HarnessBuilder> hb) {
  //  System.out.println("Starting Printing from RCB");

    stBuilder = hb.get();
    ArrayList<OptionTree> ll = new ArrayList<OptionTree>();


   // System.out.println("getDriver");
   // printoptions(stBuilder.getDriver().getOptionTree().getAllOptionsInfo());

    ll.add(stBuilder.getScenario().getOptionTree());
   // System.out.println("getScenario");
   // printoptions(stBuilder.getScenario().getOptionTree().getAllOptionsInfo());

   // System.out.println("getWorkloadGroup");
          //ll.add(stBuilder.getWorkloadGroup().getOptionTree());
   // printoptions(stBuilder.getWorkloadGroup().getOptionTree().getAllOptionsInfo());

    ll.add(clusterBuilder.getOptionTree());
   // System.out.println("clusterBuilder");
   // printoptions(clusterBuilder.getOptionTree().getAllOptionsInfo());

   // System.out.println("Completed Printing from RCB");

    trees = Collections.unmodifiableList(ll);
  }

  @Override
  public boolean isConfigured() {
    return configured;
  }


  /**
   * Finalizes the options for this object. This will parse the plugin
   * sub-options and properly create the {@link RunDB} instance.
   */
  @Override
  public void configure() {
    System.out.println("Starting Printing from ConfiguredRCB");
    for (OptionTree tree : trees) {
      OptionUtils.sealTree(tree);
    }

    try {
      database = stBuilder.createDatabase();
    } catch (IOException ex) {
      throw HarnessException.create(HarnessError.CONFIG, ex);
    } catch (SQLException ex) {
      throw HarnessException.create(HarnessError.DB, ex);
    }

    Configured<ClusterBuilder> confCLB = Configured.create(clusterBuilder);
    stBuilder.getScenario().configure(confCLB);
    configured = true;
  }

  @Override
  public OptionTree getOptionTree() {
    OptionTree dummy = new OptionTree();
    for (OptionTree tree : trees) {
      dummy.addChild(tree);
    }
    return dummy;
  }

  public ClusterBuilder getClusterBuilder() {
    return clusterBuilder;
  }

  /**
   * Gets the database.
   * @return The database. Will be {@code null} if not yet {@link #configure()}d
   */
  RunDB getDatabase() {
    return database;
  }

  public RunContext buildContext() {
    // Set the logging stuff
    RunDBAppender.setDatabase(database);

    return new RunContext(database,
                          Configured.create(clusterBuilder),
                          Configured.create(stBuilder.getScenario()),
                          Unconfigured.create(stBuilder.getWorkloadGroup()));
  }

  public static void  printoptions(Collection<OptionInfo> optionsInfo ){
    Iterator<OptionInfo> itr = optionsInfo.iterator();
    while(itr.hasNext()){
      OptionInfo op= itr.next();
      System.out.println("Option Info Name: "+op.getOption().getName());
      System.out.println("Option Info Value: "+op.getOption().getCurrentRawValue());

    }
  }
}
