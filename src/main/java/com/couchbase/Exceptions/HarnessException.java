package com.couchbase.Exceptions;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Class to indicate an exception while running the harness. This will always
 * wrap another exception
 *
 * @author mnunberg
 */
public class HarnessException extends RuntimeException {

  private final HarnessError err;

  public HarnessError getCode() {
    return err;
  }

  public HarnessException(Throwable e) {
    super(e);
    err = HarnessError.GENERIC;
  }

  public HarnessException(HarnessError code, Throwable e) {
    super(e);
    err = code;
  }

  public HarnessException(HarnessError code, String msg) {
    super(msg);
    err = code;
  }

  @Override
  public String toString() {
    return String.format("Harness error of type: %s: %s", err, super.toString());
  }


  public static HarnessException create(HarnessError err, Throwable thrown) {
    HarnessException exHarness = null;
    // Find the first "harness error"

    for (Throwable cur = thrown; cur != null; cur = cur.getCause()) {
      if (cur instanceof HarnessException) {
        exHarness = (HarnessException) cur;
      }
    }

    if (exHarness != null) {
      return exHarness;
    }

    return new HarnessException(err, thrown);
  }
}