/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.ssh;

import com.couchbase.sdkdclient.util.LineGobbler;
import java.io.IOException;
import java.io.InputStream;

/**
 * Command which captures its input and output streams
 */
public class SSHCapturedCommand extends SSHCommand {

  private static class SbGobbler extends LineGobbler {
    final StringBuilder sb;

    public SbGobbler(InputStream strm, StringBuilder toAppend) {
      super(strm);
      sb = toAppend;
    }

    @Override
    protected void msg(String ln) {
      sb.append(ln).append('\n');
    }
  }


  private final StringBuilder sbStdout = new StringBuilder();
  private final StringBuilder sbStderr = new StringBuilder();

  private LineGobbler gobbleStdout;
  private LineGobbler gobbleStderr;

  public SSHCapturedCommand(SSHConnection parent, String command) {
    super(parent, command);
  }

  @Override
  public void handleStdout(InputStream sout) throws IOException {
    gobbleStdout = new SbGobbler(sout, sbStdout);
    gobbleStdout.setLoggingPrefix("Remote-stdout");
    gobbleStdout.start();
  }

  @Override
  public void handleStderr(InputStream serr) throws IOException {
    gobbleStderr = new SbGobbler(serr, sbStderr);
    gobbleStderr.setLoggingPrefix("Remote-stderr");
    gobbleStderr.start();
  }

  public String getStdoutBuffer() {
    try {
      gobbleStdout.join();
    } catch (InterruptedException ex) {
      logger.warn("While joining..", ex);
      //
    }
    return sbStdout.toString();
  }

  public String getStderrBuffer() {
    try {
      gobbleStderr.join();
    } catch (InterruptedException ex)  {
      logger.warn("While joining..", ex);
    }
    return sbStderr.toString();
  }
}