/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.stester;

import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.context.RunContext;
import com.couchbase.sdkdclient.options.*;
import com.couchbase.sdkdclient.rundb.DatabasePath;
import com.couchbase.sdkdclient.rundb.RunDB;
import com.couchbase.sdkdclient.rundb.RunDB.Format;
import com.couchbase.sdkdclient.scenario.Scenario;
import com.couchbase.sdkdclient.util.CallOnceSentinel;
import com.couchbase.sdkdclient.util.Configurable;
import com.couchbase.sdkdclient.workload.WorkloadGroup;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * This class contains the options for the harness itself. Here you may
 * configure which plugins to load and which database path to log to.
 *
 * When finished configuring these options, you should construct a
 * {@link RunContextBuilder} which will further refine these options into
 * something that can create a {@link RunContext} object.
 */
public class HarnessBuilder implements OptionConsumer, Configurable {
  private final StringOption testcase = OptBuilder.startString("scenario")
          .alias("testcase")
          .shortAlias("c")
          .defl("basic")
          .argName("PLUGIN")
          .help("Scenario to use")
          .build();

  private final StringOption sdkd = OptBuilder.startString("sdkd-config")
          .help("SDKD configuration to use. This may either be a path " +
                        "to a known plugin or a host:port combination")
          .required()
          .shortName("C")
          .argName("PLUGIN or HOST:PORT")
          .alias("sdkd")
          .build();

  private final StringOption output =
          OptBuilder.<StringOption>start(new StringOption("output"))
          .help("Output file for logging database")
          .shortName("o")
          .argName("OUTPUT_FILE")
          .defl("sdkdclient.logdb")
          .build();

  private final BoolOption overrideOutput =
          OptBuilder.startBool("overwrite-output")
          .help("Overwrite existing logfile, if it exists")
          .help("Overwrite existing logfile, if it exists")
          .alias("override-output")
          .defl("false")
          .build();

  private final BoolOption noOutput =
          OptBuilder.startBool("no-log")
          .alias("no-output")
          .help("Don't write to a logfile")
          .defl("false")
          .build();

  private final StringOption workload =
          OptBuilder.startString("workload")
          .help("Workload group to use. This should be a Java class")
          .defl("GetSetWorkloadGroup")
          .argName("PLUGIN")
          .shortName("W")
          .build();

  @SuppressWarnings("unused")
  private final MultiOption comment =
          OptBuilder.startMulti("comment")
          .help("This is ignored. It may be used in argfiles for comments")
          .build();
  // </editor-fold>

  private final OptionTree tree;
  private Scenario scenario = null;
  private WorkloadGroup wlGroup = null;
  private DatabasePath dbPath = null;
  private boolean configured = false;

  public OptionTree getOptionTree() {
    return tree;
  }

  /**
   * Gets the loaded scenario.
   * @return The scenario. Must be called after {@link #configure()}
   */
  public Scenario getScenario() {
    return scenario;
  }

  /**
   * Gets the loaded workload group.
   * @return The workload group. Should be called after {@link #configure()}
   */
  public WorkloadGroup getWorkloadGroup() {
    return wlGroup;
  }



  public DatabasePath getDatabasePath() {
    return dbPath;
  }

  RunDB createDatabase() throws SQLException, IOException {
    return RunDB.createWriter(dbPath);
  }


  public HarnessBuilder() {
    OptionTreeBuilder otb = new OptionTreeBuilder();
    tree = otb.source(this, HarnessBuilder.class)
            .description("These options define the locations of plugins and logfiles")
            .group("main")
            .build();
  }


  private DatabasePath makeDbPath() throws SQLException, IOException {
    if (noOutput.getValue()) {
      return DatabasePath.createMemory(Format.H2);
    }
    File dbOut = new File(output.getValue());
    boolean overwrite;

    if (overrideOutput.getValue()) {
      overwrite = true;
    } else if (overrideOutput.wasPassed() == false && output.wasPassed() == false) {
      overwrite = true;
    } else {
      overwrite = false;
    }

    return DatabasePath.createNew(dbOut, Format.H2, overwrite);
  }

  private final CallOnceSentinel co_Configure = new CallOnceSentinel();
  /**
   * Configures this object by loading all the relevant plugins and establishing
   * the database.
   *
   * @throws HarnessError if there was a problem in loading a plugin or creating
   * the log. This is usually the result of a configuration error.
   */
  @Override
  public void configure() {
    co_Configure.check();
    // Show throw an error if something is missing.
    try {
      OptionUtils.sealTree(getOptionTree());
     // System.out.println("testcase.getValue(): "+testcase.getValue());

      scenario = Scenario.load(testcase.getValue());
     // System.out.println("scenario: "+scenario);
      wlGroup = WorkloadGroup.findGroup(workload.getValue());
    } catch (MissingArgumentException ex) {
      throw new HarnessException(HarnessError.CONFIG, ex);
    }

    try {
      dbPath = makeDbPath();
    } catch (Exception ex) {
      throw HarnessException.create(HarnessError.CONFIG, ex);
    }

    configured = true;
  }


  public  void  printhboptions(){
     Iterator<OptionInfo> itr =this.getOptionTree().getAllOptionsInfo().iterator();
    while(itr.hasNext()){
      OptionInfo op= itr.next();
      System.out.println("Option Info Name: "+op.getOption().getName());
      System.out.println("Option Info Value: "+op.getOption().getCurrentRawValue());
    }
  }

  @Override
  public boolean isConfigured() {
    return configured;
  }
}
