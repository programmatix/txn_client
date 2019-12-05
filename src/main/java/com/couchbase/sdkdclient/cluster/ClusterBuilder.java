/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.sdkdclient.cluster;

import com.couchbase.cbadmin.client.AliasLookup;
import com.couchbase.sdkdclient.cluster.installer.ClusterInstaller;
import com.couchbase.sdkdclient.cluster.installer.InstallerOptions;
import com.couchbase.sdkdclient.logging.LogUtil;
import com.couchbase.sdkdclient.options.OptionConsumer;
import com.couchbase.sdkdclient.options.OptionDomains;
import com.couchbase.sdkdclient.options.OptionTree;
import com.couchbase.sdkdclient.util.CallOnceSentinel;
import com.couchbase.sdkdclient.util.Configurable;
import com.couchbase.sdkdclient.util.Configured;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class can be used to provide the configuration for a cluster
 * and the buckets it will contain.
 */
public class ClusterBuilder implements OptionConsumer, Configurable {
  final static Logger logger = LogUtil.getLogger(ClusterBuilder.class);

  final private CBBucketOptions bucketOptions = new CBBucketOptions();
  final private CBClusterOptions clusterOptions = new CBClusterOptions();
  final InstallerOptions instOptions = new InstallerOptions();
  final private AliasLookup ourAliases = new AliasLookup();
  final private ArrayList<NodeHost> nodes = new ArrayList<NodeHost>();
  public String driverHost = "";

  private NodelistBuilder nlb = null;
  private boolean configured = false;

  @Override
  public OptionTree getOptionTree() {
    OptionTree me = new OptionTree();
    me.addChild(bucketOptions.getOptionTree());
    me.addChild(clusterOptions.getOptionTree());


    //me.addChild(instOptions.getOptionTree());
    OptionTree prefixedInstOptions = instOptions.getOptionTree();
    prefixedInstOptions.setPrefix(OptionDomains.INSTALLER);
    me.addChild(prefixedInstOptions);
    return me;
  }

  /**
   * Get the options controlling cluster configuration
   * @return The cluster options.
   */
  public CBClusterOptions getClusterOptions() {
    return clusterOptions;
  }

  /**
   * Get the options controlling bucket creation
   * @return The bucket options.
   */
  public CBBucketOptions getBucketOptions() {
    return bucketOptions;
  }

  /**
   * Get the underlying object controlling node management.
   * This must be called <b>after</b> {@link #configure()}
   * @return The nodelist builder.
   */
  public NodelistBuilder getNodelistBuilder() {
    if (!configured) {
      throw new IllegalStateException("Must be called after configure");
    }
    return nlb;
  }

  /**
   * Adds a node to the cluster.
   * @param nn A node to add
   * @return The builder
   */
  public ClusterBuilder node(NodeHost nn) {
    if (nodes.contains(nn)) {
      logger.warn("Node {} already exists. Replacing", nn);
      nodes.remove(nn);
    }
    nodes.add(nn);
    logger.debug("Nodes collection {}", nodes);
    return this;
  }

  /**
   * @see #node(NodeHost)
   */
  public ClusterBuilder node(Collection<NodeHost> nn) {
    for (NodeHost node : nn) {
      node(node);
    }
    return this;
  }

  /**
   * Returns a collection of current nodes.
   *
   * This method will return the recognized nodes added via calls to
   * {@link #node(NodeHost)}
   * @return A collection of current known nodes.
   */
  public Collection<NodeHost> getCurrentNodes() {
    return nodes;
  }


  @Override
  public boolean isConfigured() {
    return configured;
  }

  /**
   * Adds nodes from the specification strings provided to the cluster
   * options.
   */
  private void addNodesFromSpec() {
      NodeHost nn;
      // Gets all the relevant options and handles them there.
    for (String spec : clusterOptions.getRawNodes()) {
      String[] nodeInfo = spec.split(":");

      nn = NodeHost.fromSpec(nodeInfo[0].concat(nodeInfo.length > 1 ? (":" + nodeInfo[1]) : ""),
                                      clusterOptions.getRestLogin(),
                                      clusterOptions.getUnixLogin(),
                                      nodeInfo.length >= 3 ? nodeInfo[2] : "",
                                      nodeInfo.length >= 4 ? nodeInfo[3] : "" );
      logger.debug("Nodes order {}", nn.asUri());
      node(nn);
    }
  }


  private final CallOnceSentinel co_Configure = new CallOnceSentinel();
  @Override
  public void configure() {
    co_Configure.check();
    configured = true;

    for (String alias : clusterOptions.getIpAliases()) {
      List<String> aliases = Arrays.asList(alias.split("/"));
      ourAliases.associateAlias(aliases);
    }

    addNodesFromSpec();

    for (NodeHost nn : nodes) {
      nn.getAdmin().getAliasLookupCache().merge(ourAliases);
    }

    nlb = new NodelistBuilder(nodes, clusterOptions.getGroupCount(), clusterOptions.getGroupPolicy(), clusterOptions.getUpgradeVersion());
  }

  /**
   * Builds an installer instance
   *
   * This will return a cluster installer conforming to the specifications
   * within the options. Note that this will always return an installer even
   * if the {@code skip} option is true.
   *
   * @return a cluster installer
   */
  public Configured<ClusterInstaller> buildInstaller() {
    return Configured.create(new ClusterInstaller(instOptions));
  }

  /**
   * Builds an installer if the options allow it.
   * @param createOnSkip Whether to create an installer even if the options
   * specify it should be skipped.
   * @return An installer instance, or {@code null} if skip was specified in
   * the options and {@code createOnSkip} is false
   */
  ClusterInstaller buildInstaller(boolean createOnSkip) {
    if (instOptions.shouldSkip() && createOnSkip == false) {
      return null;
    }
    return buildInstaller().get();
  }

  public void setDriverHost(String host) {
    driverHost = host;
  }



}