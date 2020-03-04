// TODO port these
///*
// * Copyright (c) 2018 Couchbase, Inc.
// *
// * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
// * which may be found at https://www.couchbase.com/ESLA-11132015.  All rights reserved.
// */
//
//package com.couchbase.transactions.losttxns;
//
//import com.couchbase.client.java.Bucket;
//import com.couchbase.client.java.Cluster;
//import com.couchbase.client.java.Collection;
//import com.couchbase.transactions.TestUtils;
//import com.couchbase.transactions.log.SimpleEventBusLogger;
//import com.couchbase.transactions.tracing.TracingUtils;
//import com.couchbase.transactions.tracing.TracingWrapper;
//import io.opentracing.Span;
//import io.opentracing.Tracer;
//import org.junit.jupiter.api.*;
//import reactor.core.publisher.Hooks;
//
//import java.util.*;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.stream.Stream;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.fail;
//
///**
// * Permutation tests related to LostCleanupDistributed.  In summary these test:
// *
// * If we make a random txn of 1-3 operations (replaces, inserts, removes)...
// * And we abort that txn at any stage...
// * Can we then successfully clean it up?
// * How about if that cleanup then fails at any stage...
// * Can cleanup then rerun and successfully finish off cleaning up that txn?
// * How about if we rollback the txns rather than commit then, and fail mid-rollback?
// *
// * See also LostCleanupDistributed.
// *
// * @author Graham Pople
// */
////@RunWith(MockitoJUnitRunner.class)
//public class LostTxnsCleanupPermutationsTest {
//    static Cluster cluster;
//    static Bucket bucket;
//    static Collection collection;
//    public static SimpleEventBusLogger LOGGER;
//    private static Tracer tracer;
//    private static TracingWrapper tracing;
//    private static Span span;
//    private static AtomicInteger droppedErrors = new AtomicInteger(0);
//
//    @BeforeAll
//    public static void beforeOnce() {
//        tracing = TracingUtils.getTracer();
//        tracer = tracing.tracer();
//        span = tracer.buildSpan("losttxnscleanup").ignoreActiveSpan().start();
//        cluster = TestUtils.getCluster();
//        bucket = cluster.bucket("default");
//        collection = bucket.defaultCollection();
//        LOGGER = new SimpleEventBusLogger(cluster.environment().eventBus());
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
//    ArrayList<Command> cloneTransaction(ArrayList<Command> in) {
//        ArrayList<Command> out = new ArrayList<>();
//
//        in.forEach(c -> {
//          if (c instanceof Insert) {
//              out.add(new Insert());
//          }
//            else if (c instanceof Replace) {
//                out.add(new Replace());
//            }
//            else if (c instanceof Remove) {
//                out.add(new Remove());
//            }
//        });
//
//        return out;
//    }
//
//    ArrayList<ArrayList<Command>> createTransactions() {
//        ArrayList<ArrayList<Command>> perms = new ArrayList<>();
//
//        perms.add(new ArrayList<>(Collections.singletonList(new Insert())));
//        perms.add(new ArrayList<>(Collections.singletonList(new Replace())));
//        perms.add(new ArrayList<>(Collections.singletonList(new Remove())));
//
//        for (int x = 0; x < 2; x++) {
//            ArrayList<ArrayList<Command>> newPerms = new ArrayList<>();
//
//            perms.forEach(p -> {
//
//
//                ArrayList<Command> i = cloneTransaction(p);
//                i.add(0, new Insert());
//                ArrayList<Command> r = cloneTransaction(p);
//                r.add(0, new Replace());
//                ArrayList<Command> d = cloneTransaction(p);
//                d.add(0, new Remove());
//
//                newPerms.add(i);
//                newPerms.add(r);
//                newPerms.add(d);
//            });
//
//            perms.addAll(newPerms);
//        }
//
//        // Safety check that the docIds are unique
//        Set<String> docIds = new HashSet<>();
//        perms.forEach(p -> {
//           p.forEach(p2 -> {
//               if (docIds.contains(p2.docId)) {
//                   fail("Found an existing command containing docId " + p2.docId + " at " + docIds.size());
//               }
//
//             docIds.add(p2.docId);
//           });
//        });
//
//        // Remove txns of length 2.  Adds 1000+ tests and very little value, as it's a subset of txns of length 3.
//        // Ok to leave txns of length 1.  Only adds ~100 tests.
//        perms.removeIf(v -> v.size() == 2);
//
//        return perms;
//    }
//
//    List<FailTransactionAt> getFailTransactionAt(int txnLen) {
//        ArrayList<FailTransactionAt> values = new ArrayList<>(Arrays.asList(FailTransactionAt.values()));
//
//        // See comment in FailTransactionAt for why these are removed
//        values.remove(FailTransactionAt.BeforeStagingCommand1LeaveAborted);
//        values.remove(FailTransactionAt.BeforeStagingCommand2LeaveAborted);
//        values.remove(FailTransactionAt.BeforeStagingCommand3LeaveAborted);
//        values.remove(FailTransactionAt.BeforeATRCommitLeaveAborted);
//
//        if (txnLen < 3) {
//            values.remove(FailTransactionAt.BeforeStagingCommand3LeavePending);
//            values.remove(FailTransactionAt.BeforeStagingCommand3LeaveAborted);
//            values.remove(FailTransactionAt.BeforeCommitOrRollbackCommand3);
//        }
//
//        if (txnLen < 2) {
//            values.remove(FailTransactionAt.BeforeStagingCommand2LeavePending);
//            values.remove(FailTransactionAt.BeforeStagingCommand2LeaveAborted);
//            values.remove(FailTransactionAt.BeforeCommitOrRollbackCommand2);
//        }
//
//        return values;
//    }
//
//    List<FailCleanupAt> getFailCleanupAt(FailTransactionAt fta, int txnLen) {
//        ArrayList<FailCleanupAt> values = new ArrayList<>(Arrays.asList(FailCleanupAt.values()));
//
//        switch (fta) {
//            case BeforeATRPending:
//            case BeforeStagingCommand1LeavePending:
//            case BeforeStagingCommand2LeavePending:
//            case BeforeStagingCommand3LeavePending:
//            case BeforeATRCommitLeavePending:
//
//            case BeforeStagingCommand1LeaveAborted:
//            case BeforeStagingCommand2LeaveAborted:
//            case BeforeStagingCommand3LeaveAborted:
//            case BeforeATRCommitLeaveAborted:
//
//                // If txn does not get to Committed state then cleanup of individual docs will not be attempted
//                values.remove(FailCleanupAt.BeforeCleaningOnGetCommand1);
//                values.remove(FailCleanupAt.BeforeCleaningCommand1);
//                values.remove(FailCleanupAt.BeforeCleaningCommand2);
//                values.remove(FailCleanupAt.BeforeCleaningCommand3);
//                break;
//
//            case BeforeCommitOrRollbackCommand2:
//                // If we failed to commit command 2, no point trying to abort cleanup on command 1, as there will be nothing
//                // done on that command anyway
//                values.remove(FailCleanupAt.BeforeCleaningCommand1);
//                break;
//            case BeforeCommitOrRollbackCommand3:
//                values.remove(FailCleanupAt.BeforeCleaningCommand1);
//                values.remove(FailCleanupAt.BeforeCleaningCommand2);
//                break;
//            case BeforeATRCompleteOrRolledBack:
//                values.remove(FailCleanupAt.BeforeCleaningCommand1);
//                values.remove(FailCleanupAt.BeforeCleaningCommand2);
//                values.remove(FailCleanupAt.BeforeCleaningCommand3);
//                break;
//        }
//
//        if (txnLen < 3) {
//            values.remove(FailCleanupAt.BeforeCleaningCommand3);
//        }
//
//        if (txnLen < 2) {
//            values.remove(FailCleanupAt.BeforeCleaningCommand2);
//        }
//
//        return values;
//    }
//
//    List<Permutation> createPermutations(ArrayList<ArrayList<Command>> transactions) {
//        ArrayList<Permutation> out = new ArrayList<>();
//
//        transactions.forEach(txn -> {
//            getFailTransactionAt(txn.size()).forEach(fta -> {
//                getFailCleanupAt(fta, txn.size()).forEach(fca -> {
//
//                    // Create commit version
//                    Permutation committed = new Permutation(cloneTransaction(txn), fta, fca, true, true, true);
//                    out.add(committed);
//
//                    switch (fta) {
//                        case BeforeCommitOrRollbackCommand1:
//                        case BeforeCommitOrRollbackCommand2:
//                        case BeforeCommitOrRollbackCommand3:
//                        case BeforeATRCompleteOrRolledBack:
//
//                            // Create rolled back version.  No need to do this for other `fta` values, as those won't
//                            // reach the commit point.
//                            Permutation rolledBack = new Permutation(cloneTransaction(txn), fta, fca, false, true, true);
//                            out.add(rolledBack);
//                    }
//
//                });
//            });
//        });
//
//        // Safety check that the docIds are unique
//        Set<String> docIds = new HashSet<>();
//        out.forEach(p -> {
//            p.transaction.forEach(p2 -> {
//                if (docIds.contains(p2.docId)) {
//                    fail("Found an existing command containing docId " + p2.docId + " at " + docIds.size());
//                }
//
//                docIds.add(p2.docId);
//            });
//        });
//
//        return out;
//    }
//
//    @TestFactory
//    Stream<DynamicTest> dynamicTestStream() {
//        ArrayList<ArrayList<Command>> txns = createTransactions();
//        List<Permutation> perms = createPermutations(txns);
//        AtomicInteger count = new AtomicInteger(1);
//
//        return perms.stream()
//                .map(p -> {
//                    String idx = "(" + count.getAndIncrement() + " of " + perms.size() + ") ";
//                    return DynamicTest.dynamicTest(idx + p.toString(),
//                            () -> p.runAndAssertSuccess(cluster, collection, span));
//                });
//    }
//}
