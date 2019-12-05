/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.options.RawOption.OptionAttribute;
import com.couchbase.sdkdclient.util.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.util.*;

/**
 * Parses a set of options. This class is optimized for the pattern of parsing
 * and reparsing a single command line based on dynamically loaded option
 * definitions.
 *
 * To use this class, one typically instantiates a new instance, passing the
 * initial argument array, and then performs these steps:
 *
 * <ol>
 * <li>Add option definitions using {@link #addTarget(OptionTree)}</li>
 * <li>Scan the arguments for strings matching the options just pushed, using the
 * {@link #apply() } method</li>
 * <li>Examine the parse options and determine which other consumers may be
 * available.</li>
 * <li>Once no more option definitions remain, the {@link #throwOnUnrecognized() }
 * method is called. This will cause the parser to throw an exception if anything
 * remains in the argv array, as it indicates an unrecognized option</li>
 * </ol>
 */
public class OptionParser {
  private String[] argv;
  private String[] remainingArgv;
  private boolean printExitOnError = false;
  private OptionLookup currentOptions;
  private OptionTree infoTree = new OptionTreeBuilder()
          .group("info")
          .description("Miscellaneous informational options")
          .build();

  private OptionLookup infoOptions = null;
  private List<OptionTree> allOptions = new ArrayList<OptionTree>();
  private static Logger logger = LogUtil.getLogger(OptionParser.class);

  public OptionParser(String[] origArgv) {
    argv = origArgv;
    remainingArgv = argv;
  }

  public OptionParser(List<String> origArgv) {
    this(origArgv.toArray(new String[origArgv.size()]));
  }

  /**
   * Sets whether the parser should print a help message and exit the program
   * when an error is encountered.
   * @param on
   */
  public void setPrintAndExitOnError(boolean on) {
    printExitOnError = on;
  }

  /**
   * Adds additional options definitions to the stack. This may be called
   * multiple times before calling {@link #apply() }. Option definitions are
   * retained, and the {@link #apply() } method will check any remaining
   * arguments inside the argv array to find a matching option.
   * @param options
   */
  public void addTarget(OptionTree options) {
    if (currentOptions == null) {
      currentOptions = new OptionLookup();
    }

    currentOptions.addGroup(options);
    allOptions.add(options);
  }

  void addInfoOptions(RawOption option) {
    if (infoOptions == null) {
      infoOptions = new OptionLookup();
      infoOptions.addGroup(infoTree);
    }
    logger.trace("Adding {}", option);
    infoTree.addOption(option);
    infoOptions.flatten(true);
  }


  //<editor-fold defaultstate="collapsed" desc="argv accessors">
  public void appendArgv(String[] toAdd) {
    String[] nextArgv = new String[remainingArgv.length + toAdd.length];
    int i;
    for (i = 0; i < remainingArgv.length; i++) {
      nextArgv[i] = remainingArgv[i];
    }

    for (String aToAdd : toAdd) {
      nextArgv[i++] = aToAdd;
    }
    remainingArgv = nextArgv;
  }

  public void prependArgv(String[] toAdd) {
    String[] nextArgv = new String[remainingArgv.length + toAdd.length];
    int i;
    for (i = 0; i < toAdd.length; i++) {
      nextArgv[i] = toAdd[i];
    }

    for (String aRemainingArgv : remainingArgv) {
      nextArgv[i++] = aRemainingArgv;
    }
    remainingArgv = nextArgv;
  }

  /**
   * Gets the original argument array as passed to the constructor.
   * @return
   */
  public List<String> getArgv() {
    return Arrays.asList(argv);
  }

  /**
   * Gets the remaining argv array containing all unparsed options. This is an
   * alternative to {@link #throwOnUnrecognized() } which allows the user to pass
   * unrecognized options to another parser.
   * @return
   */
  public List<String> getRemainingArgv() {
    if (remainingArgv == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(remainingArgv);
  }
  //</editor-fold>
  /**
   * Scans the remaining argv elements against the Option definitions passed
   * to the previous calls to {@link #addTarget(OptionTree)}. Once this method has
   * returned, the list of option definitions is empty.
   *
   * This method will throw an exception if:
   * <ul>
   * <li>Required options were not found</li>
   * <li>Options requiring an argument were specified without arguments</li>
   * <li>Values provided for an option failed to validate</li>
   * </ul>
   */
  public void apply() {
    logger.trace("Sweeping with argv={}", StringUtils.join(remainingArgv, " "));
    if (currentOptions == null) {
      throw new IllegalStateException("No current options");
    }

    currentOptions.flatten();
    if (remainingArgv == null) {
      logger.trace("No options remain. Sealing current");
      for (RawOption opt : currentOptions.getOptions()) {
        opt.seal();
      }
      return;
    }

    try {
      remainingArgv = CliParse.parseArgv(remainingArgv, currentOptions);
      boolean strict = true;

      if (infoOptions != null) {
        logger.trace("Scanning info options...");
        infoOptions.flatten();
        remainingArgv = CliParse.parseArgv(remainingArgv, infoOptions);
        for (RawOption opt : infoOptions.getOptions()) {
          opt.seal();
          if (opt.wasPassed()) {
            strict = false;
          }
        }
        sealCurrent(infoOptions, false);
      }
      sealCurrent(currentOptions, strict);


    } catch (CommandLineException ex) {
      if (printExitOnError) {
        logger.error(ex.getMessage());
        dumpUsage();
        System.exit(1);
      }
      throw new IllegalArgumentException(ex);

    } finally {
      currentOptions = null;
    }
  }

  private static void sealCurrent(OptionLookup coll, boolean strictMode)
          throws CommandLineException {

    // Scan all the options for required/remaining stuff..
    for (OptionInfo info : coll.getOptionsInfo()) {
      RawOption opt = info.getOption();
      if (opt.getAttribute(OptionAttribute.REQUIRED) && opt.wasPassed() == false && strictMode) {
        throw new CommandLineException(
                HelpUtils.prettyOption(info), "Required but missing");
      }

      if (opt.wasPassed() && opt.wasProvidedValue() == false && strictMode) {
        if (opt.mustHaveArgument()) {
          throw new CommandLineException(
                  HelpUtils.prettyOption(info), "Must have argument");
        }
      }
      if (!strictMode) {
        try {
          opt.seal();
        } catch (Exception ex) {
          logger.warn("Ignoring exception because not in strict mode", ex);
        }
      } else {
        opt.seal();
      }
    }
  }

  /**
   * Indicates that the parser has no more option definitions to parse.
   * This forces the parser to throw an exception if any unparsed items
   * remain inside its internal argv array.
   */
  public void throwOnUnrecognized() {
    if (remainingArgv == null || remainingArgv.length == 0) {
      return;
    }

    CommandLineException ex =
            new CommandLineException("Unrecognized Options: "
            + StringUtils.join(remainingArgv, ","));

    if (printExitOnError) {
      logger.error(ex.getMessage());
      dumpUsage();
      System.exit(1);
    } else {
      throw new RuntimeException(ex);
    }
  }

  public void dumpUsage() {
    PrintWriter pw = new PrintWriter(System.err);
    pw.printf("Usage: [CMDNAME] < ");

    for (OptionTree coll : allOptions) {
      for (OptionInfo info : coll.getAllOptionsInfo()) {
        if (!info.getOption().getAttribute(OptionAttribute.REQUIRED)) {
          continue;
        }
        pw.printf("%s=%s ",
                  HelpUtils.prettyOption(info), HelpUtils.prettyArgname(info));
      }
    }

    pw.println(">");
    pw.flush();
  }

  /**
   * Dumps the help text to stdout.
   */
  public void dumpAllHelp() {
    PrintWriter pw = new PrintWriter(System.out);
    Map<String,Collection<OptionInfo>> tmpOpts = new HashMap<String, Collection<OptionInfo>>();
    allOptions.add(infoTree);

    // Pre-Sort
    for (OptionTree coll : allOptions) {
      for (OptionInfo info : coll.getAllOptionsInfo()) {
        if (info.getOption().getAttribute(OptionAttribute.HIDDEN)) {
          continue;
        }

        OptionTree parent = info.getContainer();
        MapUtils.addToValue(tmpOpts, parent.getDescription(), info);
      }
    }

    for (Map.Entry<String, Collection<OptionInfo>> ent : tmpOpts.entrySet()) {
      pw.println(StringUtils.repeat("_", HelpUtils.LINE_WIDTH));
      pw.println(ent.getKey() + ":");
      for (OptionInfo info : ent.getValue()) {
        HelpUtils.dumpSingleOption(pw, info);
      }
      pw.println();
    }
    pw.flush();
  }

  public Collection<OptionTree> getAllTrees() {
    return allOptions;
  }
}