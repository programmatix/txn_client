/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.options;

public interface OptionInfo {
  /**
   * Gets the actual option
   * @return the Option object
   */
  public RawOption getOption();

  /**
   * Gets the container for this option, i.e the tree in which this option
   * was found. Note that this information is not authorative and that the
   * container may change. This represents the container at the time the
   * OptionInfo object was created
   * @return
   */
  public OptionTree getContainer();


  /**
   * Returns the "canonical" name for the option. The canonical name
   * is the option with all its prefixes.
   *
   * In this format, prefixes are delimited by a slash ('/')
   * @return The canonical name
   */
  public String getCanonicalName();
}
