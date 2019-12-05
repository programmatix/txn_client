/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.options;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An option prefix defines the "environment" of a specific option. A
 * prefix requires a canonical 'Name' as well as several prefix aliases.
 *
 * See {@link OptionDomains} for usages of this class.
 */
public class OptionPrefix {
  final private String name;
  final private Set<String> aliases = new HashSet<String>();

  /** The "Root" prefix. This is the empty string */
  public static final OptionPrefix ROOT = new OptionPrefix("");

  /** Character used to delimit prefix names */
  public static final String PREFIX_DELIM = "/";

  /**
   * Create a new prefix
   * @param nn The primary name to use
   * @param names An array of aliases.
   */
  public OptionPrefix(String nn, String... names) {
    name = RawOption.transformName(nn);
    aliases.add(nn);
    for (String ss : names) {
      aliases.add(RawOption.transformName(ss));
    }
  }

  public OptionPrefix(String[] nn) {
    if (nn.length == 0) {
      throw new IllegalArgumentException("Must have at least one item");
    }

    name = RawOption.transformName(nn[0]);
    for (String s : nn) {
      aliases.add(RawOption.transformName(s));
    }
  }

  private static String makeHier(String parent, String child) {
    if (parent == null || parent.isEmpty()) {
      return child;
    }
    if (child == null || child.isEmpty()) {
      return parent;
    }

    return parent + RawOption.DELIMITER + child;
  }

  /**
   * Create a new prefix combining the parent and child prefixes. The
   * resultant prefix may be {@code parent/child} if both parent and child
   * are not-null and not root; If {@code child} is null or empty, the prefix
   * will be identical to the parent. If {@code parent} is null, the prefix
   * will be identical to the child.
   * @param parent The child to use
   * @param child The parent to use.
   */
  public OptionPrefix(OptionPrefix parent, OptionPrefix child) {
    name = makeHier(parent.getName(), child.getName());
    for (String ca : child.getAliases()) {
      for (String pa :parent.getAliases()) {
        aliases.add(makeHier(pa, ca));
      }
    }
  }

  /**
   * Assuming that {@code nn} is a descendent of this prefix, return the
   * fully qualified name of {@code nn}
   * @param nn The name to evaluate.
   * @return A string in the form of {@code /-.../name/nn } where {@code name}
   * is the name of this current object's prefix.
   */
  String canonicalFormat(String nn) {
    return makeHier(name, nn);
  }

  /**
   * Like {@link #canonicalFormat(String)}, but returns a collection of all
   * prefixes - considering any aliases as well.
   * @param nn
   * @return
   */
  Collection<String> formatAll(String nn) {
    List<String> ll = new ArrayList<String>();
    for (String alias : aliases) {
      ll.add(makeHier(alias, nn));
    }
    return ll;
  }

  /**
   * @return The fully qualified name of this prefix.
   */
  public String getName() {
    return name;
  }

  public boolean isRoot() {
    return aliases.size() == 0 && name.equals("");
  }

  /**
   * @return Any aliases associated with this prefix.
   */
  public Collection<String> getAliases() {
    return aliases;
  }

  /**
   * Evaluates the string {@code s} against the prefix's name and aliases
   * @param s The string to match
   * @return true if the string matches the prefix name or any of its aliases.
   */
  public boolean matches(String s) {
    for (String aa : aliases) {
      if (s.equals(aa)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return StringUtils.join(aliases, ";");
  }
}
