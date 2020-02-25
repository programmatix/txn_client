/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.transactions.test;

import java.util.function.BooleanSupplier;

/**
 * Provides a bunch of utility APIs that help with testing.
 */
public class Util {

  /**
   * Waits and sleeps for a little bit of time until the given condition is met.
   *
   * <p>Sleeps 10ms between "false" invocations.</p>
   *
   * @param supplier return true once it should stop waiting.
   */
  public static void waitUntilCondition(final BooleanSupplier supplier) {
    while (!supplier.getAsBoolean()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

}
