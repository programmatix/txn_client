package com.couchbase.transactions.losttxns;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.codec.Transcoder;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.transactions.AttemptContext;
import com.couchbase.transactions.TransactionGetResult;
import com.couchbase.transactions.cleanup.CleanerMock;
import com.couchbase.transactions.components.DocumentGetter;
import com.couchbase.transactions.config.TransactionConfig;
import com.couchbase.transactions.error.internal.AttemptExpired;
import com.couchbase.transactions.support.SpanWrapper;
import com.couchbase.transactions.util.TransactionMock;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.junit.Assert.*;

public class Remove extends Command {
    @Override
    public String toString() {
        return "Remove";
    }

    @Override
    public void failDuringPendingLeavePending(TransactionMock mock) {
        mock.beforeStagedRemove = (ctx, id) -> {
            if (id.equals(docId)) {
                return ERROR_LEAVE_PENDING;
            } else {
                return Mono.just(0);
            }
        };
    }

    @Override
    public void failDuringPendingLeaveAborted(TransactionMock mock) {
        mock.beforeStagedRemove = (ctx, id) -> {
            if (id.equals(docId)) {
                return Mono.error(new AttemptExpired(ctx, false));
            } else {
                return Mono.just(0);
            }
        };
    }

    @Override
    public void abortCleanupTxnWasCommitted(CleanerMock mock) {
        mock.beforeRemoveDocStagedForRemoval = (id) -> {
            if (id.equals(docId)) {
                return ERROR_LEAVE_PENDING;
            } else {
                return Mono.just(0);
            }
        };
    }

    @Override
    public void abortCleanupTxnWasAborted(CleanerMock mock) {
        mock.beforeRemoveLinks = (id) -> {
            if (id.equals(docId)) {
                return ERROR_LEAVE_PENDING;
            } else {
                return Mono.just(0);
            }
        };
    }

    @Override
    public void failDuringCommit(TransactionMock mock) {
        mock.beforeDocRemoved = (ctx, id) -> {
            if (id.equals(docId)) {
                // LostTxnsCleanupPermutationsTest.LOGGER.info("Remove failing before committing doc " + docId);
                return ERROR_LEAVE_PENDING;
            } else {
                return Mono.just(0);
            }
        };
    }

    @Override
    public void failDuringRollback(TransactionMock mock) {
        mock.beforeDocRolledBack = (ctx, id) -> {
            if (id.equals(docId)) {
                // LostTxnsCleanupPermutationsTest.LOGGER.info("Remove failing before rolling back doc " + docId);
                return ERROR_LEAVE_PENDING;
            } else {
                return Mono.just(0);
            }
        };
    }

    @Override
    public void execute(AttemptContext ctx, Collection collection) {
        TransactionGetResult doc = ctx.get(collection, docId);

        ctx.remove(doc);
    }

    @Override
    public void prepareBeforeTxn(Collection collection) {
        // LostTxnsCleanupPermutationsTest.LOGGER.info("Remove inserting initial doc at INITIAL " + docId);

        JsonObject content = JsonObject.create().put("VALUE", "INITIAL");

        collection.upsert(docId, content);
    }

    @Override
    public void assertPreStaged(Collection collection, SpanWrapper span, TransactionConfig config, Transcoder transcoder) {
        Optional<TransactionGetResult> doc =
                DocumentGetter.justGetDoc(collection.reactive(),  config,  docId,  span, transcoder).block();
        assertTrue(doc.isPresent());
        assertFalse(doc.get().links().isDocumentInTransaction());
        assertEquals("INITIAL", doc.get().contentAs(JsonObject.class).getString("VALUE"));
    }

    @Override
    public void assertStagedButNotCommitted(Collection collection, SpanWrapper span, TransactionConfig config, Transcoder transcoder) {
        Optional<TransactionGetResult> doc =
                DocumentGetter.justGetDoc(collection.reactive(),  config,  docId,  span, transcoder).block();
        assertTrue(doc.isPresent());
        assertTrue(doc.get().links().isDocumentInTransaction());
        assertEquals("INITIAL", doc.get().contentAs(JsonObject.class).getString("VALUE"));
    }

    @Override
    public void assertCommitted(Collection collection, SpanWrapper span, TransactionConfig config, Transcoder transcoder) {
        Optional<TransactionGetResult> doc =
                DocumentGetter.justGetDoc(collection.reactive(),  config,  docId,  span, transcoder).block();
        assertFalse(doc.isPresent());
    }

    @Override
    public void assertRolledBack(Collection collection, SpanWrapper span, TransactionConfig config, Transcoder transcoder) {
        Optional<TransactionGetResult> doc =
                DocumentGetter.justGetDoc(collection.reactive(),  config,  docId,  span, transcoder).block();
        assertTrue(doc.isPresent());
        assertFalse(doc.get().links().isDocumentInTransaction());
        assertEquals("INITIAL", doc.get().contentAs(JsonObject.class).getString("VALUE"));
    }
}
