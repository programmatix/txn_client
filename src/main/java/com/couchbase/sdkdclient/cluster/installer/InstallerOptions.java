/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.cluster.installer;

import com.couchbase.sdkdclient.cluster.installer.ClusterInstaller.VersionTuple;
import com.couchbase.sdkdclient.options.*;

public class InstallerOptions implements OptionConsumer {
  private StringOption optVersion = OptBuilder.startString("version")
          .help("Cluster version to install")
          .defl("2.2.0")
          .build();

  private BoolOption optSkip = OptBuilder.startBool("skip")
          .help("Don't install cluster")
          .defl("true")
          .build();

  private StringOption optURL = OptBuilder.startString("url")
          .help("Install builds using this url")
          .defl("")
          .build();

  private StringOption optBuildType = OptBuilder.startString("buildType")
          .help("Build type - enterprise, community, enterprise-dbg")
          .defl("enterprise")
          .build();

  public VersionTuple getVTuple() throws Exception {
    return VersionTuple.parse(optVersion.getValue());
  }

  @Override
  public OptionTree getOptionTree() {
    return new OptionTreeBuilder()
            .source(this, InstallerOptions.class)
            .group("installation")
            .description("Options controlling the installing of Couchbase")
            .build();
  }

  public boolean shouldSkip() {
    return optSkip.getValue();
  }

  public String getURL() {
    return optURL.getValue();
  }

  public String getBuildType() {
    return optBuildType.getValue();
  }
}
