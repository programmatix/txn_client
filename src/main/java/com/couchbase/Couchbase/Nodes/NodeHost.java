package com.couchbase.Couchbase.Nodes;/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



import com.couchbase.Couchbase.Couchbase.CouchbaseAdmin;
import com.couchbase.Exceptions.RestApiException;
import com.couchbase.Logging.LogUtil;
import com.couchbase.Utils.RemoteUtils.RemoteCommands;
import com.couchbase.Utils.RemoteUtils.SSHConnection;
import com.couchbase.Utils.RemoteUtils.ServiceLogin;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.EnumSet;

/**
 * This class represents a single node as a network entity. It can contain
 * state about the node's membership as well as various login information.
 */
public class NodeHost implements Comparable<NodeHost> {
  public enum State {
    UNINIT,
    /** Was added to the cluster */
    ACTIVE,

    /** Was never added to the cluster, or is not known to the cluster */
    FREE,

    /** Was failed over */
    FAILED_OVER,

    /** Upgrade Node */
    UPGRADE
  }

  private final URI restUri;
  private final URI n1qlUri;
  private final URI ftsUri;
  private final URI cbasUri;

  private final CouchbaseAdmin admin;
  private final ServiceLogin sshLogin;
  private final String version;
  private SSHConnection sshConn;
  public Boolean isWindows = false;
  public Boolean isDocker = false;
  private final String services;

  /** The node as it exists as an 'Admin' node */
  private Node adminNode = null;
  private Set<State> lastState = EnumSet.of(State.UNINIT);
  private final Logger logger = LogUtil.getLogger(NodeHost.class);
  public String host = null;


  public void setState(Set<State> st) {
    lastState = st;
  }

  public Set<State> getState() {
    return lastState;
  }

  /**
   * Returns the Node object associated with this node.
   * It may be null during early stages of initialization.
   *
   * @see #setNObject
   */
  public Node getNObject() {
    return adminNode;
  }

  /**
   * Ensures the Administrative node is set.
   * @throws RestApiException
   */
  public void ensureNObject() throws RestApiException {
    adminNode = admin.getAsNode();
  }

  /**
   * Blindly sets the Node object for this node
   * @param adm
   */
  public void setNObject(Node adm) {
    adminNode = adm;
  }

  /**
   * Gets the CouchbaseAdmin instance which is connected to this node.
   * @return The administrative client, or {@code null} if not set.
   */
  public CouchbaseAdmin getAdmin() {
    return admin;
  }


  /**
   * Returns the REST port for the node
   * @return The REST port
   */
  public int getRestPort() {
    return restUri.getPort();
  }

  /**
   * Returns a string uniquely identifying this node
   * @return The "key"
   */
  public String getKey() {
    return restUri.toString();
  }

  public String getVersion() {
      return  version;
  }

  public String getServices() {
    return  services;
  }

  @Override
  public String toString() {
    return getKey();
  }

  public String getHostname() {
    return restUri.getHost();
  }

  String getUnixUser() {
    return sshLogin.getUsername();
  }
  String getUnixPassword() {
    return sshLogin.getPassword();
  }

  public void initSSH() throws IOException {
    sshConn = createSSH();
    // Try to start the service..
    if (!isWindows && !isDocker) {
      RemoteCommands.startCouchbase(sshConn);
    }
  }

  public synchronized SSHConnection createSSH() throws IOException {
    if (sshConn != null) {
      return sshConn;
    }

    sshConn = new SSHConnection(sshLogin.getUsername(),
                                sshLogin.getPassword(),
                                restUri.getHost());
    sshConn.connect();
    logger.debug("SSH Initialized for {}", this);
    return sshConn;
  }

  public SSHConnection getSSH() {
    return sshConn;
  }

  public URI asUri() {
    return restUri;
  }

  public NodeHost(String host, ServiceLogin restLogin, ServiceLogin n1qlLogin, ServiceLogin ftsLogin, ServiceLogin cbasLogin, ServiceLogin sshLogin, String version, String services) {
    restLogin = new ServiceLogin(restLogin, "Administrator", "12345", 8091);
    sshLogin = new ServiceLogin(sshLogin, "root", null, 22);

    restUri = URI.create("http://" + host + ":" + restLogin.getPort());
    n1qlUri = URI.create("http://" + host + ":" + n1qlLogin.getPort());
    ftsUri = URI.create("http://" + host + ":" + ftsLogin.getPort());
    cbasUri = URI.create("http://" + host + ":" + cbasLogin.getPort());
    this.version = version;
    this.sshLogin = sshLogin;
    this.services = services;

    URL url, n1qlUrl, ftsUrl, cbasUrl;
    try {
      url = restUri.toURL();
      n1qlUrl = n1qlUri.toURL();
      ftsUrl = ftsUri.toURL();
      cbasUrl = cbasUri.toURL();
    } catch (MalformedURLException ex) {
      throw new RuntimeException(ex);
    }

    this.admin = new CouchbaseAdmin(url,
                                    n1qlUrl,
                                    ftsUrl,
                                    cbasUrl,
                                    restLogin.getUsername(),
                                    restLogin.getPassword());
    this.host = host;
  }

  public NodeHost(String host, String user, String pass, String version, String services) {
    this(host, new ServiceLogin(user, pass, 8091), new ServiceLogin(user, pass, 8093), new ServiceLogin(user, pass, 8094), new ServiceLogin(user, pass, 8095), ServiceLogin.createEmpty(),version, services);
  }

  static public NodeHost fromSpec(String spec, ServiceLogin restLogin, ServiceLogin unixLogin, String version, String services) {
   return fromSpec(spec,
            ServiceLogin.create(restLogin.getUsername(), restLogin.getPassword(), 8091),
            ServiceLogin.create(restLogin.getUsername(), restLogin.getPassword(), 8093),
            ServiceLogin.create(restLogin.getUsername(), restLogin.getPassword(), 8094),
            ServiceLogin.create(restLogin.getUsername(), restLogin.getPassword(), 8095),
            ServiceLogin.create(unixLogin.getUsername(), unixLogin.getPassword(), 22), version, services);
  }

  static NodeHost fromSpec(String spec, ServiceLogin deflRest, ServiceLogin deflN1QL, ServiceLogin deflFTS, ServiceLogin deflCBAS, ServiceLogin deflSSH, String version, String services) {
    String hostname = null;
    String[] uriStrings = spec.split(",");
    List<URI> uris = new ArrayList<URI>();

    for (String s : uriStrings) {
      if (!s.contains("://")) {
            // Simple 'host:port'
            s = "http://" + s;

      }
      uris.add(URI.create(s));
      }

    if (uris.size() > 2) {
      throw new IllegalArgumentException("Too many URIs specified");
    }

    Set<String> schemesFound = new HashSet<String>();
    ServiceLogin curUnix = deflSSH;
    ServiceLogin curRest = deflRest;
    ServiceLogin curN1QL = deflN1QL;
    ServiceLogin curFTS = deflFTS;
    ServiceLogin curCBAS = deflCBAS;

    for (URI uri : uris) {
      String scheme = uri.getScheme().toLowerCase();
      if (schemesFound.contains(scheme)) {
        throw new IllegalArgumentException("Scheme specified twice");
      }

      schemesFound.add(scheme);

      if (scheme.equals("http")) {
        hostname = uri.getHost();
        curRest = ServiceLogin.create(uri, deflRest);
        curN1QL = ServiceLogin.create(uri, deflN1QL);
        curFTS = ServiceLogin.create(uri, deflFTS);
        curCBAS = ServiceLogin.create(uri, deflCBAS);
      } else if (scheme.equals("ssh")) {
        curUnix = ServiceLogin.create(uri, deflSSH);
      } else {
        throw new IllegalArgumentException("Unknown scheme");
      }
    }

    if (hostname == null) {
      throw new IllegalArgumentException("Host must not be null");
    }
     //creating a new node with version information
    return new NodeHost(hostname, curRest, curN1QL, curFTS, curCBAS, curUnix, version, services);
  }

  @Override
  public int compareTo(@Nonnull NodeHost other) {
    return asUri().compareTo(other.asUri());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NodeHost)) {
      return false;
    }

    NodeHost nodeHost = (NodeHost) o;

    if (restUri != null ? !restUri.equals(nodeHost.restUri) : nodeHost.restUri != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return restUri != null ? restUri.hashCode() : 0;
  }
}
