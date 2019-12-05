/**
 * Copyright 2013, Couchbase Inc.
 */
package com.couchbase.sdkdclient.batch.suites;

import com.couchbase.sdkdclient.options.OptionConsumer;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.workload.Workload;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Standard" test suite. This is similar to the older Python suite which
 * read the information from an embedded SQLite database. Here however the
 * information is parsed at runtime from a JSON document. See the
 * {@code suites/standard.json} file for the definitions.
 */
public class StandardSuite extends Suite implements OptionConsumer {
  static class TestEntry {
    String id;
    String description;
    String scenario;
    Map<String,String> options;
  }

  final List<TestEntry> selectedEntries = new ArrayList<TestEntry>();
  final Set<Integer> variants = new HashSet<Integer>();
  final StandardSuiteOptions options = new StandardSuiteOptions();

  private void dumpTestList(Collection<TestEntry> entries) {
    for (TestEntry ent : entries) {
      System.out.printf("%s: %s%n", ent.id, ent.description);
    }
    System.exit(0);
  }

  @Override
  public OptionTree getOptionTree() {
    return options.getOptionTree();
  }


  /**
   * Loads a map of {@link TestEntry objects from a JSON array}.
   *
   * The format of the JSON should
   * @param stream An input stream containing the JSON
   * @return A map of test entries indexed by ID
   */
  public static Map<String,TestEntry> load(InputStream stream) throws IOException {
    InputStreamReader rdr = new InputStreamReader(stream);
    JsonArray arr = new Gson().fromJson(rdr, JsonArray.class);
    Gson gs = new Gson();
    Map<String,TestEntry> ret = new HashMap<String, TestEntry>();
    for (JsonElement elem : arr) {
      TestEntry e = gs.fromJson(elem, TestEntry.class);
      ret.put(e.id, e);
    }

    if (ret.isEmpty()) {
      throw new IOException("No definitions found");
    }
    return ret;
  }

  private InputStream loadBuiltins() throws IOException {
    ClassLoader loader = StandardSuite.class.getClassLoader();
    InputStream stream = loader.getResourceAsStream(options.getSuitePath());
    if (stream == null) {
      throw new IOException("Couldn't find builtin definitions");
    }
    return stream;
  }

  @Override
  public void prepare() throws IOException {


    File userJson = options.getUserDefinitions();
    InputStream stream = userJson == null
            ? loadBuiltins() : FileUtils.openInputStream(userJson);
    Map<String,TestEntry> allDefinitions = load(stream);
    Set<String> wanted = options.getSelectedTests();

    if (wanted.isEmpty()) {
      wanted = allDefinitions.keySet();
    }

    if (options.shouldDumpTests()) {
      dumpTestList(allDefinitions.values());
    }

    for (String s : wanted) {
      TestEntry entry = allDefinitions.get(s);
      if (entry == null) {
        throw new IllegalArgumentException("Test does not exist: "+s);
      }
      selectedEntries.add(entry);
    }

    variants.addAll(options.getVariants());

  }

  private static class TestInfoImpl implements TestInfo {
    final Map<String,String> options;
    final TestEntry entry;
    final String testName;
    final Collection<TestAnalysisOptions> analysisOptions;

    TestInfoImpl(TestEntry entry, int variant) {
      this.options = new HashMap<String, String>(entry.options);
      this.entry = entry;
      this.testName = entry.id + "-" + Features.makeName(variant);

      options.put("testcase", entry.scenario);

      if (Features.HYBRID.is(variant)) {
        options.put("workload", "HybridWorkloadGroup");
      }

      if (Features.SPATIAL.is(variant)) {
        options.put("workload", "SpatialWorkloadGroup");
      }

      if (Features.VIEW.is(variant)) {
        options.put("workload", "ViewWorkloadGroup");
      }

      if (Features.SASL.is(variant)) {
        options.put("bucket/name", "protected");
        options.put("bucket/password", "secret");
      }

      if (Features.MEMD.is(variant)) {
        options.put("bucket/type", "memcached");
      }

      if (Features.EPHM.is(variant)) {
        options.put("bucket/type", "ephemeral");
      }

      if (Features.N1QL.is(variant)) {
        options.put("workload", "N1QLWorkloadGroup");
      }

      if (Features.N1QLHYBRID.is(variant)) {
        options.put("workload", "N1QLHybridWorkloadGroup");
      }
      if (Features.DCP.is(variant)) {
        options.put("workload", "DCPWorkloadGroup");
      }
      if (Features.SUBDOC.is(variant)) {
        options.put("workload", "SubdocWorkloadGroup");
      }
      if (Features.FTS.is(variant)) {
        options.put("workload", "FTSWorkloadGroup");
      }
      if (Features.CBAS.is(variant)) {
        options.put("workload", "AnalyticsWorkloadGroup");
      }

      analysisOptions = buildAnalysisOptions(variant);
    }

    @Override
    public Collection<Map.Entry<String, String>> getOptions() {
      return options.entrySet();
    }

    @Override
    public String getTestName() {
      return testName;
    }

    @Override
    public boolean shouldExamineGrade() {
      return !testName.startsWith("passthrough");
    }

    @Override
    public Collection<TestAnalysisOptions> getAnalysisParams() {
      return analysisOptions;
    }
  }

  final static String[] httpWorkloadNames = { Workload.NAME_HT, Workload.NAME_CB };

  static Collection<TestAnalysisOptions> buildAnalysisOptions(int variant) {
    ArrayList<TestAnalysisOptions> ret = new ArrayList<TestAnalysisOptions>();

    if (Features.N1QL.is(variant)) {
      ret.add(new TestAnalysisOptions() {
        @Override
        public String getWorkloadName() {
          return Workload.NAME_N1QL;
        }

        @Override
        public void configureScorerOptions(OptionTree options) {

        }
      });
    }
    else if (Features.SPATIAL.is(variant)) {
      ret.add(new TestAnalysisOptions() {
        @Override
        public String getWorkloadName() {
          return Workload.NAME_SPATIAL;
        }

        @Override
        public void configureScorerOptions(OptionTree options) {

        }
      });
    }
    else if (Features.SUBDOC.is(variant)) {
      ret.add(new TestAnalysisOptions() {
        @Override
        public String getWorkloadName() {
          return Workload.NAME_SUBDOC;
        }

        @Override
        public void configureScorerOptions(OptionTree options) {

        }

      });
    }
    else if (Features.FTS.is(variant)) {
    ret.add(new TestAnalysisOptions() {
      @Override
      public String getWorkloadName() {
        return Workload.NAME_FTS;
      }

      @Override
      public void configureScorerOptions(OptionTree options) {

      }

    });
    }
    else if (Features.CBAS.is(variant)) {
      ret.add(new TestAnalysisOptions() {
        @Override
        public String getWorkloadName() {
          return Workload.NAME_CBAS;
        }

        @Override
        public void configureScorerOptions(OptionTree options) {

        }

      });
    }
    else if (Features.EPHM.is(variant)){
      ret.add(new TestAnalysisOptions(){
        @Override
        public String getWorkloadName() { return Workload.NAME_EPHM; }

        @Override
        public void configureScorerOptions(OptionTree options) {

        }

      });
    }

    else {
      ret.add(new TestAnalysisOptions() {
        @Override
        public String getWorkloadName() {
          return Workload.NAME_KV;
        }

        @Override
        public void configureScorerOptions(OptionTree options) {
          // Nothing special here.
        }
      });
      if (Features.HYBRID.is(variant)) {
        for (final String vwl : httpWorkloadNames) {
          ret.add(new TestAnalysisOptions() {
            @Override
            public String getWorkloadName() {
              return vwl;
            }

            @Override
            public void configureScorerOptions(OptionTree options) {
              options.setInt("score/max-latency", 6000000);
            }
          });
        }
      }
      if (Features.N1QLHYBRID.is(variant)) {
        ret.add(new TestAnalysisOptions() {
          @Override
          public String getWorkloadName() {
            return Workload.NAME_N1QL;
          }
          @Override
          public void configureScorerOptions(OptionTree options) {

          }

        });
      }
    }

    if (Features.VIEW.is(variant)) {
        ret.add(new TestAnalysisOptions() {
          @Override
          public String getWorkloadName() {
            return Workload.NAME_CB;
          }
          @Override
          public void configureScorerOptions(OptionTree options) {
            options.setInt("score/max-latency", 60000);
          }
        });
    }

    return ret;
  }

  @Override
  public List<TestInfo> getTests() throws IOException {
    List<TestInfo> retList = new ArrayList<TestInfo>();
    for (final TestEntry ent : selectedEntries) {
      for (final int variant : variants) {
        retList.add(new TestInfoImpl(ent, variant));
      }
    }

    return retList;
  }
}

