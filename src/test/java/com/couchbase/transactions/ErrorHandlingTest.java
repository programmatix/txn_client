/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.  All rights reserved.
 */

package com.couchbase.transactions;

import com.couchbase.Constants.Strings;
import com.couchbase.client.core.cnc.Event;
import com.couchbase.client.core.error.CasMismatchException;
import com.couchbase.client.core.error.RequestCanceledException;
import com.couchbase.client.core.error.TemporaryFailureException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.transactions.error.TransactionExpired;
import com.couchbase.transactions.log.SimpleEventBusLogger;
import com.couchbase.transactions.tracing.TracingUtils;
import com.couchbase.transactions.tracing.TracingWrapper;
import com.couchbase.transactions.util.DocValidator;
import com.couchbase.transactions.util.ResultValidator;
import com.couchbase.transactions.util.SharedTestState;
import com.couchbase.transactions.util.TestAttemptContextFactory;
import com.couchbase.transactions.util.TransactionFactoryBuilder;
import com.couchbase.transactions.util.TransactionFactoryWrapper;
import com.couchbase.transactions.util.TransactionMock;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.couchbase.Constants.Strings.INITIAL_CONTENT_VALUE;
import static com.couchbase.Constants.Strings.UPDATED_CONTENT_VALUE;
import static com.couchbase.grpc.protocol.TxnClient.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;

/**
 * Test to do with errors, usually injected with hooks, happening at specific points of a transaction.
 */
public class ErrorHandlingTest {
    private static SharedTestState shared;
    private static Collection collection;

    @BeforeAll
    public static void beforeOnce() {
        shared = SharedTestState.create();
        collection = shared.collection();
    }

    @BeforeEach
    public void beforeEach() {
        TestUtils.cleanupBefore(collection);
    }

    @AfterEach
    public void afterEach() {
        assertEquals(0, shared.droppedErrors().get());
        shared.droppedErrors().set(0);
    }

    @AfterAll
    public static void afterOnce() {
        shared.close();
    }

    @Test
    public void replacingDocAndHittingCasErrorOnce() {
        String docId = TestUtils.docId(collection, 0);
        JsonObject initial = JsonObject.create().put(Strings.CONTENT_NAME, INITIAL_CONTENT_VALUE);
        JsonObject mutation = JsonObject.create().put(Strings.CONTENT_NAME, "SET");
        JsonObject after = JsonObject.create().put(Strings.CONTENT_NAME, UPDATED_CONTENT_VALUE);
        collection.upsert(docId, initial);

        try (TransactionFactoryWrapper wrap = TransactionFactoryBuilder.create(shared)
            .addHook(TxnClient.Hook.newBuilder()
                .setHookPoint(HookPoint.AFTER_GET_COMPLETE)
                .setHookCondition(HookCondition.ON_CALL)
                .setHookConditionParam(1)
                .setHookAction(HookAction.MUTATE_DOC)
                .setHookActionParam1(collection.bucketName() + "/" + collection.name() + "/" + docId)
                .setHookActionParam2(mutation.toString())
                .build())
            .build()) {

            assertEquals(0, wrap.state().getAttemptNumber());

            shared.logger().info("Replace should fail on CAS mismatch. Txn should retry.");
            wrap.replaceExpectFailure(docId, after.toString());

            assertEquals(1, wrap.state().getAttemptNumber());

            shared.logger().info("In retry: 2nd attempt to replace should succeed");
            wrap.replace(docId, after.toString());

            TransactionResultObject result = wrap.commitAndClose();

            ResultValidator.assertCompletedInMultipleAttempts(collection, result);
            DocValidator.assertDocExistsAndNotInTransactionAndContentEquals(collection, docId, after);
        }
    }


    //
    //    @Test
    //    public void replacingDocAndHittingCasErrorContinuously() {
    //        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
    //            String docId = TestUtils.docId(collection, 0);
    //            JsonObject initial = JsonObject.create().put("val", "ORIGINAL");
    //            MutationResult r = collection.insert(docId, initial);
    //            LOGGER.info("Wrote initial version of doc, cas is " + r.cas());
    //            AtomicInteger attempt = new AtomicInteger(0);
    //
    //            // Turn down logging as this is meant to fail a lot and it looks bad
    //            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope)
    //                    .expirationTime(Duration.of(3, ChronoUnit.SECONDS))
    //                    .logDirectly(Event.Severity.VERBOSE))) {
    //                Transactions spied = TestUtils.prepareTransactionContextMock(transactions, (spy) -> {
    //                    Mockito.doAnswer(v -> collection.reactive().get(docId)
    //
    //                            .flatMap(d -> {
    //                                JsonObject doc = d.contentAsObject();
    //                                LOGGER.info("Doing a SET after txn's get, current CAS is " + d.cas());
    //                                JsonObject content = doc.put("val", "SET");
    //                                return collection.reactive().replace(docId, content)
    //                                        .doOnNext(x -> {
    //                                            LOGGER.info("Finished SET, CAS now " + x.cas());
    //                                        });
    //                            })
    //
    //                            .thenReturn(1)
    //                    ).when(spy).afterGetComplete(any(), any());
    //                });
    //
    //                try {
    //                    spied.run((ctx1) -> {
    //                        attempt.set(attempt.get() + 1);
    //                        TransactionGetResult doc = ctx1.get(collection, docId);
    //                        LOGGER.info("Got doc inside txn, cas is " + doc.cas());
    //                        JsonObject content = doc.contentAs(JsonObject.class).put("val", "TXN " + attempt.get());
    //                        ctx1.replace(doc, content);
    //                        fail("replace should fail with CAS");
    //                        ctx1.commit();
    //                    }, TestUtils.defaultPerConfig(scope));
    //                    fail("txn should never succeed, always fails on replace (CAS)");
    //                } catch (Throwable err) {
    //                    assertTrue(err instanceof TransactionExpired);
    //                }
    //
    //                assertEquals("SET", collection.get(docId).contentAs(JsonObject.class).getString("val"));
    //                assertTrue(attempt.get() > 3);
    //                // Timer should expire first
    //                assertTrue(attempt.get() < TransactionsReactive.MAX_ATTEMPTS);
    //            }
    //        }
    //    }
    //

    //    // TXNJ-73
    //    @Test
    //    public void creatingATRFails() throws InterruptedException {
    //        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
    //            String docId = TestUtils.docId(collection, 0);
    //            AtomicInteger attempt = new AtomicInteger(0);
    //
    //            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
    //
    //                TransactionMock mock = new TransactionMock();
    //
    //                TestAttemptContextFactory factory = new TestAttemptContextFactory(mock);
    //                transactions.reactive().setAttemptContextFactory(factory);
    //
    //                mock.beforeAtrPending = (ctx) -> {
    //                   if (attempt.get() == 1) return Mono.error(new TemporaryFailureException(null));
    //                   else return Mono.just(1);
    //                };
    //
    //                TransactionResult result = transactions.run((ctx1) -> {
    //                    attempt.set(attempt.get() + 1);
    //
    //                    JsonObject initial = JsonObject.create().put("val", "TXN");
    //
    //                    ctx1.insert(collection, docId, initial);
    //
    //                });
    //
    //                assertEquals("TXN", collection.get(docId).contentAs(JsonObject.class).getString("val"));
    //            }
    //        }
    //    }
    //
    //  // TXNJ-73
    //  @Test
    //  public void creatingATREntryFails() throws InterruptedException {
    //    try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
    //      String docId = TestUtils.docId(collection, 0);
    //
    //      try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
    //
    //        LOGGER.info("Running txn1 to create the ATR");
    //
    //        TransactionResult result = transactions.run((ctx1) -> {
    //          JsonObject initial = JsonObject.create().put("val", "TXN");
    //          ctx1.insert(collection, docId, initial);
    //        });
    //
    //        // CLeamup first txn
    //        collection.remove(docId);
    //        collection.get(result.attempts().get(0).atrId().get());
    //
    //        LOGGER.info("Running txn2 to create the problem");
    //
    //        TransactionMock mock = new TransactionMock();
    //
    //        TestAttemptContextFactory factory = new TestAttemptContextFactory(mock);
    //        transactions.reactive().setAttemptContextFactory(factory);
    //
    //        AtomicInteger attempt = new AtomicInteger(0);
    //
    //        mock.beforeAtrPending = (ctx) -> {
    //          if (attempt.get() == 1) return Mono.error(new TemporaryFailureException(null));
    //          else return Mono.just(1);
    //        };
    //
    //        TransactionResult result2 = transactions.run((ctx1) -> {
    //          attempt.set(attempt.get() + 1);
    //
    //          JsonObject initial = JsonObject.create().put("val", "TXN");
    //
    //          ctx1.insert(collection, docId, initial);
    //
    //        });
    //
    //        assertEquals("TXN", collection.get(docId).contentAs(JsonObject.class).getString("val"));
    //      }
    //    }
    //  }
    //
    //  // TXNJ-81
    //    @Test
    //    public void creatingATREntryFailsAndAbortingATRFails() {
    //        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
    //            String docId = TestUtils.docId(collection, 0);
    //            AtomicInteger attempt = new AtomicInteger(0);
    //            AtomicBoolean atrAbortFirst = new AtomicBoolean(true);
    //
    //            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope)
    //            .logDirectly(Event.Severity.VERBOSE))) {
    //
    //                TransactionMock mock = new TransactionMock();
    //
    //                TestAttemptContextFactory factory = new TestAttemptContextFactory(mock);
    //                transactions.reactive().setAttemptContextFactory(factory);
    //
    //                mock.afterAtrPending = (ctx) -> {
    //                    // Faking that the ATR entry was actually created but that memcached was restarted just after,
    //                    // before it could notify the client
    //                    if (attempt.get() == 1) return Mono.error(new TemporaryFailureException(null));
    //                    else return Mono.just(1);
    //                };
    //
    //                mock.beforeAtrAborted = (ctx) -> {
    //                    // Faking that we then fail to abort the ATR
    //                    boolean isFirst = atrAbortFirst.compareAndSet(true, false);
    //
    //                    if (isFirst) return Mono.error(new TemporaryFailureException(null));
    //                    else return Mono.just(1);
    //                };
    //
    //                TransactionResult result = transactions.run((ctx1) -> {
    //                    attempt.incrementAndGet();
    //
    //                    JsonObject initial = JsonObject.create().put("val", "TXN");
    //
    //                    ctx1.insert(collection, docId, initial);
    //
    //                });
    //
    //                assertEquals("TXN", collection.get(docId).contentAs(JsonObject.class).getString("val"));
    //            }
    //        }
    //    }
    //
    //  // TXNJ-81
    //  @Test
    //  public void creatingATREntryFailsAndGettingATRBeforeATRFails() {
    //    try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
    //      String docId = TestUtils.docId(collection, 0);
    //      AtomicInteger attempt = new AtomicInteger(0);
    //      AtomicBoolean atrAbortFirst = new AtomicBoolean(true);
    //
    //      try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope).logDirectly
    //      (Event.Severity.VERBOSE))) {
    //
    //        TransactionMock mock = new TransactionMock();
    //
    //        TestAttemptContextFactory factory = new TestAttemptContextFactory(mock);
    //        transactions.reactive().setAttemptContextFactory(factory);
    //
    //        mock.afterAtrPending = (ctx) -> {
    //          // Faking that the ATR entry was actually created but that memcached was restarted just after,
    //          // before it could notify the client
    //          if (attempt.get() == 1) return Mono.error(new TemporaryFailureException(null));
    //          else return Mono.just(1);
    //        };
    //
    //        mock.beforeGetAtrForAbort = (ctx) -> {
    //          // Faking that we then fail to get the ATR
    //          boolean isFirst = atrAbortFirst.compareAndSet(true, false);
    //
    //          if (isFirst) return Mono.error(new TemporaryFailureException(null));
    //          else return Mono.just(1);
    //        };
    //
    //        TransactionResult result = transactions.run((ctx1) -> {
    //          attempt.incrementAndGet();
    //
    //          JsonObject initial = JsonObject.create().put("val", "TXN");
    //
    //          ctx1.insert(collection, docId, initial);
    //
    //        });
    //
    //        assertEquals("TXN", collection.get(docId).contentAs(JsonObject.class).getString("val"));
    //      }
    //    }
    //  }
    //
    //    // TXNJ-174
    //    @Test
    //    public void repeatedRequestCancelledDuringInsertCausesTransactionToExpire() {
    //        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
    //            String docId = TestUtils.docId(collection, 0);
    //
    //            TransactionMock mock = new TransactionMock();
    //            TestAttemptContextFactory factory = new TestAttemptContextFactory(mock);
    //
    //            mock.beforeStagedInsert = (ctx, id) -> {
    //                return Mono.error(new RequestCanceledException("faking", null));
    //            };
    //
    //            try (Transactions transactions = Transactions.create(cluster,
    //                TestUtils.defaultConfig(scope)
    //                    .expirationTime(Duration.ofMillis(200))
    //                    .testFactories(factory, null, null))) {
    //
    //                try {
    //                    transactions.run((ctx1) -> {
    //                        JsonObject initial = JsonObject.create().put("val", "TXN");
    //
    //                        ctx1.insert(collection, docId, initial);
    //                    });
    //                    fail();
    //                }
    //                catch (TransactionExpired err) {
    //                }
    //            }
    //        }
    //    }
}
