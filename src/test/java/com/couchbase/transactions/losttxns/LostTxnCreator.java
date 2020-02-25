package com.couchbase.transactions.losttxns;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.transactions.config.TransactionConfig;
import io.opentracing.Span;

import java.util.List;

/**
 * For testing, create lost txns in various situations.
 */
public class LostTxnCreator {
    private LostTxnCreator() {}

    public static void create(FailTransactionAt fta,
                              List<Command> txn,
                              Cluster cluster,
                              Collection collection,
                              TransactionConfig config,
                              Span span) {
        Permutation lost1 = new Permutation(txn,
                fta,
                FailCleanupAt.DoNotFailCleanup,
                true,
                false,
                false);
        lost1.runAndAssertSuccess(cluster, collection,  config, span);
    }
}
