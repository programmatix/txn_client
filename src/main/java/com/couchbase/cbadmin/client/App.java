/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.cbadmin.client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.couchbase.cbadmin.assets.Bucket;
import com.couchbase.cbadmin.assets.Node;
import com.couchbase.cbadmin.assets.NodeGroup;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Command line application utilizing the cbadmin Java toolkit.
 */
public class App {
  static final PrintStream out = System.out;
  static final PrintStream err = System.err;

  static final String INDENT = "  ";
  static final String LSBUCKETS = "buckets";
  static final String LSNODES = "nodes";
  static final String ADDNODE = "add-node";
  static final String CLINIT = "init-cluster";
  static final String CLJOIN = "join-cluster";
  static final String REBALANCE = "rebalance";
  static final String BKTCREATE = "create-bucket";
  static final String BKTDEL = "delete-bucket";
  static final String RBINFO = "rebalance-info";
  static final String RBSTOP = "rebalance-stop";
  static final String FAILOVER = "failover-node";
  static final String EJNODE = "eject-node";
  static final String READDNODE = "readd-node";
  static final String LSGROUPS = "groups";
  static final String ASGNGROUPS = "assign-group";
  static final String MVGROUP = "rename-group";
  static final String MKGROUP = "create-group";
  static final String RMGROUP = "delete-group";
  static final String INFO = "info";

  public static URL stringToUrl(String host, int port) {
    if (host.contains(":")) {
      String[] pair = host.split(":", 2);
      port = Integer.parseInt(pair[1]);
      host = pair[0];
    }

    try {
      return new URL("http", host, port, "/");
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  abstract class CommandOptions {
    @Parameter(names = { "-?", "--help", "-h" }, help=true,
            description="Show this message")
    boolean help = false;

    abstract void run(App app) throws Exception;
  }

  /**
   * Parameters for listing buckets.
   */
  @Parameters(commandDescription="List buckets")
  class ListBucketOptions extends CommandOptions {

    @Override
    void run(App ap) throws Exception {
      ap.listBuckets();
    }
  }

  @Parameters(commandDescription="List groups")
  class ListGroupOptions extends CommandOptions {
    @Override void run(App ap) throws Exception {
      ap.listGroups();
    }
  }

  /**
   * Parameters for listing nodes.
   */
  @Parameters(commandDescription="List nodes")
  class ListNodesOptions extends CommandOptions {
    @Override
    void run(App ap) throws Exception {
      ap.listNodes();
    }
  }

  /**
   * Parameters for adding a new node to the cluster.
   */
  @Parameters(commandDescription="Add new node to cluster")
  class AddNodeOptions extends CommandOptions {
    @Parameter(names="--nn-host",
               description="Host:port for new node", required=true)
    String newHost;

    @Parameter(names="--nn-password", description="Password for new node")
    String newPassword;

    @Parameter(names="--nn-user", description="Username for new node")
    String newUsername;

    @Override
    void run(App ap) throws Exception {
      ap.addNode();
    }
  }

  /**
   * Parameters for initializing a new node with its own cluster.
   */
  @Parameters(commandDescription="Initialize a new cluster")
  class InitClusterOptions extends CommandOptions {
    @Parameter(names="--memory-quota",
               required=true,
               description="Cluster Quota (in MB)")
    int ramQuota;

    @Override
    void run(App ap) throws Exception {
      ap.initCluster();
    }
  }

  @Parameters(commandDescription="Join an existing cluster")
  class JoinClusterOptions extends CommandOptions {
    @Parameter(names="--cluster-host",
               required=true,
               description="Entry point of existing cluster")
    String clHost;

    @Parameter(names="--cluster-port",
               description="Entry point of existing cluster")
    int clPort = 8091;

    @Override
    void run(App ap) throws Exception {
      ap.joinCluster();
    }
  }

  @Parameters(commandDescription = "Rebalance the cluster")
  class RebalanceOptions extends CommandOptions {
    @Override
    void run(App ap) throws Exception {
      ap.client.rebalance();
    }
  }

  @Parameters(commandDescription = "Create a bucket")
  class BucketCreateOptions extends CommandOptions  {
    @Parameter(names = "--name",
               description = "Name for bucket", required = true)
    String name;

    @Parameter(names = "--type",
               description = "Bucket Type (couchbase or memcached or ephemeral)")
    String type = "couchbase";

    @Parameter(names = "--quota",
               description = "How much memory should this bucket use (MB)")
    int ramQuota = 512;

    @Parameter(names = "--sasl-password",
               description = "SASL password for bucket")
    String saslPasswd;

    @Parameter(names = "--replicas",
               description = "How many replicas for this bucket")
    int replicas = 1;

    @Parameter(names = "--proxy-port",
               description = "New proxy port (for non-SASL only)")
    int proxyPort = 0;

    @Override
    void run(App ap) throws Exception {
      ap.createBucket();
    }
  }

  @Parameters(commandDescription = "Delete a bucket")
  class BucketDeleteOptions extends CommandOptions {
    @Parameter(names = "--name", description = "name of bucket to delete")
    String name;

    @Override
    public void run(App ap) throws Exception {
      ap.client.deleteBucket(name);
    }
  }

  @Parameters(commandDescription = "Stop a rebalance operation")
  class StopRebalanceOptions extends CommandOptions {
    @Override
    public void run(App ap) throws Exception {
      ap.client.stopRebalance();
    }
  }

  @Parameters(commandDescription = "Get rebalance progress")
  class RebalanceInfoOptions extends CommandOptions {
    @Parameter(names={"-i", "--poll-interval"},
               description="Run every n seconds in loop until done")
    float pollInterval = 0;

    @Override
    public void run(App ap) throws Exception {
      ap.rebalanceProgress();
    }
  }

  @Parameters(commandDescription = "Failover a node from the cluster")
  class FailoverOptions extends CommandOptions {
    @Parameter(names = "--fo-host", description = "Host to fail over",
               required = true)
    String host;

    @Parameter(names = "--fo-port", description = "Port of the node to failover")
    int port;

    @Override
    public void run(App ap) throws Exception {
      ap.failover();
    }
  }

  @Parameters(commandDescription = "Get information about the node")
  class InfoOptions extends CommandOptions {
    @Override
    public void run(App ap) throws Exception {
      ap.info();
    }
  }

  @Parameters(commandDescription = "eject an inactive node from the cluster")
  class EjectNodeOptions extends CommandOptions {
    @Parameter(names = "--ej-host", required=true, description = "Node to eject")
    String host;

    @Parameter(names = "--ej-port", description = "Port")
    int port;

    @Override
    public void run(App ap) throws Exception {
      ap.eject();
    }
  }

  @Parameters(commandDescription = "Re-add a failed over node")
  class ReaddNodeOptions extends CommandOptions {
    @Parameter(names = "--ra-host", required=true, description = "Node to readd")
    String host;

    @Parameter(names = "--ra-port", description = "Port")
    int port = 0;

    @Override
    public void run(App ap) throws Exception {
      ap.reAdd();
    }
  }

  @Parameters(commandDescription = "Rename a group")
  class RenameGroupOptions extends CommandOptions {
    @Parameter(names = "--from", required=true, description = "Group to rename")
    String from;

    @Parameter(names = "--to", required = true, description = "New name")
    String to;

    @Override public void run(App ap) throws Exception {
      ap.client.renameGroup(from, to);
    }
  }

  @Parameters(commandDescription = "Allocate a node to a group")
  class AssignGroupOptions extends CommandOptions {
    @Parameter(names = "--ag-host", required = true, description = "Node to assign")
    String host;

    @Parameter(names = "--ag-port", description = "Port of the node to assign")
    int port = -1;

    @Parameter(names = "--group", description = "New group to assign to", required = true)
    String groupName = null;

    @Override public void run(App ap) throws Exception {
      Node nn = ap.client.findNode(stringToUrl(host, port));
      NodeGroup group = ap.client.findGroup(groupName);
      ap.client.assignNodeToGroup(group, nn);
    }
  }

  @Parameters(commandDescription = "Create a new group")
  class CreateGroupOptions extends CommandOptions {
    @Parameter(names = "--group", required = true, description = "Group name to create")
    String group = null;
    @Override public void run(App ap) throws Exception {
      ap.client.addGroup(group);
    }
  }

  @Parameters(commandDescription = "Remove a group")
  class DeleteGroupOptions extends CommandOptions {
    @Parameter(names = "--group", required = true, description = "Group to delete")
    String group = null;
    @Override public void run(App ap) throws Exception {
      ap.client.deleteGroup(group);
    }
  }

  class MainOptions {
    @Parameter(names = { "--host", "-H" }, description="Entry point for cluster")
    String host;

    @Parameter(names = { "--username", "-u" }, description="REST username")
    String username;

    @Parameter(names = { "--password", "-p" }, description="REST password")
    String password;

    @Parameter(names = {"-c", "--config"}, description="Configuration file")
    String config;

    @Parameter(names = { "--port" }, description="REST port")
    int port = 8091;

    @Parameter(names = { "-A", "alias" },
               description = "comma-separated host aliases. Each " +
               "alias grouping should be specified once on the commandline")

    List<String> aliases = new ArrayList<String>();

    @Parameter(help=true, names={"-h", "--help", "-?"},
                          description="Show this message")
    boolean help = false;

    @Parameter(names = "--commands", description = "Show commands", help=true)
    boolean helpCommands = false;
  }


  // Variables
  final CouchbaseAdmin client;
  final MainOptions mainOpts = new MainOptions();

  // Command-specific options
  final ListBucketOptions lsBucketOptions = new ListBucketOptions();
  final ListNodesOptions lsNodesOptions = new ListNodesOptions();
  final AddNodeOptions addNodeOptions = new AddNodeOptions();
  final InitClusterOptions clInitOptions = new InitClusterOptions();
  final JoinClusterOptions clJoinOptions = new JoinClusterOptions();
  final RebalanceOptions  rebalanceOptions = new RebalanceOptions();
  final BucketCreateOptions bktCreateOptions = new BucketCreateOptions();
  final BucketDeleteOptions bktDeleteOptions = new BucketDeleteOptions();
  final StopRebalanceOptions rbStopOptions = new StopRebalanceOptions();
  final RebalanceInfoOptions rbInfoOptions = new RebalanceInfoOptions();
  final FailoverOptions foOptions = new FailoverOptions();
  final InfoOptions infoOptions = new InfoOptions();
  final EjectNodeOptions ejOptions = new EjectNodeOptions();
  final ReaddNodeOptions raOptions = new ReaddNodeOptions();
  final ListGroupOptions grplsOptions = new ListGroupOptions();
  final RenameGroupOptions mvgrpOptions = new RenameGroupOptions();
  final AssignGroupOptions asgngrpOptions = new AssignGroupOptions();
  final CreateGroupOptions mkgrpOptions = new CreateGroupOptions();
  final DeleteGroupOptions rmgrpOptions = new DeleteGroupOptions();


  final JCommander jc = new JCommander(mainOpts);
  final Map<String,CommandOptions> optMap = new HashMap<String, CommandOptions>();

  // Initialize the command mapping
  {
    optMap.put(LSBUCKETS, lsBucketOptions);
    optMap.put(LSNODES, lsNodesOptions);
    optMap.put(ADDNODE, addNodeOptions);
    optMap.put(CLINIT, clInitOptions);
    optMap.put(CLJOIN, clJoinOptions);
    optMap.put(REBALANCE, rebalanceOptions);
    optMap.put(BKTCREATE, bktCreateOptions);
    optMap.put(BKTDEL, bktDeleteOptions);
    optMap.put(RBSTOP, rbStopOptions);
    optMap.put(RBINFO, rbInfoOptions);
    optMap.put(FAILOVER, foOptions);
    optMap.put(INFO, infoOptions);
    optMap.put(EJNODE, ejOptions);
    optMap.put(READDNODE, raOptions);
    optMap.put(LSGROUPS, grplsOptions);
    optMap.put(MVGROUP, mvgrpOptions);
    optMap.put(ASGNGROUPS, asgngrpOptions);
    optMap.put(MKGROUP, mkgrpOptions);
    optMap.put(RMGROUP, rmgrpOptions);

    for (Entry<String,CommandOptions> ent : optMap.entrySet()) {
      jc.addCommand(ent.getKey(), ent.getValue());
    }
  }

  private CommandOptions getOptionsInstance() throws Exception {
    String cmdStr = jc.getParsedCommand();
    if (cmdStr == null) {
      err.println("Must have command");
      exitHelp(err, 1);
    }

    CommandOptions ret = optMap.get(cmdStr);
    if (ret != null) {
      return ret;
    }

    err.printf("Unrecognized command '%s'%n", cmdStr);
    exitHelp(err, 1);
    return null;
  }

  static class DetailItems {
    private List<String> lines = new ArrayList<String>();
    DetailItems add(String item, Object value) {
      String s = String.format("%s: %s", item, value);
      lines.add(s);
      return this;
    }

    void write(PrintStream out) {
      for (String line : lines) {
        out.print(INDENT);
        out.printf("%s%n", line);
      }
    }

    static DetailItems start() {
      return new DetailItems();
    }
  }

  void listBuckets() throws Exception {
    Map<String,Bucket> bkts = client.getBuckets();

    for (Bucket b : bkts.values()) {
      out.println(b.getName());
      DetailItems.start()
              .add("Type", b.getType())
              .add("Replicas", b.getReplicaCount())
              .write(out);

      out.println();
    }
  }

  void listGroups() throws Exception {
    Collection<NodeGroup> groups = client.getGroupList().getGroups();
    for (NodeGroup group : groups) {
      out.println(group.getName());
      DetailItems itmDetail = DetailItems.start();
      itmDetail.add("URI", group.getUri());
      for (Node nn : group.getNodes()) {
        itmDetail.add("Node", nn.getRestUrl());
      }
      itmDetail.write(out);
      out.println();
    }
  }

  void listNodes() throws Exception {
    List<Node> nodes = client.getNodes();
    for (Node n : nodes) {
      out.printf("%s [%s]%n", n.getRestUrl(), n.isOk() ? "OK" : "ERR");
      DetailItems.start()
              .add("Version", n.getClusterVersion())
              .add("Views URI", n.getCouchUrl())
              .add("OTP ID", n.getNSOtpNode())
              .add("Membership", n.getMembership())
              .add("Status", n.getStatus())
              .write(out);
      out.println();
    }
  }

  void addNode() throws Exception {
    // Get our options
    URL url = stringToUrl(addNodeOptions.newHost, -1);
    if (addNodeOptions.newPassword == null) {
      client.addNewNode(url, "");
    } else {
      client.addNewNode(url,
                        addNodeOptions.newUsername,
                        addNodeOptions.newPassword, "");
    }
    listNodes();
  }

  void initCluster() throws Exception {
    ClusterConfig conf = new ClusterConfig();
    conf.memoryQuota = clInitOptions.ramQuota;
    client.initNewCluster(conf);
  }

  void joinCluster() throws Exception {
    URL url = stringToUrl(clJoinOptions.clHost, clJoinOptions.clPort);
    client.joinCluster(url);
  }

  void createBucket() throws Exception {
    BucketConfig conf = new BucketConfig(bktCreateOptions.name);
    if (bktCreateOptions.saslPasswd != null &&
            bktCreateOptions.proxyPort != 0) {
      throw new IllegalArgumentException(
              "Proxy port cannot be specified with SASL password");
    }

    if (bktCreateOptions.saslPasswd != null) {
      conf.setSaslPassword(bktCreateOptions.saslPasswd);
    } else if (bktCreateOptions.proxyPort != 0) {
      conf.setNoAuth(bktCreateOptions.proxyPort);
    }

    conf.ramQuotaMB = bktCreateOptions.ramQuota;
    conf.replicaCount = bktCreateOptions.replicas;
    if (bktCreateOptions.type.equals("couchbase")) {
      conf.bucketType = Bucket.BucketType.COUCHBASE;
    } else if (bktCreateOptions.type.equals("memcached")) {
      conf.bucketType = Bucket.BucketType.MEMCACHED;
    } else if (bktCreateOptions.type.equals("ephemeral")) {
      conf.bucketType = Bucket.BucketType.EPHEMERAL;
    } else {
      throw new IllegalArgumentException(
              "Bucket type must be couchbase or memcached");
    }

    client.createBucket(conf);
  }

  private boolean rbInfoSweep() throws Exception {
    RebalanceInfo rbInfo = client.getRebalanceStatus();
    if (rbInfo.isComplete()) {
      if (rbInfo.isStopped()) {
        out.println("** STOPPED");
      } else {
        out.println("** Done!");
      }
      return false;
    }

    // Format the output
    out.printf("PROGRESS: %f%n", rbInfo.getProgress());
    out.flush();
    return true;
  }

  void rebalanceProgress() throws Exception {
    long pollInterval = (long) (rbInfoOptions.pollInterval * 1000);
    while (true) {
      if (!rbInfoSweep()) {
        break;
      }

      if (pollInterval <= 0) {
        break;
      }

      Thread.sleep(pollInterval);
    }
  }

  void failover() throws Exception {
    URL url = stringToUrl(foOptions.host, foOptions.port);
    Node n = client.findNode(url);
    client.failoverNode(n);
  }

  void info() throws Exception {
    ConnectionInfo info = client.getInfo();
    DetailItems dt = new DetailItems()
            .add("Version", info.getVersion())
            .add("Has Cluster", "" + info.hasCluster())
            .add("Cluster ID",
                 info.hasCluster() ? info.getClusterIdentifier() : "NONE")
            .add("Is Administrator", "" + info.isAdmin());
    dt.write(out);
  }

  void eject() throws Exception {
    URL url = stringToUrl(ejOptions.host, ejOptions.port);
    Node n = client.findNode(url);
    client.ejectNode(n);
  }

  void reAdd() throws Exception {
    URL url = stringToUrl(raOptions.host, raOptions.port);
    Node n = client.findNode(url);
    client.readdNode(n);
  }

  private void exitHelp(PrintStream pw, int ec, String cmdname) {
    StringBuilder sb = new StringBuilder();
    if (cmdname != null) {
      jc.usage(cmdname, sb);
    } else {
      new JCommander(mainOpts).usage(sb);
    }
    pw.print(sb.toString());
    System.exit(ec);

  }

  private void exitHelp(PrintStream pw, int ec) {
    exitHelp(pw, ec, null);
  }

  public App(String argv[]) throws Exception {
    try {
      jc.parse(argv);
    } catch (ParameterException ex) {
      err.println(ex.getMessage());
      exitHelp(err, 1, jc.getParsedCommand());
    }

    if (mainOpts.helpCommands) {
      for (String n : optMap.keySet()) {
        out.printf("%-20s%s%n",n, jc.getCommandDescription(n));
      }
      System.exit(0);
    }

    if (mainOpts.help) {
      exitHelp(out, 0);
    }

    if (mainOpts.config != null) {
      Properties props = new Properties();
      props.load(new FileInputStream(new File(mainOpts.config)));
      if (mainOpts.username == null) {
        mainOpts.username = props.getProperty("username");
      }
      if (mainOpts.password == null) {
        mainOpts.password = props.getProperty("password");
      }
      if (mainOpts.host == null) {
        mainOpts.host = props.getProperty("host");
      }
      String s = props.getProperty("port");
      if (s != null) {
        mainOpts.port = Integer.parseInt(s);
      }
    }

    if (mainOpts.username == null) {
      mainOpts.username = "Administrator";
    }

    if (mainOpts.password == null) {
      throw new IllegalArgumentException("Must have password");
    }

    if (mainOpts.host == null) {
      throw new IllegalArgumentException("Must have host");
    }

    CommandOptions opts = getOptionsInstance();
    URL url = stringToUrl(mainOpts.host, mainOpts.port);
    URL n1qlUrl = new URL("");
    URL ftsUrl = new URL("");
    URL cbasUrl = new URL("");
    client = new CouchbaseAdmin(url, n1qlUrl, ftsUrl, cbasUrl, mainOpts.username, mainOpts.password);
    for (String s : mainOpts.aliases) {
      List<String> aliases = Arrays.asList(s.split("/"));
      client.getAliasLookupCache().associateAlias(aliases);
    }

    if (opts.help) {
      exitHelp(out, 0, jc.getParsedCommand());
    }
    opts.run(this);
  }

  public static void main(String argv[]) throws Exception {
    new App(argv);
  }
}
