/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.cbadmin.client;

import com.couchbase.cbadmin.assets.Bucket;
import com.couchbase.cbadmin.assets.Node;
import com.couchbase.cbadmin.assets.NodeGroup;
import com.couchbase.cbadmin.assets.NodeGroupList;
import com.couchbase.sdkdclient.cluster.ClusterException;
import com.couchbase.sdkdclient.util.MapUtils;
import com.couchbase.sdkdclient.cluster.RemoteCommands;
import com.couchbase.sdkdclient.ssh.SSHConnection;
import com.couchbase.sdkdclient.ssh.SSHLoggingCommand;
import com.couchbase.sdkdclient.cluster.Nodelist;
import com.couchbase.sdkdclient.cluster.NodeHost;
import com.couchbase.sdkdclient.ssh.SimpleCommand;
import com.couchbase.sdkdclient.util.ServiceLogin;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Couchbase Administrative Client
 *
 * @author mnunberg
 */
public class CouchbaseAdmin implements ICouchbaseAdmin {
  static final Gson gs = new Gson();
  private final URL entryPoint;
  private final URL n1qlEntryPoint;
  private final URL ftsEntryPoint;
  private final URL cbasEntryPoint;

  private CloseableHttpClient cli;
  private final String user;
  private final String passwd;
  private Nodelist nodelist;
  private final Logger logger = LoggerFactory.getLogger(CouchbaseAdmin.class);
  private Node myNode = null;
  private AliasLookup aliasLookup = new AliasLookup();
  private String clusterCert;
  private SSHConnection sshConn;

  {
    // common idioms
    aliasLookup.associateAlias("127.0.0.1", "localhost");
  }

  /**
   * Known URIS.
   */
  public static final String P_BUCKETS = "/pools/default/buckets";

  public static final String P_SETTINGS_INDEXES = "/settings/indexes";

  // From cluster_connect
  public static final String P_SETTINGS_WEB = "/settings/web";

  // Not in api.txt, but present in manual
  public static final String P_POOL_NODES = "/pools/nodes";

  // Set max connections of the cluster
  public static final String P_POOL_MAX = "/pools/default/settings/memcached/global"; //minimum is 1000

  public static final String P_SETUP_SERVICES = "/node/controller/setupServices";

  public static final String P_SETUP_HOSTNAME = "/node/controller/rename";

  // Standard
  public static final String P_ADDNODE = "/controller/addNode";
  public static final String P_POOLS_DEFAULT = "/pools/default";
  public static final String P_POOLS = "/pools";
  public static final String P_JOINCLUSTER = "/node/controller/doJoinCluster";
  public static final String P_REBALANCE = "/controller/rebalance";
  public static final String P_REBALANCE_STOP = "/controller/stopRebalance";
  public static final String P_REBALANCE_PROGRESS = "/pools/default/rebalanceProgress";
  public static final String P_FAILOVER = "/controller/failOver";
  public static final String P_READD = "/controller/reAddNode";
  public static final String P_RESET = "/diag/eval";
  public static final String P_EJECT = "/controller/ejectNode";
  public static final String _P_NODES_SELF = "/nodes/self";
  public static final String P_STARTLOGSCOLLECTION = "/controller/startLogsCollection";
  public static final String P_TASKS = "/pools/default/tasks";
  public static final String _P_SERVERGROUPS = "/pools/default/serverGroups";
  public static final String _P_CLUSTERCERTIFICATE = "/pools/default/certificate";
  public static final String P_N1QL = "/query";
  public static final String P_ANALYTICS = "/analytics/service";
  public static final String P_FTS = "/api/index";
  public static final String P_AUTOFAILOVER = "/settings/autoFailover";
  public static final String P_AUTOFAILOVERRESET = "/settings/autoFailover/resetCount";
  public static final String P_ACTIVEREQ = "/admin/active_requests";

  public static HashMap<String, String> logPaths = new HashMap<String, String>();
  /**
   * Constructs a new connection to the Couchbase administrative API
   *
   * @param url The URL to the server. The path is ignored
   * @param username The administrative username, usually 'Administrator'
   * @param password The administrative password
   */
  public CouchbaseAdmin(URL url, URL n1qlUrl, URL ftsUrl, URL cbasUrl, String username, String password) {
    if (url == null) {
      throw new IllegalArgumentException("Cannot pass a null URL");
    }
    entryPoint = url;
    n1qlEntryPoint = n1qlUrl;
    ftsEntryPoint = ftsUrl;
    cbasEntryPoint = cbasUrl;
    user = username;
    passwd = password;

    BasicHeader hdr = new BasicHeader(
            HttpHeaders.AUTHORIZATION,
            "Basic " + Base64.encodeBase64String(
              (username+":"+password).getBytes()));

    List<Header> hdrList = new ArrayList<Header>();
    hdrList.add(hdr);

    cli = HttpClients.custom()
            .setDefaultHeaders(hdrList)
            .build();
  }

  private JsonElement extractResponse(
          HttpResponse res,
          HttpRequestBase req,
          int expectCode)
          throws RestApiException, IOException {

    JsonElement ret;
    HttpEntity entity;
    entity = res.getEntity();
    if (entity == null) {
      ret = new JsonObject();

    } else {
      Header contentType = entity.getContentType();
      if (contentType == null || contentType.getValue().contains("json") == false) {
        ret = new JsonObject();
        ret.getAsJsonObject().addProperty("__raw_response",
                                          IOUtils.toString(entity.getContent()));
      } else {
        JsonReader reader = new JsonReader(
                new InputStreamReader(entity.getContent()));
        ret = gs.fromJson(reader, JsonObject.class);
      }
    }
    if (res.getStatusLine().getStatusCode() != expectCode) {
      if (ret.toString().contains("index exist")) {
        logger.warn("surpress index recreation error:{}", ret.toString());
      } else if (ret.toString().contains("unsupported key storageMode")) {
        logger.warn("surpress storageMode error:{}", ret.toString());
      } else {
        logger.warn("error:req:{},res:{}", req.toString(), ret.toString());
          throw new RestApiException(ret, res.getStatusLine(), req);
      }
    }

    return ret;
  }


  private JsonElement getResponseJson(HttpRequestBase req, int expectCode)
      throws RestApiException, IOException {
    logger.debug("{} {}", req.getMethod(), req.getURI());
    
    CloseableHttpResponse res = cli.execute(req);
    try {
      return extractResponse(res, req, expectCode);
    } finally {
      if (res.getEntity() != null) {
        // Ensure the content is completely removed from the stream,
        // so we can re-use the connection
        EntityUtils.consumeQuietly(res.getEntity());
      }
    }
  }

  private JsonElement getResponseJson(
          HttpRequestBase req, String path, int expectCode, boolean isFTS, boolean isN1QL, boolean isCBAS)
          throws RestApiException, IOException {

    URL url;
    try {
      url = new URL(entryPoint, path);
      if (isFTS) {
        url = new URL(ftsEntryPoint, path);
      }
      if (isN1QL) {
        url = new URL(n1qlEntryPoint, path);
      }
      if (isCBAS) {
        url = new URL(cbasEntryPoint, path);
      }
      req.setURI(url.toURI());
    } catch (MalformedURLException ex) {
      throw new IOException(ex);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
    logger.info("request to "+url.getHost().toString());

    return getResponseJson(req, expectCode);
  }

  private JsonElement getResponseJson(
          HttpRequestBase req, String path, int expectCode)
          throws RestApiException, IOException {

    URL url;
    try {
      url = new URL(entryPoint, path);
      req.setURI(url.toURI());
      System.out.println("req.toString():"+req.toString());
    } catch (MalformedURLException ex) {
      throw new IOException(ex);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }

    return getResponseJson(req, expectCode);
  }

  private String getResponsePlain(
          HttpRequestBase req, String path, int expectCode)
          throws RestApiException, IOException {
    URL url;
    try {
      url = new URL(entryPoint, path);
      req.setURI(url.toURI());

    } catch (MalformedURLException ex) {
      throw new IOException(ex);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
    CloseableHttpResponse res = cli.execute(req);

    try {
      logger.trace("{} {}", req.getMethod(), req.getURI());

      if (res.getStatusLine().getStatusCode() != expectCode) {
        throw new RestApiException(res.getStatusLine(), req);
      }

      HttpEntity entity = res.getEntity();
      if (entity == null) {
        throw new ClusterException("Bad response for get certificate");
      }

      return IOUtils.toString(entity.getContent());

    } finally {
      if (res.getEntity() != null) {
        // Ensure the content is completely removed from the stream,
        // so we can re-use the connection
        EntityUtils.consumeQuietly(res.getEntity());
      }
    }
  }

  @Override
  public JsonElement getJson(String path) throws IOException, RestApiException {
    return getResponseJson(new HttpGet(), path, 200);
  }

  private static UrlEncodedFormEntity makeFormEntity(Map<String,String> params) {

    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    for (Entry<String,String> ent : params.entrySet()) {
      nvps.add(new BasicNameValuePair(ent.getKey(), ent.getValue()));
    }
    try {
      // The encoding is a must here, otherwise it'll use the default ISO-8859-1 encoding and might cause issues...
      return new UrlEncodedFormEntity(nvps, "utf-8");

    } catch (UnsupportedEncodingException ex) {
      throw new IllegalArgumentException(ex);
    }

  }

  @Override
  public Map<String, Bucket> getBuckets() throws RestApiException {
    JsonElement e;
    Map<String,Bucket> ret = new HashMap<String, Bucket>();

    try {
      e = getJson(P_BUCKETS);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    JsonArray arr;
    if (!e.isJsonArray()) {
      throw new RestApiException("Expected JsonObject", e);
    }

    arr = e.getAsJsonArray();
    for (int i = 0; i < arr.size(); i++) {
      JsonElement tmpElem = arr.get(i);
      if (!tmpElem.isJsonObject()) {
        throw new RestApiException("Expected JsonObject", tmpElem);
      }

      Bucket bucket = new Bucket(tmpElem.getAsJsonObject());
      ret.put(bucket.getName(), bucket);
    }
    return ret;
  }

  @Override
  public NodeGroupList getGroupList() throws RestApiException {
    JsonElement e ;
    Map<String, NodeGroup> ret = new HashMap<String, NodeGroup>();
    try {
      e = getJson(_P_SERVERGROUPS);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    if (!e.isJsonObject()) {
      throw new RestApiException("Expected JSON object", e);
    }
    return new NodeGroupList(e.getAsJsonObject());
  }

  @Override
  public List<Node> getNodes() throws RestApiException {
    List<Node> ret = new ArrayList<Node>();
    JsonElement e;
    try {
      e = getJson(P_POOL_NODES);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    if (!e.isJsonObject()) {
      throw new RestApiException("Expected JsonObject", e);
    }

    JsonObject obj = e.getAsJsonObject();
    JsonArray nodesArr;
    e = obj.get("nodes");

    if (e == null) {
      throw new RestApiException("Expected 'nodes' array", obj);
    }

    nodesArr = e.getAsJsonArray();
    for (int i = 0; i < nodesArr.size(); i++) {
      e = nodesArr.get(i);
      JsonObject nObj;
      if (!e.isJsonObject()) {
        throw new RestApiException("Malformed node entry", e);
      }
      nObj = e.getAsJsonObject();
      Node n = new Node(nObj);
      ret.add(n);
    }

    return ret;
  }

  private static Inet4Address getIp4Lookup(String host) throws RestApiException {
    Inet4Address inaddr = null;
    InetAddress[] addrList;
    try {
      addrList = InetAddress.getAllByName(host);
    } catch (UnknownHostException ex) {
      throw new RestApiException(ex);
    }

    for (InetAddress addr : addrList) {
      if (addr instanceof Inet4Address) {
        inaddr = (Inet4Address) addr;
        break;
      }
    }

    if (inaddr == null) {
      throw new RestApiException("Couldn't get IPv4 address");
    }
    return inaddr;
  }

  @Override
  public void addNewNode(URL newNode, String services) throws RestApiException {
    addNewNode(newNode, user, passwd, services);
  }

  @Override
  public void addNewNode(CouchbaseAdmin newNode, String services) throws RestApiException {
    addNewNode(newNode.getEntryPoint(), services);
  }

  private SSHConnection conn = null;
  @Override
  public void addNewNode(URL newNode, String nnUser, String nnPass, String services)
          throws RestApiException {

    int ePort = newNode.getPort();
    if (ePort == -1) {
      ePort = entryPoint.getPort();
    }

    InetAddress inaddr = getIp4Lookup(newNode.getHost());

    if (newNode.getHost().equals(entryPoint.getHost())
            && ePort == entryPoint.getPort()) {
      throw new IllegalArgumentException("Can't join node to self");
    }
    
    logger.debug("URL: {}, ePort: {}, User: {}, password: {}", newNode, ePort, nnUser, nnPass);
    
    Map<String,String> params = new HashMap<String, String>();
    params.put("user", nnUser);
    params.put("password", nnPass);

    String internalIP = null;
    if (newNode.getHost().contains(".com")) {
      try {
        conn = new SSHConnection("root",
                                 "couchbasego",
                                  inaddr.getHostName(), 22);
        conn.connect();
        internalIP = RemoteCommands.getInternalIP(conn);
      }
      catch(IOException e) {
        e.printStackTrace();
      }
      params.put("hostname", "http://" + internalIP + ":" + ePort);
      logger.debug("Internal IP of this host: {}", "http://" + internalIP);
    }
    else {
      params.put("hostname", "http://" + inaddr.getHostAddress() + ":" + ePort);
      logger.debug("inaddr.getHostAddress(): {}", "http://" + inaddr.getHostAddress());
    }
    if (services != "") {
      //if no services are added. kv will be default
      params.put("services", services);
      logger.debug("services are {} inaddr", services, newNode);
    }


    HttpPost post = new HttpPost();
    post.setEntity(makeFormEntity(params));

    try {
      getResponseJson(post, P_ADDNODE, 200);
      if (newNode.getHost().contains(".com")) {
        logger.debug("Enable alternate address if is use hostname in cloud");
        String cmd = null;
        cmd = "curl -X PUT -u Administrator:password -d ";
        cmd += "'hostname=" + inaddr.getHostName() + "' ";
        cmd += " http://" + internalIP + ":8091/node/controller/setupAlternateAddresses/external ";
        System.out.println("enable alternate address");
        SimpleCommand retn = RemoteCommands.runSimple(conn, "date");
        String out = retn.getStdout();
        logger.debug("infor {} ", out);
        retn = RemoteCommands.runSimple(conn, cmd);
        out = retn.getStdout();
        logger.debug("infor {} ", out);
      }
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void createUser(String username, String password) throws RestApiException {
    Map<String,String> params = new HashMap<String, String>();
    params.put("name", "sdkadmin"); // this user name can be different from username
    params.put("roles", "admin,cluster_admin");
    //params.put("roles", "admin");
    params.put("password", password);

    String uri = "/settings/rbac/users/local/"+username;

    HttpPut putReq = new HttpPut();
    putReq.setHeader("Content-Type", "application/x-www-form-urlencoded");
    //putReq.setHeader("Authorization", "Basic QWRtaW5pc3RyYXRvcjpwYXNzd29yZA=="); //base64 of Administrator:password
    putReq.setEntity(makeFormEntity(params));
    try {
      getResponseJson(putReq, uri, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    } catch (Exception e) {
      logger.info("RBAC user creation is allowed only on SPOCK. Skipping user creation");
    }
  }

  @Override
  public Node findNode(URL node) throws RestApiException {
    Node ret = null;
    Collection<String> aliases = aliasLookup.getForAlias(node.getHost());
    String retUrl = aliases.toString();
    retUrl = retUrl.substring(1, retUrl.length()-1);
    String internalIP = null;
    if (node.toString().contains(".com")) {
      try {
        conn = new SSHConnection("root",
                                 "couchbasego",
                                  retUrl, 22);
        conn.connect();
        internalIP = RemoteCommands.getInternalIP(conn);
      }
      catch(IOException e) {
        e.printStackTrace();
      }
    }
    List<Node> nodes = getNodes();
    for (Node n : nodes) {

      boolean hostMatches = false;
      // Not the same host

      InetAddress addr;
      for (String alias : aliases) {
        String hostname = n.getRestUrl().getHost();
        addr = null;
        try {
          addr = InetAddress.getByName(hostname);
        } catch (Exception e) {
          logger.warn("Exception:{}", e.getMessage());
        }
        if (addr != null) {
          hostname = addr.getHostAddress();
        }

        if (internalIP != null) {
          alias = internalIP;
        }

        if (hostname.equals(alias)) {
          hostMatches = true;
          break;
        }
      }

      if (!hostMatches) {
        continue;
      }

      if (node.getPort() != -1) {
        if (n.getRestUrl().getPort() == node.getPort()) {
          return n;
        }
      } else {
        if (ret != null) {
          throw new IllegalArgumentException(
                  "Found more than one node with the same hostname. Need port");
        }
        ret = n;
      }
    }

    if (ret == null) {
      throw new RestApiException("Couldn't find node " + node);
    }

    return ret;
  }

  @Override
  public void initNewCluster(ClusterConfig config) throws RestApiException {
    // We need two requests, one to set the memory quota, the other
    // to set the authentication params.
    HttpPost memInit = new HttpPost();
    Map<String,String> params = new HashMap<String, String>();
    params.put("memoryQuota", "" + config.memoryQuota);
    memInit.setEntity(makeFormEntity(params));
    try {
      getResponseJson(memInit, P_POOLS_DEFAULT, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    params.clear();
    HttpPost authInit = new HttpPost();
    params.put("port", "SAME");
    params.put("username", this.user);
    params.put("password", this.passwd);
    authInit.setEntity(makeFormEntity(params));
    try {
      getResponseJson(authInit, P_SETTINGS_WEB, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void setupInitialService (String services) throws RestApiException {
    HttpPost req = new HttpPost();
    Map<String,String> params = new HashMap<String, String>();
    if (services != "") {
      //if no services are added. kv will be default
      params.put("services", services);
      logger.debug("initial services are {}", services);
      req.setEntity(makeFormEntity(params));
    }
    try {
      getResponseJson(req, P_SETUP_SERVICES, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

  }

  public void setupInitialHostname (String hostname) throws RestApiException {
    HttpPost req = new HttpPost();
    Map<String,String> params = new HashMap<String, String>();
    if (hostname != "") {
      //it will rename node to hostname
      params.put("hostname", hostname);
      logger.debug("node will rename to {}", hostname);
      req.setEntity(makeFormEntity(params));
    }
    try {
      getResponseJson(req, P_SETUP_HOSTNAME, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

  }

  @Override
  public void joinCluster(URL clusterUrl) throws RestApiException {
    HttpPost req = new HttpPost();
    int ePort = clusterUrl.getPort();
    if (ePort == -1) {
      ePort = entryPoint.getPort();
    }

    Map<String,String> params = new HashMap<String, String>();
    params.put("clusterMemberHostIp", clusterUrl.getHost());
    params.put("clusterMemberPort", "" + ePort);
    params.put("user", user);
    params.put("password", passwd);

    req.setEntity(makeFormEntity(params));
    try {
      getResponseJson(req, P_JOINCLUSTER, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void setClusterMaxConn(int maxConnections) throws RestApiException {

      HttpPost maxConn = new HttpPost();
      Map<String,String> params = new HashMap<String, String>();
      params.put("maxconn", "" + maxConnections);

      maxConn.setEntity(makeFormEntity(params));
      try {
          getResponseJson(maxConn, P_POOL_MAX, 202);
      } catch (IOException ex) {
          throw new RestApiException(ex);
      }
  }

  @Override
  public void changeIndexerSetting(String key, String value) throws RestApiException {

    HttpPost indexerSetting = new HttpPost();
    Map<String,String> params = new HashMap<String, String>();
    params.put(key, value);

    indexerSetting.setEntity(makeFormEntity(params));
    try {
      getResponseJson(indexerSetting, P_SETTINGS_INDEXES, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void rebalance(List<Node> remaining,
                        List<Node> failed_over,
                        List<Node> to_remove)
          throws RestApiException {

    List<String> ejectedIds = new ArrayList<String>();
    List<String> remainingIds = new ArrayList<String>();
    Map<String,String> params = new HashMap<String, String>();

    if (failed_over != null) {
      for (Node ejectedNode : failed_over) {
        ejectedIds.add(ejectedNode.getNSOtpNode());
      }
    }

    if (remaining == null) {
      remaining = getNodes();
    }

    for (Node remainingNode : remaining) {
      if (failed_over != null && failed_over.contains(remainingNode)) {
        continue;
      }
      remainingIds.add(remainingNode.getNSOtpNode());
    }

    if (to_remove != null) {
      for (Node nn : to_remove) {
        ejectedIds.add(nn.getNSOtpNode());
      }
    }

    params.put("knownNodes", StringUtils.join(remainingIds, ","));
    params.put("ejectedNodes", StringUtils.join(ejectedIds, ","));

    HttpPost req = new HttpPost();
    req.setEntity(makeFormEntity(params));
    try {
      getResponseJson(req, P_REBALANCE, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void rebalance() throws RestApiException {
    rebalance(null, null, null);
  }

  @Override
  public void createBucket(BucketConfig config) throws RestApiException {
    HttpPost req = new HttpPost();
    req.setEntity(makeFormEntity(config.makeParams()));
    try {

      // 202 Accepted
      getResponseJson(req, P_BUCKETS, 202);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void deleteBucket(String name) throws RestApiException {
    HttpDelete req = new HttpDelete();
    try {
      getResponseJson(req, P_BUCKETS + "/" +  name, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void stopRebalance() throws RestApiException {
    HttpPost post = new HttpPost();
    try {
      getResponseJson(post, P_REBALANCE_STOP, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public RebalanceInfo getRebalanceStatus() throws RestApiException {
    JsonElement js;

    try {
      js = getResponseJson(new HttpGet(), P_REBALANCE_PROGRESS, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    if (!js.isJsonObject()) {
      throw new RestApiException("Expected JSON object", js);
    }
    return new RebalanceInfo(js.getAsJsonObject());
  }


  private void otpPostCommon(Node node, String uri) throws RestApiException {
    HttpPost post = new HttpPost();
    Map<String,String> params = new HashMap<String, String>();
    params.put("otpNode", node.getNSOtpNode());
    post.setEntity(makeFormEntity(params));
    try {
      getResponseJson(post, uri, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void failoverNode(Node node) throws RestApiException {
    otpPostCommon(node, P_FAILOVER);
  }

  @Override
  public void readdNode(Node node) throws RestApiException {
    otpPostCommon(node, P_READD);
  }

  public void reset() throws RestApiException {
    HttpPost post = new HttpPost();
    try {
      StringEntity entity = new StringEntity("gen_server:cast(ns_cluster, leave).", "UTF-8");
      post.setEntity(entity);
      getResponseJson(post, P_RESET, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void ejectNode(Node node) throws RestApiException {
    otpPostCommon(node, P_EJECT);
  }

  @Override
  public ConnectionInfo getInfo() throws RestApiException {
    try {
      JsonElement js = getResponseJson(new HttpGet(), P_POOLS, 200);
      if (!js.isJsonObject()) {
        throw new RestApiException("Expected JSON Object", js);
      }

      return new ConnectionInfo(js.getAsJsonObject());

    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public Node getAsNode(boolean forceRefresh) throws RestApiException {
    if (myNode != null && forceRefresh == false) {
      return myNode;
    }

    myNode = findNode(entryPoint);
    return myNode;
  }

  @Override
  public Node getAsNode() throws RestApiException {
    return getAsNode(true);
  }


  public NodeGroup findGroup(String name) throws RestApiException {
    NodeGroup group = getGroupList().find(name);
    if (group == null) {
      throw new RestApiException("No such group");
    }
    return group;
  }

  @Override
  public void renameGroup(NodeGroup from, String to) throws RestApiException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("name", to);
    // Create a PUT request.
    HttpPut putReq = new HttpPut();
    putReq.setEntity(makeFormEntity(params));
    try {
      getResponseJson(putReq, from.getUri().toString(), 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  public void renameGroup(String from, String to) throws RestApiException {
    renameGroup(findGroup(from), to);
  }

  @Override
  public void addGroup(String name) throws RestApiException {
    Map<String, String> params = new HashMap<String, String>();
    params.put("name", name);
    HttpPost postReq = new HttpPost();
    postReq.setEntity(makeFormEntity(params));
    try {
      getResponseJson(postReq, _P_SERVERGROUPS, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  @Override
  public void deleteGroup(NodeGroup group) throws RestApiException {
    HttpDelete del = new HttpDelete();
    try {
      getResponseJson(del, group.getUri().toString(), 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  public void deleteGroup(String name) throws RestApiException {
    deleteGroup(findGroup(name));
  }

  @Override
  public void allocateGroups(Map<NodeGroup, Collection<Node>> config,
                             NodeGroupList existingGroups) throws RestApiException {

    Set<NodeGroup> groups;
    if (existingGroups == null) {
      existingGroups = getGroupList();
    }

    groups = new HashSet<NodeGroup>(existingGroups.getGroups());
    if (config.keySet().size() > groups.size()) {
      throw new IllegalArgumentException("Too many groups specified");
    }

    JsonObject payload = new JsonObject();
    JsonArray groupsArray = new JsonArray();
    payload.add("groups", groupsArray);

    Set<Node> changedNodes = new HashSet<Node>();
    for (Collection<Node> ll : config.values()) {
      // Sanity
      for (Node nn : ll) {
        if (changedNodes.contains(nn)) {
          throw new IllegalArgumentException("Node " + nn + " specified twice");
        }
        changedNodes.add(nn);
      }
    }

    // Now go through our existing groups and see which ones are to be modified
    for (NodeGroup group : groups) {
      JsonObject curJson = new JsonObject();
      JsonArray nodesArray = new JsonArray();
      groupsArray.add(curJson);

      curJson.addProperty("uri", group.getUri().toString());
      curJson.add("nodes", nodesArray);
      Set<Node> nodes = new HashSet<Node>(group.getNodes());
      if (config.containsKey(group)) {
        nodes.addAll(config.get(group));
      }

      for (Node node : nodes) {
        boolean nodeRemains;

        /**
         * If the node was specified in the config, it either belongs to us
         * or it belongs to a different group. If it does not belong to us then
         * we skip it, otherwise it's placed back inside the group list.
         */
        if (!changedNodes.contains(node)) {
          nodeRemains = true;
        } else if (config.containsKey(group) && config.get(group).contains(node)) {
          nodeRemains = true;
        } else {
          nodeRemains = false;
        }

        if (!nodeRemains) {
          continue;
        }

        // Is it staying with us, or is it moving?
        JsonObject curNodeJson = new JsonObject();
        curNodeJson.addProperty("otpNode", node.getNSOtpNode());
        nodesArray.add(curNodeJson);
      }
    }

    HttpPut putReq = new HttpPut();
    try {
      putReq.setEntity(new StringEntity(new Gson().toJson(payload)));
    } catch (UnsupportedEncodingException ex) {
      throw new IllegalArgumentException(ex);
    }

    try {
      getResponseJson(putReq, existingGroups.getAssignmentUri().toString(), 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  public void assignNodeToGroup(NodeGroup target, Node src) throws RestApiException {
    Map<NodeGroup, Collection<Node>> mm = new HashMap<NodeGroup, Collection<Node>>();
    MapUtils.addToValue(mm, target, src);
    allocateGroups(mm, null);
  }

  /**
   * Get the existing Node object, without refreshing
   * @return null if no node, the Node otherwise
   */
  public Node getCachedNode() {
    return myNode;
  }

  public String getUsername() {
    return user;
  }

  public String getPassword() {
    return passwd;
  }

  public URL getEntryPoint() {
    return entryPoint;
  }

  public AliasLookup getAliasLookupCache() {
    return aliasLookup;
  }

  /**
   * Copy this administrative client with its credentials for a new host
   * @param newHost The new host for the new object
   * @return The new client
   */
  public CouchbaseAdmin copyForHost(URL newHost, URL n1qlHost, URL ftsHost, URL cbasHost) {
    return new CouchbaseAdmin(newHost, n1qlHost, ftsHost, cbasHost, user, passwd);
  }

  public void createAnalyticsDataSet(String bucketName) throws RestApiException {
    HttpPost req = new HttpPost();

    ArrayList<NameValuePair> nvp = new ArrayList<NameValuePair>();
    String value = "create dataset `defaultDataSet` on `"+bucketName+ "`";
    nvp.add(new BasicNameValuePair("statement", value));

    try {
      req.setEntity(new UrlEncodedFormEntity(nvp));
      logger.info("createAnalyticsDataSet req_entity="+value);
      getResponseJson(req, P_ANALYTICS, 200, false, false, true);
    } catch (IOException ex) {
      logger.info("failed req_entity="+req.getEntity().toString()+":uri="+req.getURI());
      logger.error("Exception:{}", ex);
      throw new RestApiException(ex);
    }
  }

  public void connectLocalAnalyticsDataSet() throws RestApiException {
    HttpPost req = new HttpPost();

    ArrayList<NameValuePair> nvp = new ArrayList<NameValuePair>();
    String value = "connect link Local";
    nvp.add(new BasicNameValuePair("statement", value));

    try {
      req.setEntity(new UrlEncodedFormEntity(nvp));
      logger.info("connectLocalAnalyticsDataSet req_entity="+value);
      getResponseJson(req, P_ANALYTICS, 200, false, false, true);
    } catch (IOException ex) {
      logger.info("failed req_entity="+req.getEntity().toString()+":uri="+req.getURI());
      logger.error("Exception:{}", ex);
      throw new RestApiException(ex);
    }
  }

  /**
   * Create n1ql index using cbq-engine
   * @param  bucketName bucket name
   * @param indexName name of the index to be created
   * @param params parameters of index
   * @return
   * @throws RestApiException
   */
  public void setupN1QLIndex(String indexName, String indexType, String bucketName, String[] params, String targetNode) throws RestApiException {
    HttpPost req = new HttpPost();
    StringBuilder paramStr = new StringBuilder();
    paramStr.append("(");
    for (String param : params) {
      paramStr.append(param);
      paramStr.append(",");
    }
    paramStr.deleteCharAt(paramStr.lastIndexOf(","));
    paramStr.append(")");

    ArrayList<NameValuePair> nvp = new ArrayList<NameValuePair>();
    String value = "";
    if (indexType.equals("primary")) {
      value = "create primary index on `" + bucketName + "` using gsi";
    } else {
      value = "create index " + indexName + " on `" + bucketName + "`" + paramStr.toString();
    }

    if (targetNode != null && !targetNode.isEmpty()) {
      value += " with {\"nodes\":[\""+targetNode+":8091\"]}";
    }
    nvp.add(new BasicNameValuePair("statement", value));

    try {
      req.setEntity(new UrlEncodedFormEntity(nvp));
      logger.info("setupN1QLIndex req_entity="+value);
      getResponseJson(req, P_N1QL, 200, false, true, false);
    } catch (IOException ex) {
      logger.info("failed req_entity="+req.getEntity().toString()+":uri="+req.getURI());
      logger.error("Exception:{}", ex);
      throw new RestApiException(ex);
    }
  }

  /**
   * drop n1ql index
   * @param  bucketName bucket name
   * @param indexName name of the index to be dropped
   * @return
   * @throws RestApiException
   */
  public void dropN1QLIndex(String indexName, String bucketName, String indexType) throws RestApiException {
    HttpPost req = new HttpPost();
    ArrayList<NameValuePair> nvp = new ArrayList<NameValuePair>();
    String value = "";
    if (indexType.equals("primary")) {
      value = "drop primary index on `" + bucketName + "` using gsi";
    } else {
      value = "drop index `" + bucketName + "`.`" + indexName + "` using gsi";
    }

    nvp.add(new BasicNameValuePair("statement", value));

    try {
      req.setEntity(new UrlEncodedFormEntity(nvp));
      logger.info("dropN1QLIndex req_entity="+value);
      getResponseJson(req, P_N1QL, 200, false, true, false);
    } catch (IOException ex) {
      logger.info("failed req_entity="+req.getEntity().toString()+":uri="+req.getURI());
      throw new RestApiException(ex);
    }
  }

  /**
   * Create fts index
   * @param indexName name of the index to be created
   * @param bucketName bucket name
   * @return
   * @throws RestApiException
   */
  public void setupFTSIndex(String indexName, String bucketName) throws RestApiException {
    HttpPut req = new HttpPut();

    try {
      getResponseJson(req, P_FTS + "/" + indexName + "?sourceType=couchbase&indexType=fulltext-index&sourceName=" + bucketName , 200, true, false, false);
     } catch (IOException ex) {
    throw new RestApiException(ex);
    }
  }


  /**
   * Get logs from all nodes
   * @return
   * @throws RestApiException
   */

  public String getClusterCertificate() throws RestApiException {
    if (clusterCert == null) {
      HttpGet getReq = new HttpGet();

      try {
        clusterCert = getResponsePlain(getReq, _P_CLUSTERCERTIFICATE, 200);
      } catch (IOException ex) {
        throw new RestApiException(ex);
      }
    }
    return clusterCert;
  }

  public void startCBCollectInfo() throws RestApiException {
    HttpPost postReq = new HttpPost();
    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    nvps.add(new BasicNameValuePair("nodes", "*"));
    try {
      postReq.setEntity(new UrlEncodedFormEntity(nvps));
      getResponseJson(postReq, P_STARTLOGSCOLLECTION, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  public String getCBCollectionStatus() throws RestApiException {
    HttpGet getReq = new HttpGet();
    String status = null;
    try {
      JsonArray taskList =  (JsonArray)getResponseJson(getReq, P_TASKS, 200);
      for (JsonElement task: taskList) {
        JsonObject taskObj = (JsonObject)task;
        if (taskObj.get("type").getAsString().compareToIgnoreCase("clusterLogsCollection") == 0) {
          status = taskObj.get("status").getAsString();
          if (status.compareToIgnoreCase("completed") == 0) {
            JsonObject nodeLogObj = (JsonObject)taskObj.get("perNode");
            for (Entry<String,JsonElement> entry: nodeLogObj.entrySet()) {
                logPaths.put(entry.getKey().replace("ns_1@", ""), ((JsonObject) entry.getValue()).get("path").getAsString());
            }
          }
          logger.debug("Log collection progress: {}%", taskObj.get("progress").getAsString());
        }
      }
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
    return status;
  }

  @Override
  public void setupAutoFailover(boolean isEnabled, int timeout) throws RestApiException {

    HttpGet getAuto = new HttpGet();
    try {
      JsonElement response = getResponseJson(getAuto, P_AUTOFAILOVER, 200);
      logger.info("Auto failover properties: " + response);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    Map<String,String> params = new HashMap<String, String>();

    params.put("enabled", Boolean.toString(isEnabled));
    params.put("timeout", Integer.toString(timeout));

    HttpPost post = new HttpPost();
    post.setEntity(makeFormEntity(params));
    try {
      getResponseJson(post, P_AUTOFAILOVER, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }

    params.clear();
    HttpPost postReset = new HttpPost();
    postReset.setEntity(makeFormEntity(params));
    try {
      getResponseJson(postReset, P_AUTOFAILOVERRESET, 200);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }

  public JsonElement getActiveQueryStats() throws RestApiException {
    HttpGet req = new HttpGet();
    try {
      return getResponseJson(req, P_ACTIVEREQ, 200, false, true, false);
    } catch (IOException ex) {
      throw new RestApiException(ex);
    }
  }
}
