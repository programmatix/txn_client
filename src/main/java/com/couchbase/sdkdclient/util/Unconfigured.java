/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.util;

import javax.annotation.Nonnull;

/**
 * This object is the opposite of {@link Configured}. Whereas the latter ensures
 * that an object <b>is</b> configured, this ensures an object is <b>not</b>
 * configured. This is useful if the configuration requires or depends on state
 * which the recipient is intended to configure.
 *
 * @param <T>
 */
public class Unconfigured <T extends Configurable> {
  private final T inner;
  private Unconfigured(@Nonnull T obj) {
    if (obj.isConfigured()) {
      throw new IllegalStateException("Object must not be configured");
    }
    inner = obj;
  }

  public T get() {
    return inner;
  }

  public static <T extends Configurable> Unconfigured<T> create(T obj) {
    return new Unconfigured<T>(obj);
  }
}
