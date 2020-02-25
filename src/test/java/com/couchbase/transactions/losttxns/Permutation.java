package com.couchbase.transactions.losttxns;

import com.couchbase.client.core.cnc.Event;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.codec.Transcoder;
import com.couchbase.transactions.TestUtils;
import com.couchbase.transactions.TransactionAttempt;
import com.couchbase.transactions.TransactionResult;
import com.couchbase.transactions.Transactions;
import com.couchbase.transactions.cleanup.CleanerMock;
import com.couchbase.transactions.cleanup.ClusterData;
import com.couchbase.transactions.components.ATREntry;
import com.couchbase.transactions.config.TransactionConfig;
import com.couchbase.transactions.error.TransactionFailed;
import com.couchbase.transactions.error.internal.AbortedAsRequestedNoRollbackNoCleanup;
import com.couchbase.transactions.error.internal.AttemptExpired;
import com.couchbase.transactions.log.LogDefer;
import com.couchbase.transactions.log.SimpleEventBusLogger;
import com.couchbase.transactions.log.TransactionCleanupAttempt;
import com.couchbase.transactions.support.AttemptStates;
import com.couchbase.transactions.support.SpanWrapper;
import com.couchbase.transactions.util.TransactionMock;
import io.opentracing.Span;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class Permutation {
    public final List<Command> transaction;
    public final FailTransactionAt failTransactionAt;
    public final FailCleanupAt failCleanupAt;
    public final boolean commit;
    public final boolean preClean;
    public final boolean cleanup;
    private SimpleEventBusLogger LOGGER;

    public Permutation(List<Command> transaction,
                       FailTransactionAt failTransactionAt,
                       FailCleanupAt failCleanupAt,
                       boolean commit) {
        this(transaction, failTransactionAt, failCleanupAt, commit, true, false);
    }

    public Permutation(List<Command> transaction,
                       FailTransactionAt failTransactionAt,
                       FailCleanupAt failCleanupAt,
                       boolean commit,
                       boolean preClean,
                       boolean cleanup) {
        this.transaction = transaction;
        this.failTransactionAt = failTransactionAt;
        this.failCleanupAt = failCleanupAt;
        this.commit = commit;
        this.preClean = preClean;
        this.cleanup = cleanup;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Run txn ");
        sb.append(transaction);
        sb.append(", abort ");
        sb.append(failTransactionAt);
        sb.append(' ');
        switch (failTransactionAt) {
            case BeforeATRPending:
            case BeforeStagingCommand1LeavePending:
            case BeforeStagingCommand2LeavePending:
            case BeforeStagingCommand3LeavePending:
            case BeforeATRCommitLeavePending:
                sb.append("pre-commit, leaving lost txn in PENDING");
                break;
            case BeforeStagingCommand1LeaveAborted:
            case BeforeStagingCommand2LeaveAborted:
            case BeforeStagingCommand3LeaveAborted:
            case BeforeATRCommitLeaveAborted:
                sb.append("pre-commit, leaving lost txn in ABORTED");
                break;
            case BeforeCommitOrRollbackCommand1:
            case BeforeCommitOrRollbackCommand2:
            case BeforeCommitOrRollbackCommand3:
            case BeforeATRCompleteOrRolledBack:
                if (commit) {
                    sb.append("after commit, leaving lost txn in COMMITTED");
                } else {
                    sb.append("after rollback, leaving lost txn in ABORTED");
                }
                break;
        }
        if (failCleanupAt == FailCleanupAt.DoNotFailCleanup) {
            sb.append(", cleanup once");
        } else {
            sb.append(", abort 1 st cleanup ");
            sb.append(failCleanupAt);
        }
        return sb.toString();
    }

    public void runAndAssertSuccess(Cluster cluster,
                                    Collection collection,
                                    Span span) {
        runAndAssertSuccess(cluster, collection, TestUtils.defaultConfig(null).build(), span);
    }

    public void runAndAssertSuccess(Cluster cluster,
                                    Collection collection,
                                    TransactionConfig config,
                                    Span span) {
        LOGGER = new SimpleEventBusLogger(cluster.environment().eventBus());

        transaction.forEach(txn -> txn.LOGGER = LOGGER);

        SpanWrapper sw = TestUtils.from(span);
        LOGGER.info("Starting: " + toString());

        if (preClean) {
            // Start with a clean setup
            LOGGER.info("Wiping all existing ATRs");
            TestUtils.cleanupBefore(collection);
        }
        else {
            LOGGER.info("Leaving existing ATRs alone");
        }

        try (Transactions transactions = Transactions.create(cluster, config)) {

            TransactionMock mock = TestUtils.prepareTest(transactions);

            AttemptStates expectedStateOfLostTxn = configureTransactionMock(mock);

            setupPreTxn(collection);

            TransactionAttempt lastAttempt = runTxn(collection, span, transactions);

            checkDocsPostTxn(collection, sw, transactions, cluster.environment().transcoder());

            performCleanup(cluster, collection, span, sw, transactions, expectedStateOfLostTxn, lastAttempt);

            cleanupPostTest(collection);
        }
    }

    private void cleanupPostTest(Collection collection) {
        if (cleanup) {
            // Cleanup after test.  Not that important so we don't try to do it on failures too.
            transaction.forEach(cmd -> {
                try {
                    collection.remove(cmd.docId);
                } catch (DocumentNotFoundException err) {
                }
            });
        }
        else {
            LOGGER.info("Skipping post-test cleanup to leave lost txn");
        }
    }

    private void performCleanup(Cluster cluster, Collection collection, Span span, SpanWrapper sw,
                                Transactions transactions, AttemptStates expectedStateOfLostTxn,
                                TransactionAttempt lastAttempt) {
        Mono<Integer> error = Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
        ClusterData cd = new ClusterData(cluster);
        Transcoder transcoder = cluster.environment().transcoder();

        CleanerMock attempt1 = new CleanerMock(transactions.config(), cd);

        Command cmd = null;
        switch (failCleanupAt) {
            case BeforeCleaningOnGetCommand1: {
                attempt1.beforeDocGet = (id) -> {
                    return Mono.error(new RuntimeException());
                };
                break;
            }
            case BeforeCleaningCommand1: {
                cmd = transaction.get(0);
                break;
            }
            case BeforeCleaningCommand2:
                if (transaction.size() >= 2) {
                    cmd = transaction.get(1);
                }
                break;
            case BeforeCleaningCommand3:
                if (transaction.size() >= 2) {
                    cmd = transaction.get(2);
                }
                break;
            case BeforeATRRemove:
                attempt1.beforeAtrRemove = () -> error;
                break;
        }

        if (cmd != null) {
            switch (expectedStateOfLostTxn) {
                case COMMITTED:
                    cmd.abortCleanupTxnWasCommitted(attempt1);
                    break;
                case ABORTED:
                    cmd.abortCleanupTxnWasAborted(attempt1);
                    break;
                default:
                    fail("Should not get here, trying to abort cleanup at a point that won't trigger");
            }
        }


        // If fails BeforeATRPending, don't have anything to clean up
        if (failTransactionAt != FailTransactionAt.BeforeATRPending) {
            ATREntry atrEntry = TestUtils.atrEntryForAttempt(collection, lastAttempt, transactions.config(), span);

            assertEquals(expectedStateOfLostTxn, atrEntry.state());

            LOGGER.info("ATR state is " + atrEntry.state() + " as expected");

            if (cleanup) {
                if (failCleanupAt != FailCleanupAt.DoNotFailCleanup) {
                    // Cleanup the txn once.  This may or may not fail.

                    LOGGER.info("Making 1st cleanup attempt, will abort at " + failCleanupAt);

                    try {
                        List<LogDefer> log = attempt1.cleanupATREntry(collection.reactive(),
                                atrEntry.atrId(),
                                atrEntry.attemptId(),
                                atrEntry,
                                false)
                                .block()
                                .logs();

                        List<LogDefer> warningsOrErrors = log.stream()
                                .filter(l -> l.level() == Event.Severity.WARN || l.level() == Event.Severity.ERROR)
                                .collect(Collectors.toList());

                        if (log.size() == 0 || warningsOrErrors.size() == 0) {
                            log.forEach(l -> LOGGER.info("cleanup: " + l.toString()));
                        }

                        // Should always have some logging
                        assertTrue(log.size() > 0);

                        // Something should have gone wrong and we should have at least one line of bad trace
                        assertTrue(warningsOrErrors.size() > 0);
                    } catch (RuntimeException err) {
                    }
                }

                // Cleanup the txn a second time.  This should succeed.
                CleanerMock attempt2 = new CleanerMock(transactions.config(), cd);

                LOGGER.info("Making 2nd cleanup attempt which should succeed");

                try {
                    TransactionCleanupAttempt cleanupResult = attempt2.cleanupATREntry(collection.reactive(),
                            atrEntry.atrId(),
                            atrEntry.attemptId(),
                            atrEntry,
                            false)
                            .block();

                    assertTrue(cleanupResult.success());
                } catch (RuntimeException err) {
                }

                // Check txn is cleaned up
                // Can only cleanup properly once reach COMMIT/ABORT stages
                if (expectedStateOfLostTxn == AttemptStates.COMMITTED) {
                    for (int i = 0; i < transaction.size(); i++) {
                        transaction.get(i).assertCommitted(collection, sw, transactions.config(), transcoder);
                    }
                }
            }

        }
        else {
            LOGGER.info("Skipping cleanup, leaving lost txn");

        }

        if (cleanup) {
            List<ATREntry> atrEntriesAfterCleanup =
                    TestUtils.allAtrEntries(collection, transactions.config(), span).collectList().block();
            if (0 != atrEntriesAfterCleanup.size()) {
                assertEquals(0, atrEntriesAfterCleanup.size());
            }
        }
    }

    private TransactionAttempt runTxn(Collection collection, Span span, Transactions transactions) {
        // Run the txn
        LOGGER.info("Running transaction " + transaction);

        TransactionAttempt lastAttempt = null;
        try {
            TransactionResult result = transactions.run((ctx) -> {

                for (int i = 0; i < transaction.size(); i++) {
                    Command cmd = transaction.get(i);
                    ctx.logger().info(ctx.attemptId(),
                            "Executing transaction command " + cmd + " " + (i + 1) + " of " + transaction.size());

                    try {
                        cmd.execute(ctx, collection);
                    } catch (RuntimeException err) {
                        // Check the right operation failed
                        int expected = -1;

                        switch (failTransactionAt) {
                            case BeforeStagingCommand1LeavePending:
                                expected = 0;
                                break;
                            case BeforeStagingCommand2LeavePending:
                                expected = 1;
                                break;
                            case BeforeStagingCommand3LeavePending:
                                expected = 2;
                                break;
                        }

                        if (expected != -1 && i != expected) {
                            fail("Txn should have failed " + failTransactionAt + " but failed on command " + (i + 1));
                        }

                        throw err;
                    }
                }

                switch (failTransactionAt) {
                    case BeforeATRCommitLeavePending:
                        ctx.logger().info(ctx.attemptId(), "Failing deliberately at BeforeATRCommitLeavePending");

                        throw new AbortedAsRequestedNoRollbackNoCleanup();

                    case BeforeATRCommitLeaveAborted:
                        ctx.logger().info(ctx.attemptId(), "Failing deliberately at BeforeATRCommitLeaveAborted");

                        throw new AttemptExpired(TestUtils.getContext(ctx), false);
                }

                if (commit) {
                    ctx.commit();
                } else {
                    ctx.rollback();
                }
            });

            // Txn should not succeed
            fail();
        } catch (TransactionFailed err) {
            lastAttempt = err.result().attempts().get(err.result().attempts().size() - 1);
        }
        return lastAttempt;
    }

    private void setupPreTxn(Collection collection) {
        // Insert any required docs
        LOGGER.info("Inserting any required docs");

        for (int i = 0; i < transaction.size(); i++) {
            transaction.get(i).prepareBeforeTxn(collection);
        }
    }

    private void checkDocsPostTxn(Collection collection, SpanWrapper sw, Transactions transactions, Transcoder transcoder) {
        LOGGER.info("Txn failed, as expected.  Checking docs in expected state");

        switch (failTransactionAt) {
            case BeforeATRPending: // drop-through
            case BeforeStagingCommand1LeavePending:
                for (int i = 0; i < transaction.size(); i++) {
                    LOGGER.info("Checking doc " + i + " is in correct state");
                    transaction.get(i).assertPreStaged(collection, sw, transactions.config(), transcoder);
                }
                break;

            case BeforeStagingCommand2LeavePending: {
                int crossover = 1;
                for (int i = 0; i < crossover; i++) {
                    LOGGER.info("Checking doc " + i + " is in correct state");
                    transaction.get(i).assertStagedButNotCommitted(collection, sw, transactions.config(), transcoder);
                }
                for (int i = crossover; i < transaction.size(); i++) {
                    LOGGER.info("Checking doc " + i + " is in correct state");
                    transaction.get(i).assertPreStaged(collection, sw, transactions.config(), transcoder);
                }
                break;
            }

            case BeforeStagingCommand3LeavePending: {
                int crossover = Math.min(2, transaction.size());
                for (int i = 0; i < crossover; i++) {
                    LOGGER.info("Checking doc " + i + " is in correct state");
                    transaction.get(i).assertStagedButNotCommitted(collection, sw, transactions.config(), transcoder);
                }
                for (int i = crossover; i < transaction.size(); i++) {
                    LOGGER.info("Checking doc " + i + " is in correct state");
                    transaction.get(i).assertPreStaged(collection, sw, transactions.config(), transcoder);
                }
                break;
            }

            case BeforeATRCommitLeavePending: // drop-through
            case BeforeCommitOrRollbackCommand1: {
                for (int i = 0; i < transaction.size(); i++) {
                    LOGGER.info("Checking doc " + i + " is in correct state");
                    transaction.get(i).assertStagedButNotCommitted(collection, sw, transactions.config(), transcoder);
                }
                break;
            }

            case BeforeCommitOrRollbackCommand2: {
                int crossover = Math.min(1, transaction.size());
                for (int i = 0; i < crossover; i++) {
                    LOGGER.info("Checking doc " + i + " is in correct state");
                    if (commit) {
                        transaction.get(i).assertCommitted(collection, sw, transactions.config(), transcoder);
                    } else {
                        transaction.get(i).assertRolledBack(collection, sw, transactions.config(), transcoder);
                    }
                }
                for (int i = crossover; i < transaction.size(); i++) {
                    LOGGER.info("Checking doc " + i + " is in correct state");
                    transaction.get(i).assertStagedButNotCommitted(collection, sw, transactions.config(), transcoder);
                }
                break;
            }
            case BeforeCommitOrRollbackCommand3: {
                int crossover = Math.min(2, transaction.size());
                for (int i = 0; i < crossover; i++) {
                    LOGGER.info("Checking doc " + i + " is in correct state");
                    if (commit) {
                        transaction.get(i).assertCommitted(collection, sw, transactions.config(), transcoder);
                    } else {
                        transaction.get(i).assertRolledBack(collection, sw, transactions.config(), transcoder);
                    }
                }
                for (int i = crossover; i < transaction.size(); i++) {
                    LOGGER.info("Checking doc " + i + " is in correct state");
                    transaction.get(i).assertStagedButNotCommitted(collection, sw, transactions.config(), transcoder);
                }
                break;
            }

            case BeforeATRCompleteOrRolledBack: {
                for (int i = 0; i < transaction.size(); i++) {
                    LOGGER.info("Checking doc " + i + " is in correct state");
                    if (commit) {
                        transaction.get(i).assertCommitted(collection, sw, transactions.config(), transcoder);
                    } else {
                        transaction.get(i).assertRolledBack(collection, sw, transactions.config(), transcoder);
                    }
                }
                break;
            }
        }
    }

    private AttemptStates configureTransactionMock(TransactionMock mock) {
        Mono<Integer> error = Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());

        // Check we have the desired form of lost txn
        AttemptStates expectedStateOfLostTxn = AttemptStates.PENDING;

        switch (failTransactionAt) {
            case BeforeATRPending:
                mock.beforeAtrPending = (ctx) -> error;
                break;
            case BeforeStagingCommand1LeavePending: {
                Command cmd1 = transaction.get(0);
                cmd1.failDuringPendingLeavePending(mock);
                break;
            }
            case BeforeStagingCommand2LeavePending:
                if (transaction.size() >= 2) {
                    Command cmd = transaction.get(1);
                    cmd.failDuringPendingLeavePending(mock);
                }
                break;
            case BeforeStagingCommand3LeavePending:
                if (transaction.size() >= 3) {
                    Command cmd = transaction.get(2);
                    cmd.failDuringPendingLeavePending(mock);
                }
                break;
            case BeforeATRCommitLeavePending:
                break;
            case BeforeStagingCommand1LeaveAborted: {
                expectedStateOfLostTxn = AttemptStates.ABORTED;
                Command cmd1 = transaction.get(0);
                cmd1.failDuringPendingLeaveAborted(mock);
                break;
            }
            case BeforeStagingCommand2LeaveAborted:
                expectedStateOfLostTxn = AttemptStates.ABORTED;
                if (transaction.size() >= 2) {
                    Command cmd = transaction.get(1);
                    cmd.failDuringPendingLeaveAborted(mock);
                }
                break;
            case BeforeStagingCommand3LeaveAborted:
                expectedStateOfLostTxn = AttemptStates.ABORTED;
                if (transaction.size() >= 3) {
                    Command cmd = transaction.get(2);
                    cmd.failDuringPendingLeaveAborted(mock);
                }
                break;
            case BeforeATRCommitLeaveAborted:
                expectedStateOfLostTxn = AttemptStates.ABORTED;
                break;
            case BeforeCommitOrRollbackCommand1: {
                Command cmd = transaction.get(0);
                if (commit) {
                    expectedStateOfLostTxn = AttemptStates.COMMITTED;
                    cmd.failDuringCommit(mock);
                } else {
                    expectedStateOfLostTxn = AttemptStates.ABORTED;
                    cmd.failDuringRollback(mock);
                }
                break;
            }
            case BeforeCommitOrRollbackCommand2:
                expectedStateOfLostTxn = AttemptStates.COMMITTED;
                if (transaction.size() >= 2) {
                    Command cmd = transaction.get(1);
                    if (commit) {
                        expectedStateOfLostTxn = AttemptStates.COMMITTED;
                        cmd.failDuringCommit(mock);
                    } else {
                        expectedStateOfLostTxn = AttemptStates.ABORTED;
                        cmd.failDuringRollback(mock);
                    }
                }
                break;
            case BeforeCommitOrRollbackCommand3:
                expectedStateOfLostTxn = AttemptStates.COMMITTED;
                if (transaction.size() >= 3) {
                    Command cmd = transaction.get(2);
                    if (commit) {
                        expectedStateOfLostTxn = AttemptStates.COMMITTED;
                        cmd.failDuringCommit(mock);
                    } else {
                        expectedStateOfLostTxn = AttemptStates.ABORTED;
                        cmd.failDuringRollback(mock);
                    }
                }
                break;
            case BeforeATRCompleteOrRolledBack:
                if (commit) {
                    expectedStateOfLostTxn = AttemptStates.COMMITTED;
                    mock.beforeAtrComplete = (ctx) -> error;
                } else {
                    expectedStateOfLostTxn = AttemptStates.ABORTED;
                    mock.beforeAtrRolledBack = (ctx) -> error;
                }
                break;
        }
        return expectedStateOfLostTxn;
    }
}
