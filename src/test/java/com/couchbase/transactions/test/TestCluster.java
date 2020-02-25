/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.transactions.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class TestCluster implements ExtensionContext.Store.CloseableResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestCluster.class);

  protected static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * The topology spec defined by the child implementations.
   */
  private volatile TestClusterConfig config;

  /**
   * Creates the Test cluster (either managed ur unmanaged).
   */
  public static TestCluster create() {
    Properties properties = loadProperties();
    String clusterType = properties.getProperty("cluster.type");

    if (clusterType.equals("containerized")) {
      return new ContainerizedTestCluster(properties);
    } else if (clusterType.equals("mocked")) {
      return new MockTestCluster(properties);
    } else if (clusterType.equals("unmanaged")) {
      return new UnmanagedTestCluster(properties);
    } else {
      throw new IllegalStateException("Unsupported test cluster type: " + clusterType);
    }
  }

  /**
   * Implemented by the child class to start the cluster if needed.
   */
  abstract TestClusterConfig _start() throws Exception;

  abstract ClusterType type();

  abstract public void createBucket(String name);
  abstract public void deleteBucket(String name);

  TestCluster() { }

  public void start() {
    try {
      config = _start();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public TestClusterConfig config() {
    return config;
  }

  /**
   * Helper method to extract the node configs from a raw bucket config.
   *
   * @param config the config.
   * @return the extracted node configs.
   */
  @SuppressWarnings({"unchecked"})
  protected List<TestNodeConfig> nodesFromRaw(final String inputHost, final String config) {
    List<TestNodeConfig> result = new ArrayList<>();
    Map<String, Object> decoded;
    try {
      decoded = (Map<String, Object>)
        MAPPER.readValue(config.getBytes(UTF_8), Map.class);
    } catch (IOException e) {
      throw new RuntimeException("Error decoding, raw: " + config, e);
    }
    List<Map<String, Object>> ext = (List<Map<String, Object>>) decoded.get("nodesExt");
    for (Map<String, Object> node : ext) {
      Map<String, Integer> services = (Map<String, Integer>) node.get("services");
      String hostname = (String) node.get("hostname");
      if (hostname == null) {
        hostname = inputHost;
      }
      Map<Services, Integer> ports = new HashMap<>();
      if (services.containsKey("kv")) {
        ports.put(Services.KV, services.get("kv"));
      }
      if (services.containsKey("kvSSL")) {
        ports.put(Services.KV_TLS, services.get("kvSSL"));
      }
      if (services.containsKey("mgmt")) {
        ports.put(Services.MANAGER, services.get("mgmt"));
      }
      if (services.containsKey("mgmtSSL")) {
        ports.put(Services.MANAGER_TLS, services.get("mgmtSSL"));
      }
      if (services.containsKey("n1ql")) {
        ports.put(Services.QUERY, services.get("n1ql"));
      }
      if (services.containsKey("n1qlSSL")) {
        ports.put(Services.QUERY_TLS, services.get("n1qlSSL"));
      }
      if (services.containsKey("cbas")) {
        ports.put(Services.ANALYTICS, services.get("cbas"));
      }
      if (services.containsKey("cbasSSL")) {
        ports.put(Services.ANALYTICS_TLS, services.get("cbasSSL"));
      }
      if (services.containsKey("fts")) {
        ports.put(Services.SEARCH, services.get("fts"));
      }
      if (services.containsKey("ftsSSL")) {
        ports.put(Services.SEARCH_TLS, services.get("ftsSSL"));
      }
      result.add(new TestNodeConfig(hostname, ports));
    }
    return result;
  }

  protected int replicasFromRaw(final String config) {
    Map<String, Object> decoded;
    try {
      decoded = (Map<String, Object>)
        MAPPER.readValue(config.getBytes(UTF_8), Map.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Map<String, Object> serverMap = (Map<String, Object>) decoded.get("vBucketServerMap");
    return (int) serverMap.get("numReplicas");
  }

  protected Set<Capabilities> capabilitiesFromRaw(final String config) {
    Set<Capabilities> capabilities = new HashSet<>();
    Map<String, Object> decoded;
    try {
      decoded = (Map<String, Object>)
        MAPPER.readValue(config.getBytes(UTF_8), Map.class);
    } catch (IOException e) {
      throw new RuntimeException("Error decoding, raw: " + config, e);
    }
    List<Map<String, Object>> ext = (List<Map<String, Object>>) decoded.get("nodesExt");
    for (Map<String, Object> node : ext) {
      Map<String, Integer> services = (Map<String, Integer>) node.get("services");
      for (String name : services.keySet()) {
        if (name.equals("n1ql") || name.equals("n1qlSSL")) {
          capabilities.add(Capabilities.QUERY);
        }
        if (name.equals("cbas") || name.equals("cbasSSL")) {
          capabilities.add(Capabilities.ANALYTICS);
        }
        if (name.equals("fts") || name.equals("ftsSSL")) {
          capabilities.add(Capabilities.SEARCH);
        }
      }
    }
    return capabilities;
  }

  /**
   * Load properties from the defaults file and then override with sys properties.
   */
  private static Properties loadProperties() {
    Properties defaults = new Properties();
    try {
      try {
        // This file is unversioned.  Good practice is to copy integration.properties to this and make changes to this.
        URL url = TestCluster.class.getResource("/integration.local.properties");
        if (url != null) {
          LOGGER.info("Found test config file {}", url.getPath());
        }
        defaults.load(url.openStream());
      }
      catch (Exception ex) {
        URL url = TestCluster.class.getResource("/integration.properties");
        if (url != null) {
          LOGGER.info("Found test config file {}", url.getPath());
        }
        defaults.load(url.openStream());
      }
    } catch (Exception ex) {
      throw new RuntimeException("Could not load properties", ex);
    }

    Properties all = new Properties(System.getProperties());
    for (Map.Entry<Object, Object> property : defaults.entrySet()) {
      if (all.getProperty(property.getKey().toString()) == null) {
        all.put(property.getKey(), property.getValue());
      }
    }
    return all;
  }

}
