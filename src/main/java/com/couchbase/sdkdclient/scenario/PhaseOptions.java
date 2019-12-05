/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.scenario;

import com.couchbase.sdkdclient.options.BoolOption;
import com.couchbase.sdkdclient.options.IntOption;
import com.couchbase.sdkdclient.options.OptBuilder;
import com.couchbase.sdkdclient.options.OptionConsumer;
import com.couchbase.sdkdclient.options.OptionDomains;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.options.OptionTreeBuilder;

final class PhaseOptions implements OptionConsumer {
  private final IntOption ramp = OptBuilder.start(new IntOption("ramp"))
          .defl("30")
          .help("Ramp time in seconds")
          .globalAlias("ramp")
          .argName("SECONDS")
          .build();

  private final IntOption rebound = OptBuilder.start(new IntOption("rebound"))
          .defl("90")
          .help("Rebound time in seconds")
          .globalAlias("rebound")
          .argName("SECONDS")
          .build();

  private final BoolOption noSleep = OptBuilder.startBool("change-only")
          .alias("sleepless")
          .alias("no-wait")
          .help("Equivalent to setting `--ramp' and `--rebound' to 0. Useful for " +
                        "testing only the cluster change itself")
          .build();
  
  private final BoolOption noChange = OptBuilder.startBool("no-change")
          .help("Don't execute any changes")
          .defl("false")
          .hidden()
          .build();

  @Override
  public OptionTree getOptionTree() {
    return new OptionTreeBuilder()
            .prefix(OptionDomains.SCENARIO)
            .source(this, PhaseOptions.class)
            .description("Common options for phased scenario")
            .group("phases")
            .build();
  }

  int getRampSleep() {
    return ramp.getValue();
  }

  int getReboundSleep() {
    return rebound.getValue();
  }

  boolean shouldSleep() {
    return !noSleep.getValue();
  }

  boolean shouldChange() {
    return !noChange.getValue();
  }
}
