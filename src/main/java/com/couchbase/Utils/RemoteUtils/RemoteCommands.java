package com.couchbase.Utils.RemoteUtils;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import com.couchbase.Couchbase.Couchbase.CouchbaseInstaller;
import com.couchbase.Logging.LogUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.io.File;
import java.io.FileOutputStream;


/**
 * Utility class containing all sorts of commands to use via SSH
 */
public class RemoteCommands {
  private final static Logger logger = LogUtil.getLogger(RemoteCommands.class);
  private ServiceLogin sshLogin;
  private SSHConnection sshConn;
  public static Future<SimpleCommand> runCommand(final SSHConnection conn,
                                                 final String cmd,
                                                 final String stdin) {
    ExecutorService svc = Executors.newSingleThreadExecutor();
    Future<SimpleCommand> ret = svc.submit(new Callable<SimpleCommand>() {

      @Override
      public SimpleCommand call() throws Exception {

        final SSHCapturedCommand captured = new SSHCapturedCommand(conn, cmd);
        if (stdin != null) {
          captured.setInputString(stdin);
        }

        captured.execute();

        SimpleCommand cmdRet = new SimpleCommand() {
          @Override
          public String getStderr() {
            return captured.getStderrBuffer();
          }
          @Override
          public String getStdout() {
            return captured.getStdoutBuffer();
          }
          @Override
          public boolean isSuccess() {
            if (!captured.hasExitCode()) {
              throw new RuntimeException("Code not yet available");
            }
            return captured.getExitCode() == 0;
          }
        };

        try {
          if (captured.waitForExit(10000) < 1) {
            logger.warn("Command not yet done");
          }
        } finally {
          captured.close();
        }

        return cmdRet;
      }
    });

    svc.shutdown();
    return ret;
  }


  public static Future<SimpleCommand> runCommand(SSHConnection conn, String cmd) {
    return runCommand(conn, cmd, null);
  }

  final static public String START = "start";
  final static public String STOP = "stop";
  final static public String RESTART = "restart";

  public static SimpleCommand runSimple(SSHConnection conn, String cmdStr, boolean checkStatus)
          throws IOException {

    Future<SimpleCommand> cmd = runCommand(conn, cmdStr);
    SimpleCommand cmdRet;

    try {
      cmdRet = cmd.get();
    } catch (Exception ex) {
      throw new IOException(ex);
    }
    if (checkStatus && cmdRet.isSuccess() == false) {
      throw new IOException("Command Failed! " + cmdRet.getStderr() + " " + cmdRet.getStdout());
    }
    return cmdRet;
  }

  public static SimpleCommand runSimple(SSHConnection conn, String cmdStr) throws IOException {
    return runSimple(conn, cmdStr, true);
  }

  public static void couchbaseServer(SSHConnection conn, String action) throws
          IOException {
    String baseStr = "service couchbase-server " + action;
    runSimple(conn, baseStr);
  }

  public static void iptablesClear(SSHConnection conn) throws IOException {
    runSimple(conn, "iptables -F");
    runSimple(conn, "iptables -t nat -F");
  }

  public static String iptablesGet(SSHConnection conn) throws IOException {
    return runSimple(conn, "iptables-save").getStdout();
  }

  public synchronized SSHConnection createSSH(String hostname) throws IOException {
    if (sshConn != null) {
      return sshConn;
    }

    sshConn = new SSHConnection(sshLogin.getUsername(),
                                sshLogin.getPassword(),
                                hostname);
    sshConn.connect();
    logger.info("SSH Initialized for {}", this);
    return sshConn;
  }

  public static String getInternalIP(SSHConnection conn) throws IOException {
    String cmd = "ifconfig | grep -Po 'inet \\K[\\d.]+'";
    String internalIP = null;
    internalIP = runSimple(conn, cmd).getStdout();
    if (internalIP.contains("127.0.0.1")) {
      internalIP = internalIP.replaceAll("\\s+","");
      internalIP = internalIP.replace("127.0.0.1", "");
    }
    System.out.println("\n\ninternal IP : " + internalIP);
    return internalIP;
  }

  public static void startCouchbase(SSHConnection conn) throws IOException {
    List<String> cmdList = new ArrayList<String>();
    cmdList.add("service couchbase-server start");
    cmdList.add("pkill -CONT -f memcached");
    cmdList.add("pkill -CONT -f beam.smp");
    cmdList.add("iptables -F");
    cmdList.add("iptables -t nat -F");

    String cmd = StringUtils.join(cmdList, " && ");
    runSimple(conn, cmd);
  }

  public interface OSInfo {
    public String getArch();
    public String getPlatform();
    public String getPackageType();
  }

  public static void downloadLog(SSHConnection conn, String host, String path) throws IOException {
    File outfile = new File(host + ".zip");
    OutputStream outputStream = new FileOutputStream(outfile);
    conn.copyFrom(path, outputStream);
  }

  /**
   * Gets basic information about a node.
   * @param conn
   * @return A structure which can be queried for system information.
   * @throws IOException
   */
  public static OSInfo getSystemInfo(SSHConnection conn) throws IOException {
    SimpleCommand cmd = runSimple(conn, "cat /etc/os-release");
    if (!cmd.isSuccess()) {
      throw new IOException("Command failed! ***Installer does not support windows and 32 bit builds ***");
    }

    String os = null;
    String version = null;
    String _packageType = null;
    String _platform = null;
    String _arch = null;

    String[] outs = cmd.getStdout().split("\n");

    for (String line:outs) {
      String[] parts = line.split("=");
      if (parts[0].compareToIgnoreCase("ID") == 0) {
        os = parts[1].toLowerCase().replaceAll("\\s","").replace("\"", "");
      }
      if (parts[0].compareToIgnoreCase("VERSION_ID") == 0) {
        version = (parts[1]).replaceAll("\\s","").replace("\"","");
        logger.debug("{}", version);
      }
    }

    cmd = runSimple(conn, "lscpu | head -n 1");
    if (!cmd.isSuccess()) {
      throw new IOException("Command failed! ***Installer does not support windows and 32 bit builds ***");
    }
    String out = cmd.getStdout();

    if (out.contains(CouchbaseInstaller.ARCH_64)) {
      _arch = CouchbaseInstaller.ARCH_64;
    } else if (out.contains(CouchbaseInstaller.AMD_64)){
      _arch = CouchbaseInstaller.AMD_64;
    } else if (out.contains(CouchbaseInstaller.ARCH_32)) {
      _arch = CouchbaseInstaller.ARCH_32;
    }

    if (os.compareToIgnoreCase("centos") == 0 && Integer.parseInt(version) == 6) {
      _platform = "centos6";
      _packageType = "rpm";
    }
    if (os.compareToIgnoreCase("centos") == 0  && Integer.parseInt(version) == 7) {
      _platform = "centos7";
      _packageType = "rpm";
    }
    if (os.compareToIgnoreCase("ubuntu") == 0 ) {
      _platform = os + version;
      _packageType = "deb";
      if (_arch == CouchbaseInstaller.ARCH_64) {
        _arch = CouchbaseInstaller.AMD_64;
      }
    }
    if (os.compareToIgnoreCase("debian") == 0) {
      _platform = os + version;
      _packageType = "deb";
      if (_arch == CouchbaseInstaller.ARCH_64) {
        _arch = CouchbaseInstaller.AMD_64;
      }
    }

    final String platform = _platform;
    final String packageType = _packageType;
    final String arch = _arch;

    return new OSInfo() {
      @Override
      public String getArch() {
        return arch;
      }

      @Override
      public String getPlatform() {
        return platform;
      }

      @Override
      public String getPackageType(){
        return packageType;
      }
    };
  }
}
