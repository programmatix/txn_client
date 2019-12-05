package com.couchbase.sdkdclient.batch;

import com.couchbase.sdkdclient.batch.suites.TestInfo;
import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.rundb.RunDB;
import com.couchbase.sdkdclient.stester.STester;
import com.couchbase.sdkdclient.util.CallOnceSentinel;
import com.couchbase.sdkdclient.util.Configured;
import com.couchbase.sdkdclient.util.Symlinks;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Internal module to be used with {@link BRun}. It allows to group an
 * {@link STester} instance with its relevant suite information.
 */
class TestContext {
  private final File outDir;
  private final TestInfo info;
  private final Calendar curTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
  private final Calendar roundTime;
  private STester stester = null;
  private RunDB runDatabase;

  static final Logger logger = LogUtil.getLogger(TestContext.class);
  static final DateFormat dateFormat = new SimpleDateFormat("MM-dd-yy");

  private String getTimeSuffix() {
    long diff = (curTime.getTimeInMillis() - roundTime.getTimeInMillis()) / 1000;
    return String.format("%06d", diff);
  }

  /**
   * Create a new context
   * @param inf The info object
   * @param sdkId The SDK identifier.
   */
  public TestContext(TestInfo inf, String sdkId, File outputDirectory, String clId) {
    info = inf;
    outDir = outputDirectory;

    roundTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    roundTime.clear();

    //noinspection MagicConstant
    roundTime.set(curTime.get(Calendar.YEAR),
                  curTime.get(Calendar.MONTH),
                  curTime.get(Calendar.DATE));
  }

  /**
   * Set this instance' {@link STester instance}
   * @param st The instance info.
   */
  final CallOnceSentinel st_Once = new CallOnceSentinel();
  public void setStester(Configured<STester >st) {
    st_Once.check();
    stester = st.get();
    runDatabase = stester.getDatabase();
  }

  /**
   * Get the associated stester instance, if set.
   * @return The stester instance.
   */
  public STester getStester() {
    return  stester;
  }

  public RunDB getRunDatabase() {
    return runDatabase;
  }

  /**
   * Get the underlying test information
   * @return The test information.
   */
  public TestInfo getTestInfo() {
    return info;
  }


  /**
   * Analyzes the database and produces a set of logfiles for the given workload
   * @param db The database to analyze
   * @param wlName The workload name to use.
   * @throws java.io.IOException
   */

}
