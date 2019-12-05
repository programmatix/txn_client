/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.batch.suites;

import com.couchbase.sdkdclient.options.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StandardSuiteOptions implements OptionConsumer {
  final private FileOption optJson =
          OptBuilder.start(new FileOption("defs", FileOption.Policy.EXISTING))
          .help("File to existing JSON definitions file (will use built in otherwise)")
          .build();

  final private MultiOption optVariants =
          OptBuilder.startMulti("variants")
          .help("A list of variants with which the tests should be run. " +
                "variants should be supplied as list of |-delimited options. " +
                "Each test will be executed once for each variant grouping found")
          .build();

  final private BoolOption optAllVariants =
          OptBuilder.startBool("all-variants")
          .help("Select all possible variants")
          .defl("false")
          .build();

  final private MultiOption optTests = OptBuilder.startMulti("test")
          .help("Specify a single test to run (without any suffixes). " +
                "this option may be specified multiple times for multiple "+
                "tests. Use `--list-tests` to get the list of tests available")
          .shortAlias("T")
          .build();

  final private BoolOption optListTests = OptBuilder.startBool("list-tests")
          .help("List all tests and exits.")
          .defl("false")
          .build();


  final private StringOption optSuitePath = OptBuilder.startString("suite")
          .help("Json file where the tests are defined")
          .defl("suites/standard.json")
          .build();


  @Override
  public OptionTree getOptionTree() {
    return new OptionTreeBuilder()
            .prefix(new OptionPrefix("testsuite"))
            .source(this, StandardSuiteOptions.class)
            .group("stdsuite")
            .description("Suite test selection options")
            .build();
  }

  final static Integer[] allVariants = {
          Features.HYBRID.value,
          Features.SASL.value,
          Features.MEMD.value,
          Features.EPHM.value,
          Features.N1QL.value,
          Features.HYBRID.value | Features.SASL.value,
          Features.MEMD.value, Features.SASL.value,
          Features.SPATIAL.value,
          Features.N1QLHYBRID.value,
          Features.FTS.value,
          Features.CBAS.value
  };

  public List<Integer> getVariants() {

    if (optAllVariants.getValue()) {
      return Arrays.asList(allVariants);
    }

    List<Integer> ret = new ArrayList<Integer>();

    // Now figure out the variants..
    for (String s : optVariants.getRawValues()) {
      int curVariant = 0;
      for (String vspec : s.split("\\|")) {


        Features ft = Features.valueOf(vspec.toUpperCase());
        System.out.println(ft.value);

        curVariant |= ft.value;
      }
      if (Features.HYBRID.is(curVariant) && Features.MEMD.is(curVariant)) {
        throw new IllegalArgumentException("Hybrid and Memcached not supported");
      }
      ret.add(curVariant);
    }
    if (ret.isEmpty()) {
      ret.add(0);
    }
    return ret;
  }

  public File getUserDefinitions() {
    if (!optJson.wasPassed()) {
      return null;
    }
    return optJson.getValue();
  }

  public Set<String> getSelectedTests() {
    return new LinkedHashSet<String>(optTests.getRawValues());
  }

  public boolean shouldDumpTests() {
    return optListTests.getValue();
  }

  public String getSuitePath() { return optSuitePath.getValue(); }
}
