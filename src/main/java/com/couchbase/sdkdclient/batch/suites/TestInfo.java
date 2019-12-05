package com.couchbase.sdkdclient.batch.suites;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public interface TestInfo extends Serializable {
  /** Input functions */

  /**
   * Gets the equivalent of the required options for this run.
   * @return A collection of options to use.
   */
  public Collection<Map.Entry<String,String>> getOptions();

  /**
   * Gets the name of the test. This will be displayed as an identifier
   * in the logs. This should be common to all tests which will be run
   * with these similar inputs.
   * @return A string identifying the test.
   */
  public String getTestName();

  /**
   * Get all the workload names for this test, if this test contains
   * multiple workloads.
   * @return A non-empty list of all the workload names for this run.
   */
  public Collection<TestAnalysisOptions> getAnalysisParams();

  public boolean shouldExamineGrade();
}