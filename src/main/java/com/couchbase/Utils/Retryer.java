package com.couchbase.Utils;/*
 * Copyright (c) 2013 Couchbase, Inc.
 */


import com.couchbase.Exceptions.RestApiException;

import java.io.IOException;

/**
 * This class is meant to cut out a common boilerplate idiom where we tolerate
 * a certain amount (and type) of errors for a specific amount of time, and
 * then raise the last caught error if the operation did not succeed.
 *
 * The normal usage of this class is something like this:
 *
 * <pre>{@code
 * new Retryer<MyException>(60000, 1000, MyException.class) {
 *   @Override
 *   protected boolean tryOnce() throws MyException {
 *     if (something.doStuff() == SUCCESS) {
 *       return true;
 *     } else if (
 *       something.hasErrorCondition() {
 *       throw new MyException();
 *     }
 *     return false;
 *   }
 * }.call();
 * }</pre>
 *
 * @param <T> The type of exception to be caught and rethrown
 */
public abstract class Retryer <T extends Exception> {
  public static class RetryTimedOutException extends RuntimeException {

  }
  public static class RetryExecutionException extends RuntimeException {
    public RetryExecutionException(Throwable e) {
      super(e);
    }
  }

  private final long tmoMax;
  private final long interval;
  private final Class<? extends T> clsObj;
  private boolean shouldRethrow = false;

  /**
   * Create a new retryer object
   * @param runFor Run at most for this length of time, in milliseconds
   * @param sleep Sleep this amount, in milliseconds between each failure
   * @param cls Class object of the exception to throw.
   */
  public Retryer(long runFor, long sleep, Class<? extends T> cls) {
    tmoMax = runFor;
    interval = sleep;
    clsObj = cls;
  }

  public Retryer(long runFor, Class<? extends T> cls) {
    this(runFor, 0, cls);
  }

  /**
   * Try to perform the specified operation.
   *
   * This operation may throw an
   * exception of type {@link T} if it fails, or it may return {@code false}
   * to indicate a failure as well. In both these cases, the thread will sleep
   * for the time specified in the constructor if the operation did not
   * succeed. If the total time elapsed is greater than specified in the
   * constructor then an exception (either of type {@link T} if any exception
   * was previously caught - or a {@link java.lang.RuntimeException} if no
   * other exception was found, or if an exception of a different type was
   * caught) is thrown.
   * @return true if the operation succeeded. False otherwise.
   * @throws T
   */
  protected abstract boolean tryOnce() throws T;


  /**
   * This may be overridden by a subclass to specify a different behavior if
   * an exception is caught.
   *
   * This is called once the interval has elapsed and an exception from a prior
   * iteration has been caught. The default behavior is to rethrow the exception,
   * but an implementation may choose to throw a different exception or suppress
   * it.
   *
   * @param caught The caught exception.
   * @throws T
   */
  protected void handleError(T caught) throws T, InterruptedException {
    if (caught instanceof RestApiException) {
      RestApiException ex = (RestApiException)caught;
      if (ex.getStatusLine().getStatusCode() >= 500) {
        call();
      }
    }
    throw caught;
  }

  /**
   * Causes the caught exception to be thrown on the next iteration.
   */
  protected void rethrow() {
    shouldRethrow = true;
  }

  /**
   * Start performing the operation. See {@link #tryOnce()} for a description.
   * @throws T
   */
  public void call() throws T, InterruptedException {
    long tmo = System.currentTimeMillis() + tmoMax;
    T caught;

    do {
      try {
        if (tryOnce()) {
          return;
        }

        caught = null;

      } catch (Exception ex) {
        if (clsObj.isInstance(ex)) {
          caught = clsObj.cast(ex);
          if (shouldRethrow) {
            throw caught;
          }
        } else {
          throw new RetryExecutionException(ex);
        }
      }
      if (interval > 0) {
        Thread.sleep(interval);
      }
    } while(System.currentTimeMillis() < tmo);

    if (caught != null) {
      handleError(caught);
    } else {
      throw new RetryTimedOutException();
    }
  }

  /**
   * Convenience class to wrap an IOException.
   */
  public static abstract class IORetryer extends Retryer<IOException> {
    public IORetryer(long runFor, long interval) {
      super(runFor, interval, IOException.class);
    }

    public IORetryer(long runFor) {
      this(runFor, 0);
    }
  }
}
