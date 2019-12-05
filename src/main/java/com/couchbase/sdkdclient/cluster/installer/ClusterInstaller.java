/*
* Copyright (c) 2013 Couchbase, Inc.
*/

package com.couchbase.sdkdclient.cluster.installer;

import com.couchbase.sdkdclient.cluster.NodeHost;
import com.couchbase.sdkdclient.cluster.RemoteCommands;
import com.couchbase.sdkdclient.cluster.RemoteCommands.OSInfo;
import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.options.OptionConsumer;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.ssh.SSHConnection;
import com.couchbase.sdkdclient.ssh.SSHLoggingCommand;
import com.couchbase.sdkdclient.util.Configurable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * Class to handle cluster installation. This provides a <b>much</b> quicker
 * alternative to what is provided with {@code testrunner}.
 *
 * To use this class you should first initialize it with a version you wish
 * which provides a {@code main()} method.
 */
public class ClusterInstaller implements OptionConsumer, Configurable {

  static class VersionTuple {
    final String full;
    final String major;
    final String minor;
    final String patch;
    final String build;

    public VersionTuple(String major, String minor, String patch, String full, String build) {
      this.major = major;
      this.minor = minor;
      this.patch = patch;
      this.full = full;
      this.build = build;
    }
    public static VersionTuple parse(String vString) throws Exception {
      String[] ret = new String[2];
      String[] parts = vString.split("\\.");

      String trailer[] = parts[parts.length - 1].split("-");

      return new VersionTuple(parts[0], parts[1], trailer[0], vString, trailer[1]);
    }
  }


  final private static Logger logger = LogUtil.getLogger(ClusterInstaller.class);
  public static final String ARCH_32 = "x86";
  public static final String ARCH_64 = "x86_64";
  public static final String AMD_64 = "amd64";
  static final String SHERLOCK_BUILD_URL = "http://172.23.120.24/builds/latestbuilds/couchbase-server/sherlock/";
  static final String WATSON_BUILD_URL = "http://172.23.120.24/builds/latestbuilds/couchbase-server/watson/";
  static final String SPOCK_BUILD_URL = "http://172.23.120.24/builds/latestbuilds/couchbase-server/spock/";
  static final String VULCAN_BUILD_URL = "http://172.23.120.24/builds/latestbuilds/couchbase-server/vulcan/";
  static final String ALICE_BUILD_URL="http://172.23.120.24/builds/latestbuilds/couchbase-server/alice/";
  static final String MADHATTER_BUILD_URL="http://172.23.120.24/builds/latestbuilds/couchbase-server/mad-hatter/";
  static final String SHERLOCK_RELEASE_URL = "http://172.23.120.24/builds/releases/";
  static final String WATSON_RELEASE_URL = "http://172.23.120.24/builds/releases/";
  public static final String RSRC_SCRIPT = "installer/cluster-install.py";


  final InstallerOptions options;
  VersionTuple versionTuple;

  private static InputStream getInstallScript() {
    InputStream is = ClusterInstaller.class
            .getClassLoader().getResourceAsStream(RSRC_SCRIPT);
    if (is == null) {
      throw new RuntimeException("Can't find script:" + RSRC_SCRIPT);
    }
    return is;
  }


  private String buildURL(VersionTuple vTuple, OSInfo osInfo) throws IOException {
    if (options.getURL() != "") {
      return options.getURL();
    }
    if (osInfo.getArch() == null || osInfo.getPlatform() == null || osInfo.getPackageType() == null) {
      throw new IOException("Unable to get os info");
    }
    String baseUrl = "";
    if (vTuple.major.equals("4")) {
      int minor = Integer.parseInt(vTuple.minor);
      baseUrl = SHERLOCK_BUILD_URL;
      if (minor >= 5) {
        baseUrl = WATSON_BUILD_URL;
      }
    } else if (vTuple.major.equals("5")) {
      int minor = Integer.parseInt(vTuple.minor);
      baseUrl = SPOCK_BUILD_URL;
      if (minor >= 5){
        baseUrl = VULCAN_BUILD_URL;
      }
    }
    else if (vTuple.major.equals("6")) {
      int minor = Integer.parseInt(vTuple.minor);
      baseUrl = ALICE_BUILD_URL;
      if (minor >= 5){
        baseUrl = MADHATTER_BUILD_URL;
      }
    }
    if (baseUrl == "") {
      throw new IOException("Base url not found for build. Installer supports only sherlock, watson and spock");
    }

    StringBuilder urlStr = new StringBuilder();
    urlStr.append(baseUrl + vTuple.build + "/");
    urlStr.append("couchbase-server-" + options.getBuildType());
    if (osInfo.getPlatform().contains("centos")) {
      urlStr.append("-");
      urlStr.append(vTuple.full + "-");
      urlStr.append(osInfo.getPlatform());
      urlStr.append("." + osInfo.getArch() + "." + osInfo.getPackageType());
    } else {
      urlStr.append("_");
      urlStr.append(vTuple.full + "-");
      urlStr.append(osInfo.getPlatform());
      urlStr.append("_" + osInfo.getArch() + "." + osInfo.getPackageType());
    }
    return urlStr.toString();
  }

  /**
   * Runs the install process for a single node.
   * @param nn The node to install the installation for.
   * @throws IOException
   */
  private void runNode(NodeHost nn, boolean upgrade, String upgradeVersion) throws IOException {

    /**
     * Now to get system information.. we need SSH
     */
    SSHConnection sshConn = nn.createSSH();
    OSInfo osInfo = RemoteCommands.getSystemInfo(sshConn);
    VersionTuple vTuple;

    try{
        if (upgrade) {
          vTuple = VersionTuple.parse(upgradeVersion);
        } else {
          vTuple = options.getVTuple();
        }
    } catch (Exception ex) {
      throw new IOException("Unable to parse version " + ex.getStackTrace());
    }
    /**
     * Build URL.
     */

    URL dlUrl = new URL(buildURL(vTuple, osInfo));

    InputStream is = getInstallScript();

    String remoteScript = "cluster-install.py";

    //noinspection OctalInteger
    sshConn.copyTo(remoteScript, is, 0755);

    SSHLoggingCommand cmd = new SSHLoggingCommand(sshConn, "python " + remoteScript + " " + dlUrl);
    try {
      cmd.execute();
      cmd.waitForExit(Integer.MAX_VALUE);
    } finally {
      cmd.close();
    }
  }

  public void install(Collection<NodeHost> nodes) throws ExecutionException {
      install(nodes, false, "");
  }

  /**
   * Installs the cluster on a collection of nodes simultaneously. The
   * {@link #configure()} method must have been called.
   * @param nodes The nodes to install the cluster on.
   * @throws ExecutionException
   */
  public void install(Collection<NodeHost> nodes, final boolean upgrade, final String upgradeVersion) throws ExecutionException {
    ExecutorService svc = Executors.newFixedThreadPool(nodes.size());
    List<Future> futures = new ArrayList<Future>();

    for (final NodeHost node : nodes) {
      Future f = svc.submit(new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          runNode(node, upgrade, upgradeVersion);
          return null;
        }
      });
      futures.add(f);
    }

    svc.shutdown();
    for (Future f : futures) {
      try {
        f.get();
      } catch (InterruptedException ex) {
        throw new ExecutionException(ex);
      }
    }
  }

  boolean configured = false;

  @Override
  public void configure() {
    try {
      versionTuple = options.getVTuple();
    } catch (Exception ex) {
      logger.error("Unable to parse version string {}" + ex.getStackTrace());
    }
    configured = true;
  }

  @Override
  public boolean isConfigured() {
    return configured;
  }

  public String getWantedVersion() {
    return versionTuple.full;
  }


  @Override
  public OptionTree getOptionTree() {
    return options.getOptionTree();
  }

  ClusterInstaller() {
    options = new InstallerOptions();
  }

  public ClusterInstaller(InstallerOptions options) {
    this.options = options;
  }
}
