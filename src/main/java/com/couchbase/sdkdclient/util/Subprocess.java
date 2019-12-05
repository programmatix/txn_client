/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.util;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface used by various {@link com.couchbase.sdkdclient.driver.ExecutingDriver} implementations to represent
 * a child process.
 */
public interface Subprocess {

  /**
   * Get the standard input.
   * @return a stream representing the process' {@code stdin}
   */
  public OutputStream getStdin();
  public InputStream getStdout();
  public InputStream getStderr();

  /**
   * Get the termination code for the SDKD.
   * @return The exit code. This value is only valid if {@link #terminate()}
   * has been called and/or {@link #isTerminated()} returns {@code true}.
   */
  public int getExitCode();

  /**
   * Unconditionally terminates this process. This should not return until
   * the process has terminated.
   */
  public void terminate();

  /**
   * Indicates whether this process has been terminated.
   *
   * A process may be terminated either gracefully or by calling the
   * {@link #terminate()} method.
   *
   * @return true if terminated, false if still running.
   */
  public boolean isTerminated();
}
