/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.  All rights reserved.
 */

package com.couchbase.transactions;

import com.couchbase.client.core.cnc.EventBus;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.codec.Transcoder;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.LookupInSpec;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.transactions.atr.ATRIds;
import com.couchbase.transactions.cleanup.ClientRecord;
import com.couchbase.transactions.components.ATR;
import com.couchbase.transactions.components.ATREntry;
import com.couchbase.transactions.components.ActiveTransactionRecord;
import com.couchbase.transactions.components.DocumentGetter;
import com.couchbase.transactions.config.PerTransactionConfig;
import com.couchbase.transactions.config.PerTransactionConfigBuilder;
import com.couchbase.transactions.config.TransactionConfig;
import com.couchbase.transactions.config.TransactionConfigBuilder;
import com.couchbase.transactions.error.TransactionFailed;
import com.couchbase.transactions.log.PersistedLogWriter;
import com.couchbase.transactions.log.SimpleEventBusLogger;
import com.couchbase.transactions.support.AttemptStates;
import com.couchbase.transactions.support.SpanWrapper;
import com.couchbase.transactions.util.TestAttemptContextFactory;
import com.couchbase.transactions.util.TransactionMock;
import io.opentracing.Scope;
import io.opentracing.Span;
import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;
import scala.Function1;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.couchbase.transactions.cleanup.ClientRecord.CLIENT_RECORD_DOC_ID;
import static com.couchbase.transactions.cleanup.ClientRecord.FIELD_RECORDS;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;


public class TestUtils {
    public static final Duration timeout = Duration.of(2500, ChronoUnit.MILLIS);

    public static Cluster getCluster() {
        return getCluster(null);
    }

    public static Cluster getCluster(EventBus eventBus) {

        // Make reactive stacktraces easier
        // Update: very slow, decimates test performance
//         Hooks.onOperatorDebug();
        ReactorDebugAgent.init();

        String ip = System.getenv("TXNS_TEST_CLUSTER_IP");

        System.out.println("TXNS_TEST_CLUSTER_IP = " + ip);

        if (ip == null) {
            ip = "localhost";
        }
        //PRANEETH
        ip = "172.23.105.55";
        ClusterEnvironment.Builder env = ClusterEnvironment.builder();
        if (eventBus != null) {
            env.eventBus(eventBus);
        }
        Cluster cluster = Cluster.connect(ip, ClusterOptions
                .clusterOptions("Administrator", "password")
                .environment(env.build()));

        return cluster;
    }

    public static void assertFailedInXAttempts(TransactionFailed err, int attempts) {
        assertEquals(attempts, err.result().attempts().size());
    }

    public static Flux<ATR> getExistingATRs(Collection collection, TransactionConfig config, Span span) {
        return Flux.fromIterable(Transactions.allAtrs(config.numAtrs()))
                // Introducing this check reduces time taken by an order of magnitude
                .flatMap(atrId -> collection.reactive().get(atrId)

                        .flatMap(atrDoc -> {
                            return ActiveTransactionRecord.getAtr(collection.reactive(), atrId, timeout, config,
                                    TestUtils.from(span));
                        })

                        .onErrorResume(err -> {
                            if (err instanceof DocumentNotFoundException) return Mono.empty();
                            else return Mono.error(err);
                        })

                        .flatMap(v -> {
                            if (v.isPresent()) return Mono.just(v.get());
                            else return Mono.empty();
                        }));
    }

    // Cheeky way around package-private
    public static AttemptContextReactive getContext(AttemptContext ctx) {
        return ctx.ctx();
    }

    public static ATREntry atrEntryForAttempt(Collection collection, String attemptId, TransactionConfig config,
                                              Span span) {
        return getExistingATRs(collection, config, span)
                .flatMap(v -> Flux.fromIterable(v.entries()))
                .filter(v -> v.attemptId().equals(attemptId))
                .blockLast();
    }

    public static ATREntry atrEntryForAttempt(Collection collection, TransactionAttempt attempt,
                                              TransactionConfig config,
                                              Span span) {
        return ActiveTransactionRecord.getAtr(collection.reactive(), attempt.atrId().get(), Duration.ofSeconds(10),
                config, TestUtils.from(span))
                .map(v -> v.get().entries().stream().filter(entry -> entry.attemptId().equals(attempt.attemptId())).findFirst().get())
                .block();
    }

    public static Flux<AttemptStates> atrEntriesStates(Collection collection, TransactionConfig config, Span span) {
        return getExistingATRs(collection, config, span)
                .flatMap(v -> Flux.fromIterable(v.entries()))
                .map(v -> v.state());
    }

    public static Flux<ATREntry> atrEntries(Collection collection, String atrId, TransactionConfig config, Span span) {
        return ActiveTransactionRecord.getAndTouchAtr(collection.reactive(), atrId, timeout, TestUtils.from(span)
                , config)

                .flatMap(v -> {
                    if (v.isPresent()) return Mono.just(v.get());
                    else return Mono.empty();
                })

                .flatMapMany(v -> Flux.fromIterable(v.entries()));
    }

    public static Flux<ATREntry> allAtrEntries(Collection collection, TransactionConfig config, Span span) {
        return Flux.fromIterable(Transactions.allAtrs(config.numAtrs()))

                .flatMap(atrId -> ActiveTransactionRecord.getAndTouchAtr(collection.reactive(), atrId, timeout,
                        TestUtils.from(span), config)

                        .flatMap(v -> {
                            if (v.isPresent()) return Mono.just(v.get());
                            else return Mono.empty();
                        })
                )

                .flatMap(v -> Flux.fromIterable(v.entries()));
    }

    public static Flux<ATR> atrEntriesWithTouch(Collection collection, TransactionConfig config, Span span) {
        return Flux.fromIterable(ATRIds.allAtrs(config.numAtrs()))

                .flatMap(atrId ->
                        ActiveTransactionRecord.getAndTouchAtr(collection.reactive(), atrId, timeout,
                                TestUtils.from(span), config)

                                .flatMap(v -> {
                                    if (v.isPresent()) return Mono.just(v.get());
                                    else return Mono.empty();
                                }));
    }

    public static Mono<Long> atrEntriesCount(Collection collection, TransactionConfig config, Span span) {
        return getExistingATRs(collection, config, span)
                .flatMap(v -> Flux.fromIterable(v.entries()))
                .count();
    }

    public static void blockUntilAtrsEmpty(Collection collection, TransactionConfig config, Span span) {
        blockUntilAtrsEmpty(collection, 10, config, span);
    }

    public static void blockUntilAtrsEmpty(Collection collection,
                                           long timeoutSecs,
                                           TransactionConfig config,
                                           Span span) {
        blockUntilAtrsEmpty(collection, timeoutSecs, config, span, 250);
    }

    public static void blockUntilAtrsEmpty(Collection collection,
                                           long timeoutSecs,
                                           TransactionConfig config,
                                           Span span,
                                           int checkEveryMsecs) {
        blockUntilAtrsExpected(collection, timeoutSecs, config, span, checkEveryMsecs, 0);
    }

    public static void blockUntilAtrsExpected(Collection collection,
                                              long timeoutSecs,
                                              TransactionConfig config,
                                              Span span,
                                              int checkEveryMsecs,
                                              int expectedCount) {

        System.out.println(String.format("Waiting %d secs for all ATR entries to be removed", timeoutSecs));

        final long start = System.nanoTime();
        while (true) {
            if ((System.nanoTime() - start) / 1_000_000_000L >= timeoutSecs) {
                assert (false);
            }

            final long iterationStart = System.nanoTime();

            long count = atrEntriesCount(collection, config, span).block();

            System.out.println(String.format("ATR entries now %d, took %dms", count,
                    (System.nanoTime() - iterationStart) / 1_000_000));

            if (count == expectedCount) {
                return;
            }
            else if (count < expectedCount) {
                fail("Count " + count  + " is below expected " + expectedCount);
            }

            try {
                Thread.sleep(checkEveryMsecs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static long numAtrs(Collection collection, TransactionConfig config, Span span) {
        return getExistingATRs(collection, config, span)
                .count()
                .block();
    }


    public static void assertAtrEntriesCount(Collection collection, long count, TransactionConfig config, Span span) {
        assertEquals(count, atrEntriesStates(collection, config, span).count().block().longValue());
    }

    public static void assertAtrEntriesCount(Collection collection, TransactionResult result, long count,
                                             TransactionConfig config, Span span) {
        List<String> atrIds = result.attempts().stream().map(v -> v.atrId().get()).collect(Collectors.toList());
        atrIds.forEach(atrId -> assertEquals(count,
                atrEntries(collection, atrId, config, span).count().block().longValue()));
    }

    public static String docId(int idx) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        return st[2].getMethodName() + "_" + idx;
    }

    public static String docId(Collection collection, int idx) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        String id = st[2].getMethodName() + "_" + idx;
        cleanup(collection, id);
        return id;
    }

    public static String testName() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        return st[2].getMethodName().toLowerCase(); // lower case because Jaeger doesn't seem to love mixed-case
    }

    public static void assertOneAtrEntry(Collection collection, AttemptStates state, TransactionConfig config,
                                         Span span) {
        List<AttemptStates> states = atrEntriesStates(collection, config, span)
                .collectList()
                .block();
        assertEquals(1, states.size());
        assertEquals(state, states.get(0));
    }

    public static boolean isDocInTxn(Collection collection, String id, TransactionConfig config, SpanWrapper span, Transcoder transcoder) {
        TransactionGetResult doc = DocumentGetter.getAsync(collection.reactive(), config, id, null,
                span, transcoder).block().get();
        return doc.links().isDocumentInTransaction();
    }

    public static boolean isDocInTxn(Collection collection, String id, TransactionConfig config, Span span, Transcoder transcoder) {
        TransactionGetResult doc = DocumentGetter.getAsync(collection.reactive(), config, id, null,
                from(span), transcoder).block().get();
        return doc.links().isDocumentInTransaction();
    }

    public static SpanWrapper from(Span span) {
        // Once OpenTelemetry is available, this can do something again
        return new SpanWrapper();
    }

    public static SpanWrapper from(Scope scope) {
        // Once OpenTelemetry is available, this can do something again
        return new SpanWrapper();
    }


    // Prefer TransactionConfigBuilder#testFactories now
    public static TransactionMock prepareTest(Transactions transactions) {
        TransactionMock transactionMock = new TransactionMock();

        TestAttemptContextFactory factory = new TestAttemptContextFactory(transactionMock);
        transactions.reactive().setAttemptContextFactory(factory);

        return transactionMock;
    }

    // Prefer TransactionMock now
    public static Transactions prepareTransactionContextMock(Transactions transactions,
                                                             Consumer<AttemptContextReactive> consumer) {
        Transactions spy = Mockito.spy(transactions);
        spy.reactive = Mockito.spy(spy.reactive);

        Mockito.when(spy.reactive().createAttemptContext(any(), any(), any())).then(v -> {
            TransactionContext overall = (TransactionContext) v.getArguments()[0];
            AttemptContextReactive context = new AttemptContextReactive(overall, spy.config(),
                    UUID.randomUUID().toString(), transactions.reactive(), overall.span());
            AttemptContextReactive ctxSpy = Mockito.spy(context);
            overall.LOGGER.info("Mock createAttemptContext returning new txn " + context.attemptId());
            consumer.accept(ctxSpy);
            return ctxSpy;
        });

        return spy;
    }

    public static void assertInsertedDocIsStaged(TxnClient.TransactionGenericResponse insert, String docId, Collection defaultCollection) {
        assertTrue(insert.getSuccess());
        GetResult get = defaultCollection.get(docId);
        // TXNJ-125: inserted doc will be there, but should be empty
        assertEquals(0, get.contentAsObject().size());
    }

    public static void assertCompleted(TransactionConfig config, TransactionResult result, Collection collection,
                                       Span span, Transcoder transcoder) {
        assertCompleted(config, result.attempts().get(result.attempts().size() - 1), collection, span, transcoder);
    }

    //
    public static void assertRolledBack(TransactionConfig config, TransactionResult result, Collection collection,
                                        Span span, Transcoder transcoder) {
        result.attempts().forEach(attempt -> assertRolledBack(config, attempt, collection, span, transcoder));
    }

    public static void assertEmptyTxn(TransactionResult result, AttemptStates expectedState) {
        assertEquals(1, result.attempts().size());
        TransactionAttempt attempt = result.attempts().stream().findFirst().get();
        Assert.assertEquals(expectedState, attempt.finalState());
        assertFalse(attempt.atrCollection().isPresent());
        assertFalse(attempt.atrId().isPresent());
    }

    public static void assertCompletedIn1Attempt(TransactionConfig config, TransactionResult result,
                                                 Collection collection, Span span, Transcoder transcoder) {
        assertEquals(1, result.attempts().size());
        TransactionAttempt attempt = result.attempts().stream().findFirst().get();
        Assert.assertEquals(AttemptStates.COMPLETED, attempt.finalState());
        TestUtils.assertCompleted(config, result, collection, span, transcoder);
        // ATREntry is checked in assertCompleted
    }

    public static void assertRolledBackIn1Attempt(TransactionConfig config, TransactionResult result,
                                                  Collection collection, Span span, Transcoder transcoder) {
        assertEquals(1, result.attempts().size());
        TransactionAttempt attempt = result.attempts().stream().findFirst().get();
        Assert.assertEquals(AttemptStates.ROLLED_BACK, attempt.finalState());
        TestUtils.assertRolledBack(config, result, collection, span, transcoder);
        // ATREntry is checked in assertRolledBack
    }

    public static void assertCompleted(TransactionConfig config, TransactionAttempt attempt, Collection collection,
                                       Span span, Transcoder transcoder) {
        Optional<ATREntry> entry = ActiveTransactionRecord.findEntryForTransaction(attempt, timeout, config,
                TestUtils.from(span));
        assertTrue(entry.isPresent());
        Assert.assertEquals(AttemptStates.COMPLETED, entry.get().state());

        attempt.stagedInsertIds().forEach(doc -> {
            assertNotNull(collection.get(doc));
            assertFalse(isDocInTxn(collection, doc, config, span, transcoder));
        });
        attempt.stagedRemoveIds().forEach(doc -> assertThrows(DocumentNotFoundException.class, () -> collection.get(doc)));
        attempt.stagedReplaceIds().forEach(doc -> {
            // Limited in what we can check here
            assertNotNull(collection.get(doc));
            assertFalse(isDocInTxn(collection, doc, config, span, transcoder));
        });
    }

    public static void assertRolledBack(TransactionConfig config, TransactionAttempt attempt, Collection collection,
                                        Span span, Transcoder transcoder) {
        Optional<ATREntry> entry = ActiveTransactionRecord.findEntryForTransaction(attempt, timeout, config,
                TestUtils.from(span));
        assertTrue(entry.isPresent());
        Assert.assertEquals(AttemptStates.ROLLED_BACK, entry.get().state());

        attempt.stagedInsertIds().forEach(doc -> assertThrows(DocumentNotFoundException.class, () -> collection.get(doc)));
        attempt.stagedRemoveIds().forEach(doc -> {
            assertNotNull(collection.get(doc));
            assertFalse(isDocInTxn(collection, doc, config, span, transcoder));
        });
        attempt.stagedReplaceIds().forEach(doc -> {
            // Limited in what we can check here
            assertNotNull(collection.get(doc));
            assertFalse(isDocInTxn(collection, doc, config, span, transcoder));
        });
    }


    public static void expireTxnAFterXAnswer(AttemptContextReactive spy, int answerCount) {
        doAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                count++;
                boolean ret = false;
                if (count >= answerCount) {
                    ret = true;
                }
                spy.LOGGER.info("Call " + count + " to hasExpiredClientSide, returning " + ret);
                return ret;
            }
        }).when(spy).hasExpiredClientSide(any());
    }

    public static void expireAttemptStartOfCommit(AttemptContextReactive spy) {
        expireTxnAFterXAnswer(spy, 3);
    }

    // For the errors that cause AttemptWrappedExceptionNoRetry directly
    public static Throwable assertTransactionFailed(Throwable e) {
        assertTrue(e instanceof TransactionFailed);
        TransactionFailed err = (TransactionFailed) e;
        return err.getCause();
    }


    public static void assertAtrEntryDocs(Collection collection,
                                          TransactionResult result,
                                          List<String> insertedIds,
                                          List<String> replacedIds,
                                          List<String> removedIds,
                                          TransactionConfig config,
                                          Span span) {
        ATREntry atrEntry = TestUtils.atrEntryForAttempt(collection, result.attempts().get(0).attemptId(), config,
                span);

        // We expect these fields to be written even if there were no docs in them
        assertTrue(atrEntry.insertedIds().isPresent());
        assertTrue(atrEntry.removedIds().isPresent());
        assertTrue(atrEntry.replacedIds().isPresent());


        if (insertedIds != null) {
            assertTrue(atrEntry.insertedIds().isPresent());
            assertEquals(insertedIds.size(), atrEntry.insertedIds().get().size());
            for (int i = 0; i < insertedIds.size(); i++) {
                assertEquals(insertedIds.get(i), atrEntry.insertedIds().get().get(i).id());
            }
        } else {
            assertEquals(0, atrEntry.insertedIds().get().size());
            //            assertFalse(atrEntry.insertedIds().isPresent());
        }

        if (replacedIds != null) {
            assertTrue(atrEntry.replacedIds().isPresent());
            assertEquals(replacedIds.size(), atrEntry.replacedIds().get().size());
            for (int i = 0; i < replacedIds.size(); i++) {
                assertEquals(replacedIds.get(i), atrEntry.replacedIds().get().get(i).id());
            }
        } else {
            assertEquals(0, atrEntry.replacedIds().get().size());
            //            assertFalse(atrEntry.replacedIds().isPresent());
        }

        if (removedIds != null) {
            assertTrue(atrEntry.removedIds().isPresent());
            assertEquals(removedIds.size(), atrEntry.removedIds().get().size());
            for (int i = 0; i < removedIds.size(); i++) {
                assertEquals(removedIds.get(i), atrEntry.removedIds().get().get(i).id());
            }
        } else {
            assertEquals(0, atrEntry.removedIds().get().size());
            //            assertFalse(atrEntry.removedIds().isPresent());
        }
    }

    public static PerTransactionConfig defaultPerConfig(Scope scope) {
        return PerTransactionConfigBuilder.create().build();
    }


    public static boolean isOnCI() {
        return System.getenv("TXNS_TEST_CLUSTER_IP") != null;
    }

    public static TransactionConfigBuilder defaultConfig(Scope scope) {
        TransactionConfigBuilder out = TransactionConfigBuilder.create()
                // If something goes wrong, don't have it going wrong for 15s
                .expirationTime(Duration.of(3000, ChronoUnit.MILLIS))
                // Expensive, logging can be confusing, slows tests
                .cleanupLostAttempts(false)
                // Also adds confusion
                .cleanupClientAttempts(false);
                // Useful to see logging
                // Buuuuut also TXNJ-66.  Trying to reduce CI logging in a hurry so disabling.
                //                .logDirectly(Event.Severity.VERBOSE)
                //                .logDirectlyCleanup(Event.Severity.INFO)

        return out;
    }

    public static void cleanup(Collection collection, String docId) {
        try {
            collection.remove(docId);
        } catch (Exception e) {
        }
    }

    public static TransactionConfigBuilder defaultConfig() {
        return defaultConfig(null);
    }

    public static TransactionConfigBuilder concurrentTestConfig() {
        // Designed to maximise throughput and minimise confusion
        return TransactionConfigBuilder.create()
                .durabilityLevel(TransactionDurabilityLevel.MAJORITY)
                .cleanupClientAttempts(false)
                .cleanupLostAttempts(false);
    }


    public static int activeClientCount(Collection collection) {
        return collection.reactive().lookupIn(CLIENT_RECORD_DOC_ID,
                Arrays.asList(LookupInSpec.get(FIELD_RECORDS).xattr()))

                .map(cr -> ClientRecord.parseClientRecord(cr, "not_client").numActiveClients())

                .doOnError(err -> System.out.println("Error getting client record: " + err.getMessage()))

                .onErrorReturn(0)

                .doOnNext(v -> System.out.println("Active clients: " + v))

                .block();
    }


    public static void cleanupBefore(Collection collection) {
        //        TestUtils.cleanupBefore(collection);
        AtomicLong removed = new AtomicLong(0);

        // Should really check MAX_ATRs, but that's very slow and most testing is done with default
        ArrayList<String> ids = new ArrayList<>(ATRIds.allAtrs(ATRIds.NUM_ATRS_DEFAULT));
        ids.add(CLIENT_RECORD_DOC_ID);
        ids.add(PersistedLogWriter.PERSISTED_LOG_DOC_ID);

        Flux.fromIterable(ids)
                .flatMap(id -> {
                            return collection.reactive().remove(id)
                                    .onErrorResume(err -> {
                                        if (err instanceof DocumentNotFoundException) {
                                            return Mono.empty();
                                        } else return Mono.error(err);
                                    })
                                    .doOnNext(x -> {
                                        removed.incrementAndGet();
                                        //                                        System.out.println("Removed doc " +
                                        //                                        x);
                                    });
                        }
                ).blockLast();

        //        System.out.println("Cleaned up " + removed.get() + " txn metadata docs");
    }

    public static class ConcurrentTxnsParams {
        public Function1<Long, Mono<Void>> onIteration;

        // The max number of concurrent txns
        public int maxConcurrent = 128;

        // Will log a status message every X transactions
        int logEvery = 100;

        // Will log extra detailed (and more expensive) status message every X transactions
        int logExtraEvery = 3000;

        //  The actual transaction to run
        BiFunction<AttemptContextReactive, ConcurrentTxnsRun, Mono<Void>> txnLogic = (ctx, v) -> {

            int docId = v.txn;// % 1000;
            return ctx.getOptional(v.collection.reactive(), "test-" + docId)
                    .flatMap(doc -> {
                        if (!doc.isPresent()) {
                            String id = "test-" + docId;
                            JsonObject content = JsonObject.create().put("val", 1);
                            return ctx.insert(v.collection.reactive(), id, content);
                        } else {
                            JsonObject content = doc.get().contentAs(JsonObject.class);
                            content.put("val", content.getLong("val") + 1);
                            return ctx.replace(doc.get(), content);
                        }
                    })
                    .flatMap(ignore -> ctx.commit());
        };
    }

    static class ConcurrentTxnsRun {
        private Collection collection;
        private int txn;

        public ConcurrentTxnsRun(Collection collection, int txn) {
            this.collection = collection;
            this.txn = txn;
        }

        public Collection collection() {
            return collection;
        }

        public int txn() {
            return txn;
        }
    }

    public static void concurrentTxns(Transactions transactions, int numTxns, int concurrency, Collection collection,
                                      Scope scope) {
        ConcurrentTxnsParams params = new ConcurrentTxnsParams();
        params.maxConcurrent = concurrency;
        concurrentTxns(transactions, numTxns, collection, params, scope);
    }

    public static void concurrentTxns(Transactions transactions, int numTxns, Collection collection, Scope scope) {
        concurrentTxns(transactions, numTxns, collection, new ConcurrentTxnsParams(), scope);
    }


    public static void concurrentTxns(Transactions transactions, int numTxns, Collection collection,
                                      ConcurrentTxnsParams params, Scope scope) {
        CountDownLatch latch = new CountDownLatch(numTxns);
        final long start = System.nanoTime();
        AtomicReference<Long> last = new AtomicReference<>(System.nanoTime());
        AtomicReference<Long> count = new AtomicReference<>(0L);
        AtomicReference<Long> errors = new AtomicReference<>(0L);
        SimpleEventBusLogger LOGGER = new SimpleEventBusLogger(collection.core().context().environment().eventBus());

        Flux.range(0, numTxns)
                .subscribeOn(Schedulers.elastic())
                .flatMap(v -> transactions.reactive((ctx) -> {
                            return params.txnLogic.apply(ctx, new ConcurrentTxnsRun(collection, v));
                        }, TestUtils.defaultPerConfig(scope)).onErrorResume(err -> {
                            //                            System.out.println("Got error " + err);
                            errors.accumulateAndGet(1l, (a, b) -> a + b);
                            return Mono.empty();
                        })
                        , params.maxConcurrent)

                .doOnError(v -> latch.countDown())
                .doOnNext(v -> latch.countDown())

                .flatMap(ignore -> {
                    long curr = count.accumulateAndGet(1l, (a, b) -> a + b);

                    if (curr % params.logEvery == 0) {
                        long msTaken = (System.nanoTime() - last.get()) / 1_000_000;
                        long elapsedSecs = (System.nanoTime() - start) / 1_000_000_000;
                        LOGGER.info(String.format("Elapsed %ssecs, iteration %d, %d errs, %d msecs, %d threads %dmb " +
                                        "memory",
                                elapsedSecs, curr, errors.get(), msTaken,
                                Thread.activeCount(), Runtime.getRuntime().totalMemory() / 1_000_000
                        ));
                        last.set(System.nanoTime());
                    }

                    if (curr % params.logExtraEvery == 0) {
                        // Kick off in background
                        TestUtils.allAtrEntries(collection, transactions.config(), null)
                                .collectList()
                                .subscribeOn(Schedulers.elastic())
                                .doOnNext(entries -> {
                                    long atrCount = entries.stream().map(ATREntry::atrId).distinct().count();

                                    LOGGER.info(String.format("Iteration %d, %d entries across %d ATRs",
                                            curr, entries.size(), atrCount));
                                })
                                .subscribe();
                    }

                    if (params.onIteration != null) {
                        return params.onIteration.apply(curr);
                    } else {
                        return Mono.empty();
                    }
                })

                .subscribe(next -> {
                        },
                        err -> {
                            // Should not get here
                            fail();
                        },
                        // Complete
                        () -> LOGGER.info(String.format("Finished: %d successful with %d errors in %ds, latch = %d",
                                count.get(), errors.get(), (System.nanoTime() - start) / 1000, latch.getCount())));

        try {
            long waitForSecs = Math.max(10, (long) ((1.0 / 15.0) * numTxns));
            LOGGER.info(String.format("Waiting for %ds for %d txns to complete, %d concurrent txns",
                    waitForSecs, numTxns, params.maxConcurrent));
            boolean completedInTime = latch.await(waitForSecs, TimeUnit.SECONDS);
            if (!completedInTime) {
                System.out.println(String.format("Aborting after %d secs", waitForSecs));
            }
            LOGGER.info(String.format("Final count is %d completed, %d errors", count.get(), errors.get()));
            assertTrue(completedInTime);
            assertEquals(0l, errors.get().longValue());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void forceATRCleanup(Transactions transactions, TransactionFailed err) {
        forceATRCleanup(transactions, err.result().attempts().get(0));
    }

    public static void forceATRCleanup(Transactions transactions, TransactionAttempt attempt) {
        transactions.cleanup()
                .forceATRCleanup(attempt.atrCollection().get(), attempt.atrId().get())
                .block();
    }
}
