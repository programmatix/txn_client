package com.couchbase.Utils.RemoteUtils;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.couchbase.Logging.LogUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents a single command being run under an SSH session context
 *
 * @author mnunberg
 */
public abstract class SSHCommand implements Closeable {
  private static int NO_EXITCODE = Integer.MAX_VALUE;
  private volatile int exitCode = NO_EXITCODE;
  private volatile boolean closed = false;

  private String stdinString = null;
  private final String cmdline;
  private final SSHConnection conn;
  protected final static Logger logger = LogUtil.getLogger(SSHCommand.class);
  ChannelExec execChannel;

  public SSHCommand(SSHConnection parent, String command) {
    cmdline = command;
    conn = parent;
  }

  public void setInputString(String input) {
    stdinString = input;
  }

  public void handleStdin(OutputStream sin) throws IOException {
    if (stdinString == null || stdinString.isEmpty()) {
      sin.close();
      return;
    }

    sin.write(stdinString.getBytes());
    sin.close();
  }

  /**
   * Hook to handle the OutputStream for the commands' stdout
   *
   * @param sout
   */
  public abstract void handleStdout(InputStream sout) throws IOException;

  /**
   * Hook to handle the OutputStream for the command's stderr
   *
   * @param serr
   */
  public abstract void handleStderr(InputStream serr) throws IOException;

  /**
   * Actually exectutes the command. The command may not yet be completed when
   * this function returns.
   *
   * @throws IOException
   */
  public void execute() throws IOException {
    execChannel = conn.wireCommand(this);
    execChannel.setCommand(cmdline);
    logger.info("Running {} on {}", cmdline, conn.getHostname());
    InputStream stdout = execChannel.getInputStream();
    InputStream stderr = execChannel.getErrStream();
    OutputStream stdin = execChannel.getOutputStream();

    try {
      execChannel.connect();
    } catch (JSchException ex) {
      throw new IOException(ex);
    }

    handleStdin(stdin);
    handleStdout(stdout);
    handleStderr(stderr);
  }

  /**
   * Wait until the command has exited, blocking the current thread.
   * @param timeout
   * @return >= 0 if exited, -1 if timed out
   */
  public int waitForExit(int timeout) {
    long tmo = System.currentTimeMillis() + timeout;
    do {
      if (execChannel.isClosed()) {
        return 1;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        logger.warn("While waiting for exit", ex);
      }
    } while (System.currentTimeMillis() < tmo);

    return -1;
  }

  public int getExitCode() {
    return exitCode;
  }

  public boolean hasExitCode() {
    return exitCode != NO_EXITCODE;
  }

  public boolean isDone() {
    return waitForExit(0) != -1;
  }

  public void kill(boolean graceful) throws IOException {
    if (isDone()) {
      return;
    }
    String sigStr;
    if (graceful) {
      sigStr = "TERM";
    } else {
      sigStr = "KILL";
    }
    try {
      execChannel.sendSignal(sigStr);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    logger.debug("Closing channel {}", execChannel);
    exitCode = execChannel.getExitStatus();
    execChannel.disconnect();
  }
}
