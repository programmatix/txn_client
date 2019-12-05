/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.batch;

import com.couchbase.sdkdclient.options.FileOption;
import com.couchbase.sdkdclient.options.OptBuilder;
import com.couchbase.sdkdclient.options.OptionConsumer;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.options.OptionTreeBuilder;
import com.couchbase.sdkdclient.options.StringOption;
import java.io.File;

/**
 */
public class HistoryOptions implements OptionConsumer {

  private StringOption optSdkdId = OptBuilder.startString("sdk-id")
          .help("SDK Identifier")
          .shortAlias("S")
          .defl("SDK")
          .build();

  private FileOption optHistDb = OptBuilder.startFile("histfile")
          .shortAlias("H")
          .help("Path to history database directory")
          .defl(".brun_history")
          .build();

  private StringOption optClusterId = OptBuilder.startString("cluster-id")
          .help("Cluster identifier")
          .shortAlias("V")
          .build();

  public String getSdkIdentifier() {
    return optSdkdId.getValue();
  }

  public String getClusterIdentifier() {
    return optClusterId.getValue();
  }

  public File getHistoryFile() {
    //noinspection ResultOfMethodCallIgnored
    optHistDb.getValue().mkdirs();
    return new File(optHistDb.getValue(), "history");
  }

  @Override
  public OptionTree getOptionTree() {
    return new OptionTreeBuilder()
            .group("runhist")
            .description("Options for controlling the indexing of results")
            .source(this, HistoryOptions.class)
            .build();
  }
}