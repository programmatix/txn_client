/**
 * Copyright 2013, Couchbase Inc.
 */
package com.couchbase.sdkdclient.batch;

import com.couchbase.sdkdclient.batch.suites.Suite;
import com.couchbase.sdkdclient.cluster.installer.ClusterInstaller;
import com.couchbase.sdkdclient.cluster.installer.InstallerOptions;
import com.couchbase.sdkdclient.options.BoolOption;
import com.couchbase.sdkdclient.options.FileOption;
import com.couchbase.sdkdclient.options.OptBuilder;
import com.couchbase.sdkdclient.options.OptionDomains;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.options.RawOption;
import com.couchbase.sdkdclient.options.StringOption;

import java.io.File;

class BrunOptions extends HistoryOptions {
  private InstallerOptions instOptions = new InstallerOptions();
  private StringOption optSuite = OptBuilder.startString("suite")
          .help("Suite to execute. This should be a plugin name")
          .defl("StandardSuite")
          .build();

  private BoolOption optDryrun = OptBuilder.startBool("dry-run")
          .help("Don't run the tests, just display configuration info")
          .defl("false")
          .build();

  private BoolOption optNoUpload = OptBuilder.startBool("no-upload")
          .defl("false")
          .build();

  private FileOption optOutDir = OptBuilder.startFile("outdir")
          .defl("log")
          .shortAlias("o")
          .help("Directory in which to create logs")
          .build();

  private BoolOption optBrowser = OptBuilder.startBool("launch-browser")
          .shortAlias("B")
          .help("Launch web browser with HTML report for each run")
          .defl("false").
          build();

  private FileOption optS3Creds = OptBuilder.startExistingFile("s3auth")
          .help("S3 credentials")
          .shortAlias("A")
          .build();

  private StringOption optS3Bucket = OptBuilder.startString("s3-bucket")
          .help("Bucket name for S3 uploads")
          .defl("sdk-testresults.couchbase.com")
          .build();
 
  private StringOption optSdkdReServ = OptBuilder.startString("sdkd-result")
          .help("Couchbase server store sdkd test results")
          .shortAlias("S")
          .defl("172.23.120.157")
          .build();

  private StringOption optSdkdReServUser = OptBuilder.startString("sdkd-result-user")
          .help("Couchbase server store sdkd test results User name")
          .shortAlias("U")
          .defl("Administrator")
          .build();

  private StringOption optSdkdReServPasswd = OptBuilder.startString("sdkd-result-passwd")
          .help("Couchbase server store sdkd test results password")
          .shortAlias("P")
          .defl("password")
          .build();

  private BoolOption optContinueOnExc = OptBuilder.startBool("continue-on-exceptions")
          .alias("continue")
          .help("Continue processing even after configuration errors are found. " +
                "Note that this only affects non-SDKD-related exceptions (i.e. " +
                "a bad cluster)")
          .defl("false")
          .build();

  private BoolOption optCreateSymlinks = OptBuilder.startBool("create-symlinks")
          .alias("symlink")
          .help("Create symbolic links in the top level log directory. If enabled "+
                "then the name of the test will be linked to its correponsing " +
                "output file")
          .defl("true")
          .build();

  public File getOutdir() {
    return optOutDir.getValue();
  }

  public File getS3Credentials() {
    return optS3Creds.getValue();
  }

  public String getS3Bucket() {
    return optS3Bucket.getValue();
  }

  public boolean shouldLaunchBrowser() {
    return optBrowser.getValue();
  }

  public boolean isDryRun() {
    return optDryrun.getValue();
  }

  public boolean shouldUpload() {
    return optNoUpload.getValue() == false;
  }

  public boolean shouldSymlink() {
    return optCreateSymlinks.getValue();
  }

  public Suite findSuite() throws ClassNotFoundException{
    return Suite.find(optSuite.getValue());
  }

  public boolean shouldContinueOnExceptions() {
    return optContinueOnExc.getValue();
  }
 
  public String getSdkdReServ() {
      return optSdkdReServ.getValue();
  }

  public String getSdkdReServUser() {
    return optSdkdReServUser.getValue();
  }

  public String getSdkdReServPasswd() {
    return optSdkdReServPasswd.getValue();
  }

  public ClusterInstaller getInstaller() {
    if (instOptions.shouldSkip()) {
      return null;
    }
    return new ClusterInstaller(instOptions);
  }

  @Override
  public OptionTree getOptionTree() {
    OptionTree tree = super.getOptionTree();
    for (RawOption opt : OptionTree.extract(this, BrunOptions.class)) {
      tree.addOption(opt);
    }
    OptionTree wrapper = new OptionTree(OptionDomains.INSTALLER);
    wrapper.addChild(instOptions.getOptionTree());
    tree.addChild(wrapper);
    return tree;
  }
}
