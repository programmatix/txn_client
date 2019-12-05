/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.batch.suites;

import com.couchbase.sdkdclient.options.OptionConsumer;
import com.couchbase.sdkdclient.util.PluginLoader;
import java.io.IOException;
import java.util.List;

/**
 * A suite provides a means to provide a collection of {@link TestInfo} objects.
 */
public abstract class Suite implements OptionConsumer {
  final static String SPKG = "com.couchbase.sdkdclient.batch.suites";

  /**
   * Get a list of {@link TestInfo} objects.
   * @return a list of test info objects.
   * @throws IOException
   */
  public abstract List<TestInfo> getTests() throws IOException;
  public abstract void prepare() throws Exception;

  public static Suite find(String name) throws ClassNotFoundException {
    PluginLoader<Suite> pl = new PluginLoader<Suite>(Suite.class, SPKG);
    return pl.getPluginInstance(name);
  }
}