/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.batch;

import com.couchbase.sdkdclient.options.*;

import java.io.*;

/**
 */
public class ReporterOptions extends HistoryOptions {
  FileOption optOutput = OptBuilder.startFile("output")
          .help("Location where the spreadsheet should be written to")
          .defl("report.xls")
          .shortAlias("o")
          .build();

  File getOutputFile() {
    return optOutput.getValue();
  }

  @Override
  public OptionTree getOptionTree() {
    OptionTree parent = super.getOptionTree();
    for (RawOption opt : OptionTree.extract(this, ReporterOptions.class)) {
      parent.addOption(opt);
    }
    return parent;
  }
}
