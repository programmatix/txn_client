/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster;

import com.couchbase.sdkdclient.options.*;
import com.couchbase.sdkdclient.util.ServiceLogin;

import java.io.File;
import java.util.Collection;

/**
 * Class for specifying cluster options. This contains only the options needed
 * for the cluster nodes configuration itself.
 *
 * @see com.couchbase.sdkdclient.cluster.installer.InstallerOptions
 * @see com.couchbase.sdkdclient.cluster.CBBucketOptions
 * @see com.couchbase.sdkdclient.cluster.ClusterBuilder
 */
public final class CBClusterOptions implements OptionConsumer {
  final private MultiOption nodes =
          OptBuilder.startMulti("nodes")
          .alias("node")
          .help("Specify a node. This option may be specified multiple times")
          .shortName("N")
          .build();

  final private MultiOption ipAliases =
          OptBuilder.startMulti("ip-alias")
          .help("Specify IP equivalencies. The value accepts a sequence of " +
                "addresses or hostnames separated by a slash (/). " +
                "You may specify this multiple times for multiple host " +
                "identities ")
          .alias("ip-aliases")
          .alias("aliases")
          .build();

  final private BoolOption noInit = OptBuilder.startBool("noinit")
          .alias("no-init")
          .help("Don't reinitialize the cluster")
          .build();

  final private BoolOption useHostname = OptBuilder.startBool("usehostname")
          .alias("use-hostname")
          .help("Cluster setup with hostname")
          .build();

  final private BoolOption noSsh =
          OptBuilder.startBool("disable-ssh")
          .alias("ssh-disable")
          .help("Don't establish SSH connections to the nodes")
          .build();

  final private StringOption username =
          OptBuilder.startString("username")
          .help("REST username for the cluster")
          .defl("Administrator")
          .build();

  final private StringOption password =
          OptBuilder.startString("password")
          .help("REST password for the cluster")
          .defl("password")
          .build();

  final private BoolOption useSSL =
          OptBuilder.startBool("useSSL")
          .help("use SSL to connect to cluster")
          .defl("false")
          .build();

  final private StringOption sshUsername  = OptBuilder.startString("ssh-username")
          .alias("ssh-user")
          .help("Default username for SSH access")
          .defl("root")
          .build();

  final private StringOption sshPassword = OptBuilder.startString("ssh-password")
          .help("SSH Password to use")
          .build();

  final private BoolOption useBackups =
          OptBuilder.startBool("aux-nodelist")
          .alias("use_backups")
          .defl("true")
          .help("Whether to pass backup nodes to the Handle options")
          .build();

  final private IntOption nodeQuota =
          OptBuilder.startInt("node-quota")
          .help("Per-Node memory quota (MB)")
          .defl("1500")
          .argName("MB")
          .build();

  final private BoolOption useMinNodes =
          OptBuilder.startBool("minimal-nodes")
          .help("Only use as many nodes as required")
          .defl("false")
          .build();

  final private IntOption restTimeout =
          OptBuilder.startInt("rest-timeout")
          .help("Timeout for any sorts of operations involving the REST API. " +
                "Note that this not a network/TCP timeout, but rather a timeout " +
                "operating at the operation layer")
          .defl("45")
          .build();

  final private FileOption clusterIni =
          OptBuilder.startExistingFile("ini")
          .help("Cluster INI file. This can be used instead of `node'")
          .shortName("i").
          build();

  final private IntOption numGroups =
          OptBuilder.startInt("group-count")
          .help("Number of groups (serverGroups) to create. 2.5+ only. " +
                "The groups will be configured in a manner such that the nodes " +
                "are evenly distributed between the groups. If there are more  " +
                "nodes than groups, the group count will be reduced to match the " +
                "number of nodes")
          .defl("1")
          .alias("groups")
          .build();

  final  private StringOption upgradeVersion = OptBuilder.startString("upgrade-version")
            .shortAlias("u")
            .defl("")
            .help("Specify an upgrade version to add nodes of a particular version to rebalance")
            .build();

  final private EnumOption<NodelistBuilder.GroupPolicy> groupPolicy =
          OptBuilder.start("group-policy", NodelistBuilder.GroupPolicy.class)
          .help("Whether added/removed nodes should be part of the same group " +
                "or part of distinct groups.")
          .defl("SEPARATE")
          .build();


  final private BoolOption isWindowsCluster =
          OptBuilder.startBool("isWindowsCluster")
          .defl("false")
          .build();

  final private BoolOption isDockerCluster =
          OptBuilder.startBool("isDockerCluster")
                  .defl("false")
                  .build();


  final private BoolOption useMaxConn =
          OptBuilder.startBool("useMaxConn")
          .defl("false")
          .build();

  final private IntOption maxConnections =
          OptBuilder.startInt("maxConn")
          .defl("1000")
          .build();

  final private BoolOption setStorageMode =
          OptBuilder.startBool("setStorageMode")
          .defl("false")
          .build();

  final private StringOption n1qlFieldsToIndex =
          OptBuilder.startString("n1qlFields")
          .defl("tag,type")
          .build();

  final private StringOption ftsIndexName =
          OptBuilder.startString("ftsIndexName")
          .defl("ftsIdx1")
          .build();

  final private StringOption n1qlIndexName =
          OptBuilder.startString("n1qlIndexName")
          .defl("n1qlIdx1")
          .build();

  final private StringOption n1qlIndexType =
          OptBuilder.startString("n1qlIndexType")
                  .defl("secondary")
                  .build();


  final private BoolOption optAutoFailOver =
          OptBuilder.startBool("autoFailOver")
          .defl("false")
          .build();

  final private IntOption optAutoFaioverTimeout =
          OptBuilder.startInt("autoFailoverTimeoutSeconds")
          .defl("5")
          .build();

  private BoolOption checkQueryDistribution =
          OptBuilder.startBool("queryDist")
          .help("Check query distribution")
          .defl("false")
          .build();

  final
  @Override
  public OptionTree getOptionTree() {
    return new OptionTreeBuilder()
            .source(this, CBClusterOptions.class)
            .group("cluster")
            .description("Options for cluster/node setup")
            .prefix(OptionDomains.CLUSTER)
            .build();
  }

  public boolean getUseSSL() {
    return useSSL.getValue();
  }

  public int getNodeQuota() {
    return nodeQuota.getValue();
  }

  public boolean shouldUseMinNodes() {
    return useMinNodes.getValue();
  }

  public boolean shouldUseSSH() {
    return noSsh.getValue() == false;
  }

  public boolean shouldPassAuxNodes() {
    return useBackups.getValue();
  }

  public boolean shouldNotInitialize() {
    return noInit.getValue();
  }

  public boolean clusterWithHostname() {
	    return useHostname.getValue();
  }

  public Collection<String> getRawNodes() {
    return nodes.getRawValues();
  }

  public Collection<String> getIpAliases() {
    return ipAliases.getRawValues();
  }

  public int getRestTimeout() {
    return restTimeout.getValue();
  }

  public ServiceLogin getRestLogin() {
    return new ServiceLogin(username.getValue(),
                            password.getValue(),
                            -1);
  }

  public ServiceLogin getUnixLogin() {
    return new ServiceLogin(sshUsername.getValue(),
                            sshPassword.getValue(),
                            -1);
  }

  public File getIniPath() {
    return clusterIni.getValue();
  }

  public int getGroupCount() {
    return numGroups.getValue();
  }

  public String getUpgradeVersion() {
      return upgradeVersion.getValue();
  }

  public NodelistBuilder.GroupPolicy getGroupPolicy() {
    return groupPolicy.getValue();
  }

  public boolean getIsWindowsCluster() { return isWindowsCluster.getValue(); }
  public boolean getIsDockerCluster() { return isDockerCluster.getValue(); }

  public boolean getUseMaxConn() { return useMaxConn.getValue(); }

  public int getMaxConn() { return maxConnections.getValue(); }

  public boolean getSetStorageMode() { return setStorageMode.getValue();  }

  public String getn1qlFieldsToIndex() { return n1qlFieldsToIndex.getValue(); }

  public String getFtsIndexName() { return  ftsIndexName.getValue(); }

  public String getN1QLIndexName() { return n1qlIndexName.getValue(); }

  public boolean isAutoFailoverEnabled() { return optAutoFailOver.getValue(); }

  public int getAutoFailoverTimeout() { return optAutoFaioverTimeout.getValue(); }
  public String getN1QLIndexType() { return n1qlIndexType.getValue(); }

  public Boolean checkQueryDistribution() { return checkQueryDistribution.getValue(); }
}
