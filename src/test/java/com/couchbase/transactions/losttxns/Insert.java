package com.couchbase.transactions.losttxns;

import com.couchbase.client.core.error.DocumentNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Insert extends Command {
    @Override
    public String toString() {
        return "Insert";
    }

    private String bp = "Insert " + docId + ":";

    @Override
    public void failDuringPendingLeavePending(TransactionMock mock) {
        mock.beforeStagedInsert = (ctx, id) -> {
            if (id.equals(docId)) {
                return Command.ERROR_LEAVE_PENDING;
            } else {
                return Mono.just(0);
            }
        };
    }

    @Override
    public void failDuringPendingLeaveAborted(TransactionMock mock) {
        mock.beforeStagedInsert = (ctx, id) -> {
            if (id.equals(docId)) {
                return Mono.error(new AttemptExpired(ctx, false));
            } else {
                return Mono.just(0);
            }
        };
    }

    @Override
    public void prepareBeforeTxn(Collection collection) {
        LOGGER.info("Insert removing doc " + docId + " (very unlikely to exist, just making sure)");

        try {
            collection.remove(docId);
        }
        catch (DocumentNotFoundException err) {
        }
    }

    @Override
    public void failDuringCommit(TransactionMock mock) {
        mock.beforeDocCommitted = (ctx, id) -> {
            if (id.equals(docId)) {
//                // LostTxnsCleanupPermutationsTest.LOGGER.info("Insert failing before committing doc " + docId);
                return Command.ERROR_LEAVE_PENDING;
            } else {
                return Mono.just(0);
            }
        };
    }

    @Override
    public void failDuringRollback(TransactionMock mock) {
        mock.beforeRollbackDeleteInserted = (ctx, id) -> {
            if (id.equals(docId)) {
//                // LostTxnsCleanupPermutationsTest.LOGGER.info("Insert failing before rolling back doc " + docId);
                return Command.ERROR_LEAVE_PENDING;
            } else {
                return Mono.just(0);
            }
        };
    }

    @Override
    public void abortCleanupTxnWasCommitted(CleanerMock mock) {
        mock.beforeCommitDoc = (id) -> {
            if (id.equals(docId)) {
                return Command.ERROR_LEAVE_PENDING;
            } else {
                return Mono.just(0);
            }
        };
    }

    @Override
    public void abortCleanupTxnWasAborted(CleanerMock mock) {
        mock.beforeRemoveDoc = (id) -> {
            if (id.equals(docId)) {
                return Command.ERROR_LEAVE_PENDING;
            } else {
                return Mono.just(0);
            }
        };
    }

    @Override
    public void execute(AttemptContext ctx, Collection collection) {
        JsonObject content = JsonObject.create().put("VALUE", "TXN");

        ctx.insert(collection, docId, content);
    }

    @Override
    public void assertPreStaged(Collection collection, SpanWrapper span, TransactionConfig config, Transcoder transcoder) {
        assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
        LOGGER.info(bp + "Doc does not exist as expected");
    }

    @Override
    public void assertStagedButNotCommitted(Collection collection, SpanWrapper span, TransactionConfig config, Transcoder transcoder) {
        Optional<TransactionGetResult> doc =
                DocumentGetter.justGetDoc(collection.reactive(),  config,  docId,  span, transcoder).block();
        assertTrue(doc.isPresent());
        assertTrue(doc.get().links().isDocumentInTransaction());
        assertEquals(0, doc.get().contentAs(JsonObject.class).getNames().size());
        LOGGER.info(bp + "Doc exists with empty body and has staged content, as expected");
    }

    @Override
    public void assertCommitted(Collection collection, SpanWrapper span, TransactionConfig config, Transcoder transcoder) {
        Optional<TransactionGetResult> doc =
                DocumentGetter.justGetDoc(collection.reactive(),  config,  docId,  span, transcoder).block();
        assertTrue(doc.isPresent());
        assertFalse(doc.get().links().isDocumentInTransaction());
        assertEquals("TXN", doc.get().contentAs(JsonObject.class).getString("VALUE"));
        LOGGER.info(bp + "Doc exists with body set and no staged content, as expected");
    }

    @Override
    public void assertRolledBack(Collection collection, SpanWrapper span, TransactionConfig config, Transcoder transcoder) {
        Optional<TransactionGetResult> doc =
                DocumentGetter.justGetDoc(collection.reactive(),  config,  docId,  span, transcoder).block();
        assertFalse(doc.isPresent());
        LOGGER.info(bp + "Doc does not exist as expected");
    }
}
