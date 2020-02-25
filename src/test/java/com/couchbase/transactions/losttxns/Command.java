package com.couchbase.transactions.losttxns;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.codec.Transcoder;
import com.couchbase.transactions.AttemptContext;
import com.couchbase.transactions.cleanup.CleanerMock;
import com.couchbase.transactions.config.TransactionConfig;
import com.couchbase.transactions.error.internal.AbortedAsRequestedNoRollbackNoCleanup;
import com.couchbase.transactions.log.SimpleEventBusLogger;
import com.couchbase.transactions.support.SpanWrapper;
import com.couchbase.transactions.util.TransactionMock;
import reactor.core.publisher.Mono;

import java.util.UUID;

public abstract class Command {
    // This many chars should be enough for uniqueness, and balances readability
    public String docId = UUID.randomUUID().toString().substring(0, 8);
    // The LOGGER is set after creation.
    public SimpleEventBusLogger LOGGER;

    public static final Mono<Integer> ERROR_LEAVE_PENDING = Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());

    abstract public void failDuringPendingLeavePending(TransactionMock mock);

    abstract public void failDuringPendingLeaveAborted(TransactionMock mock);

    abstract public void execute(AttemptContext ctx, Collection collection);

    public abstract void prepareBeforeTxn(Collection collection);

    public abstract void assertPreStaged(Collection collection, SpanWrapper span, TransactionConfig config, Transcoder transcoder);

    public abstract void assertStagedButNotCommitted(Collection collection, SpanWrapper span, TransactionConfig config, Transcoder transcoder);

    public abstract void assertCommitted(Collection collection, SpanWrapper span, TransactionConfig config, Transcoder transcoder);

    public abstract void assertRolledBack(Collection collection, SpanWrapper span, TransactionConfig config, Transcoder transcoder);

    public abstract void failDuringCommit(TransactionMock mock);

    public abstract void failDuringRollback(TransactionMock mock);

    /**
     * Configure cleanup so it will bail out when it tries to cleanup the result of this Command.
     *
     * The transaction reached Committed state (important as different routes are followed during cleanup).
     */
    public abstract void abortCleanupTxnWasCommitted(CleanerMock mock);

    /**
     * Configure cleanup so it will bail out when it tries to cleanup the result of this Command.
     *
     * The transaction reached Aborted state (important as different routes are followed during cleanup).
     */
    public abstract void abortCleanupTxnWasAborted(CleanerMock mock);
}
