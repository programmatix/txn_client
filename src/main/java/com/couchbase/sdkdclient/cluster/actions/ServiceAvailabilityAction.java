/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.couchbase.sdkdclient.cluster.actions;

import com.couchbase.sdkdclient.cluster.NodeHost;
import com.couchbase.sdkdclient.cluster.ClusterBuilder;
import com.couchbase.sdkdclient.cluster.RemoteCommands;
import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.options.OptionConsumer;
import com.couchbase.sdkdclient.options.OptionDomains;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.options.OptionTreeBuilder;
import com.couchbase.sdkdclient.ssh.SimpleCommand;

import java.util.Enumeration;

import org.slf4j.Logger;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.util.Collection;
import java.util.concurrent.Future;
import java.net.UnknownHostException;

/**
 * Changes the availability status of a given service.
 * <p/>
 * Most routines here will require SSH access into the cluster.
 * <p/>
 * See the {@link ServiceAvailabilityOptions} for the options to control
 * this class' behavior.
 */
public class ServiceAvailabilityAction implements OptionConsumer {

  /**
   * This enum specifies the server process to affect
   */
  public enum ProcessType {

    /**
     * The <code>memcached</code> process. This process is responsible for
     * handling the key/value protocol which clients use.
     * <p/>
     * Actions available:
     * <ul>
     * <li>{@link ActionType#HANG} To hang the process and make it unresponsive.
     * This will cause all key-value operations on clients to timeout. This will
     * also cause any future connections to hang or otherwise be unresponsive.</li>
     * <li>{@link ActionType#KILL} To kill the process forcefully. This will
     * cause any memcached connections on the clients to reset</li>
     * </ul>
     */
    MEMCACHED,

    /**
     * The <code>ns_server</code> process (which actually runs as
     * <code>beam.smp</code>. This is the server behind the REST API and is
     * also the component which detects and handles topology changes for each
     * node.
     * <p/>
     * Actions Available:
     * <ul>
     * <li>{@link ActionType#HANG} Will cause REST connections to become
     * unresponsive.</li>
     * <li>{@link ActionType#KILL} Honestly, I don't know how this will affect
     * clients, but it should be interesting to see
     * </li>
     * </ul>
     */
    NS_SERVER,
    /**
     * The <code>indexer</code> process (which actually runs as
     * <code>indexer</code></code>.
     */

    INDEXER,
    /**
     * The <code>projector</code> process (which actually runs as
     * <code>projector</code>. This is the server behind the REST API and is
     * also the component which detects and handles topology changes for each
     * node.
     */
    PROJECTOR,
    /**
     * The <code>cbqengine</code> process (which actually runs as
     * <code>cbqengine</code>.
     */
    CBQ_ENGINE,

    /**
     * This represents the Couchbase <i>service</i> as a whole. While this
     * includes both {@link #MEMCACHED} and {@link #NS_SERVER}, the actions
     * available for this service will be different.
     */
    SYSV_SERVICE,

    /**
     * Actions Available:
     * <ul>
     * <li>{@link ActionType#KILL} Resetting traffic between server and the client.
     * Reset the firewall to default after 45 seconds
     * </li>
     * </ul>
     */
    RST_CONNECT,

    /**
     * Actions Available:
     * <ul>
     * <li>{@link ActionType#KILL} Blocking traffic between server and the client.
     * Reset the firewall to default after 45 seconds
     * </li>
     * </ul>
     */
    DROP_CONNECT,

    /**
     * Actions Available:
     * <ul>
     * <li>{@link ActionType#KILL} Blocking query traffic between server and the client.
     * </li>
     * </ul>
     */
    DROP_QUERY,


    SINGLETXN_MULTICLIENT
  }

  public enum ActionType {

    /**
     * Kills a service, any existing connections to this service will be
     * shut down, and any subsequent connections to this service will be
     * rejected with e.g. {@code ECONNREFUSED}
     */
    KILL,

    /**
     * Hangs a service. Any existing connections to this service will appear
     * to become unresponsive. Any new connections will "hang".
     */
    HANG,

  }

  final ServiceAvailabilityOptions options = new ServiceAvailabilityOptions();
  Collection<NodeHost> nodes;

  private String onStr;
  private String offStr;
  private final Logger logger = LogUtil.getLogger(getClass());
  private String driver;

  @Override
  public OptionTree getOptionTree() {
    return new OptionTreeBuilder()
            .source(options, options.getClass())
            .prefix(OptionDomains.SERVICE)
            .group("service")
            .description("Options controlling service failure")
            .build();
  }

  public int getRequiredActiveCount() {
    return options.getNumNodes();
  }

  public void prepare() {
    verifyCommands();
    // MEMCACHED|NS_SERVER
    switch (options.getProcess()) {
      case MEMCACHED:
      case NS_SERVER:
      case INDEXER:
      case CBQ_ENGINE:
        String pName = "beam.smp";
        if (options.getProcess() == ProcessType.MEMCACHED) {
          pName = "memcached";
        }
        if (options.getProcess() == ProcessType.INDEXER) {
          pName = "indexer";
        }
        if (options.getProcess() == ProcessType.PROJECTOR) {
          pName = "projector";
        }
        if (options.getProcess() == ProcessType.CBQ_ENGINE) {
          pName = "cbq-engine";
        }


        if (options.getAction() == ActionType.HANG) {
          offStr = "pkill -STOP -f " + pName;
          onStr = "pkill -CONT -f " + pName;

        } else if (options.getAction() == ActionType.KILL) {
          offStr = "pkill -KILL -f  " + pName;
          onStr = "service couchbase-server restart";
        }
        break;

      case SYSV_SERVICE:
        offStr = "service couchbase-server stop";
        onStr = "service couchbase-server start";
        break;

      case RST_CONNECT:
        setSdkdclientIP();
        offStr = "iptables -A INPUT -s " + this.driver +" -p tcp --dport 22 -j ACCEPT; iptables -A INPUT -s " + this.driver + " -p tcp --match multiport  --dports 11210,8091,11211,18091,8092,8093,18093,18094 -j REJECT";
        onStr = "iptables -F";
        break;

      case DROP_CONNECT:
        setSdkdclientIP();
        offStr = "iptables -A INPUT -s " + this.driver +" -p tcp --dport 22 -j ACCEPT; iptables -A INPUT -s " + this.driver + " -p tcp --match multiport  --dports 11210,8091,11211,8092,18091,8093,18093,18094 -j DROP";
        onStr = "iptables -F";
        break;


      case DROP_QUERY:
        offStr = "iptables -A INPUT -s " + this.driver + " -p tcp --dport 8093 -j DROP";
        onStr = "iptables -F";
        break;

    }
  }

  private void verifyCommands() {
    switch (options.getProcess()) {
      case MEMCACHED:
      case NS_SERVER:
        switch (options.getAction()) {
          case HANG:
          case KILL:
            break;
          default:
            throw new IllegalArgumentException("Action/Process Mismatch");
        }
        break;
      case SYSV_SERVICE:
        if (options.getAction() != ActionType.KILL) {
          throw new IllegalArgumentException("Action/Process Mismatch");
        }
      case RST_CONNECT:
        if (options.getAction() != ActionType.KILL) {
          throw new IllegalArgumentException("Action/Process Mismatch");
        }
    }
  }

  public void setup(ClusterBuilder clb) {
    nodes = clb.getNodelistBuilder().reserveActive(options.getNumNodes(),
            options.shouldUseEpt());
    this.driver = clb.driverHost;
    logger.debug("Driver {} ",driver);
  }

  private void staggerNodes(String cmd) throws IOException {
    for (NodeHost nn : nodes) {
      Future<SimpleCommand> ft = RemoteCommands.runCommand(nn.getSSH(), cmd);
      try {
        SimpleCommand res = ft.get();
        if (!res.isSuccess()) {
          logger.warn("Command {} failed. {}, {}",
                  cmd, res.getStderr(), res.getStdout());
        }
      } catch (Exception ex) {
        throw new IOException(ex);
      }

      try {
        Thread.sleep(options.getStaggerDelay() * 1000);
      } catch (InterruptedException exc) {
        throw new IOException(exc);
      }
    }
  }

  // Turns off a service on the selected nodes.
  public void turnOff() throws IOException {
    staggerNodes(offStr);
  }

  public void turnOn() throws IOException {
    staggerNodes(onStr);
    try {
      Thread.sleep(options.getRecoverDelay() * 1000);

    } catch (InterruptedException exc) {
      throw new IOException(exc);
    }
  }

  public Collection<NodeHost> getNodes() {
    return nodes;
  }

  public void setSdkdclientIP() {
    if (this.driver == "127.0.0.1") {
      try {
        for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
          NetworkInterface intf = (NetworkInterface) en.nextElement();
          for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
            InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
              String ipAddress = inetAddress.getHostAddress().toString();
              this.driver = ipAddress;
            }
          }
        }
      } catch (Exception ex) {
        logger.debug(ex.getMessage() + ex.getStackTrace());
      }
    }
  }

  //<editor-fold defaultstate="collapsed" desc="Description methods">
  public String getFailureDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append("Execute '").append(offStr).append("'");
    sb.append(" on ").append(options.getNumNodes()).append(" nodes");
    if (options.shouldUseEpt()) {
      sb.append(" (including EPT)");
    }
    if (options.getStaggerDelay() > 0) {
      sb.append(" waiting ").append(options.getStaggerDelay()).append(" seconds");
      sb.append(" between each invocation");
    }
    return sb.toString();
  }

  public String getActivationDescription() {
    return "re-active the services by invoking " + onStr + "on each of the nodes";
  }
  //</editor-fold>
}
