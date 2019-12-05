/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.stester;

import com.couchbase.sdkdclient.context.HarnessError;
import com.couchbase.sdkdclient.context.HarnessException;
import com.couchbase.sdkdclient.context.RunContext;
import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.logging.RunDBAppender;
import com.couchbase.sdkdclient.options.OptionLookup;
import com.couchbase.sdkdclient.options.OptionParser;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.options.OptionUtils;
import com.couchbase.sdkdclient.options.OptionUtils.InfoOption;
import com.couchbase.sdkdclient.options.RawOption;
import com.couchbase.sdkdclient.rundb.DatabasePath;
import com.couchbase.sdkdclient.rundb.MiscEntry;
import com.couchbase.sdkdclient.rundb.RunDB;
import com.couchbase.sdkdclient.util.ClientVersion;
import com.couchbase.sdkdclient.util.Configurable;
import com.couchbase.sdkdclient.util.Configured;
import com.google.gson.Gson;
import org.slf4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * This is the main entry point for the SDKD client. STester utilizes the
 * {@link HarnessBuilder} class to parse values from the commandline. It
 * loads several plugins and then passes them to the {@link RunContext} which
 * then performs further dispatching. See the {@link RunContext} for more
 * information.
 */
public class STester implements Configurable {
  static Logger logger = LogUtil.getLogger(STester.class);
  private final HarnessBuilder hb;
  private RunContextBuilder rcBuilder = null;
  private final String[] args;
  private final OptionParser parser;
  private final boolean isMain;
  private boolean dumpPluginHelp = false;
  private boolean configured = false;

  /**
   * Writes the configuration of our harness into the database.
   * @param db
   * @throws SQLException
   */
  public void writeConfiguration(RunDB db, OptionParser configParser) throws SQLException {
    OptionLookup lookup = new OptionLookup();

    for (OptionTree child : configParser.getAllTrees()) {
      lookup.addGroup(child);
    }

    db.addConfig(lookup);
    db.addProperty(MiscEntry.K_CMDLINE, new Gson().toJson(Arrays.asList(args)));
    db.addProperty(MiscEntry.K_STVERSION, ClientVersion.getRevision());
  }

  /**
   * Sets up the various informational hb which can be retrieved via
   * the stester commandline. This is only used when the class is created
   * from {@code main}.
   */
  private void setupInfoOptions() {
    OptionUtils.addHelp(parser);
    new InfoOption("version", "Print version", parser) {
      @Override public Action call() {
        System.out.println("STester/sdkdclient version: "+
                ClientVersion.getRevision());
        return Action.EXIT;
      }
    }.addShortAlias("V");

    RawOption pluginHelp = new InfoOption("plugin-help", "Show plugin help", parser) {
      @Override public Action call() {
        dumpPluginHelp = true;
        return Action.CONTINUE;
      }
    };
    pluginHelp.addLongAlias("test-help");
    pluginHelp.addLongAlias("full-help");

  }

  /**
   * @param rcb
   */
  public void configure(Configured<RunContextBuilder> rcb) {
    rcBuilder = rcb.get();
    RunDBAppender.setDatabase(rcBuilder.getDatabase());
    configured = true;
  }

  /**
   * Configures the harness using the built-in parser. This should only be used
   * from within the current class.
   */
  @Deprecated
  @Override
  public void configure() {
    try {
      OptionUtils.scanInlcudes(parser);
    } catch (IOException ex) {
      throw HarnessException.create(HarnessError.CONFIG, ex);
    }

    parser.addTarget(hb.getOptionTree());
    LogUtil.addDebugOption(parser);

    if (isMain) {
      setupInfoOptions();
    }

    parser.apply();
    Configured<HarnessBuilder> confHB = Configured.create(hb);
    rcBuilder = new RunContextBuilder(confHB);

    parser.addTarget(rcBuilder.getOptionTree());
    parser.apply();

    configure(Configured.create(rcBuilder));

    // Now that all plugins are loaded, add the 'test-help' option.
    if (dumpPluginHelp) {
      parser.dumpAllHelp();
      exit(0);
    }

    parser.throwOnUnrecognized();


    try {
      writeConfiguration(rcBuilder.getDatabase(), parser);
    } catch (SQLException ex) {
      throw new HarnessException(HarnessError.CONFIG, ex);
    }

  }

  @Override
  public boolean isConfigured() {
    return configured;
  }

  public void run() {
    RunContext rctx = rcBuilder.buildContext();
    rctx.run();
  }

  public RunDB getDb() {
    return rcBuilder.getDatabase();
  }

  public DatabasePath getDbPath() {
    return hb.getDatabasePath();
  }

  public synchronized void close() {


    RunDB db = rcBuilder.getDatabase();
    if (db != null) {
      db.close();
    }
  }

  private void exit(int code) {
    if (isMain) {
      System.exit(code);
    } else {
      throw new IllegalStateException("Cannot exit in non-main");
    }
  }

  public RunDB getDatabase() {
    return rcBuilder.getDatabase();
  }

  /**
   * Construct a new STester object.
   * @param argv The arguments
   */
  private STester(String[] argv) {
    isMain = true;
    args = argv;
    hb = new HarnessBuilder();
    parser = new OptionParser(argv);
    //noinspection ConstantConditions
    parser.setPrintAndExitOnError(isMain);
  }

  public STester(Configured<HarnessBuilder> hb) {
    this.hb = hb.get();
    this.isMain = false;
    this.args = new String[] {};
    this.parser = new OptionParser(this.args);
  }

  /**
   * Main entry point.
   * @param argv
   */
  public static void main(String[] argv) {
    Thread.currentThread().setName("STester Main");
    try {
      STester o = new STester(argv);
      //noinspection deprecation
      o.configure();
      o.run();
      System.exit(0);
    } catch (HarnessException ex) {
      logger.error("Caught harness exception of type {}", ex.getCode());
      logger.error("Details", ex);
    } catch (Exception ex) {
      logger.error("Got unknown exception", ex);
    }
    System.exit(1);
  }

}