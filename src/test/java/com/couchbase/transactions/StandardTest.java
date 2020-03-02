/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.  All rights reserved.
 */

package com.couchbase.transactions;

import com.couchbase.Constants.Strings;
import com.couchbase.Logging.LogUtil;
import com.couchbase.client.core.cnc.Event;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.core.error.TemporaryFailureException;
import com.couchbase.client.core.logging.LogRedaction;
import com.couchbase.client.core.logging.RedactionLevel;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.manager.bucket.*;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.*;
import com.couchbase.grpc.protocol.ResumableTransactionServiceGrpc;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.transactions.components.ATREntry;
import com.couchbase.transactions.components.ActiveTransactionRecord;
import com.couchbase.transactions.components.DocumentGetter;
import com.couchbase.transactions.config.TransactionConfig;
import com.couchbase.transactions.config.TransactionConfigBuilder;
import com.couchbase.transactions.error.TransactionFailed;
import com.couchbase.transactions.error.attempts.AttemptException;
import com.couchbase.transactions.error.internal.AbortedAsRequestedNoRollbackNoCleanup;
import com.couchbase.transactions.log.SimpleEventBusLogger;
import com.couchbase.transactions.support.AttemptStates;
import com.couchbase.transactions.tracing.TracingUtils;
import com.couchbase.transactions.tracing.TracingWrapper;
import com.couchbase.transactions.util.ATRValidator;
import com.couchbase.transactions.util.DocValidator;
import com.couchbase.transactions.util.ResultValidator;
import com.couchbase.transactions.util.TestAttemptContextFactory;
import com.couchbase.transactions.util.TransactionFactoryWrapper;
import com.couchbase.transactions.util.TransactionMock;
import com.couchbase.transactions.TestUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.couchbase.transactions.support.TransactionFields.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * All unit tests that don't go into a more specialised file.
 */


//TODO Use Scope. For now ignoring this parameter
// Enable creating and configuring a cluster

public class StandardTest {
    private static Cluster cluster;
    private static Bucket bucket;
    private static BucketManager bucketManager;
    private static Collection collection;
    private static Tracer tracer;
    private static TracingWrapper tracing;
    private static Span span;
    private static boolean clusterHasPartialLogRedactionEnabled = true;
    private static SimpleEventBusLogger LOGGER;
    private static AtomicInteger droppedErrors = new AtomicInteger(0);
    private static String TXN_SERVER_HOSTNAME = "localhost";
    // TODO make this generic
    //    private static String CLUSTER_HOSTNAME = "172.23.105.65";
    private static String CLUSTER_HOSTNAME = "localhost";
    private static int PORT = 8050;
    static Logger logger;
    private static ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub = null;


    @BeforeAll
    public static void beforeOnce() {
        tracing = TracingUtils.getTracer();
        tracer = tracing.tracer();
        span = tracer.buildSpan("standard").ignoreActiveSpan().start();

        cluster = Cluster.connect(CLUSTER_HOSTNAME, Strings.ADMIN_USER, Strings.PASSWORD);
        bucket = cluster.bucket("default");
        collection = bucket.defaultCollection();

        /*
        Map<String,String> map = new HashMap<String,String>();
        map.put("flush","true");
        bucketManager.updateBucket(new BucketSettings("default",map,null,0,false,0,null,null,null,null));
         */

        long now = System.currentTimeMillis();
        bucketManager = cluster.buckets();

        // bucketManager.flushBucket("default");

        LogUtil.setLevelFromSpec("all:Info");
        logger = LogUtil.getLogger(StandardTest.class);

        Hooks.onErrorDropped(v -> {
            logger.info("onError: " + v.getMessage());
            droppedErrors.incrementAndGet();
        });
    }

    @BeforeAll
    static void beforeAll() {
        // GRPC is used to connect to the server(s)
        ManagedChannel channel = ManagedChannelBuilder.forAddress(TXN_SERVER_HOSTNAME, PORT).usePlaintext().build();
        stub = ResumableTransactionServiceGrpc.newBlockingStub(channel);

        cluster = Cluster.connect(CLUSTER_HOSTNAME, Strings.ADMIN_USER, Strings.PASSWORD);
        collection = cluster.bucket("default").defaultCollection();
        TxnClient.conn_info conn_create_req =
            TxnClient.conn_info.newBuilder()
                .setHandleHostname(CLUSTER_HOSTNAME)
                .setHandleBucket("default")
                .setHandlePort(8091)
                .setHandleUsername(Strings.ADMIN_USER)
                .setHandlePassword(Strings.PASSWORD)
                .setHandleAutofailoverMs(5)
                .build();
        TxnClient.APIResponse response = stub.createConn(conn_create_req);
    }


    @BeforeEach
    public void beforeEach() {
        // Was reading all and removing all ATRs to avoid the flush, but it really screws up the OpenTracing output
        //        collection.bucketManager().flush();
        TestUtils.cleanupBefore(collection);
    }

    @AfterEach
    public void afterEach() {
        assertEquals(0, droppedErrors.get());
        droppedErrors.set(0);
    }

    @AfterAll
    public static void afterOnce() {
        span.finish();
        tracing.close();
        cluster.disconnect();
    }


    private static TxnClient.TransactionsFactoryCreateRequest.Builder createTransactionsFactory(int expiry,
                                                                                                String durability,
                                                                                                boolean CleanupClientAttempts, boolean CleanupLostAttempts) {
        return TxnClient.TransactionsFactoryCreateRequest.newBuilder()

            // Disable these threads so can run multiple Transactions (and hence hooks)
            .setCleanupClientAttempts(CleanupClientAttempts)
            .setCleanupLostAttempts(CleanupLostAttempts)

            // This is default durability for txns library
            //  .setDurability(TxnClient.Durability.MAJORITY)
            .setDurability(TxnClient.Durability.valueOf(durability))

            // Plenty of time for manual debugging
            .setExpirationSeconds(expiry);
    }

    private TransactionConfig storetxnconfig(int expiry, String durability, boolean CleanupClientAttempts,
                                             boolean CleanupLostAttempts) {
        TransactionDurabilityLevel durabilityLevel = TransactionDurabilityLevel.MAJORITY;
        switch (durability) {
            case "NONE":
                durabilityLevel = TransactionDurabilityLevel.NONE;
                break;
            case "MAJORITY":
                durabilityLevel = TransactionDurabilityLevel.MAJORITY;
                break;
            case "MAJORITY_AND_PERSIST_TO_ACTIVE":
                durabilityLevel = TransactionDurabilityLevel.MAJORITY_AND_PERSIST_TO_ACTIVE;
                break;
            case "PERSIST_TO_MAJORITY":
                durabilityLevel = TransactionDurabilityLevel.PERSIST_TO_MAJORITY;
                break;
        }
        return TransactionConfigBuilder.create()
            .durabilityLevel(durabilityLevel)
            .cleanupLostAttempts(CleanupLostAttempts)
            .cleanupClientAttempts(CleanupClientAttempts)
            .expirationTime(Duration.ofSeconds(expiry))
            .build();
    }


    private void executeVerification(TxnClient.TransactionResultObject txnclose, TransactionConfig transactionConfig,
                                     String attemptstate) {
        logger.info("Running assertions");
        assertEquals(0, TestUtils.numAtrs(collection, transactionConfig, span));

        // TODO replace with assertCompletedInSingleAttempt
//        logger.info("txnclose.getTxnAttemptsSize():" + txnclose.getTxnAttemptsSize());
//        assertEquals(1, txnclose.getTxnAttemptsSize());
//
//        logger.info("txnclose.getAttemptFinalState():" + txnclose.getAttemptFinalState());
//        assertEquals(attemptstate, txnclose.getAttemptFinalState());
//
//        logger.info("txnclose.getAtrCollectionPresent():" + txnclose.getAtrCollectionPresent());
//        assertFalse(txnclose.getAtrCollectionPresent());
//
//        logger.info("txnclose.getAtrIdPresent():" + txnclose.getAtrIdPresent());
//        assertFalse(txnclose.getAtrIdPresent());

        logger.info("txnclose.getMutationTokensSize():" + txnclose.getMutationTokensSize());
        assertEquals(0, txnclose.getMutationTokensSize());
    }

    @Test
    public void createEmptyTransaction() {
        TxnClient.TransactionsFactoryCreateResponse factory =
            stub.transactionsFactoryCreate(createTransactionsFactory(120, "MAJORITY", false, false)
                .build());
        assertTrue(factory.getSuccess());
        TransactionConfig transactionConfig = storetxnconfig(120, "MAJORITY", false, false);

        TxnClient.TransactionCreateResponse create =
            stub.transactionCreate(TxnClient.TransactionCreateRequest.newBuilder()
                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                .build());
        assertTrue(create.getSuccess());

        String transactionRef = create.getTransactionRef();
        TxnClient.TransactionGenericResponse empty =
            stub.transactionEmpty(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());
        assertTrue(empty.getSuccess());

        TxnClient.TransactionResultObject txnclose =
            stub.transactionClose(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());

        executeVerification(txnclose, transactionConfig, "COMPLETED");


        assertTrue(stub.transactionsFactoryClose(TxnClient.TransactionsFactoryCloseRequest.newBuilder()
            .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
            .build()).getSuccess());
    }

    @Test
    public void commitEmptyTransaction() {
        TxnClient.TransactionsFactoryCreateResponse factory =
            stub.transactionsFactoryCreate(createTransactionsFactory(120, "MAJORITY", false, false)
                .build());
        assertTrue(factory.getSuccess());
        TransactionConfig transactionConfig = storetxnconfig(120, "MAJORITY", false, false);

        TxnClient.TransactionCreateResponse create =
            stub.transactionCreate(TxnClient.TransactionCreateRequest.newBuilder()
                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                .build());
        assertTrue(create.getSuccess());

        String transactionRef = create.getTransactionRef();

        TxnClient.TransactionGenericResponse commit =
            stub.transactionCommit(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());
        assertTrue(commit.getSuccess());

        TxnClient.TransactionResultObject txnclose =
            stub.transactionClose(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());

        executeVerification(txnclose, transactionConfig, "COMPLETED");

        assertTrue(stub.transactionsFactoryClose(TxnClient.TransactionsFactoryCloseRequest.newBuilder()
            .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
            .build()).getSuccess());

    }

    @Test
    public void rollbackEmptyTransaction() {
        TxnClient.TransactionsFactoryCreateResponse factory =
            stub.transactionsFactoryCreate(createTransactionsFactory(120, "MAJORITY", false, false)
                .build());
        assertTrue(factory.getSuccess());
        TransactionConfig transactionConfig = storetxnconfig(120, "MAJORITY", false, false);

        TxnClient.TransactionCreateResponse create =
            stub.transactionCreate(TxnClient.TransactionCreateRequest.newBuilder()
                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                .build());
        assertTrue(create.getSuccess());

        String transactionRef = create.getTransactionRef();
        TxnClient.TransactionGenericResponse rollback =
            stub.transactionRollback(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());
        assertTrue(rollback.getSuccess());

        TxnClient.TransactionResultObject txnclose =
            stub.transactionClose(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());

        executeVerification(txnclose, transactionConfig, "ROLLED_BACK");

        assertTrue(stub.transactionsFactoryClose(TxnClient.TransactionsFactoryCloseRequest.newBuilder()
            .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
            .build()).getSuccess());
    }

    @Disabled("disabling for now as hangs")
    @Test
    public void rollbackCommittedEmptyTransaction() {
        TxnClient.TransactionsFactoryCreateResponse factory =
            stub.transactionsFactoryCreate(createTransactionsFactory(120, "MAJORITY", false, false)
                .build());
        assertTrue(factory.getSuccess());

        TxnClient.TransactionCreateResponse create =
            stub.transactionCreate(TxnClient.TransactionCreateRequest.newBuilder()
                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                .build());
        assertTrue(create.getSuccess());

        String transactionRef = create.getTransactionRef();
        String docId = UUID.randomUUID().toString();
        JsonObject docContent = JsonObject.create().put(Strings.CONTENT_NAME, Strings.DEFAULT_CONTENT_VALUE);

        // TODO this is test rollbackCommitted*Empty*Transaction, shouldn't contain anything
        TxnClient.TransactionGenericResponse insert =
            stub.transactionInsert(TxnClient.TransactionInsertRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .setDocId(docId)
                .setContentJson(docContent.toString())
                .build());

        assertTrue(insert.getSuccess());

        GetResult get = cluster.bucket("default").defaultCollection().get(docId);
        assertEquals(0, get.contentAsObject().size());

        TxnClient.TransactionGenericResponse commit =
            stub.transactionCommit(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());

        assertTrue(commit.getSuccess());

        // TODO this is rolling back after committing, doesn't make sense
        TxnClient.TransactionGenericResponse rollback =
            stub.transactionRollback(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());
        assertFalse(rollback.getSuccess());

        TxnClient.TransactionResultObject txnclose =
            stub.transactionClose(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef)
                .build());

        assertEquals(txnclose.getExceptionName(), "com.couchbase.transactions.error.attempts.AttemptException");
        LogRedaction.setRedactionLevel(RedactionLevel.PARTIAL);
        txnclose.getLogList().forEach(l -> {
            logger.info("Checking logreadaction: " + l);
            if (l.contains(docId)) {
                //TODO below assertion not working . Need to check if this should be actually working
                // assertTrue(l.contains("<ud>" + docId + "</ud>"));
            }
        });

        assertTrue(stub.transactionsFactoryClose(TxnClient.TransactionsFactoryCloseRequest.newBuilder()
            .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
            .build()).getSuccess());
    }

    @Test
    public void oneInsertCommitted() {
        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(stub)) {
            String docId = TestUtils.docId(collection, 0);
            JsonObject docContent = JsonObject.create().put(Strings.CONTENT_NAME, Strings.DEFAULT_CONTENT_VALUE);

            wrap.insert(docId, docContent.toString());

            DocValidator.assertInsertedDocIsStaged(collection, docId);

            TxnClient.TransactionResultObject result = wrap.commitAndClose();

            ResultValidator.dumpLogs(result);
            ResultValidator.assertCompletedInSingleAttempt(collection, result);
        }
    }
//
//    @Test
//    public void tranasctionOnClosedTransactionsShouldFail() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope));
//
//            transactions.close();
//
//            assertThrows(IllegalStateException.class, () -> {
//                TransactionResult result = transactions.run((ctx) -> {
//                });
//            });
//
//            // Double-close is fine
//            transactions.close();
//        }
//    }
//
//    //    @Disabled
//    //    @Test
//    //    public void loop() {
//    //        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//    //            try (Transactions transactions = Transactions.create(cluster, TransactionConfigBuilder.create()
//    //            .replicateTo(ReplicateTo.NONE).build())) {
//    //                String docId = TestUtils.docId(collection, 0);
//    //                JsonObject initial = JsonObject.create().put("val", 1);
//    //
//    //                while (true) {
//    //                    try {
//    //                        TransactionResult result = transactions.run((ctx) -> {
//    //                            ctx.insert(collection, docId, initial);
//    //                            ctx.get(collection, docId);
//    //                            ctx.get(collection, docId);
//    //                            TransactionGetResult doc = ctx.get(collection, docId).get();
//    //                            ctx.remove(doc);
//    //                            ctx.commit();
//    //                        }, TestUtils.defaultPerConfig(scope));
//    //                    } catch (TransactionFailed err) {
//    //                        fail();
//    //                    }
//    //                }
//    //            }
//    //        }
//    //    }
//    //
//    //    @Disabled
//    //    @Test
//    //    public void runJunk() {
//    //        Transactions transactions = Transactions.create(cluster, TransactionConfigBuilder.create()
//    //                .replicateTo(ReplicateTo.NONE)
//    ////                .logDirectly(Event.Severity.INFO)
//    //                .build());
//    //
//    //        collection.upsert("anotherDoc", JsonObject.create().put("foo", "bar")));
//    //        String docId = TestUtils.docId(collection, 0);
//    //        collection.upsert(docId, JsonObject.create().put("foo", "bar")));
//    //
//    //        for (int i = 0; i < 1000000; i++) {
//    //            try {
//    //                transactions.run((ctx) -> {
//    //                    // Inserting a doc:
//    ////                    JsonDocument toInsert = testName, JsonObject.create().));
//    ////                    ctx.insert(collection, toInsert);
//    //
//    //                    // Getting documents:
//    //                    Optional<TransactionGetResult> docOpt = ctx.get(collection, docId);
//    //                    TransactionGetResult doc = ctx.get(collection, docId);
//    //
//    //                    // Replacing a doc:
//    //                    TransactionGetResult anotherDoc = ctx.get(collection, "anotherDoc");
//    //                    anotherJsonObject content = doc.contentAs(JsonObject.class);
//    //content.put("transactions", "are awesome");
//    //                    ctx.replace(anotherDoc);
//    //
//    //                    // Removing a doc:
//    //                    Optional<TransactionGetResult> yetAnotherDoc = ctx.get(collection, "yetAnotherDoc");
//    //                    if (yetAnotherDoc.isPresent()) {
//    //                        ctx.remove(yetAnotherDoc.get());
//    //                    }
//    //
//    //                    ctx.commit();
//    //                }, TestUtils.defaultPerConfig(null));
//    //            }
//    //            catch (TransactionFailed e) {
//    //                System.err.println("Transaction failed iteration " + i + ": " + e.getMessage());
//    //                for (LogDefer err: e.result().log().logs()) {
//    //                    System.err.println("Transaction failure: " + err);
//    //                }
//    //                break;
//    //            }
//    //        }
//    //    }
//    //
//
//    @Test
//    public void oneInsertCommittedAsync() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.insert(collection.reactive(), docId, initial).flatMap(ignore -> {
//                        return collection.reactive().get(docId);
//                    }).doOnNext(d -> {
//                        assertFalse(d.contentAs(JsonObject.class).containsKey("val"));
//                    }).flatMap(ignore -> ctx.commit());
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//
//                assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                TestUtils.assertCompletedIn1Attempt(transactions.config(), r, collection, scope.span(),
//                    cluster.environment().transcoder());
//                TestUtils.assertAtrEntryDocs(collection, r, Arrays.asList(docId), null, null, transactions.config(),
//                    scope.span());
//                assertEquals(1, r.mutationTokens().size());
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//
//
//    @Test
//    public void docXattrsEmptyAfterTxn() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.insert(collection.reactive(), docId, initial).flatMap(ignore -> ctx.commit());
//                }, TestUtils.defaultPerConfig(scope));
//                TransactionResult r = result.block();
//
//                assertEquals(1, TestUtils.numAtrs(collection, transactions.config(), span));
//                TransactionGetResult doc = DocumentGetter.justGetDoc(collection.reactive(), transactions.config(),
//                    docId, TestUtils.from(scope), cluster.environment().transcoder()).block().get();
//                assertFalse(doc.links().atrId().isPresent());
//                assertFalse(doc.links().stagedAttemptId().isPresent());
//                assertFalse(doc.links().stagedContent().isPresent());
//                assertFalse(doc.links().isDocumentInTransaction());
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//
//    @Test
//    public void oneInsertRolledBack() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, initial);
//
//                    assertFalse(collection.get(docId).contentAs(JsonObject.class).containsKey("val"));
//
//                    ctx.rollback();
//                }, TestUtils.defaultPerConfig(scope));
//                assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//                TestUtils.assertRolledBackIn1Attempt(transactions.config(), result, collection, scope.span(),
//                    cluster.environment().transcoder());
//                TestUtils.assertAtrEntryDocs(collection, result, Arrays.asList(docId), null, null,
//                    transactions.config(), span);
//                assertEquals(1, result.mutationTokens().size());
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//    @Test
//    public void oneInsertRolledBackAsync() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.insert(collection.reactive(), docId, initial).flatMap(ignore -> {
//                        return ctx.rollback();
//                    });
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//                assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//                TestUtils.assertRolledBackIn1Attempt(transactions.config(), r, collection, scope.span(),
//                    cluster.environment().transcoder());
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//
//    @Test
//    public void twoInsertsCommitted() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                JsonObject initial2 = JsonObject.create().put("val", 2);
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, initial);
//                    ctx.insert(collection, docId2, initial2);
//
//                    ctx.commit();
//                }, TestUtils.defaultPerConfig(scope));
//                assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                assertTrue(2 == collection.get(docId2).contentAs(JsonObject.class).getInt("val"));
//                TestUtils.assertCompletedIn1Attempt(transactions.config(), result, collection, scope.span(),
//                    cluster.environment().transcoder());
//                TestUtils.assertAtrEntryDocs(collection, result, Arrays.asList(docId, docId2), null, null,
//                    transactions.config(), span);
//                assertEquals(2, result.mutationTokens().size());
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//    @Test
//    public void twoInsertsCommittedAsync() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                JsonObject initial2 = JsonObject.create().put("val", 2);
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.insert(collection.reactive(), docId, initial).flatMap(ignore -> ctx.insert(collection.reactive(), docId2, initial2)).flatMap(ignore -> ctx.commit());
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//                TestUtils.assertCompletedIn1Attempt(transactions.config(), r, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                assertTrue(2 == collection.get(docId2).contentAs(JsonObject.class).getInt("val"));
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//
//
//    public static TransactionConfig createConfig() {
//        return TransactionConfigBuilder.create().build();
//    }
//
//    @Test
//    public void oneUpdateCommitted() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                try {
//                    TransactionResult result = transactions.run((ctx) -> {
//                        TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//                        JsonObject content = doc.contentAs(JsonObject.class);
//                        content.put("val", 2);
//                        ctx.replace(doc, content);
//
//                        assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//
//                        ctx.commit();
//                    }, TestUtils.defaultPerConfig(scope));
//
//                    TestUtils.assertCompletedIn1Attempt(transactions.config(), result, collection, scope.span(),
//                        cluster.environment().transcoder());
//                    assertEquals(2, (int) collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                    checkLogRedactionIfEnabled(result, docId);
//
//                } catch (TransactionFailed e) {
//                    e.printStackTrace();
//                    fail();
//                }
//            }
//        }
//    }
//
//    @Test
//    public void oneUpdateCommittedAsync() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.get(collection.reactive(), docId).flatMap(doc -> {
//                        JsonObject content = doc.contentAs(JsonObject.class);
//                        content.put("val", 2);
//                        return ctx.replace(doc, content);
//                    }).flatMap(ignore -> ctx.commit());
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//                TestUtils.assertCompletedIn1Attempt(transactions.config(), r, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertEquals(2, (int) collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                TestUtils.assertAtrEntryDocs(collection, r, null, Arrays.asList(docId), null, transactions.config(),
//                    scope.span());
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//
//    @Test
//    public void oneUpdateImplicit() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//                    JsonObject content = doc.contentAs(JsonObject.class);
//                    content.put("val", 2);
//                    ctx.replace(doc, content);
//
//                    assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                }, TestUtils.defaultPerConfig(scope));
//                TestUtils.assertCompletedIn1Attempt(transactions.config(), result, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertTrue(2 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                TestUtils.assertAtrEntryDocs(collection, result, null, Arrays.asList(docId), null,
//                    transactions.config(), span);
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//    @Test
//    public void oneUpdateCommittedAsyncImplicit() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//
//
//                    //            return ctx.get(collection, testName)
//                    //                    .flatMap(doc -> {
//                    //                        if (doc.isPresent()) {
//                    //                            doc.contentAs(JsonObject.class).put("val", 2);
//                    //                            return ctx.replace(doc.get());
//                    //                        }
//                    //                        else {
//                    //                            return Mono.empty();
//                    //                        }
//                    //                    })
//                    //                    .then(ctx.commit());
//
//
//                    return ctx.get(collection.reactive(), docId).flatMap(doc -> {
//                        JsonObject content = doc.contentAs(JsonObject.class);
//                        content.put("val", 2);
//                        return ctx.replace(doc, content);
//                    }).then();
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//                TestUtils.assertCompletedIn1Attempt(transactions.config(), r, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertTrue(2 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//
//    @Test
//    public void oneUpdateRolledBack() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//                    JsonObject content = doc.contentAs(JsonObject.class);
//                    content.put("val", 2);
//                    doc = ctx.replace(doc, content);
//
//                    assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//
//                    ctx.rollback();
//                }, TestUtils.defaultPerConfig(scope));
//                TestUtils.assertRolledBackIn1Attempt(transactions.config(), result, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                assertFalse(TestUtils.isDocInTxn(collection, docId, transactions.config(), TestUtils.from(span),
//                    cluster.environment().transcoder()));
//                TestUtils.assertAtrEntryDocs(collection, result, null, Arrays.asList(docId), null,
//                    transactions.config(), span);
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//    @Test
//    public void allTimestampsWrittenAfterRollback() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, initial);
//                    ctx.rollback();
//                }, TestUtils.defaultPerConfig(scope));
//                ATREntry entry = TestUtils.atrEntryForAttempt(collection, result.attempts().get(0).attemptId(),
//                    transactions.config(), span);
//
//                assertFalse(entry.timestampCommitMsecs().isPresent());
//                assertFalse(entry.timestampCompleteMsecs().isPresent());
//                assertTrue(entry.timestampStartMsecs().isPresent());
//                assertTrue(entry.timestampRollBackMsecs().isPresent());
//                assertTrue(entry.timestampRolledBackMsecs().isPresent());
//                assertTrue(entry.timestampRollBackMsecs().get() >= entry.timestampStartMsecs().get());
//                assertTrue(entry.timestampRolledBackMsecs().get() >= entry.timestampRollBackMsecs().get());
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//    @Test
//    public void allTimestampsWrittenAfterCommit() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, initial);
//                    ctx.commit();
//                }, TestUtils.defaultPerConfig(scope));
//                ATREntry entry = TestUtils.atrEntryForAttempt(collection, result.attempts().get(0).attemptId(),
//                    transactions.config(), span);
//
//                assertTrue(entry.timestampCommitMsecs().isPresent());
//                assertTrue(entry.timestampCompleteMsecs().isPresent());
//                assertTrue(entry.timestampStartMsecs().isPresent());
//                assertFalse(entry.timestampRollBackMsecs().isPresent());
//                assertFalse(entry.timestampRolledBackMsecs().isPresent());
//                assertTrue(entry.timestampCommitMsecs().get() >= entry.timestampStartMsecs().get());
//                assertTrue(entry.timestampCompleteMsecs().get() >= entry.timestampCommitMsecs().get());
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//
//    @Test
//    public void oneUpdateRolledBackAsync() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.get(collection.reactive(), docId).flatMap(doc -> {
//                        JsonObject content = doc.contentAs(JsonObject.class).put("val", 2);
//                        return ctx.replace(doc, content);
//                    }).flatMap(ignore -> ctx.rollback());
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//                TestUtils.assertRolledBackIn1Attempt(transactions.config(), r, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                TestUtils.assertAtrEntryDocs(collection, r, null, Arrays.asList(docId), null, transactions.config(),
//                    scope.span());
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//
//    @Test
//    public void oneUpdateRolledBackOnFailAsync() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                collection.insert(docId, JsonObject.create().put("val", 1));
//                collection.insert(docId2, JsonObject.create().put("val", 2));
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.get(collection.reactive(), docId).flatMap(doc -> {
//                        JsonObject content = doc.contentAs(JsonObject.class).put("val", 3);
//                        return ctx.replace(doc, content);
//                    }).flatMap(ignore -> ctx.get(collection.reactive(), docId2)).flatMap(doc -> {
//                        if (doc.contentAs(JsonObject.class).getInt("val") == 2) {
//                            return ctx.rollback();
//                        } else {
//                            return Mono.just(doc);
//                        }
//                    }).flatMap(ignore -> ctx.commit());
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//                TestUtils.assertRolledBackIn1Attempt(transactions.config(), r, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                assertTrue(2 == collection.get(docId2).contentAs(JsonObject.class).getInt("val"));
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//
//    @Test
//    public void ifElseLogicInAsync() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                TestUtils.cleanup(collection, "a");
//                TestUtils.cleanup(collection, "b");
//
//                collection.insert("a", JsonObject.create().put("val", 1));
//                collection.insert("b", JsonObject.create().put("val", 2));
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.get(collection.reactive(), "a").flatMap(doc -> {
//                        JsonObject content = doc.contentAs(JsonObject.class).put("val", 3);
//                        return ctx.replace(doc, content);
//                    }).then(ctx.get(collection.reactive(), "b")).flatMap(doc -> {
//                        if (doc.contentAs(JsonObject.class).getInt("val") == 3) {
//                            return ctx.rollback();
//                        } else {
//                            JsonObject content = doc.contentAs(JsonObject.class);
//                            content.put("val", 4);
//                            return ctx.replace(doc, content).then(ctx.commit());
//                        }
//                    });
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//                TestUtils.assertCompletedIn1Attempt(transactions.config(), r, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertTrue(3 == collection.get("a").contentAs(JsonObject.class).getInt("val"));
//                assertTrue(4 == collection.get("b").contentAs(JsonObject.class).getInt("val"));
//            }
//        }
//    }
//
//
//    @Test
//    public void twoUpdatesCommitted() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                JsonObject initial2 = JsonObject.create().put("val", 2);
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial2);
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc1 = ctx.getOptional(collection, docId).get();
//                    JsonObject content = doc1.contentAs(JsonObject.class).put("val", 3);
//                    ctx.replace(doc1, content);
//
//                    TransactionGetResult doc2 = ctx.getOptional(collection, docId2).get();
//                    JsonObject content2 = doc2.contentAs(JsonObject.class).put("val", 4);
//                    ctx.replace(doc2, content2);
//
//                    ctx.commit();
//                }, TestUtils.defaultPerConfig(scope));
//                TestUtils.assertCompletedIn1Attempt(transactions.config(), result, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertTrue(3 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                assertTrue(4 == collection.get(docId2).contentAs(JsonObject.class).getInt("val"));
//                TestUtils.assertAtrEntryDocs(collection, result, null, Arrays.asList(docId, docId2), null,
//                    transactions.config(), span);
//                assertEquals(2, result.mutationTokens().size());
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//    @Test
//    public void twoUpdatesCommittedAsync() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                JsonObject initial2 = JsonObject.create().put("val", 2);
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial2);
//
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.get(collection.reactive(), docId).flatMap(doc -> {
//                        JsonObject content = doc.contentAs(JsonObject.class);
//                        content.put("val", 3);
//                        return ctx.replace(doc, content);
//                    }).flatMap(v -> ctx.get(collection.reactive(), docId2)).flatMap(doc -> {
//                        JsonObject content = doc.contentAs(JsonObject.class);
//                        content.put("val", 4);
//                        return ctx.replace(doc, content);
//                    }).flatMap(ignore -> ctx.commit());
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//                TestUtils.assertCompletedIn1Attempt(transactions.config(), r, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertTrue(3 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                assertTrue(4 == collection.get(docId2).contentAs(JsonObject.class).getInt("val"));
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//
//    @Test
//    public void twoUpdatesRolledBackAsync() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                JsonObject initial2 = JsonObject.create().put("val", 2);
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial2);
//
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.get(collection.reactive(), docId).flatMap(doc -> {
//                        JsonObject content = doc.contentAs(JsonObject.class);
//                        content.put("val", 3);
//                        return ctx.replace(doc, content);
//                    }).flatMap(v -> ctx.get(collection.reactive(), docId2)).flatMap(doc -> {
//                        JsonObject content = doc.contentAs(JsonObject.class);
//                        content.put("val", 4);
//                        return ctx.replace(doc, content);
//                    }).flatMap(ignore -> ctx.rollback());
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//                assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                assertTrue(2 == collection.get(docId2).contentAs(JsonObject.class).getInt("val"));
//                TestUtils.assertOneAtrEntry(collection, AttemptStates.ROLLED_BACK, transactions.config(), span);
//                TestUtils.assertRolledBackIn1Attempt(transactions.config(), r, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertEquals(2, r.mutationTokens().size());
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//
//
//    @Test
//    public void oneDeleteCommitted() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                collection.insert(docId, JsonObject.create().put("val", 1));
//
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//
//                    ctx.remove(doc);
//
//                    assertTrue(collection.get(docId).contentAs(JsonObject.class).containsKey("val"));
//
//                    ctx.commit();
//                }, TestUtils.defaultPerConfig(scope));
//                assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//                TestUtils.assertCompletedIn1Attempt(transactions.config(), result, collection, scope.span(),
//                    cluster.environment().transcoder());
//                TestUtils.assertAtrEntryDocs(collection, result, null, null, Arrays.asList(docId),
//                    transactions.config(), span);
//                assertEquals(1, result.mutationTokens().size());
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//
//    @Test
//    public void oneDeleteRolledBack() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                collection.insert(docId, JsonObject.create().put("val", 1));
//
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//
//                    ctx.remove(doc);
//
//                    assertTrue(collection.get(docId).contentAs(JsonObject.class).containsKey("val"));
//
//                    ctx.rollback();
//                }, TestUtils.defaultPerConfig(scope));
//                TestUtils.assertRolledBackIn1Attempt(transactions.config(), result, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertTrue(collection.get(docId).contentAs(JsonObject.class).containsKey("val"));
//                TestUtils.assertAtrEntryDocs(collection, result, null, null, Arrays.asList(docId),
//                    transactions.config(), span);
//                assertEquals(1, result.mutationTokens().size());
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//    // Just checking the logging output
//    @Test
//    public void logging() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope).logDirectly(Event.Severity.ERROR))) {
//                String docId = TestUtils.docId(collection, 0);
//                collection.insert(docId, JsonObject.create().put("val", 1));
//
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//
//                    ctx.remove(doc);
//
//                    ctx.rollback();
//                }, TestUtils.defaultPerConfig(scope));
//
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//    public static void checkLogRedactionIfEnabled(TransactionResult result, String id) {
//        LogRedaction.setRedactionLevel(RedactionLevel.PARTIAL);
//
//        result.log().logs().forEach(l -> {
//            String s = l.toString();
//            System.out.println(s);
//            if (s.contains(id)) {
//                assertTrue(l.toString().contains("<ud>" + id + "</ud>"));
//            }
//        });
//    }
//
//    @Test
//    public void oneDeleteRolledBackAsync() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                collection.insert(docId, JsonObject.create().put("val", 1));
//
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.get(collection.reactive(), docId).flatMap(doc -> ctx.remove(doc)).then(ctx.rollback());
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//
//                assertNotNull(collection.get(docId));
//                assertTrue(collection.get(docId).contentAs(JsonObject.class).containsKey("val"));
//                TestUtils.assertRolledBackIn1Attempt(transactions.config(), r, collection, scope.span(),
//                    cluster.environment().transcoder());
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//
//
//    @Test
//    public void casChangesOnReplace() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                collection.insert(docId, JsonObject.create().put("val", 1));
//
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc1 = ctx.getOptional(collection, docId).get();
//                    long origCas = doc1.cas();
//                    JsonObject content = doc1.contentAs(JsonObject.class);
//                    content.put("val", 3);
//                    doc1 = ctx.replace(doc1, content);
//                    assertNotEquals(origCas, doc1.cas());
//                    assertNotEquals(origCas, 0);
//                    assertNotEquals(doc1.cas(), 0);
//                    ctx.commit();
//                }, TestUtils.defaultPerConfig(scope));
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//    @Test
//    public void oneDeleteCommittedAsync() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope))) {
//                final String docId = TestUtils.docId(collection, 0);
//                AtomicBoolean called = new AtomicBoolean(false);
//
//                collection.insert(docId, JsonObject.create().put("val", 1));
//
//                TransactionMock transactionMock = TestUtils.prepareTest(transactions);
//                transactionMock.afterAtrCommit = (ctx) -> {
//                    called.set(true);
//                    return collection.reactive().get(docId).flatMap(v -> {
//                        return DocumentGetter.justGetDoc(collection.reactive(), transactions.config(), docId,
//                            TestUtils.from(scope), cluster.environment().transcoder());
//                    }).doOnNext(doc -> {
//                        assertEquals("<<REMOVE>>", doc.get().links().stagedContent().get());
//                    }).thenReturn(0);
//                };
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.get(collection.reactive(), docId)
//                        .flatMap(doc -> ctx.remove(doc))
//                        .flatMap(ignore -> ctx.commit());
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//
//                assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//                assertTrue(called.get());
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//
//    @Test
//    public void deleteDocNormallyXattrs() {
//        String docId = TestUtils.docId(collection, 0);
//
//        collection.mutateIn(docId, Arrays.asList(MutateInSpec.upsert("foo", "bar").xattr()),
//            MutateInOptions.mutateInOptions().storeSemantics(StoreSemantics.INSERT));
//
//        LookupInResult result = collection.lookupIn(docId, Arrays.asList(LookupInSpec.get("foo").xattr()));
//
//        assertEquals(result.contentAs(0, String.class), "bar");
//
//        collection.remove(docId);
//
//        assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//
//        collection.insert(docId, JsonObject.create());
//
//        // This test indicates that a normal remove is sufficient to remove all xattrs from a doc
//        LookupInResult result2 = collection.lookupIn(docId, Arrays.asList(LookupInSpec.get("foo").xattr()));
//        assertFalse(result2.exists(0));
//    }
//
//    // TXNJ-24
//    @Test
//    public void tnj24() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//
//                String docId1 = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                String docId3 = TestUtils.docId(collection, 2);
//                String docId4 = TestUtils.docId(collection, 3);
//                String docId5 = TestUtils.docId(collection, 4);
//                collection.insert(docId1, JsonObject.create().put("val", 1));
//                collection.insert(docId2, JsonObject.create().put("val", 1));
//                collection.insert(docId3, JsonObject.create().put("val", 1));
//                collection.insert(docId4, JsonObject.create().put("val", 1));
//                collection.insert(docId5, JsonObject.create().put("val", 1));
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc1 = ctx.get(collection, docId1);
//                    TransactionGetResult doc2 = ctx.get(collection, docId2);
//                    TransactionGetResult doc3 = ctx.get(collection, docId3);
//                    TransactionGetResult doc4 = ctx.get(collection, docId4);
//                    TransactionGetResult doc5 = ctx.get(collection, docId5);
//                    ctx.remove(doc1);
//                    ctx.remove(doc2);
//                    ctx.remove(doc3);
//                    ctx.remove(doc4);
//                    ctx.remove(doc5);
//                    ctx.rollback();
//                }, TestUtils.defaultPerConfig(scope));
//            }
//        }
//    }
//
//    // TXNJ-33
//    @Test
//    public void createTwoTransactionsObjects() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            Transactions transactions1 = Transactions.create(cluster, TestUtils.defaultConfig(scope));
//            Transactions transactions2 = Transactions.create(cluster, TestUtils.defaultConfig(scope));
//            transactions1.close();
//            transactions2.close();
//        }
//    }
//
//    @Test
//    void getOrError() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                collection.insert(docId, JsonObject.create().put("val", 1));
//
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc1 = ctx.getOptional(collection, docId).get();
//                    long origCas = doc1.cas();
//                    JsonObject content = doc1.contentAs(JsonObject.class);
//                    content.put("val", 3);
//                    doc1 = ctx.replace(doc1, content);
//                    assertNotEquals(origCas, doc1.cas());
//                    assertNotEquals(origCas, 0);
//                    assertNotEquals(doc1.cas(), 0);
//                    ctx.commit();
//                }, TestUtils.defaultPerConfig(scope));
//            }
//        }
//    }
//
//    @Test
//    public void updateStagesBackupMetadata() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                LookupInResult preTxnResult = collection.lookupIn(docId,
//                    Arrays.asList(LookupInSpec.get("$document").xattr()));
//                JsonObject preTxn = preTxnResult.contentAsObject(0);
//
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//                    JsonObject content = doc.contentAs(JsonObject.class);
//                    content.put("val", 2);
//                    ctx.replace(doc, content);
//
//                    LookupInResult r = collection
//                        .lookupIn(docId, Arrays.asList(
//                            LookupInSpec.get(ATR_ID).xattr(),
//                            LookupInSpec.get(TRANSACTION_ID).xattr(),
//                            LookupInSpec.get(ATTEMPT_ID).xattr(),
//                            LookupInSpec.get(STAGED_DATA).xattr(),
//                            LookupInSpec.get(ATR_BUCKET_NAME).xattr(),
//                            LookupInSpec.get(ATR_COLL_NAME).xattr(),
//                            // For {BACKUP_FIELDS}
//                            LookupInSpec.get(TRANSACTION_RESTORE_PREFIX_ONLY).xattr(),
//                            LookupInSpec.get(TYPE).xattr(),
//                            LookupInSpec.get("$document").xattr(),
//                            LookupInSpec.get("")));
//
//                    LookupInResult r2 = collection
//                        .lookupIn(docId, Arrays.asList(
//                            LookupInSpec.get("txn.dummy").xattr(),
//                            LookupInSpec.get("txn").xattr(),
//                            LookupInSpec.get("$document").xattr(),
//                            LookupInSpec.get("")));
//
//
//                    collection
//                        .lookupIn(docId, Arrays.asList(
//                            LookupInSpec.get("txn.field1").xattr(),
//                            LookupInSpec.get("txn.field2").xattr(),
//                            LookupInSpec.get("$document").xattr(),
//                            LookupInSpec.get("")));
//
//                    assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//
//                    TransactionGetResult docRaw = DocumentGetter.justGetDoc(collection.reactive(),
//                        transactions.config(),
//                        docId, TestUtils.from(scope), cluster.environment().transcoder()).block().get();
//
//                    assertEquals(preTxn.getString("CAS"), docRaw.links().casPreTxn().get());
//                    assertEquals(preTxn.getString("revid"), docRaw.links().revidPreTxn().get());
//                    assertEquals(preTxn.getLong("exptime"), docRaw.links().exptimePreTxn().get());
//                    assertEquals("replace", docRaw.links().op().get());
//
//                }, TestUtils.defaultPerConfig(scope));
//            }
//        }
//    }
//
//    @Test
//    public void removeStagesBackupMetadata() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                LookupInResult preTxnResult = collection.lookupIn(docId,
//                    Arrays.asList(LookupInSpec.get("$document").xattr()));
//                JsonObject preTxn = preTxnResult.contentAsObject(0);
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//                    ctx.remove(doc);
//
//                    TransactionGetResult docRaw = DocumentGetter.justGetDoc(collection.reactive(),
//                        transactions.config(),
//                        docId, TestUtils.from(scope), cluster.environment().transcoder()).block().get();
//
//                    assertEquals(preTxn.getString("CAS"), docRaw.links().casPreTxn().get());
//                    assertEquals(preTxn.getString("revid"), docRaw.links().revidPreTxn().get());
//                    assertEquals(preTxn.getLong("exptime"), docRaw.links().exptimePreTxn().get());
//                    assertEquals("remove", docRaw.links().op().get());
//
//                }, TestUtils.defaultPerConfig(scope));
//            }
//        }
//    }
//
//    @Test
//    void preExistingStagedInsertFoundOneFailureTryingToRemove() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock1 = new TransactionMock();
//            transactionMock1.beforeAtrCommit = (ctx) -> {
//                return Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
//            };
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(transactionMock1), null, null))) {
//
//                // Create lost txn
//                try {
//                    transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                    });
//                    fail();
//                } catch (TransactionFailed err) {
//                }
//            }
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeRemovingDocDuringStagedInsert = (ctx, id) -> {
//                if (count.incrementAndGet() <= 1) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            // Fake that the lost txn has been cleaned up by removing the ATR entry
//            TestUtils.cleanupBefore(collection);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                });
//
//            }
//
//            assertEquals(2, count.get());
//        }
//    }
//
//    @Test
//    void preExistingStagedInsertFoundSeveralFailuresTryingToRemove() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock1 = new TransactionMock();
//            transactionMock1.beforeAtrCommit = (ctx) -> {
//                return Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
//            };
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(transactionMock1), null, null))) {
//
//                // Create lost txn
//                try {
//                    transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                    });
//                    fail();
//                } catch (TransactionFailed err) {
//                }
//            }
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeRemovingDocDuringStagedInsert = (ctx, id) -> {
//                if (count.incrementAndGet() <= 5) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            // Fake that the lost txn has been cleaned up by removing the ATR entry
//            TestUtils.cleanupBefore(collection);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                });
//
//            }
//
//            assertTrue(count.get() > 0);
//        }
//    }
//
//    @Test
//    void preExistingStagedInsertFoundOneFailureTryingToGet() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock1 = new TransactionMock();
//            transactionMock1.beforeAtrCommit = (ctx) -> {
//                return Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
//            };
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(transactionMock1), null, null))) {
//
//                // Create lost txn
//                try {
//                    transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                    });
//                    fail();
//                } catch (TransactionFailed err) {
//                }
//            }
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeGetDocInExistsDuringStagedInsert = (ctx, id) -> {
//                if (count.incrementAndGet() <= 1) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            // Fake that the lost txn has been cleaned up by removing the ATR entry
//            TestUtils.cleanupBefore(collection);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                });
//
//            }
//
//            assertTrue(count.get() > 0);
//        }
//    }
//
//    @Test
//    void preExistingStagedInsertFoundSeveralFailuresTryingToGet() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock1 = new TransactionMock();
//            transactionMock1.beforeAtrCommit = (ctx) -> {
//                return Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
//            };
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(transactionMock1), null, null))) {
//
//                // Create lost txn
//                try {
//                    transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                    });
//                    fail();
//                } catch (TransactionFailed err) {
//                }
//            }
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeGetDocInExistsDuringStagedInsert = (ctx, id) -> {
//                if (count.incrementAndGet() <= 5) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            // Fake that the lost txn has been cleaned up by removing the ATR entry
//            TestUtils.cleanupBefore(collection);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                });
//            }
//
//            assertTrue(count.get() > 0);
//        }
//    }
//
//    @Test
//    void checkATREntryForBlockingDocFailsOnce() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock1 = new TransactionMock();
//            transactionMock1.beforeAtrCommit = (ctx) -> {
//                return Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
//            };
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(transactionMock1), null, null))) {
//
//                // Create lost txn
//                try {
//                    transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                    });
//                    fail();
//                } catch (TransactionFailed err) {
//                }
//            }
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeCheckATREntryForBlockingDoc = (ctx, id) -> {
//                if (count.incrementAndGet() <= 1) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            // Fake that the lost txn has been cleaned up by removing the ATR entry
//            TestUtils.cleanupBefore(collection);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                });
//
//                assertTrue(1 == 1);
//            }
//
//            assertTrue(count.get() > 0);
//        }
//    }
//
//    @Test
//    void checkATREntryForBlockingDocFailsRepeatedly() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock1 = new TransactionMock();
//            transactionMock1.beforeAtrCommit = (ctx) -> {
//                return Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
//            };
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(transactionMock1), null, null))) {
//
//                // Create lost txn
//                try {
//                    transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                    });
//                    fail();
//                } catch (TransactionFailed err) {
//                }
//            }
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeCheckATREntryForBlockingDoc = (ctx, id) -> {
//                if (count.incrementAndGet() <= 5) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            // Fake that the lost txn has been cleaned up by removing the ATR entry
//            TestUtils.cleanupBefore(collection);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                });
//            }
//
//            assertTrue(count.get() > 0);
//        }
//    }
//
//
//    @Test
//    void getDocFailsRepeatedly() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeDocGet = (ctx, id) -> {
//                if (count.incrementAndGet() <= 5) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            collection.upsert(docId, JsonObject.create().put("val", "INITIAL"));
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    ctx.getOptional(collection, docId);
//                });
//            }
//
//            assertTrue(count.get() > 0);
//        }
//    }
//
//    @Test
//    public void multipleInsertsExpire() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    for (int i = 0; i < 10; i++) {
//                        String docId = TestUtils.docId(collection, i);
//                        JsonObject initial = JsonObject.create().put("val", 1);
//                        ctx.insert(collection, docId, initial);
//                    }
//
//                    ctx.commit();
//                });
//            }
//        }
//    }
//
//    @Test
//    public void doubleCommit() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                try {
//                    TransactionResult result = transactions.run((ctx) -> {
//                        TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//                        JsonObject content = doc.contentAs(JsonObject.class);
//                        content.put("val", 2);
//                        ctx.replace(doc, content);
//
//                        ctx.commit();
//                        ctx.commit();
//                        fail();
//                    }, TestUtils.defaultPerConfig(scope));
//
//                } catch (TransactionFailed e) {
//                    // The transaction will actually have committed, but the application has a logic bug that it needs
//                    // to fix.  Throwing seems the best course of action.
//                    assertTrue(e.getCause() instanceof AttemptException);
//                }
//
//                assertEquals(2, (int) collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                TestUtils.assertOneAtrEntry(collection, AttemptStates.COMPLETED, transactions.config(), span);
//            }
//        }
//    }
//
//    @Test
//    public void doubleRollback() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                try {
//                    TransactionResult result = transactions.run((ctx) -> {
//                        TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//                        JsonObject content = doc.contentAs(JsonObject.class);
//                        content.put("val", 2);
//                        ctx.replace(doc, content);
//
//                        ctx.rollback();
//                        ctx.rollback();
//                        fail();
//                    }, TestUtils.defaultPerConfig(scope));
//
//                } catch (TransactionFailed e) {
//                    assertTrue(e.getCause() instanceof AttemptException);
//                }
//
//                assertEquals(1, (int) collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                TestUtils.assertOneAtrEntry(collection, AttemptStates.ROLLED_BACK, transactions.config(), span);
//            }
//        }
//    }
//
//    @Test
//    public void casParsing() {
//        assertEquals(1539336197457L, ActiveTransactionRecord.parseMutationCAS("0x000058a71dd25c15"));
//    }
}