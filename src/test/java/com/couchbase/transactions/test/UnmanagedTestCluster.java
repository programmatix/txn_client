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

import org.testcontainers.shaded.okhttp3.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class UnmanagedTestCluster extends TestCluster {

  private final OkHttpClient httpClient = new OkHttpClient.Builder().build();
  private final String seedHost;
  private final int seedPort;
  private final String adminUsername;
  private final String adminPassword;
  private volatile String bucketname;
  private final int numReplicas;

  UnmanagedTestCluster(final Properties properties) {
    seedHost = properties.getProperty("cluster.unmanaged.seed").split(":")[0];
    seedPort = Integer.parseInt(properties.getProperty("cluster.unmanaged.seed").split(":")[1]);
    adminUsername = properties.getProperty("cluster.adminUsername");
    adminPassword = properties.getProperty("cluster.adminPassword");
    numReplicas = Integer.parseInt(properties.getProperty("cluster.unmanaged.numReplicas"));
  }

  @Override
  ClusterType type() {
    return ClusterType.UNMANAGED;
  }

  @Override
  TestClusterConfig _start() throws Exception {
    bucketname = UUID.randomUUID().toString();

    createBucket(bucketname);

    Response getResponse = httpClient.newCall(new Request.Builder()
      .header("Authorization", Credentials.basic(adminUsername, adminPassword))
      .url("http://" + seedHost + ":" + seedPort + "/pools/default/b/" + bucketname)
      .build())
    .execute();

    String raw = getResponse.body().string();

    waitUntilAllNodesHealthy();

    Optional<X509Certificate> cert = loadClusterCertificate();

    return new TestClusterConfig(
      bucketname,
      adminUsername,
      adminPassword,
      nodesFromRaw(seedHost, raw),
      replicasFromRaw(raw),
      cert,
      capabilitiesFromRaw(raw)
    );
  }

  @Override
  public void createBucket(String name) {
    Response postResponse = null;
    try {
      postResponse = httpClient.newCall(new Request.Builder()
        .header("Authorization", Credentials.basic(adminUsername, adminPassword))
        .url("http://" + seedHost + ":" + seedPort + "/pools/default/buckets")
        .post(new FormBody.Builder()
          .add("name", name)
          .add("bucketType", "membase")
          .add("ramQuotaMB", "100")
          .add("replicaNumber", Integer.toString(numReplicas))
          .build())
        .build())
        .execute();

      if (postResponse.code() != 202) {
        throw new RuntimeException("Could not create bucket: "
                + postResponse + ", Reason: "
                + postResponse.body().string());
      }

      waitUntilAllNodesHealthy();
    } catch (Exception e) {
      throw new RuntimeException("Could not create bucket");
    }

  }

  private Optional<X509Certificate> loadClusterCertificate() {
    try {
      Response getResponse = httpClient.newCall(new Request.Builder()
        .header("Authorization", Credentials.basic(adminUsername, adminPassword))
        .url("http://" + seedHost + ":" + seedPort + "/pools/default/certificate")
        .build())
        .execute();

      String raw = getResponse.body().string();

      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      Certificate cert = cf.generateCertificate(new ByteArrayInputStream(raw.getBytes(UTF_8)));
      return Optional.of((X509Certificate) cert);
    } catch (Exception ex) {
      // could not load certificate, maybe add logging? could be CE instance.
      return Optional.empty();
    }
  }

  private void waitUntilAllNodesHealthy() throws Exception {
    while(true) {
      Response getResponse = httpClient.newCall(new Request.Builder()
        .header("Authorization", Credentials.basic(adminUsername, adminPassword))
        .url("http://" + seedHost + ":" + seedPort + "/pools/default/")
        .build())
        .execute();

      String raw = getResponse.body().string();

      Map<String, Object> decoded;
      try {
        decoded = (Map<String, Object>)
          MAPPER.readValue(raw, Map.class);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      List<Map<String, Object>> nodes = (List<Map<String, Object>>) decoded.get("nodes");
      int healthy = 0;
      for (Map<String, Object> node : nodes) {
        String status = (String) node.get("status");
        if (status.equals("healthy")) {
          healthy++;
        }
      }
      if (healthy == nodes.size()) {
        break;
      }
      Thread.sleep(100);
    }
  }

  @Override
  public void deleteBucket(String name) {
    try {
      httpClient.newCall(new Request.Builder()
              .header("Authorization", Credentials.basic(adminUsername, adminPassword))
              .url("http://" + seedHost + ":" + seedPort + "/pools/default/buckets/"+name)
              .delete()
              .build()).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    deleteBucket(bucketname);
  }
}
