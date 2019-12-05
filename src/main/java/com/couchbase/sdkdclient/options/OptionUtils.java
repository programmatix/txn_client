/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.options;

import com.couchbase.sdkdclient.options.RawOption.OptionAttribute;

import java.io.File;
import java.io.IOException;

/**
 * Utilitly class to deal with {@link OptionTree} and {@link RawOption} objects.
 */
public class OptionUtils {
  private OptionUtils() {}


  /**
   * <i>seal</i> the tree.
   * @param tree
   */
  public static void sealTree(OptionTree tree) {
    for (OptionInfo info : tree.getAllOptionsInfo()) {
      if (!info.getOption().isSealed()) {
        info.getOption().seal();
      }
      info.getOption().checkRequired();
    }
  }

  /**
   * Reset the tree. If any options contained in the tree were <i>sealed</i>,
   * they are reset.
   * @param tree
   */
  public static void resetTree(OptionTree tree) {
    for (OptionInfo info : tree.getAllOptionsInfo()) {
      info.getOption().reset();
    }
  }

  /**
   * Utility method to add an <i>informational option</i> to the parser.
   * An informational option is a switch which will dump some information
   * and exit, i.e. {@code  '-h', '--help' '--version'} etc.
   *
   * This is an abstract class and implementations must implement the
   * {@link #call()} method which shall return either {@link Action#EXIT} to
   * indicate that the application should successfully exit, or {@link Action#CONTINUE}
   * to indicate that control should continue.
   */
  public static abstract class InfoOption extends BoolOption {
    public enum Action { EXIT, CONTINUE }

    /**
     * @param name the name of the option
     * @param description The description of the option
     * @param parser The parser with which to register the option.
     */
    public InfoOption(String name, String description, OptionParser parser) {
      super(name, description, false);
      parser.addInfoOptions(this);
    }

    public abstract Action call() throws Exception;

    @Override
    void setFound() {
      super.setFound();
      try {
        Action res = call();
        if (res == Action.EXIT) {
          System.exit(0);
        }
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public static abstract class InfoHookOption extends InfoOption {
    public InfoHookOption(String name, String desc, OptionParser parser) {
      super(name, desc, parser);
    }

    abstract protected void hook();

    @Override
    public Action call() throws Exception {
      hook();
      return Action.CONTINUE;
    }
  }

  public static abstract class InfoExitOption extends InfoOption {
    public InfoExitOption(String name, String desc, OptionParser parser) {
      super(name, desc, parser);
    }

    abstract protected void hook();

    @Override
    public Action call() throws Exception {
      hook();
      return Action.EXIT;
    }
  }


  /**
   * Add a {@code help} option to the given parser.
   * @param parser
   * @return The help option.
   */
  public static RawOption addHelp(final OptionParser parser) {
    RawOption ret = new InfoOption("help", "this message", parser) {
      @Override public Action call() {
        parser.dumpAllHelp();
        return Action.EXIT;
      }
    };
    ret.addShortAlias("h");
    return ret;
  }

  /**
   * Scan the commandline for any configuration files. This is done first
   * before looking at other options, as the include files may include plugins
   * which are expected to handle the other options on the commandline.
   *
   * This method will perform the creation of the {@code include} option and then
   * invoke a parse sweep. It will then apply the found includes to the
   * {@code argv} array within the parse object.
   *
   * @param parser The parser which is being used.
   *
   */
  public static void scanInlcudes(OptionParser parser) throws IOException {
    MultiOption optInclude = new MultiOption("include");
    optInclude.setShortName("I");
    optInclude.setDescription("Include additional configuration files");

    OptionTree tree = new OptionTreeBuilder()
            .description("Extra includes")
            .group("include")
            .build();

    tree.addOption(optInclude);
    parser.addTarget(tree);
    parser.apply();
    for (String incFile : optInclude.getRawValues()) {
      Argfile af = new Argfile(new File(incFile));
      parser.prependArgv(af.getArgv());
    }
  }

  public static void hide(OptionTree tree, String... paths) {
    for (String path : paths) {
      tree.find(path).setAttribute(OptionAttribute.HIDDEN, true);
    }
  }

  public static void hideAll(OptionTree tree, String... excludes) {
    for (OptionInfo info : tree.getAllOptionsInfo()) {
      info.getOption().setAttribute(OptionAttribute.HIDDEN, true);
    }

    for (String path : excludes) {
      tree.find(path).setAttribute(OptionAttribute.HIDDEN, false);
    }
  }

  public static void makeImmutable(RawOption opt) {
    opt.setFound();
    opt.setArgFound();
    opt.seal();
    opt.setAttribute(OptionAttribute.DISABLED, true);
  }
}
