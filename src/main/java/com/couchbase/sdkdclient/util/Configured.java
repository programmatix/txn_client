/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.util;

import javax.annotation.Nonnull;

/**
 * Class which may be used to declare that a specific object has been configured.
 * This container works by calling the {@link com.couchbase.sdkdclient.util.Configurable#configure()}
 * method if the object is not yet configured.
 *
 * The benefit of using this class is that the configuration takes place upon
 * construction. A method consuming a {@code Configurable} can then be
 * assured that the underlying object has indeed been configured.
 *
 * @param <T>
 */
public class Configured<T extends Configurable> {
  private final T target;

  /**
   * Creates a new configured object
   * @param target A potentially unconfigured object.
   */
  private Configured(@Nonnull T target) {
   // System.out.println("Starting Printing from Configured START");

    if (!target.isConfigured()) {
    //  System.out.println("Starting Printing from Configured IFLOOP");

      target.configure();
    }
  //System.out.println("Starting Printing from Configured");
    this.target = target;
  }

  /**
   * Gets the <b>configured</b> object
   * @return A configured object.
   */
  public final T get() {
    return target;
  }

  /**
   * Shorthand for constructing a new instance. Equivalent to
   * {@code new Configured<T>(src) }
   * @param src
   * @param <T>
   * @return
   */
  public static <T extends Configurable> Configured<T> create(T src) {
  //  System.out.println("Starting Printing from  Configurable Configured");

    return new Configured<T>(src);


  }
}
