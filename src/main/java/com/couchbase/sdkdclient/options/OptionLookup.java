/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.options;

import com.couchbase.sdkdclient.logging.LogUtil;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Top level option lookup.
 * This class contains multiple hierarchies of 'OptionCollection' classes
 * and applies all their relevant prefixes.
 */

public class OptionLookup {
  private final Logger logger = LogUtil.getLogger(OptionLookup.class);
  private final OptionTree root = new OptionTree();

  private final Map<String, RawOption> longMap = new HashMap<String, RawOption>();
  private final Map<String, RawOption> shortMap = new HashMap<String, RawOption>();
  private final Map<String, RawOption> canonicalMap = new HashMap<String, RawOption>();
  private boolean flattened = false;

  public OptionLookup() {}

  private void addOption(String name, RawOption option, Map<String,RawOption> target) {
    if (!target.containsKey(name)) {
      logger.trace("Adding {}", name);
      target.put(name, option);
    }
  }

  private void recurseGroup(OptionTree tree, OptionPrefix prefix) {
    // Scan the options of the tree itself
    prefix = new OptionPrefix(prefix, tree.getPrefix());
    for (RawOption opt : tree.getOptions()) {
      // Add the "Main" option.
      addOption(prefix.canonicalFormat(opt.getName()), opt, canonicalMap);

      // Add main aliases.
      for (String ss : prefix.formatAll(opt.getName())) {
        addOption(ss, opt, longMap);
      }

      for (String ll : opt.getLongAliases()) {
        for (String ss : prefix.formatAll(ll)) {
          addOption(ss, opt, longMap);
        }
      }

      if (opt.getShortName() != null) {
        shortMap.put(opt.getShortName(), opt);
      }
      for (String ss : opt.getShortAliases()) {
        shortMap.put(ss, opt);
      }

      // Add absolute aliases
      for (String ss : opt.getAbsoluteAliases()) {
        addOption(ss, opt, longMap);
      }
    }

    // Recurse child options..
    for (OptionTree child : tree.getChildren()) {
      recurseGroup(child, prefix);
    }
  }

  public void addGroup(OptionTree tree) {
    flattened = false;
    root.addChild(tree);
  }


  /**
   * Recurse through all the added structures and flatten their namespaces.
   * This step is necessary before performing any sort of subsequent lookups
   */
  public void flatten() {
    flatten(false);
  }

  public void flatten(boolean force) {
    if (flattened == false || force == true) {
      recurseGroup(root, OptionPrefix.ROOT);
    }
    flattened = true;
  }

  /**
   * Retrieves the {@link RawOption} class by one of its aliases.
   * @param shortName A short alias to search for.
   * @param longName A long alias to search for.
   * @return The option, or null if no such option exists.
   *
   * Note that at least one of {@code sShort} or {@code sLong} must be specified.
   */
  public RawOption findOption(String shortName, String longName) {
    flatten();

    if (shortName == null && longName == null) {
      throw new IllegalArgumentException("one short or long option must be specified");
    }


    RawOption ret = null;
    if (longName != null) {
      longName = RawOption.transformName(longName);
      logger.trace("Looking up {}", longName);
      ret = longMap.get(longName);
    }

    if (ret == null && shortName != null) {
      ret = shortMap.get(shortName);
    }
    return ret;
  }

  public Collection<RawOption> getOptions() {
    flatten();
    return canonicalMap.values();
  }

  public Collection<OptionInfo> getOptionsInfo() {
    return root.getAllOptionsInfo();
  }
}