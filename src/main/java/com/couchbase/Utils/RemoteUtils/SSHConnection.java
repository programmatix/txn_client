package com.couchbase.Utils.RemoteUtils;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.couchbase.Logging.LogUtil;
import com.jcraft.jsch.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This represents a single SSH connection/session used for executing commands.
 *
 * @author mnunberg
 */
public class SSHConnection {
  private static class SSHLogger implements com.jcraft.jsch.Logger {

    private final static Logger logger = LogUtil.getLogger(SSHLogger.class);
    @Override
    public boolean isEnabled(int level) {
      return true;
    }

    @Override
    public void log(int level, String message) {
      if (level == DEBUG) {
        logger.debug(message);
      } else if (level == INFO) {
        logger.info(message);
      } else if (level == WARN) {
        logger.warn(message);
      } else if (level == ERROR || level == FATAL) {
        logger.error(message);
      }
    }
  }

  static {
//    JSch.setLogger(new SSHLogger());
  }

  private final String username;
  private final String password;
  private final String hostname;
  private final int port;

  private final Logger logger = LogUtil.getLogger(SSHConnection.class);
  private final JSch jsch = new JSch();
  private Session session;

  private void initUserKeys() {
    File userDir = new File(System.getProperty("user.home"), ".ssh");
    File[] children = userDir.listFiles();
    if (children == null) {
      return;
    }
    for (File child : children) {
      if (!child.isFile()) {
        continue;
      }

      if (! (child.getName().equals("id_rsa") || child.getName().equals("id_rsa"))) {
        continue;
      }
      try {
        jsch.addIdentity(child.getAbsolutePath());
        logger.debug("Loaded indentity {}", child.getAbsoluteFile());
      } catch (JSchException ex) {
        logger.warn("Bad private key", ex.getMessage());
      }
    }
  }

  public SSHConnection(String user, String pass, String host, int port) {
    username = user;
    password = pass;
    hostname = host;
    this.port = port;
   // initUserKeys();
  }

  public SSHConnection(String user, String pass, String host) {
    this(user, pass, host, 22);
  }

  public void connect() throws IOException {
    try {
      session = jsch.getSession(username, hostname, port);
    } catch (JSchException ex) {
      throw new IOException(ex);
    }

    logger.debug("Connecting to {} with User[{}] Pass[HASH={}]", session.getHost(),
            username, password == null ? "<NONE> " : DigestUtils.md5Hex(password));

    // Disable host key checking:
    // http://www.mail-archive.com/jsch-users@lists.sourceforge.net/msg00529.html
    session.setConfig("StrictHostKeyChecking", "no");

    if (password != null && password.length() > 0) {
      session.setPassword(password);
    }

    try {
      session.connect();
    } catch (JSchException ex) {
      throw new IOException(ex);
    }
  }

  public void close() throws IOException {
    session.disconnect();
  }

  synchronized ChannelExec wireCommand(SSHCommand cmd) throws IOException {
    try {
      Channel ch = session.openChannel("exec");
      return (ChannelExec) ch;
    } catch (JSchException ex) {
      throw new IOException(ex);
    }
  }

  public String getHostname() {
    return hostname;
  }

  void releaseCommand(SSHCommand cmd) {
  }

  /**
   * Write a stream to a remote file
   * @param dstName The destination filename
   * @param stream The stream to write
   * @param mode The mode (i.e. octal Unix permissions) the file should have
   * @throws IOException
   */
  public void copyTo(String dstName, InputStream stream, int mode) throws IOException {
    try {
      ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
      sftp.connect();
      sftp.put(stream, dstName, mode);

    } catch (JSchException ex) {
      throw new IOException(ex);
    } catch (SftpException ex) {
      throw new IOException(ex);
    }
  }

  /** Read from a remote file
   * @param remoteFilePath path of the file to be read
   * @param outputStream stream to read to
   * @throws IOException
   */
  public void copyFrom(String remoteFilePath, OutputStream outputStream) throws IOException {
    try {
      ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
      sftp.connect();
      sftp.get(remoteFilePath, outputStream);

    } catch (JSchException ex) {
      throw new IOException(ex);
    } catch (SftpException ex) {
      throw new IOException(ex);
    }
  }
}
