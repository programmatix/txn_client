/**
 * Copyright 2013, Couchbase Inc.
 */
package com.couchbase.sdkdclient.batch;

import com.couchbase.cbadmin.client.ConnectionInfo;
import com.couchbase.cbadmin.client.RestApiException;


import com.couchbase.sdkdclient.batch.suites.Suite;
import com.couchbase.sdkdclient.batch.suites.TestAnalysisOptions;
import com.couchbase.sdkdclient.batch.suites.TestInfo;
import com.couchbase.sdkdclient.cluster.ClusterBuilder;
import com.couchbase.sdkdclient.cluster.NodeHost;
import com.couchbase.sdkdclient.cluster.installer.ClusterInstaller;
import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.context.RunContext;
import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.options.OptionInfo;
import com.couchbase.sdkdclient.options.OptionParser;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.options.OptionUtils;
import com.couchbase.sdkdclient.options.OptionUtils.InfoHookOption;
import com.couchbase.sdkdclient.options.OptionUtils.InfoOption;

import com.couchbase.sdkdclient.stester.HarnessBuilder;
import com.couchbase.sdkdclient.stester.RunContextBuilder;
import com.couchbase.sdkdclient.stester.STester;
import com.couchbase.sdkdclient.util.Configured;
import com.couchbase.sdkdclient.util.S3Uploader;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Batch Runner.
 * The batch runner combines one or more instances of
 * the {@link STester} and {@link RunContext} classes to work on a common set
 * of inputs. This can be collectively referred to as a <i>test suite</i>.
 *
 * Unlike the previous Python {@code brun}, this does not fork a new
 * process and acts as a simple wrapper. As such, the batch runner may have
 * information that would otherwise not be available to an external process,
 * such as node, cluster, and other information.
 */
public class BRun {
  static final PrintStream out = System.out;
  static final String INDENT=  "    ";
  static final Logger logger = LogUtil.getLogger(BRun.class);

  final BrunOptions options = new BrunOptions();
  final OptionParser parser;
  final List<String> extraStArgs = new ArrayList<String>();
  private S3Uploader s3u;

  private Collection<NodeHost> nodes = null;
  private boolean showSuiteHelp;
  private File outDir;
  private boolean skipTest = false;

  static String currStesterCmd = "*";

  public static String getCurrStesterCmd(){
    return currStesterCmd;
  }
  private String normalizePath(File path) {
    String fName = path.getAbsolutePath();
    fName = fName.replaceFirst(
            Pattern.quote(outDir.getAbsolutePath() + File.separator), "");
    fName = FilenameUtils.separatorsToUnix(fName);
    return fName;
  }



  /**
   * Potentially catches or throws a HarnessException depending on the inner
   * code and the commandline configuration.
   * @param caught A caught exception
   */
  private void maybePropagate(HarnessException caught) {
    if (caught.getCode() == HarnessError.DRIVER) {
      logger.error("Suppressing driver-related error", caught);
      return;
    }
    if (options.shouldContinueOnExceptions()) {
      logger.error("Caught harness exception, but " +
              "continuing anyway (--continue-on-exceptions)", caught);
    } else {
      throw caught;
    }
  }

  /**
   * Sets up the harness to execute the current test.
   * @param ctx Context representing the current test to execute.
   */
  private void configureHarness(TestContext ctx) {
    ArrayList<String> curArgs = new ArrayList<String>();
    for (Map.Entry<String,String> ent : ctx.getTestInfo().getOptions()) {
      out.printf("%s%s=%s%n", INDENT, ent.getKey(), ent.getValue());

      if((ent.getKey().endsWith("enabled")) && (ent.getValue().equals("false"))){
        skipTest = true;
      }
      curArgs.add("--" + ent.getKey().replaceAll("/", "-"));
      curArgs.add(ent.getValue());
    }


    dumpCommandline(curArgs);

    OptionParser stParser
            = new OptionParser(extraStArgs.toArray(new String[extraStArgs.size()]));
    stParser.appendArgv(curArgs.toArray(new String[curArgs.size()]));


    HarnessBuilder hb = new HarnessBuilder();
    OptionTree hbOptions = hb.getOptionTree();
    hbOptions.setBool("override-output", true);
   // System.out.println("Before hbOptions: ");
    //printoptions(hbOptions.getAllOptionsInfo());
    stParser.addTarget(hbOptions);
    stParser.apply();
   // System.out.println("After hbOptions:");
   // printoptions(hbOptions.getAllOptionsInfo());



    RunContextBuilder rcb = new RunContextBuilder(Configured.create(hb));

    //System.out.println("Before rcbOptions:");
    //printoptions(rcb.getOptionTree().getAllOptionsInfo());
    stParser.addTarget(rcb.getOptionTree());
    stParser.apply();
    //System.out.println("After rcbOptions:");
    //printoptions(rcb.getOptionTree().getAllOptionsInfo());

    STester stester = new STester(Configured.create(hb));
    stester.configure(Configured.create(rcb));
    try {
      stester.writeConfiguration(stester.getDatabase(), stParser);
    } catch (SQLException ex) {
      logger.error("Couldn't write config", ex);
    }

    ctx.setStester(Configured.create(stester));
    stParser.throwOnUnrecognized();
    logger.info("DONE");

  }



  /**
   * Runs the configured harness and write the outputs to their proper
   * locations.
   * @param ctx
   * @throws IOException
   */
  private void runHarness(TestContext ctx) throws IOException {
    Collection<TestAnalysisOptions> workloads = ctx.getTestInfo().getAnalysisParams();
    if (workloads.isEmpty()) {
      throw new HarnessException(HarnessError.CONFIG, "No workloads found");
    }
    try {
      ctx.getStester().run();
    } catch (Exception ex) {
      logger.error("While writing logs", ex);
    }
  }



  /**
   * Displays a formatted version of the commandline arguments which would be
   * required to reproduce this test
   * @param args The arguments specific to the current test.
   */
  private void dumpCommandline(Collection<String> args) {
    out.println();
    out.println(INDENT+"To re-run the test, copy/paste the following into the shell.");
    out.println(INDENT+"You may also copy/paste (except the first line) into an argfile");

    // 8<-----------------
    out.printf("%s8<%s%n", INDENT, StringUtils.repeat("-", 40));
    out.println(INDENT + "./stester \\");
    currStesterCmd = INDENT + "./stester \\ <br>";

    List<String> allArgs = new ArrayList<String>(args);
    allArgs.addAll(extraStArgs);
    String[] split = WordUtils.wrap(StringUtils.join(allArgs, " "), 65).split("\n");

    for (int i = 0; i < split.length-1; i++) {
      out.printf("%s%s \\%n", INDENT+INDENT, split[i]);
      currStesterCmd += INDENT+INDENT + split[i] + " \\ <br>";
    }
    out.printf("%s%s%n", INDENT+INDENT, split[split.length-1]);
    currStesterCmd += INDENT+INDENT + split[split.length-1];

    // ------>8
    out.printf("%s%s>8%n", INDENT, StringUtils.repeat("-", 40));
    out.flush();
  }

  private void runSingleTest(TestContext ctx) throws IOException {
    try {
      System.out.println("calling configuureHardness");
      configureHarness(ctx);
    } catch (HarnessException ex) {
      if (ctx.getStester() != null) {
        ctx.getStester().close();
      }
      maybePropagate(ex);
      return;
    }


    try {
      System.out.println("calling runHarness(ctx);");
      runHarness(ctx);
    } catch (HarnessException ex) {
      if (ctx.getStester() != null) {
        ctx.getStester().close();
      }
      maybePropagate(ex);
      return;
    }



  }

  private Collection<NodeHost> getClusterNodes() {
    if (nodes != null) {
      return nodes;
    }

    ClusterBuilder clb = new ClusterBuilder();
    OptionParser clParser = new OptionParser(parser.getRemainingArgv());
    clParser.addTarget(clb.getOptionTree());
    clParser.apply();
    clb.configure();
    nodes = clb.getCurrentNodes();
    return nodes;
  }

  private String getClusterVersion() throws RestApiException {
    NodeHost masterNode = nodes.iterator().next();
    ConnectionInfo info = masterNode.getAdmin().getInfo();
    return info.getVersion();
  }

  public void run() throws Exception {
    Suite suite = options.findSuite();
    parser.addTarget(suite.getOptionTree());
    parser.apply();


    if (showSuiteHelp) {
      parser.dumpAllHelp();
      System.exit(0);
    }

    extraStArgs.addAll(parser.getRemainingArgv());

    suite.prepare();
   // System.out.println("Tests we got After parse apply but After prep suite : ");



    if (options.shouldUpload()) {
      logger.info("Initializing S3: "+options.getS3Bucket().toString());
      s3u = new S3Uploader(options.getS3Bucket(), options.getS3Credentials());
      s3u.connect();
    }





    ClusterInstaller installer = options.getInstaller();
    Collection<NodeHost> nodes = getClusterNodes();
    if (nodes.isEmpty()) {
      throw new IllegalArgumentException("No nodes specified");
    }


    String clVersion;
    if (installer != null) {
     // System.out.println("Installer is not null");
      installer.configure();
      installer.install(getClusterNodes());
      clVersion = installer.getWantedVersion();
    } else {
      //System.out.println("Installer is NULL");
      clVersion = getClusterVersion();
      //System.out.println("clVersion");

    }

    // Prepare the output directory
    outDir = options.getOutdir();
    if (outDir.exists() && outDir.isDirectory() == false) {
      throw new IllegalArgumentException("Output directory must be directory");
    } else if (!outDir.exists()) {
      //noinspection ResultOfMethodCallIgnored
      outDir.mkdirs();
    }




    // Figure out the versions. By now, all nodes should be up
    for (TestInfo info : suite.getTests()) {
      out.println();
      out.println(StringUtils.repeat("=", 60));
      out.printf("Running %s%n", info.getTestName());
      out.println("outDir: " + outDir.toString());
      TestContext ctx = new TestContext(info, options.getSdkIdentifier(), outDir, clVersion);
      runSingleTest(ctx);
     // printoptions(options.getOptionTree().getAllOptionsInfo());
    }
  }

  public BRun(String[] argv) throws Exception {
    parser = new OptionParser(argv);

    parser.setPrintAndExitOnError(true);
    OptionUtils.scanInlcudes(parser);

    LogUtil.addDebugOption(parser);
    OptionUtils.addHelp(parser);

    parser.addTarget(options.getOptionTree());
    new InfoOption("gen-creds", "generate credentials file", parser) {
      @Override public Action call() throws Exception {
        System.out.println("Use empty password when prompted");
        System.out.println("File will be written to 'S3Creds_tmp'");
        AWSCredentials.main(new String[] { "SDKD_Uploader", "S3Creds_tmp" });
        return Action.EXIT;
      }
    };

    new InfoHookOption("suite-help", "Show suite-specific help", parser) {
      @Override protected void hook() { showSuiteHelp = true; }
    };

    parser.apply();
  }

  public static void main(String[] argv) throws Exception {
    try {
      BRun batchRunner = new BRun(argv);
      logger.info("Hello praneeth");
      batchRunner.run();
      System.exit(0);
    } catch (Exception ex) {
      logger.error("Caught exception", ex);
      System.exit(1);
    }
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
