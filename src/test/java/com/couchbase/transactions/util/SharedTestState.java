/*
 * Copyright (c) 2020 Couchbase, Inc.
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
package com.couchbase.transactions.util;

import com.couchbase.Constants.Strings;
import com.couchbase.Logging.LogUtil;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.manager.bucket.BucketManager;
import com.couchbase.grpc.protocol.ResumableTransactionServiceGrpc;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.transactions.StandardTest;
import com.couchbase.transactions.log.SimpleEventBusLogger;
import com.couchbase.transactions.tracing.TracingWrapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.slf4j.Logger;
import reactor.core.publisher.Hooks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This will store shared test state, such as connection to a cluster
 *
 * TODO this is all super-hardcoded.  Make this generic, pick up from CLI or similar
 */
public class SharedTestState {
    private final Cluster cluster;
    private final Bucket bucket;
    private final Collection collection;
    private final AtomicInteger droppedErrors = new AtomicInteger(0);
    private final String TXN_SERVER_HOSTNAME = "localhost";
//    private final static String CLUSTER_HOSTNAME = "172.23.105.55";
     private final String CLUSTER_HOSTNAME = "localhost";
    private final static int PORT = 8050;
    private final Logger logger;
    private final ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub;

    public SharedTestState() {
        cluster = Cluster.connect(CLUSTER_HOSTNAME, Strings.ADMIN_USER, Strings.PASSWORD);
        bucket = cluster.bucket("default");
        collection = bucket.defaultCollection();

        LogUtil.setLevelFromSpec("all:Info");
        logger = LogUtil.getLogger("log");

        // GRPC is used to connect to the server(s)
        ManagedChannel channel = ManagedChannelBuilder.forAddress(TXN_SERVER_HOSTNAME, PORT).usePlaintext().build();
        stub = ResumableTransactionServiceGrpc.newBlockingStub(channel);

        TxnClient.conn_info conn_create_req =
            TxnClient.conn_info.newBuilder()
                .setHandleHostname(CLUSTER_HOSTNAME)
                .setHandleBucket(bucket.name())
                .setHandlePort(8091)
                .setHandleUsername(Strings.ADMIN_USER)
                .setHandlePassword(Strings.PASSWORD)
                .setHandleAutofailoverMs(5)
                .build();
        TxnClient.APIResponse response = stub.createConn(conn_create_req);
        assert(response.isInitialized());

        Hooks.onErrorDropped(v -> {
            logger.info("onError: " + v.getMessage());
            droppedErrors.incrementAndGet();
        });
    }

    public static SharedTestState create() {
        return new SharedTestState();
    }

    public Cluster cluster() {
        return cluster;
    }

    public Bucket bucket() {
        return bucket;
    }

    public Collection collection() {
        return collection;
    }

    public ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub() {
        return stub;
    }

    public void close() {
        cluster.disconnect();
    }

    public AtomicInteger droppedErrors() {
        return droppedErrors;
    }

    public Logger logger() {
        return logger;
    }
}
