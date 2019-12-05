/*
 * Copyright (c) 2013 Couchbase, Inc.
 */

package com.couchbase.cbadmin.client;

import com.couchbase.cbadmin.assets.Bucket;
import com.couchbase.cbadmin.assets.Node;
import com.couchbase.cbadmin.assets.NodeGroup;
import com.couchbase.cbadmin.assets.NodeGroupList;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Interface for the {@link CouchbaseAdmin} class. This only has one implementation
 * and is defined here for documentation purposes only.
 *
 * We normally don't use the 'I' suffix but we don't want to break the existing
 * name.
 */
public interface ICouchbaseAdmin {

  /**
   * Performs a GET request on a path and returns a JSON element.
   *
   * This is intended as a lower level method for operations which don't have
   * a wrapped API function in this interface.
   *
   * @param path The path to query (relative to entryPoint)
   * @return a JsonElement
   * @throws IOException
   * @throws RestApiException
   */
  public JsonElement getJson(String path) throws IOException, RestApiException;

  /**
   * Get the buckets for this cluster.
   *
   * @return A list of buckets. The list may be empty
   */
  public Map<String, Bucket> getBuckets() throws RestApiException;

  /**
   * Returns a list of nodes known to the cluster.
   *
   * The return value contains all the nodes which are known to the cluster,
   * whether they are active or inactive.
   *
   * @return A list of Node objects.
   */
  public List<Node> getNodes() throws RestApiException;

  /**
   * Adds a new node to the cluster. It is assumed this node's credentials
   * are the same as the one of the current client
   *
   * @param newNode A URI indicating the entry point of the new node
   */
  public void addNewNode(URL newNode, String services) throws RestApiException;

  /**
   * Adds the node to the cluster, copying the credential information from the
   * {@code newNode} admin object.
   *
   * @param newNode The node to add.
   * @throws RestApiException
   */
  public void addNewNode(CouchbaseAdmin newNode, String services) throws  RestApiException;


  /**
   * Adds a new node to the cluster.
   * @param newNode The URL of the new node's REST API
   * @param nnUser The username of the new node to add
   * @param nnPass The password of the new node to add
   */
  public void addNewNode(URL newNode, String nnUser, String nnPass, String services) throws RestApiException;


  /**
   * Gets a Node object based on the input URI
   *
   * @param node The URI to match against. If the URI does not contain an
   * explicit port, an exception may be thrown if there is more than one
   * node with the same IP (e.g. in cluster_run scenarios)
   *
   * @return the matching Node object
   *
   * @throws RestApiException if not found
   */
  public Node findNode(URL node) throws RestApiException;


  /**
   * Initialize this node with its own cluster. Once a node has been
   * initialized, other uninitialized nodes may be joined to the newly
   * created cluster.
   *
   * @param config A configuration object containing settings to apply to the
   *               newly created cluster.
   * @throws RestApiException
   */
  public void initNewCluster(ClusterConfig config) throws RestApiException;

  public void setupInitialService(String services) throws RestApiException;


  /**
   * Join this node to an existing cluster.
   * @param clusterUrl The URL of the existing cluster's entry point
   * @throws RestApiException
   */
  public void joinCluster(URL clusterUrl) throws RestApiException;

  /**
   * Set max connection for memecached buckets
   * @param maxConnections The URL of the existing cluster's entry point
   * @throws RestApiException
   */
  public void setClusterMaxConn(int maxConnections) throws RestApiException;

  /**
   * Change settings for indexer
   * @param key settings key
   * @param value settings value
   * @throws RestApiException
   */
  public void changeIndexerSetting(String key, String value) throws RestApiException;

  /**
   * Rebalance the cluster.
   * @param remaining the nodes to remain in the cluster. If this is not
   * null, the cluster will only end up containing these nodes.
   * @param failed_over The list of nodes that were failed over and should be
   * purged from the cluster.
   * @param to_remove Active nodes that should be removed from the cluster
   * @throws RestApiException
   */
  public void rebalance(List<Node> remaining,
                        List<Node> failed_over,
                        List<Node> to_remove) throws RestApiException;

  /**
   * Rebalance the cluster. Ejects any failed over nodes and activates
   * any pending added nodes.
   * @throws RestApiException
   */
  public void rebalance() throws RestApiException;

  public void createUser(String username, String password) throws RestApiException;


  /**
   * Creates a new bucket. The bucket must not yet exist
   * @param config An object specifying parameters of the bucket. This must
   * not be null.
   * @throws RestApiException
   */
  public void createBucket(BucketConfig config) throws RestApiException;

  /**
   * Delete a bucket
   * @param name The name of the bucket to remove
   * @throws RestApiException if the bucket does not exist.
   */
  public void deleteBucket(String name) throws RestApiException;

  /**
   * Stops a pending rebalance.
   * @throws RestApiException
   */
  public void stopRebalance() throws RestApiException;

  /**
   * Get rebalance status for a pending or completed rebalance operation
   * @return a RebalanceInfo object which can be queried for progress
   * @throws RestApiException
   */
  public RebalanceInfo getRebalanceStatus() throws RestApiException;


  /**
   * Marks a given node as being failed over. A cluster in a failover state
   * will promote the given node's replicas as being active.
   *
   * The node can then be completely removed from the cluster using {@link #ejectNode(Node)}
   * method or it may be readded using the {@link #readdNode(Node)}} method.
   *
   * @param node The node to fail over. The node must be a member of the cluster
   *
   * @throws RestApiException
   */
  public void failoverNode(Node node) throws RestApiException;


  /**
   * Readds a failed over node that has come back online. The cluster must
   * have not already been rebalanced.
   * @param node The node to add-back.
   * @throws RestApiException
   */
  public void readdNode(Node node) throws RestApiException;


  /**
   * Ejects an inactive node from the cluster
   * @param node The node to eject
   * @throws RestApiException
   */
  public void ejectNode(Node node) throws RestApiException;


  /**
   * Get information about the node used as the entry point. This method
   * will never fail as it retrieves the most basic information. It can be
   * used to check the cluster version and whether the node is part of a
   * cluster
   * @return A ConnectionInfo object
   * @throws RestApiException
   */
  public ConnectionInfo getInfo() throws RestApiException;


  /**
   * Gets the correponding Node object for this Admin client
   * @return a node object
   * @throws RestApiException
   */
  public Node getAsNode(boolean forceRefresh) throws RestApiException;

  public Node getAsNode() throws RestApiException;

  /**
   * Get a collection of all server groups in the cluster
   * @throws RestApiException
   */
  public NodeGroupList getGroupList() throws RestApiException;

  /**
   * Adds a new group with a given name
   * @param name The name to give for the new group.
   * @throws RestApiException
   */
  public void addGroup(String name) throws RestApiException;

  /**
   * Rename a group
   * @param group The group to rename.
   * @param to the new name for the group.
   * @throws RestApiException
   */
  public void renameGroup(NodeGroup group, String to) throws RestApiException;

  /**
   * Removes a group.
   *
   * The group must not have any nodes inside it.
   *
   * @param group The group to remove.
   * @throws RestApiException
   */
  public void deleteGroup(NodeGroup group) throws RestApiException;

  /**
   * Assign nodes to groups.
   *
   * This operation will assign all the value collections containing nodes
   * to their group keys. Nodes and/or groups not present in this list will
   * not be modified.
   *
   * Note that as this operation requires a list
   *
   * @param config Configuration map indicating which nodes are to be added
   * or moved to which groups. Entries here represent only the nodes to be
   * changed.
   *
   * @param existingGroups An existing group list. If null, it will be obtained
   * via {@link #getGroupList()}. This is required because modifying the group
   * list requires knowledge of all groups and their respective nodes.
   *
   * @throws RestApiException
   */
  public void allocateGroups(Map<NodeGroup, Collection<Node>> config,
                             NodeGroupList existingGroups) throws RestApiException;

  /**
   *
   * Sets autofailover setting on cluster and timeout
   *
   * @param  isEnabled  if set, autofailover is enabled, else disabled
   * @param autoFailoverTimeout Tmeout after which auto failover occurs on server
   * @throws RestApiException
   */
  public void setupAutoFailover(boolean isEnabled, int autoFailoverTimeout) throws RestApiException;
 }
