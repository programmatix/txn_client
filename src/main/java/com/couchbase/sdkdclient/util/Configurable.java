/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.util;

/**
 * A configurable object is one which is constructed and then maintains state
 * for its configuration. The configuration is performed at a certain point in
 * time - usually an indeterminate point.
 *
 * This interface provides the methods which may be used to declare an object
 * conforming to such semantics.
 *
 * @see Configured
 * @see Unconfigured
 */
public interface Configurable {

  /**
   * Configured this object. This method may end up throwing an exception
   * if the configuration itself requires additional parameters. This interface
   * does not cover these scopes. It is intended that once this method is called,
   * the {@link #isConfigured()} shall return true. Conversely, the {@link #isConfigured()}
   * method shall return false before this (or other) methods do so.
   */
  public void configure() throws UnsupportedOperationException;

  /**
   * Gets the configuration status.
   * @return true if the object has been configured, false otherwise.
   */
  public boolean isConfigured();
}
