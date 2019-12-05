/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.options;

/**
 * Interface which declares that a given class consumes a set of options.
 * Options should be configured either via the commandline or via accessing
 * the sub-properties found.
 *
 * @see OptionTree
 * @see OptionUtils
 * //TODO: Deprecate this in favor of {@link com.couchbase.sdkdclient.util.ConfigurableOptionConsumer}
 */
public interface OptionConsumer {

  /**
   * Get the options container; that is, the object which will receive the
   * options
   *
   * @return an OptionsContainer object
   */
  public OptionTree getOptionTree();
}
