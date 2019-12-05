/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.batch.suites;

import com.couchbase.sdkdclient.options.OptionTree;

import java.util.Map;

/**
 * Interface to use for providing options for DbAnalyzer. Typically a single
 * {@link TestInfo} class will yield one of these for each individual run.
 */
public interface TestAnalysisOptions {
  /**
   * Gets the workload name for this run.
   * @return the workload name.
   */
  String getWorkloadName();


  void configureScorerOptions(OptionTree options);
}
