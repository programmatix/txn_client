/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.util;

import org.apache.commons.io.IOUtils;
import org.slf4j.MDC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class is intended to gobble up streams one line at a time. It is an
 * abstract class and expects specific methods below to be implemented for
 * proper handling in the line.
 *
 * This class extends and runs in its own thread.
 */
public abstract class LineGobbler extends Thread {
  private final InputStream stream;
  private final BufferedReader lineReader;
  private String mdcName = null;


  public LineGobbler(InputStream stream) {
    super();
    setDaemon(true);
    this.stream = stream;
    lineReader = new BufferedReader(new InputStreamReader(stream));
    setName("Line Gobbler @" + stream);
  }


  /**
   * Handle a received line from the stream
   * @param ln The line received
   */
  protected abstract void msg(String ln);

  /**
   * Handle an error thrown on the stream error.
   * @param e The error thrown.
   *
   * The default implementation does nothing.
   */
  protected void err(Throwable e) { }

  /**
   * Called before the stream is about to be closed and the thread stopped.
   * This should ensure any cached data is flushed to its final destination.
   */
  protected void flush() {}

  /**
   * Sets the logging prefix for this logger.
   *
   * The prefix is used for the MDC context as well as for the thread name.
   *
   * @param pfx The prefix to use.
   */
  public void setLoggingPrefix(String pfx) {
    mdcName = pfx;
    setName("Line Gobbler: " + pfx);
  }


  @Override
  public void run() {
    while (true) {
      MDC.put("EXEC", mdcName);
      try {

        String line = lineReader.readLine();
        if (line == null) {
          flush();
          IOUtils.closeQuietly(lineReader);
          break;
        }

        msg(line);

      } catch (IOException ex) {
        flush();
        err(ex);
        IOUtils.closeQuietly(lineReader);
        break;

      } finally {
        MDC.remove("EXEC");
      }
    }
  }
}