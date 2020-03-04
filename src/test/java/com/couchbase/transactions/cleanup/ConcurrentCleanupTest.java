// TODO port these
///*
// * Copyright (c) 2018 Couchbase, Inc.
// *
// * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
// * which may be found at https://www.couchbase.com/ESLA-11132015.  All rights reserved.
// */
//
//package com.couchbase.transactions.cleanup;
//
//import com.couchbase.client.core.cnc.Event;
//import com.couchbase.client.core.error.DocumentNotFoundException;
//import com.couchbase.client.java.Bucket;
//import com.couchbase.client.java.Cluster;
//import com.couchbase.client.java.Collection;
//import com.couchbase.client.java.json.JsonObject;
//import com.couchbase.transactions.*;
//import com.couchbase.transactions.components.DocumentGetter;
//import com.couchbase.transactions.config.TransactionConfig;
//import com.couchbase.transactions.config.TransactionConfigBuilder;
//import com.couchbase.transactions.error.TransactionFailed;
//import com.couchbase.transactions.error.internal.AbortedAsRequestedNoRollbackNoCleanup;
//import com.couchbase.transactions.log.SimpleEventBusLogger;
//import com.couchbase.transactions.tracing.TracingUtils;
//import com.couchbase.transactions.tracing.TracingWrapper;
//import com.couchbase.transactions.util.TransactionMock;
//import io.opentracing.Scope;
//import io.opentracing.Span;
//import io.opentracing.Tracer;
//import org.junit.jupiter.api.*;
//import reactor.core.publisher.Hooks;
//import reactor.core.publisher.Mono;
//import com.couchbase.transactions.TestUtils;
//
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.atomic.AtomicReference;
//
//import static org.junit.Assert.*;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
///**
// * Tests related to a particular transaction being cleaned up simultaneously by multiple clients.
// *
// * @author Graham Pople
// */
//public class ConcurrentCleanupTest {
//    public static int NUM_ITERATIONS = 1; // Will be changed to higher if on Jenkins
//    static Cluster cluster;
//    static Collection collection;
//    static Bucket bucket;
//    private static Tracer tracer;
//    private static TracingWrapper tracing;
//    private static Span span;
//    private static SimpleEventBusLogger LOGGER;
//    private static AtomicInteger droppedErrors = new AtomicInteger(0);
//
//    @BeforeAll
//    public static void beforeOnce() {
//        if(TestUtils.isOnCI()) {
//            NUM_ITERATIONS = 100;
//        }
//
//        tracing = TracingUtils.getTracer();
//        tracer = tracing.tracer();
//        span = tracer.buildSpan("concurrentcleanup").ignoreActiveSpan().start();
//
//        cluster = TestUtils.getCluster();
//        bucket = cluster.bucket("default");
//        collection = bucket.defaultCollection();
//        LOGGER = new SimpleEventBusLogger(cluster.environment().eventBus());
//
//        Hooks.onErrorDropped(v -> {
//            LOGGER.info("onError: " + v.getMessage());
//            droppedErrors.incrementAndGet();
//        });
//    }
//
//    @BeforeEach
//    public void beforeEach() {
//        TestUtils.cleanupBefore(collection);
//    }
//
//    @AfterEach
//    public void afterEach() {
//        assertEquals(0, droppedErrors.get());
//        droppedErrors.set(0);
//    }
//
//    @AfterAll
//    public static void afterOnce() {
//        span.finish();
//        tracing.close();
//        cluster.disconnect();
//    }
//
//
//
//    @Test
//    public void twoReplacesTxnFullyCompleted() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial);
//
//                AtomicInteger attempt = new AtomicInteger(0);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    LOGGER.info("Doing a basic txn that will complete fully, but not be cleaned up, creating a lost " +
//                            "txn");
//
//                    TransactionResult result = null;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            TransactionGetResult doc = ctx1.get(collection, docId);
//                            JsonObject content = doc.contentAs(JsonObject.class).put("val", "TXN");
//                            ctx1.replace(doc, content);
//
//                            TransactionGetResult doc2 = ctx1.get(collection, docId2);
//                            JsonObject content2 = doc2.contentAs(JsonObject.class).put("val", "TXN");
//                            ctx1.replace(doc2, content2);
//
//                            ctx1.commit();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                    } catch (TransactionFailed e) {
//                        fail("Txn should not fail");
//                    }
//
//                    LOGGER.info("Both docs should be committed (replaced)");
//
//                    // This will also confirm that the ATR entry is cleaned up and that no cleanup process fails
//                    cleanupInTwoThreads(result);
//
//                    assertEquals("TXN", collection.get(docId).contentAs(JsonObject.class).getString("val"));
//                    assertEquals("TXN", collection.get(docId2).contentAs(JsonObject.class).getString("val"));
//                    assertFalse(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertFalse(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                }
//            }
//        }
//    }
//
//    @Test
//    public void twoRemovesTxnFullyCompleted() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial);
//
//                AtomicInteger attempt = new AtomicInteger(0);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    LOGGER.info("Doing a basic txn that will complete fully, but not be cleaned up, creating a lost " +
//                            "txn");
//
//                    TransactionResult result = null;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            TransactionGetResult doc = ctx1.get(collection, docId);
//                            ctx1.remove(doc);
//
//                            TransactionGetResult doc2 = ctx1.get(collection, docId2);
//                            ctx1.remove(doc2);
//
//                            ctx1.commit();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                    } catch (TransactionFailed e) {
//                        fail("Txn should not fail");
//                    }
//
//                    LOGGER.info("Both docs should be committed (removed)");
//
//                    // This will also confirm that the ATR entry is cleaned up and that no cleanup process fails
//                    cleanupInTwoThreads(result);
//
//                    assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//                    assertThrows(DocumentNotFoundException.class, () -> collection.get(docId2));
//                }
//            }
//        }
//    }
//
//    @Test
//    public void twoInsertsTxnFullyCompleted() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//
//                AtomicInteger attempt = new AtomicInteger(0);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    LOGGER.info("Doing a basic txn that will complete fully, but not be cleaned up, creating a lost " +
//                            "txn");
//
//                    TransactionResult result = null;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            ctx1.insert(collection, docId, initial);
//                            ctx1.insert(collection, docId2, initial);
//
//                            ctx1.commit();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                    } catch (TransactionFailed e) {
//                        fail("Txn should not fail");
//                    }
//
//                    LOGGER.info("Both docs should be committed (removed)");
//
//                    // This will also confirm that the ATR entry is cleaned up and that no cleanup process fails
//                    cleanupInTwoThreads(result);
//
//                    assertEquals(initial, collection.get(docId).contentAsObject());
//                    assertEquals(initial, collection.get(docId2).contentAsObject());
//                    assertFalse(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertFalse(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                }
//            }
//        }
//    }
//
//    @Test
//    public void twoReplacesTxnFullyRolledBack() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial);
//
//                AtomicInteger attempt = new AtomicInteger(0);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    LOGGER.info("Doing a basic txn that will rollback fully, but not be cleaned up, creating a lost " +
//                            "txn");
//
//                    TransactionResult result = null;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            TransactionGetResult doc = ctx1.get(collection, docId);
//                            JsonObject content = doc.contentAs(JsonObject.class).put("val", "TXN");
//                            ctx1.replace(doc, content);
//
//                            TransactionGetResult doc2 = ctx1.get(collection, docId2);
//                            JsonObject content2 = doc2.contentAs(JsonObject.class).put("val", "TXN");
//                            ctx1.replace(doc2, content2);
//
//                            ctx1.rollback();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                    } catch (TransactionFailed e) {
//                        fail("Txn should not fail");
//                    }
//
//                    LOGGER.info("Both docs should be rolledback (remain ORIGINAL)");
//
//                    assertEquals("ORIGINAL", collection.get(docId).contentAs(JsonObject.class).getString("val"));
//                    assertEquals("ORIGINAL", collection.get(docId2).contentAs(JsonObject.class).getString("val"));
//                    assertFalse(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertFalse(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//
//                    // This will also confirm that the ATR entry is cleaned up and that no cleanup process fails
//                    cleanupInTwoThreads(result);
//
//                    assertEquals("ORIGINAL", collection.get(docId).contentAs(JsonObject.class).getString("val"));
//                    assertEquals("ORIGINAL", collection.get(docId2).contentAs(JsonObject.class).getString("val"));
//                    assertFalse(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertFalse(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                }
//            }
//        }
//    }
//
//    @Test
//    public void twoRemovesTxnFullyRolledBack() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial);
//
//                AtomicInteger attempt = new AtomicInteger(0);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    LOGGER.info("Doing a basic txn that will rollback fully, but not be cleaned up, creating a lost " +
//                            "txn");
//
//                    TransactionResult result = null;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            TransactionGetResult doc = ctx1.get(collection, docId);
//                            ctx1.remove(doc);
//
//                            TransactionGetResult doc2 = ctx1.get(collection, docId2);
//                            ctx1.remove(doc2);
//
//                            ctx1.rollback();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                    } catch (TransactionFailed e) {
//                        fail("Txn should not fail");
//                    }
//
//                    LOGGER.info("Both docs should be rolled back (remain)");
//
//                    // This will also confirm that the ATR entry is cleaned up and that no cleanup process fails
//                    cleanupInTwoThreads(result);
//
//                    collection.get(docId);
//                    collection.get(docId2);
//                    assertFalse(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertFalse(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                }
//            }
//        }
//    }
//
//    @Test
//    public void twoInsertsTxnFullyRolledBack() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//
//                AtomicInteger attempt = new AtomicInteger(0);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    LOGGER.info("Doing a basic txn that will rollback fully, but not be cleaned up, creating a lost " +
//                            "txn");
//
//                    TransactionResult result = null;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            ctx1.insert(collection, docId, initial);
//                            ctx1.insert(collection, docId2, initial);
//
//                            ctx1.rollback();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                    } catch (TransactionFailed e) {
//                        fail("Txn should not fail");
//                    }
//
//                    LOGGER.info("Both docs should be rolled back (removed)");
//
//                    assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//                    assertThrows(DocumentNotFoundException.class, () -> collection.get(docId2));
//
//                    // This will also confirm that the ATR entry is cleaned up and that no cleanup process fails
//                    cleanupInTwoThreads(result);
//
//                    assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//                    assertThrows(DocumentNotFoundException.class, () -> collection.get(docId2));
//                }
//            }
//        }
//    }
//
//    @Test
//    public void twoReplacesAbortMidCommitAfterOne() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial);
//
//                AtomicInteger attempt = new AtomicInteger(0);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    TransactionMock transactionMock = TestUtils.prepareTest(transactions);
//
//                    transactionMock.afterDocCommitted = (ctx, x) -> {
//                        if (attempt.incrementAndGet() == 1) {
//                            throw new AbortedAsRequestedNoRollbackNoCleanup();
//                        } else {
//                            return Mono.just(1);
//                        }
//                    };
//
//                    LOGGER.info("Doing a basic txn that will fail during commit, creating a lost txn");
//
//                    TransactionResult result;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            TransactionGetResult doc = ctx1.get(collection, docId);
//                            JsonObject content = doc.contentAs(JsonObject.class).put("val", "TXN");
//                            ctx1.replace(doc, content);
//
//                            TransactionGetResult doc2 = ctx1.get(collection, docId2);
//                            JsonObject content2 = doc2.contentAs(JsonObject.class).put("val", "TXN");
//                            ctx1.replace(doc2, content2);
//
//                            ctx1.commit();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                        fail("Txn should not succeed");
//                    } catch (TransactionFailed e) {
//                        LOGGER.info("Txn failed, as expected");
//                        result = e.result();
//                    }
//
//                    final TransactionResult r = result;
//
//                    LOGGER.info("Doc 1 should be committed, doc 2 (a replace) should not");
//
//                    assertEquals("TXN", collection.get(docId).contentAs(JsonObject.class).getString("val"));
//                    assertEquals("ORIGINAL", collection.get(docId2).contentAs(JsonObject.class).getString("val"));
//                    assertFalse(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertTrue(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//
//                    cleanupInTwoThreads(r);
//
//                    assertEquals("TXN", collection.get(docId).contentAs(JsonObject.class).getString("val"));
//                    assertEquals("TXN", collection.get(docId2).contentAs(JsonObject.class).getString("val"));
//                    assertFalse(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertFalse(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                }
//            }
//        }
//    }
//
//
//    @Test
//    public void twoRemovesAbortMidCommitAfterOne() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial);
//
//                AtomicInteger attempt = new AtomicInteger(0);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    TransactionMock transactionMock = TestUtils.prepareTest(transactions);
//                    transactionMock.afterDocRemovedPostRetry = (ctx, x) -> {
//                        if (attempt.incrementAndGet() == 1) {
//                            throw new AbortedAsRequestedNoRollbackNoCleanup();
//                        } else {
//                            return Mono.just(1);
//                        }
//                    };
//
//                    LOGGER.info("Doing a basic txn that will fail during commit, creating a lost txn");
//
//                    TransactionResult result;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            TransactionGetResult doc = ctx1.get(collection, docId);
//                            ctx1.remove(doc);
//
//                            TransactionGetResult doc2 = ctx1.get(collection, docId2);
//                            ctx1.remove(doc2);
//
//                            ctx1.commit();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                        fail("Txn should not succeed");
//                    } catch (TransactionFailed e) {
//                        LOGGER.info("Txn failed, as expected");
//                        result = e.result();
//                    }
//
//                    final TransactionResult r = result;
//
//                    LOGGER.info("Doc 1 should be committed (removed), doc 2 (a remove) should not");
//
//                    assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//                    collection.get(docId2);
//
//                    cleanupInTwoThreads(r);
//
//                    assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//                    assertThrows(DocumentNotFoundException.class, () -> collection.get(docId2));
//                }
//            }
//        }
//    }
//
//    @Test
//    public void twoInsertsAbortMidCommitAfterOne() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//
//                AtomicInteger attempt = new AtomicInteger(0);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    TransactionMock transactionMock = TestUtils.prepareTest(transactions);
//
//                    transactionMock.afterDocCommitted = (ctx, x) -> {
//                        if (attempt.incrementAndGet() == 1) {
//                            throw new AbortedAsRequestedNoRollbackNoCleanup();
//                        } else {
//                            return Mono.just(1);
//                        }
//                    };
//
//                    LOGGER.info("Doing a basic txn that will fail during commit, creating a lost txn");
//
//                    TransactionResult result;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            ctx1.insert(collection, docId, initial);
//                            ctx1.insert(collection, docId2, initial);
//                            ctx1.commit();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                        fail("Txn should not succeed");
//                    } catch (TransactionFailed e) {
//                        LOGGER.info("Txn failed, as expected");
//                        result = e.result();
//                    }
//
//                    final TransactionResult r = result;
//
//                    LOGGER.info("Doc 1 should be committed (inserted), doc 2 (an insert) should not");
//
//                    assertEquals(initial, collection.get(docId).contentAsObject());
//                    assertEquals(0, collection.get(docId2).contentAsObject().getNames().size());
//
//                    cleanupInTwoThreads(r);
//
//                    assertEquals(initial, collection.get(docId).contentAsObject());
//                    assertEquals(initial, collection.get(docId2).contentAsObject());
//                    assertFalse(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertFalse(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                }
//            }
//        }
//    }
//
//    @Test
//    public void twoReplacesAbortMidRollbackAfterOne() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial);
//
//                AtomicInteger attempt = new AtomicInteger(0);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    TransactionMock transactionMock = TestUtils.prepareTest(transactions);
//
//
//                    transactionMock.afterRollbackReplaceOrRemove = (ctx, x) -> {
//                        if (attempt.incrementAndGet() == 1) {
//                            throw new AbortedAsRequestedNoRollbackNoCleanup();
//                        } else {
//                            return Mono.just(1);
//                        }
//                    };
//
//                    LOGGER.info("Doing a basic txn that will fail during rollback, creating a lost txn");
//
//                    TransactionResult result;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            TransactionGetResult doc = ctx1.get(collection, docId);
//                            JsonObject content = doc.contentAs(JsonObject.class).put("val", "TXN");
//                            ctx1.replace(doc, content);
//
//                            TransactionGetResult doc2 = ctx1.get(collection, docId2);
//                            JsonObject content2 = doc2.contentAs(JsonObject.class).put("val", "TXN");
//                            ctx1.replace(doc2, content2);
//
//                            ctx1.rollback();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                        fail("Txn should not succeed");
//                    } catch (TransactionFailed e) {
//                        LOGGER.info("Txn failed, as expected");
//                        result = e.result();
//                    }
//
//                    final TransactionResult r = result;
//
//                    LOGGER.info("Doc 1 should be rolledback, doc 2 (a replace) should not");
//
//                    assertEquals("ORIGINAL", collection.get(docId).contentAs(JsonObject.class).getString("val"));
//                    assertEquals("ORIGINAL", collection.get(docId2).contentAs(JsonObject.class).getString("val"));
//                    assertFalse(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertTrue(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//
//                    cleanupInTwoThreads(r);
//
//                    assertEquals("ORIGINAL", collection.get(docId).contentAs(JsonObject.class).getString("val"));
//                    assertEquals("ORIGINAL", collection.get(docId2).contentAs(JsonObject.class).getString("val"));
//                    assertFalse(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertFalse(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                }
//            }
//        }
//    }
//
//
//    @Test
//    public void twoRemovesAbortMidRollbackAfterOne() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial);
//
//                AtomicInteger attempt = new AtomicInteger(0);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    TransactionMock transactionMock = TestUtils.prepareTest(transactions);
//
//                    transactionMock.afterRollbackReplaceOrRemove = (ctx, x) -> {
//                        if (attempt.incrementAndGet() == 1) {
//                            throw new AbortedAsRequestedNoRollbackNoCleanup();
//                        } else {
//                            return Mono.just(1);
//                        }
//                    };
//
//                    LOGGER.info("Doing a basic txn that will fail during rollback, creating a lost txn");
//
//                    TransactionResult result;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            TransactionGetResult doc = ctx1.get(collection, docId);
//                            ctx1.remove(doc);
//
//                            TransactionGetResult doc2 = ctx1.get(collection, docId2);
//                            ctx1.remove(doc2);
//
//                            ctx1.rollback();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                        fail("Txn should not succeed");
//                    } catch (TransactionFailed e) {
//                        LOGGER.info("Txn failed, as expected");
//                        result = e.result();
//                    }
//
//                    final TransactionResult r = result;
//
//                    LOGGER.info("Doc 1 should be rolledback (remains), doc 2 (a remove) should not");
//
//                    collection.get(docId);
//                    collection.get(docId2);
//                    assertFalse(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertTrue(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//
//                    cleanupInTwoThreads(r);
//
//                    collection.get(docId);
//                    collection.get(docId2);
//                    assertFalse(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertFalse(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                }
//            }
//        }
//    }
//
//    @Test
//    public void twoInsertsAbortMidRollbackAfterOne() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//
//                AtomicInteger attempt = new AtomicInteger(0);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    TransactionMock transactionMock = TestUtils.prepareTest(transactions);
//
//                    transactionMock.afterRollbackDeleteInserted = (ctx, x) -> {
//                        if (attempt.incrementAndGet() == 1) {
//                            throw new AbortedAsRequestedNoRollbackNoCleanup();
//                        } else {
//                            return Mono.just(1);
//                        }
//                    };
//
//                    LOGGER.info("Doing a basic txn that will fail during rollback, creating a lost txn");
//
//                    TransactionResult result;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            ctx1.insert(collection, docId, initial);
//                            ctx1.insert(collection, docId2, initial);
//                            ctx1.rollback();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                        fail("Txn should not succeed");
//                    } catch (TransactionFailed e) {
//                        LOGGER.info("Txn failed, as expected");
//                        result = e.result();
//                    }
//
//                    final TransactionResult r = result;
//
//                    LOGGER.info("Doc 1 should be rolledback (removed), doc 2 (an insert) should not");
//
//                    assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//                    collection.get(docId2);
//
//                    cleanupInTwoThreads(r);
//
//                    assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//                    assertThrows(DocumentNotFoundException.class, () -> collection.get(docId2));
//                }
//            }
//        }
//    }
//
//    @Test
//    public void twoReplacesAbortInPending() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    LOGGER.info("Doing a basic txn that will fail before commit, creating a lost txn");
//
//                    TransactionResult result;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            TransactionGetResult doc = ctx1.get(collection, docId);
//                            JsonObject content = doc.contentAs(JsonObject.class).put("val", "TXN");
//                            ctx1.replace(doc, content);
//
//                            TransactionGetResult doc2 = ctx1.get(collection, docId2);
//                            JsonObject content2 = doc2.contentAs(JsonObject.class).put("val", "TXN");
//                            ctx1.replace(doc2, content2);
//
//                            throw new AbortedAsRequestedNoRollbackNoCleanup();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                        fail("Txn should not succeed");
//                    } catch (TransactionFailed e) {
//                        LOGGER.info("Txn failed, as expected");
//                        result = e.result();
//                    }
//
//                    final TransactionResult r = result;
//
//                    LOGGER.info("Both docs are pending replace");
//
//                    assertEquals("ORIGINAL", collection.get(docId).contentAs(JsonObject.class).getString("val"));
//                    assertEquals("ORIGINAL", collection.get(docId2).contentAs(JsonObject.class).getString("val"));
//                    assertTrue(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertTrue(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//
//                    cleanupInTwoThreads(r);
//
//                    // Cleanup does not handle pending state, though the ATR should be removed.  See Cleanup.md for
//                    // discussion.
//                    assertEquals("ORIGINAL", collection.get(docId).contentAs(JsonObject.class).getString("val"));
//                    assertEquals("ORIGINAL", collection.get(docId2).contentAs(JsonObject.class).getString("val"));
//                    assertTrue(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertTrue(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                }
//            }
//        }
//    }
//
//
//    public void twoRemovesAbortInPending() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//                collection.insert(docId, initial);
//                collection.insert(docId2, initial);
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    LOGGER.info("Doing a basic txn that will fail before commit, creating a lost txn");
//
//                    TransactionResult result;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            TransactionGetResult doc = ctx1.get(collection, docId);
//                            ctx1.remove(doc);
//
//                            TransactionGetResult doc2 = ctx1.get(collection, docId2);
//                            ctx1.remove(doc2);
//
//                            throw new AbortedAsRequestedNoRollbackNoCleanup();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                        fail("Txn should not succeed");
//                    } catch (TransactionFailed e) {
//                        LOGGER.info("Txn failed, as expected");
//                        result = e.result();
//                    }
//
//                    final TransactionResult r = result;
//
//                    LOGGER.info("Both docs are pending replace");
//
//                    assertEquals("ORIGINAL", collection.get(docId).contentAs(JsonObject.class).getString("val"));
//                    assertEquals("ORIGINAL", collection.get(docId2).contentAs(JsonObject.class).getString("val"));
//                    assertTrue(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertTrue(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//
//                    cleanupInTwoThreads(r);
//
//                    // Cleanup does not handle pending state, though the ATR should be removed.  See Cleanup.md for
//                    // discussion.
//                    assertEquals("ORIGINAL", collection.get(docId).contentAs(JsonObject.class).getString("val"));
//                    assertEquals("ORIGINAL", collection.get(docId2).contentAs(JsonObject.class).getString("val"));
//                    assertTrue(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertTrue(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                }
//            }
//        }
//    }
//
//    public void twoInsertsAbortInPending() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            for (int i = 0; i < NUM_ITERATIONS; i++) {
//                LOGGER.info(String.format("Starting test iteration %d.  Test involves checking for races between " +
//                        "threads so is run multiple times.", i));
//
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
//
//                try (Transactions transactions = Transactions.create(cluster,
//                        TestUtils.defaultConfig(scope)
//                                // TXNJ-66 - turn on tracing when needed
//                                .logDirectly(Event.Severity.WARN)
//                                .cleanupLostAttempts(false)
//                                .cleanupClientAttempts(false))) {
//
//                    LOGGER.info("Doing a basic txn that will fail before commit, creating a lost txn");
//
//                    TransactionResult result;
//                    try {
//                        result = transactions.run((ctx1) -> {
//                            ctx1.insert(collection, docId, initial);
//                            ctx1.insert(collection, docId2, initial);
//
//                            throw new AbortedAsRequestedNoRollbackNoCleanup();
//                        }, TestUtils.defaultPerConfig(scope));
//
//                        fail("Txn should not succeed");
//                    } catch (TransactionFailed e) {
//                        LOGGER.info("Txn failed, as expected");
//                        result = e.result();
//                    }
//
//                    final TransactionResult r = result;
//
//                    LOGGER.info("Both docs are pending replace");
//
//                    assertEquals(0, collection.get(docId).contentAs(JsonObject.class).size());
//                    assertEquals(0, collection.get(docId2).contentAs(JsonObject.class).size());
//                    assertTrue(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertTrue(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//
//                    cleanupInTwoThreads(r);
//
//                    // Cleanup does not handle pending state, though the ATR should be removed.  See Cleanup.md for
//                    // discussion.
//                    assertEquals(0, collection.get(docId).contentAs(JsonObject.class).size());
//                    assertEquals(0, collection.get(docId2).contentAs(JsonObject.class).size());
//                    assertTrue(DocumentGetter.get(collection,  docId,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                    assertTrue(DocumentGetter.get(collection,  docId2,  transactions.config(), TestUtils.from(span), cluster.environment().transcoder()).get().links().isDocumentInTransaction());
//                }
//            }
//        }
//    }
//
//    private void cleanupInTwoThreads(TransactionResult r) throws InterruptedException {
//        TransactionConfig config = TransactionConfigBuilder.create()
//                .cleanupClientAttempts(false)
//                .cleanupLostAttempts(false)
//                // For testing against localhost
//                .durabilityLevel(TransactionDurabilityLevel.NONE)
//                .build();
//        ClusterData cd = new ClusterData(cluster);
//        TransactionsCleanup x1 = new TransactionsCleanup(config, cd);
//        TransactionsCleanup x2 = new TransactionsCleanup(config, cd);
//
//        final String atrId = r.attempts().get(0).atrId().get();
//        AtomicReference<RuntimeException> err1 = new AtomicReference<>(null);
//        AtomicReference<RuntimeException> err2 = new AtomicReference<>(null);
//
//        LOGGER.info("Cleaning up the same txn in two threads, concurrently");
//
//        Thread t1 = new Thread(() -> {
//            LOGGER.info(String.format("Thread 1 cleaning up ATR %s", atrId));
//
//            try {
//                x1.forceATRCleanup(collection.reactive(), atrId).block();
//
//                LOGGER.info(String.format("Thread 1 finished cleaning up ATR %s", atrId));
//            } catch (RuntimeException e) {
//                err1.set(e);
//            }
//
//        });
//
//        Thread t2 = new Thread(() -> {
//            LOGGER.info(String.format("Thread 2 cleaning up ATR %s", atrId));
//
//            try {
//                x2.forceATRCleanup(collection.reactive(), atrId).block();
//
//                LOGGER.info(String.format("Thread 2 finished cleaning up ATR %s", atrId));
//            } catch (RuntimeException e) {
//                err2.set(e);
//            }
//        });
//
//        t1.start();
//        t2.start();
//        t1.join();
//        t2.join();
//
//        if (err1.get() != null) fail(String.format("Cleanup thread 1 failed with err %s", err1.get()));
//        if (err2.get() != null) fail(String.format("Cleanup thread 2 failed with err %s", err2.get()));
//
//        LOGGER.info("Txn should be fully cleaned up");
//
//        assertEquals(0l, TestUtils.allAtrEntries(collection, config, null).count().block().longValue());
//    }
//
//
//}