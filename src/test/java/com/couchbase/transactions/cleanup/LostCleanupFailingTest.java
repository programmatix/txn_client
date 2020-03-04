// TODO port these
//package com.couchbase.transactions.cleanup;
//
//import com.couchbase.client.core.cnc.Event;
//import com.couchbase.client.core.error.TemporaryFailureException;
//import com.couchbase.client.java.Bucket;
//import com.couchbase.client.java.Cluster;
//import com.couchbase.client.java.Collection;
//import com.couchbase.client.java.json.JsonObject;
//import com.couchbase.transactions.TestUtils;
//import com.couchbase.transactions.TransactionResult;
//import com.couchbase.transactions.Transactions;
//import com.couchbase.transactions.atr.ATRIds;
//import com.couchbase.transactions.components.ATREntry;
//import com.couchbase.transactions.config.TransactionConfig;
//import com.couchbase.transactions.error.TransactionFailed;
//import com.couchbase.transactions.error.internal.AbortedAsRequestedNoRollback;
//import com.couchbase.transactions.log.SimpleEventBusLogger;
//import com.couchbase.transactions.losttxns.FailTransactionAt;
//import com.couchbase.transactions.losttxns.Insert;
//import com.couchbase.transactions.losttxns.LostTxnCreator;
//import com.couchbase.transactions.support.AttemptStates;
//import com.couchbase.transactions.tracing.TracingUtils;
//import com.couchbase.transactions.tracing.TracingWrapper;
//import com.couchbase.transactions.util.LostTxnEventSaver;
//import com.couchbase.transactions.util.TestAttemptContextFactory;
//import com.couchbase.transactions.util.TransactionMock;
//import io.opentracing.Scope;
//import io.opentracing.Span;
//import io.opentracing.Tracer;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import reactor.core.publisher.Hooks;
//import reactor.core.publisher.Mono;
//
//import java.time.Duration;
//import java.time.temporal.ChronoUnit;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.awaitility.Awaitility.await;
//import static org.junit.Assert.*;
//
///**
// * Tests related to LostCleanupDistributed, where the cleanup fails.
// *
// * @author Graham Pople
// */
//public class LostCleanupFailingTest {
//    static Cluster cluster;
//    static Bucket bucket;
//    static Collection collection;
//    private static SimpleEventBusLogger LOGGER;
//    private static Tracer tracer;
//    private static TracingWrapper tracing;
//    private static Span span;
//    private static LostTxnEventSaver lostCleanupEvents = new LostTxnEventSaver();
//
//    @BeforeAll
//    public static void beforeOnce() {
//        tracing = TracingUtils.getTracer();
//        tracer = tracing.tracer();
//        span = tracer.buildSpan("losttxnscleanup").ignoreActiveSpan().start();
//        cluster = TestUtils.getCluster();
//        bucket = cluster.bucket("default");
//        collection = bucket.defaultCollection();
//        cluster.environment().eventBus().subscribe(ev -> lostCleanupEvents.publish(ev));
//        LOGGER = new SimpleEventBusLogger(cluster.environment().eventBus());
//    }
//
//    @BeforeEach
//    public void beforeEach() {
//        TestUtils.cleanupBefore(collection);
//        lostCleanupEvents.clear();
//    }
//
//    @AfterAll
//    public static void afterOnce() {
//        span.finish();
//        tracing.close();
//        cluster.disconnect();
//    }
//
//    @Test
//    public void atrGetFailsOnce() throws InterruptedException {
//        AtomicInteger droppedErrors = new AtomicInteger(0);
//        Hooks.onErrorDropped(v -> {
//            LOGGER.info("onError: " + v.getMessage());
//            droppedErrors.incrementAndGet();
//        });
//
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//
//            LOGGER.info("Creating lost txn, cleanup is disabled");
//            String atrId;
//
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.concurrentTestConfig()
//                    .expirationTime(Duration.ofMillis(200))
//                    .cleanupClientAttempts(false)
//                    .cleanupLostAttempts(false)
//            )) {
//                LOGGER.info("Creating lost txn");
//                Insert insert1 = new Insert();
//                LostTxnCreator.create(FailTransactionAt.BeforeCommitOrRollbackCommand1,
//                        Arrays.asList(insert1), cluster, collection, transactions.config(), span);
//
//              List<ATREntry> entries =
//                      TestUtils.allAtrEntries(collection, transactions.config(), span).collectList().block();
//
//              assertEquals(1, entries.size());
//              atrId = entries.get(0).atrId();
//            }
//
//
//          CleanerMockCreatorFactory cleanerFactory = new CleanerMockCreatorFactory();
//            AtomicBoolean first = new AtomicBoolean(true);
//            cleanerFactory.beforeAtrGet = (aid) -> {
//                if (aid.equals(atrId) && first.compareAndSet(true, false)) {
//                    LOGGER.info("Faking that ATR get fails");
//                    return Mono.error(new TemporaryFailureException(null));
//                }
//                else {
//                    return Mono.just(1);
//                }
//            };
//            runCleanupOfLostTxns(cleanerFactory, 0, null);
//
//            assertEquals(1, lostCleanupEvents.events().size());
//            lostCleanupEvents.events().stream().forEach(a -> assertTrue(a.success()));
//            assertEquals(0, droppedErrors.get());
//        }
//    }
//
//    @Test
//    public void atrGetFailsSeveralTimes() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//
//            LOGGER.info("Creating lost txn, cleanup is disabled");
//            String atrId;
//
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.concurrentTestConfig()
//                    .expirationTime(Duration.ofMillis(200))
//                    .cleanupClientAttempts(false)
//                    .cleanupLostAttempts(false)
//            )) {
//                LOGGER.info("Creating lost txn");
//                Insert insert1 = new Insert();
//                LostTxnCreator.create(FailTransactionAt.BeforeCommitOrRollbackCommand1,
//                        Arrays.asList(insert1), cluster, collection, transactions.config(), span);
//
//                List<ATREntry> entries =
//                        TestUtils.allAtrEntries(collection, transactions.config(), span).collectList().block();
//
//                assertEquals(1, entries.size());
//                atrId = entries.get(0).atrId();
//            }
//
//
//            CleanerMockCreatorFactory cleanerFactory = new CleanerMockCreatorFactory();
//            AtomicInteger count = new AtomicInteger(0);
//            cleanerFactory.beforeAtrGet = (aid) -> {
//                if (count.getAndIncrement() < 5) {
//                    LOGGER.info("Faking that ATR get fails");
//                    return Mono.error(new TemporaryFailureException(null));
//                }
//                else {
//                    return Mono.just(1);
//                }
//            };
//            runCleanupOfLostTxns(cleanerFactory, 0, null);
//
//            assertEquals(1, lostCleanupEvents.events().size());
//            lostCleanupEvents.events().stream().forEach(a -> assertTrue(a.success()));
//        }
//    }
//
//    @Test
//    public void twoLostTxns() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//
//            LOGGER.info("Creating lost txns, cleanup is disabled");
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.concurrentTestConfig()
//                    .expirationTime(Duration.ofMillis(200))
//                    .cleanupClientAttempts(false)
//                    .cleanupLostAttempts(false)
//            )) {
//                LOGGER.info("Creating lost txn 1");
//                Insert insert1 = new Insert();
//                LostTxnCreator.create(FailTransactionAt.BeforeCommitOrRollbackCommand1,
//                        Arrays.asList(insert1), cluster, collection, transactions.config(), span);
//
//                LOGGER.info("Creating lost txn 2");
//                Insert insert2 = new Insert();
//                LostTxnCreator.create(FailTransactionAt.BeforeCommitOrRollbackCommand1,
//                        Arrays.asList(insert2), cluster, collection, transactions.config(), span);
//
//                List<ATREntry> entries =
//                        TestUtils.allAtrEntries(collection, transactions.config(), span).collectList().block();
//
//                assertEquals(2, entries.size());
//            }
//
//            CleanerMockCreatorFactory cleanerFactory = new CleanerMockCreatorFactory();
//            runCleanupOfLostTxns(cleanerFactory, 0, null);
//
//            assertEquals(2, lostCleanupEvents.events().size());
//            lostCleanupEvents.events().stream().forEach(a -> assertTrue(a.success()));
//        }
//    }
//
//    @Test
//    public void twoLostTxnsSameATRIfCleanupOfFirstFailsContinuouslySecondShouldStillBeCleanedUp() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//
//            Insert insert1 = new Insert();
//
//            LOGGER.info("Creating lost txns, cleanup is disabled");
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.concurrentTestConfig()
//                    .expirationTime(Duration.ofMillis(200))
//                    .cleanupClientAttempts(false)
//                    .cleanupLostAttempts(false))) {
//                LOGGER.info("Creating lost txn 1");
//                LostTxnCreator.create(FailTransactionAt.BeforeCommitOrRollbackCommand1,
//                        Arrays.asList(insert1), cluster, collection, transactions.config(), span);
//
//                LOGGER.info("Creating lost txn 2");
//                Insert insert2 = new Insert();
//                LostTxnCreator.create(FailTransactionAt.BeforeCommitOrRollbackCommand1,
//                        Arrays.asList(insert2), cluster, collection, transactions.config(), span);
//
//                List<ATREntry> entries =
//                        TestUtils.allAtrEntries(collection, transactions.config(), span).collectList().block();
//
//                assertEquals(2, entries.size());
//            }
//
//            CleanerMockCreatorFactory cleanerFactory = new CleanerMockCreatorFactory();
//
//            CountDownLatch called = new CountDownLatch(1);
//            cleanerFactory.beforeCommitDoc = (id) -> {
//                if (id.equals(insert1.docId)) {
//                    LOGGER.info("Repeatedly failing lost txn 1");
//                    called.countDown();
//                    return Mono.error(new RuntimeException());
//                }
//                else {
//                    return Mono.just(1);
//                }
//            };
//
//            runCleanupOfLostTxns(cleanerFactory, 1, called);
//
//            await().atMost(5, TimeUnit.SECONDS).until(() -> {
//                return lostCleanupEvents.events().stream().filter(it -> it.success()).count() == 1;
//            });
//        }
//    }
//
//    @Test
//    public void twoLostTxnsDifferentATRIfCleanupOfFirstFailsContinuouslySecondShouldStillBeCleanedUp() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//
//            Insert insert1 = new Insert();
//            Insert insert2 = new Insert();
//            insert1.docId = TestUtils.docId(collection, 0);
//            insert2.docId = TestUtils.docId(collection, 1);
//            // Make sure ATR entries are on different ATRs
//            assertNotEquals(ATRIds.vbucketForKey(insert1.docId), ATRIds.vbucketForKey(insert2.docId));
//
//            LOGGER.info("Creating lost txns, cleanup is disabled");
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.concurrentTestConfig()
//                    .expirationTime(Duration.ofMillis(200))
//                    .cleanupClientAttempts(false)
//                    .cleanupLostAttempts(false))) {
//                LOGGER.info("Creating lost txn 1");
//                LostTxnCreator.create(FailTransactionAt.BeforeCommitOrRollbackCommand1,
//                        Arrays.asList(insert1), cluster, collection, transactions.config(), span);
//
//                LOGGER.info("Creating lost txn 2");
//                LostTxnCreator.create(FailTransactionAt.BeforeCommitOrRollbackCommand1,
//                        Arrays.asList(insert2), cluster, collection, transactions.config(), span);
//
//                List<ATREntry> entries =
//                        TestUtils.allAtrEntries(collection, transactions.config(), span).collectList().block();
//
//                assertEquals(2, entries.size());
//                assertNotEquals(entries.get(0).atrId(), entries.get(1).atrId());
//            }
//
//            CleanerMockCreatorFactory cleanerFactory = new CleanerMockCreatorFactory();
//
//            CountDownLatch called = new CountDownLatch(1);
//            cleanerFactory.beforeCommitDoc = (id) -> {
//                if (id.equals(insert1.docId)) {
//                    LOGGER.info("Repeatedly failing lost txn 1");
//                    called.countDown();
//                    return Mono.error(new RuntimeException());
//                }
//                else {
//                    return Mono.just(1);
//                }
//            };
//
//            runCleanupOfLostTxns(cleanerFactory, 1, called);
//
//            await().atMost(5, TimeUnit.SECONDS).until(() -> {
//                return lostCleanupEvents.events().stream().filter(it -> it.success()).count() == 1;
//            });
//        }
//    }
//
//    private void runCleanupOfLostTxns(CleanerMockCreatorFactory cleanerFactory,
//                                      int expectedCount,
//                                      CountDownLatch latch) throws InterruptedException {
//        try (Scope scope = tracer.buildSpan("cleanup").startActive(true)) {
//            LOGGER.info("Starting cleanup");
//
//            TransactionConfig config = TestUtils.concurrentTestConfig()
//                    // Make sure a) lost txns are cleaned up and b) quickly
//                    .cleanupClientAttempts(false)
//                    .cleanupLostAttempts(true)
//                    .cleanupWindow(Duration.of(2, ChronoUnit.SECONDS))
//                    .expirationTime(Duration.of(1000, ChronoUnit.MILLIS))
//                    .logDirectly(Event.Severity.ERROR)
//                    .testFactories(null, cleanerFactory, null)
//                    .build();
//
//            try (Transactions transactions = Transactions.create(cluster, config)) {
//
//                int timeoutSecs = 60;
//                int checkEveryMsecs = 250;
//
//                System.out.println(String.format("Waiting %d secs for all ATR entries to be removed", timeoutSecs));
//
//                final long start = System.nanoTime();
//                while (true) {
//                    if ((System.nanoTime() - start) / 1_000_000_000L >= timeoutSecs) {
//                        assert (false);
//                    }
//
//                    final long iterationStart = System.nanoTime();
//
//                    long count = TestUtils.atrEntriesCount(collection, config, span).block();
//
//                    System.out.println(String.format("ATR entries now %d, took %dms", count,
//                            (System.nanoTime() - iterationStart) / 1_000_000));
//
//                    if (count == expectedCount) {
//                        if (latch != null) {
//                            latch.await(60, TimeUnit.SECONDS);
//                        }
//                        return;
//                    }
//                    else if (count < expectedCount) {
//                        fail("Count " + count  + " is below expected " + expectedCount);
//                    }
//
//                    try {
//                        Thread.sleep(checkEveryMsecs);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//            }
//        }
//    }
//
//    @Test
//    public void failedCleanupOfTxnShouldNotBlockCleanupOfOtherTxnsInSameATR() throws InterruptedException {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            // Force all txns to same ATR
//            TransactionMock mock = new TransactionMock();
//            mock.randomAtrIdForVbucket = (ctx) -> Optional.of(ATRIds.allAtrs(ATRIds.NUM_ATRS_DEFAULT).get(0));
//            mock.afterAtrCommit = (ctx) -> Mono.error(new AbortedAsRequestedNoRollback());
//
//            String docId = TestUtils.docId(collection, 0);
//            String docId2 = TestUtils.docId(collection, 1);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(mock), null, null))) {
//
//                JsonObject initial = JsonObject.create().put("val", 1);
//
//                try {
//                    TransactionResult result1 = transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, initial);
//                    });
//                }
//                catch (TransactionFailed e) {}
//
//                try {
//                    TransactionResult result2 = transactions.run((ctx) -> {
//                        ctx.insert(collection, docId2, initial);
//                    });
//                }
//                catch (TransactionFailed e) {}
//
//                List<ATREntry> entries =
//                    TestUtils.allAtrEntries(collection, transactions.config(), null).collectList().block();
//
//                assertEquals(2, entries.size());
//                assertEquals(AttemptStates.COMMITTED, entries.get(0).state());
//                assertEquals(AttemptStates.COMMITTED, entries.get(1).state());
//            }
//
//            CleanerMockCreatorFactory cleanerFactory = new CleanerMockCreatorFactory();
//
//            CountDownLatch called = new CountDownLatch(1);
//            AtomicBoolean first = new AtomicBoolean(true);
//            cleanerFactory.beforeCommitDoc = (id) -> {
//                if (first.compareAndSet(true, false)) {
//                    LOGGER.info("Failing first txn");
//                    called.countDown();
//                    return Mono.error(new RuntimeException());
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            runCleanupOfLostTxns(cleanerFactory, 1, called);
//        }
//    }
//
//
//}
//
