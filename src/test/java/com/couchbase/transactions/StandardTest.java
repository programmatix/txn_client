/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.  All rights reserved.
 */

package com.couchbase.transactions;

import com.couchbase.Constants.Strings;
import com.couchbase.Logging.LogUtil;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.manager.bucket.*;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.grpc.protocol.ResumableTransactionServiceGrpc;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.transactions.components.DocumentGetter;
import com.couchbase.transactions.log.SimpleEventBusLogger;
import com.couchbase.transactions.tracing.TracingUtils;
import com.couchbase.transactions.tracing.TracingWrapper;
import com.couchbase.transactions.util.DocValidator;
import com.couchbase.transactions.util.SharedTestState;
import com.couchbase.transactions.util.TransactionFactoryBuilder;
import com.couchbase.transactions.util.ResultValidator;
import com.couchbase.transactions.util.TransactionFactoryWrapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static com.couchbase.Constants.Strings.INITIAL_CONTENT_VALUE;
import static com.couchbase.Constants.Strings.UPDATED_CONTENT_VALUE;
import static com.couchbase.grpc.protocol.TxnClient.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * All unit tests that don't go into a more specialised file.
 */
public class StandardTest {
    private static SharedTestState shared;
    private static Collection collection;

    @BeforeAll
    public static void beforeOnce() {
        shared = SharedTestState.create();
        collection = shared.collection();
    }

    @BeforeEach
    public void beforeEach() {
        // Was reading all and removing all ATRs to avoid the flush, but it really screws up the OpenTracing output
        //        collection.bucketManager().flush();
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
    public void createEmptyTransaction() {
        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            wrap.empty();
            TransactionResultObject result = wrap.txnClose();
            ResultValidator.assertEmptyTxn(result, TxnClient.AttemptStates.COMPLETED);
        }
    }


    @Test
    public void commitEmptyTransactionold() {
        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            wrap.empty();
            TransactionResultObject result = wrap.commitAndClose();
            ResultValidator.assertEmptyTxn(result, TxnClient.AttemptStates.COMPLETED);

        }

    }

    @Test
    public void rollbackEmptyTransaction() {
        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            wrap.empty();
            TransactionResultObject result = wrap.rollbackAndClose();
            ResultValidator.assertEmptyTxn(result, TxnClient.AttemptStates.ROLLED_BACK);
        }
    }


    @Disabled("disabling for now as hangs")
    @Test
    public void rollbackCommittedEmptyTransaction() {
        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            String docId = TestUtils.docId(collection, 0);
            JsonObject docContent = JsonObject.create().put(Strings.CONTENT_NAME, Strings.DEFAULT_CONTENT_VALUE);

            wrap.insert(docId, docContent.toString());
            wrap.commit();
            TransactionResultObject result = wrap.rollbackExpectingFailurendClose();

            //TODO attach codes rather than exception Names
            assertEquals(result.getExceptionName(), "com.couchbase.transactions.error.attempts.AttemptException");
            ResultValidator.assertRolledBackInSingleAttempt(collection, result);
        }
    }

    @Test
    public void oneInsertCommitted() {
        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            String docId = TestUtils.docId(collection, 0);
            JsonObject docContent = JsonObject.create().put(Strings.CONTENT_NAME, Strings.DEFAULT_CONTENT_VALUE);

            wrap.insert(docId, docContent.toString());

            DocValidator.assertInsertedDocIsStaged(collection, docId);

            TransactionResultObject result = wrap.commitAndClose();

            ResultValidator.assertCompletedInSingleAttempt(collection, result);
            DocValidator.assertDocExistsAndNotInTransactionAndContentEquals(collection, docId, docContent);
        }
    }
//
//    @Test
//    public void tranasctionOnClosedTransactionsShouldFail() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope));
//
//            transactions.close();
//
//            assertThrows(IllegalStateException.class, () -> {
//                TransactionResult result = transactions.run((ctx) -> {
//                });
//            });
//
//            // Double-close is fine
//            transactions.close();
//        }
//    }

    @Test
    public void oneInsertRolledBack() {
        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            String docId = TestUtils.docId(collection, 0);
            JsonObject docContent = JsonObject.create().put(Strings.CONTENT_NAME, Strings.DEFAULT_CONTENT_VALUE);

            wrap.insert(docId, docContent.toString());
            TransactionResultObject result = wrap.rollbackAndClose();

            ResultValidator.assertRolledBackInSingleAttempt(collection, result);
        }
    }

    @Test
    public void twoInsertsCommitted() {
        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            String docId = TestUtils.docId(collection, 0);
            String docId2 = TestUtils.docId(collection, 1);
            JsonObject docContent1 = JsonObject.create().put(Strings.CONTENT_NAME, 1);
            JsonObject docContent2 = JsonObject.create().put(Strings.CONTENT_NAME, 2);

            wrap.insert(docId, docContent1.toString());
            wrap.insert(docId2, docContent2.toString());

            DocValidator.assertInsertedDocIsStaged(collection, docId);
            DocValidator.assertInsertedDocIsStaged(collection, docId2);

            TransactionResultObject result = wrap.commitAndClose();

            ResultValidator.assertCompletedInSingleAttempt(collection, result);
            DocValidator.assertDocExistsAndNotInTransactionAndContentEquals(collection, docId, docContent1);
            DocValidator.assertDocExistsAndNotInTransactionAndContentEquals(collection, docId2, docContent2);
        }
    }

    @Test
    public void oneReplaceCommitted() {
        String docId = TestUtils.docId(collection, 0);
        JsonObject initial = JsonObject.create().put(Strings.CONTENT_NAME, INITIAL_CONTENT_VALUE);
        JsonObject after = JsonObject.create().put(Strings.CONTENT_NAME, UPDATED_CONTENT_VALUE);
        collection.upsert(docId, initial);

        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            wrap.replace(docId, after.toString());

            DocValidator.assertReplacedDocIsStagedAndContentEquals(collection, docId, initial, after);

            TransactionResultObject result = wrap.commitAndClose();

            ResultValidator.assertCompletedInSingleAttempt(collection, result);
            DocValidator.assertDocExistsAndNotInTransactionAndContentEquals(collection, docId, after);
        }
    }

    @Test
    public void oneDeleteCommitted() {
        String docId = TestUtils.docId(collection, 0);
        JsonObject initial = JsonObject.create().put(Strings.CONTENT_NAME, INITIAL_CONTENT_VALUE);
        collection.insert(docId, initial);
        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            wrap.remove(docId);

            DocValidator.assertDocExistsdAndContentEquals(collection, docId, initial);

            TransactionResultObject result = wrap.commitAndClose();

            assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
            ResultValidator.assertCompletedInSingleAttempt(collection, result);
            assertEquals(1, result.getMutationTokensSize());
        }
    }


    @Test
    public void docXattrsEmptyAfterTxn() {
        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
                String docId = TestUtils.docId(collection, 0);
                JsonObject initial = JsonObject.create().put("val", 1);

                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
                    return ctx.insert(collection.reactive(), docId, initial).flatMap(ignore -> ctx.commit());
                }, TestUtils.defaultPerConfig(scope));
                TransactionResult r = result.block();

                assertEquals(1, TestUtils.numAtrs(collection, transactions.config(), span));
                TransactionGetResult doc = DocumentGetter.justGetDoc(collection.reactive(), transactions.config(),
                        docId, TestUtils.from(scope), cluster.environment().transcoder()).block().get();
                assertFalse(doc.links().atrId().isPresent());
                assertFalse(doc.links().stagedAttemptId().isPresent());
                assertFalse(doc.links().stagedContent().isPresent());
                assertFalse(doc.links().isDocumentInTransaction());
                checkLogRedactionIfEnabled(r, docId);
            }
        }
    }



//    @Test
//    public void oneUpdateImplicit() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//                    JsonObject content = doc.contentAs(JsonObject.class);
//                    content.put("val", 2);
//                    ctx.replace(doc, content);
//
//                    assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                }, TestUtils.defaultPerConfig(scope));
//                TestUtils.assertCompletedIn1Attempt(transactions.config(), result, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertTrue(2 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                TestUtils.assertAtrEntryDocs(collection, result, null, Arrays.asList(docId), null,
//                    transactions.config(), span);
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//

    @Test
    public void oneUpdateRolledBack() {
        String docId = TestUtils.docId(collection, 0);
        JsonObject initial = JsonObject.create().put(Strings.CONTENT_NAME, INITIAL_CONTENT_VALUE);
        JsonObject after = JsonObject.create().put(Strings.CONTENT_NAME, UPDATED_CONTENT_VALUE);
        collection.upsert(docId, initial);

        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            wrap.replace(docId, after.toString());

            TransactionResultObject result = wrap.rollbackAndClose();

            ResultValidator.assertRolledBackInSingleAttempt(collection, result);
            DocValidator.assertDocExistsAndNotInTransactionAndContentEquals(collection, docId, initial);
        }
    }

    @Test
    public void oneDeleteRolledBack() {
        String docId = TestUtils.docId(collection, 0);
        JsonObject initial = JsonObject.create().put(Strings.CONTENT_NAME, INITIAL_CONTENT_VALUE);
        collection.insert(docId, initial);
        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            wrap.remove(docId);

            DocValidator.assertDocExistsdAndContentEquals(collection, docId, initial);

            TransactionResultObject result = wrap.rollbackAndClose();

            DocValidator.assertDocExistsdAndContentEquals(collection, docId, initial);
            ResultValidator.assertRolledBackInSingleAttempt(collection, result);
            assertEquals(1, result.getMutationTokensSize());
        }
    }


//
//    @Test
//    public void allTimestampsWrittenAfterRollback() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, initial);
//                    ctx.rollback();
//                }, TestUtils.defaultPerConfig(scope));
//                ATREntry entry = TestUtils.atrEntryForAttempt(collection, result.attempts().get(0).attemptId(),
//                    transactions.config(), span);
//
//                assertFalse(entry.timestampCommitMsecs().isPresent());
//                assertFalse(entry.timestampCompleteMsecs().isPresent());
//                assertTrue(entry.timestampStartMsecs().isPresent());
//                assertTrue(entry.timestampRollBackMsecs().isPresent());
//                assertTrue(entry.timestampRolledBackMsecs().isPresent());
//                assertTrue(entry.timestampRollBackMsecs().get() >= entry.timestampStartMsecs().get());
//                assertTrue(entry.timestampRolledBackMsecs().get() >= entry.timestampRollBackMsecs().get());
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//    @Test
//    public void allTimestampsWrittenAfterCommit() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, initial);
//                    ctx.commit();
//                }, TestUtils.defaultPerConfig(scope));
//                ATREntry entry = TestUtils.atrEntryForAttempt(collection, result.attempts().get(0).attemptId(),
//                    transactions.config(), span);
//
//                assertTrue(entry.timestampCommitMsecs().isPresent());
//                assertTrue(entry.timestampCompleteMsecs().isPresent());
//                assertTrue(entry.timestampStartMsecs().isPresent());
//                assertFalse(entry.timestampRollBackMsecs().isPresent());
//                assertFalse(entry.timestampRolledBackMsecs().isPresent());
//                assertTrue(entry.timestampCommitMsecs().get() >= entry.timestampStartMsecs().get());
//                assertTrue(entry.timestampCompleteMsecs().get() >= entry.timestampCommitMsecs().get());
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//
//
//
//    @Test
//    public void oneUpdateRolledBackOnFailAsync() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                collection.insert(docId, JsonObject.create().put("val", 1));
//                collection.insert(docId2, JsonObject.create().put("val", 2));
//
//                Mono<TransactionResult> result = transactions.reactive((ctx) -> {
//                    return ctx.get(collection.reactive(), docId).flatMap(doc -> {
//                        JsonObject content = doc.contentAs(JsonObject.class).put("val", 3);
//                        return ctx.replace(doc, content);
//                    }).flatMap(ignore -> ctx.get(collection.reactive(), docId2)).flatMap(doc -> {
//                        if (doc.contentAs(JsonObject.class).getInt("val") == 2) {
//                            return ctx.rollback();
//                        } else {
//                            return Mono.just(doc);
//                        }
//                    }).flatMap(ignore -> ctx.commit());
//                }, TestUtils.defaultPerConfig(scope));
//
//                TransactionResult r = result.block();
//                TestUtils.assertRolledBackIn1Attempt(transactions.config(), r, collection, scope.span(),
//                    cluster.environment().transcoder());
//                assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                assertTrue(2 == collection.get(docId2).contentAs(JsonObject.class).getInt("val"));
//                checkLogRedactionIfEnabled(r, docId);
//            }
//        }
//    }
//


    @Test
    public void twoUpdatesCommitted() {
        String docId1 = TestUtils.docId(collection, 0);
        String docId2 = TestUtils.docId(collection, 0);
        JsonObject initial1 = JsonObject.create().put(Strings.CONTENT_NAME, 1);
        JsonObject initial2 = JsonObject.create().put(Strings.CONTENT_NAME, 2);
        JsonObject after1 = JsonObject.create().put(Strings.CONTENT_NAME, 3);
        JsonObject after2 = JsonObject.create().put(Strings.CONTENT_NAME, 4);
        collection.upsert(docId1, initial1);
        collection.upsert(docId2, initial2);

        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            wrap.replace(docId1, after1.toString());
            wrap.replace(docId2, after2.toString());

            DocValidator.assertReplacedDocIsStagedAndContentEquals(collection, docId1, initial1, after1);
            DocValidator.assertReplacedDocIsStagedAndContentEquals(collection, docId2, initial2, after2);

            TransactionResultObject result = wrap.commitAndClose();

            ResultValidator.assertCompletedInSingleAttempt(collection, result);
            DocValidator.assertDocExistsAndNotInTransactionAndContentEquals(collection, docId1, after1);
            DocValidator.assertDocExistsAndNotInTransactionAndContentEquals(collection, docId2, after2);
        }
    }

    @Test
    public void twoUpdatesRolledBack() {
        String docId1 = TestUtils.docId(collection, 0);
        String docId2 = TestUtils.docId(collection, 0);
        JsonObject initial1 = JsonObject.create().put(Strings.CONTENT_NAME, 1);
        JsonObject initial2 = JsonObject.create().put(Strings.CONTENT_NAME, 2);
        JsonObject after1 = JsonObject.create().put(Strings.CONTENT_NAME, 3);
        JsonObject after2 = JsonObject.create().put(Strings.CONTENT_NAME, 4);
        collection.upsert(docId1, initial1);
        collection.upsert(docId2, initial2);

        try (TransactionFactoryWrapper wrap = TransactionFactoryWrapper.create(shared)) {
            wrap.replace(docId1, after1.toString());
            wrap.replace(docId2, after2.toString());

            TransactionResultObject result = wrap.rollbackAndClose();

            ResultValidator.assertRolledBackInSingleAttempt(collection, result);
            ResultValidator.dumpLogs(result);
            DocValidator.assertDocExistsAndNotInTransactionAndContentEquals(collection, docId1, initial1);
            DocValidator.assertDocExistsAndNotInTransactionAndContentEquals(collection, docId2, initial2);
        }
    }


    //
//
//    @Test
//    public void casChangesOnReplace() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                collection.insert(docId, JsonObject.create().put("val", 1));
//
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc1 = ctx.getOptional(collection, docId).get();
//                    long origCas = doc1.cas();
//                    JsonObject content = doc1.contentAs(JsonObject.class);
//                    content.put("val", 3);
//                    doc1 = ctx.replace(doc1, content);
//                    assertNotEquals(origCas, doc1.cas());
//                    assertNotEquals(origCas, 0);
//                    assertNotEquals(doc1.cas(), 0);
//                    ctx.commit();
//                }, TestUtils.defaultPerConfig(scope));
//                checkLogRedactionIfEnabled(result, docId);
//            }
//        }
//    }
//


//    @Test
//    public void deleteDocNormallyXattrs() {
//        String docId = TestUtils.docId(collection, 0);
//
//        collection.mutateIn(docId, Arrays.asList(MutateInSpec.upsert("foo", "bar").xattr()),
//            MutateInOptions.mutateInOptions().storeSemantics(StoreSemantics.INSERT));
//
//        LookupInResult result = collection.lookupIn(docId, Arrays.asList(LookupInSpec.get("foo").xattr()));
//
//        assertEquals(result.contentAs(0, String.class), "bar");
//
//        collection.remove(docId);
//
//        assertThrows(DocumentNotFoundException.class, () -> collection.get(docId));
//
//        collection.insert(docId, JsonObject.create());
//
//        // This test indicates that a normal remove is sufficient to remove all xattrs from a doc
//        LookupInResult result2 = collection.lookupIn(docId, Arrays.asList(LookupInSpec.get("foo").xattr()));
//        assertFalse(result2.exists(0));
//    }
//
//    // TXNJ-24
//    @Test
//    public void tnj24() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//
//                String docId1 = TestUtils.docId(collection, 0);
//                String docId2 = TestUtils.docId(collection, 1);
//                String docId3 = TestUtils.docId(collection, 2);
//                String docId4 = TestUtils.docId(collection, 3);
//                String docId5 = TestUtils.docId(collection, 4);
//                collection.insert(docId1, JsonObject.create().put("val", 1));
//                collection.insert(docId2, JsonObject.create().put("val", 1));
//                collection.insert(docId3, JsonObject.create().put("val", 1));
//                collection.insert(docId4, JsonObject.create().put("val", 1));
//                collection.insert(docId5, JsonObject.create().put("val", 1));
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc1 = ctx.get(collection, docId1);
//                    TransactionGetResult doc2 = ctx.get(collection, docId2);
//                    TransactionGetResult doc3 = ctx.get(collection, docId3);
//                    TransactionGetResult doc4 = ctx.get(collection, docId4);
//                    TransactionGetResult doc5 = ctx.get(collection, docId5);
//                    ctx.remove(doc1);
//                    ctx.remove(doc2);
//                    ctx.remove(doc3);
//                    ctx.remove(doc4);
//                    ctx.remove(doc5);
//                    ctx.rollback();
//                }, TestUtils.defaultPerConfig(scope));
//            }
//        }
//    }
//
//    // TXNJ-33
//    @Test
//    public void createTwoTransactionsObjects() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            Transactions transactions1 = Transactions.create(cluster, TestUtils.defaultConfig(scope));
//            Transactions transactions2 = Transactions.create(cluster, TestUtils.defaultConfig(scope));
//            transactions1.close();
//            transactions2.close();
//        }
//    }
//
//    @Test
//    void getOrError() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                collection.insert(docId, JsonObject.create().put("val", 1));
//
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc1 = ctx.getOptional(collection, docId).get();
//                    long origCas = doc1.cas();
//                    JsonObject content = doc1.contentAs(JsonObject.class);
//                    content.put("val", 3);
//                    doc1 = ctx.replace(doc1, content);
//                    assertNotEquals(origCas, doc1.cas());
//                    assertNotEquals(origCas, 0);
//                    assertNotEquals(doc1.cas(), 0);
//                    ctx.commit();
//                }, TestUtils.defaultPerConfig(scope));
//            }
//        }
//    }
//
//    @Test
//    public void updateStagesBackupMetadata() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                LookupInResult preTxnResult = collection.lookupIn(docId,
//                    Arrays.asList(LookupInSpec.get("$document").xattr()));
//                JsonObject preTxn = preTxnResult.contentAsObject(0);
//
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//                    JsonObject content = doc.contentAs(JsonObject.class);
//                    content.put("val", 2);
//                    ctx.replace(doc, content);
//
//                    LookupInResult r = collection
//                        .lookupIn(docId, Arrays.asList(
//                            LookupInSpec.get(ATR_ID).xattr(),
//                            LookupInSpec.get(TRANSACTION_ID).xattr(),
//                            LookupInSpec.get(ATTEMPT_ID).xattr(),
//                            LookupInSpec.get(STAGED_DATA).xattr(),
//                            LookupInSpec.get(ATR_BUCKET_NAME).xattr(),
//                            LookupInSpec.get(ATR_COLL_NAME).xattr(),
//                            // For {BACKUP_FIELDS}
//                            LookupInSpec.get(TRANSACTION_RESTORE_PREFIX_ONLY).xattr(),
//                            LookupInSpec.get(TYPE).xattr(),
//                            LookupInSpec.get("$document").xattr(),
//                            LookupInSpec.get("")));
//
//                    LookupInResult r2 = collection
//                        .lookupIn(docId, Arrays.asList(
//                            LookupInSpec.get("txn.dummy").xattr(),
//                            LookupInSpec.get("txn").xattr(),
//                            LookupInSpec.get("$document").xattr(),
//                            LookupInSpec.get("")));
//
//
//                    collection
//                        .lookupIn(docId, Arrays.asList(
//                            LookupInSpec.get("txn.field1").xattr(),
//                            LookupInSpec.get("txn.field2").xattr(),
//                            LookupInSpec.get("$document").xattr(),
//                            LookupInSpec.get("")));
//
//                    assertTrue(1 == collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//
//                    TransactionGetResult docRaw = DocumentGetter.justGetDoc(collection.reactive(),
//                        transactions.config(),
//                        docId, TestUtils.from(scope), cluster.environment().transcoder()).block().get();
//
//                    assertEquals(preTxn.getString("CAS"), docRaw.links().casPreTxn().get());
//                    assertEquals(preTxn.getString("revid"), docRaw.links().revidPreTxn().get());
//                    assertEquals(preTxn.getLong("exptime"), docRaw.links().exptimePreTxn().get());
//                    assertEquals("replace", docRaw.links().op().get());
//
//                }, TestUtils.defaultPerConfig(scope));
//            }
//        }
//    }
//
//    @Test
//    public void removeStagesBackupMetadata() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                LookupInResult preTxnResult = collection.lookupIn(docId,
//                    Arrays.asList(LookupInSpec.get("$document").xattr()));
//                JsonObject preTxn = preTxnResult.contentAsObject(0);
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//                    ctx.remove(doc);
//
//                    TransactionGetResult docRaw = DocumentGetter.justGetDoc(collection.reactive(),
//                        transactions.config(),
//                        docId, TestUtils.from(scope), cluster.environment().transcoder()).block().get();
//
//                    assertEquals(preTxn.getString("CAS"), docRaw.links().casPreTxn().get());
//                    assertEquals(preTxn.getString("revid"), docRaw.links().revidPreTxn().get());
//                    assertEquals(preTxn.getLong("exptime"), docRaw.links().exptimePreTxn().get());
//                    assertEquals("remove", docRaw.links().op().get());
//
//                }, TestUtils.defaultPerConfig(scope));
//            }
//        }
//    }
//
//    @Test
//    void preExistingStagedInsertFoundOneFailureTryingToRemove() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock1 = new TransactionMock();
//            transactionMock1.beforeAtrCommit = (ctx) -> {
//                return Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
//            };
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(transactionMock1), null, null))) {
//
//                // Create lost txn
//                try {
//                    transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                    });
//                    fail();
//                } catch (TransactionFailed err) {
//                }
//            }
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeRemovingDocDuringStagedInsert = (ctx, id) -> {
//                if (count.incrementAndGet() <= 1) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            // Fake that the lost txn has been cleaned up by removing the ATR entry
//            TestUtils.cleanupBefore(collection);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                });
//
//            }
//
//            assertEquals(2, count.get());
//        }
//    }
//
//    @Test
//    void preExistingStagedInsertFoundSeveralFailuresTryingToRemove() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock1 = new TransactionMock();
//            transactionMock1.beforeAtrCommit = (ctx) -> {
//                return Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
//            };
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(transactionMock1), null, null))) {
//
//                // Create lost txn
//                try {
//                    transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                    });
//                    fail();
//                } catch (TransactionFailed err) {
//                }
//            }
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeRemovingDocDuringStagedInsert = (ctx, id) -> {
//                if (count.incrementAndGet() <= 5) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            // Fake that the lost txn has been cleaned up by removing the ATR entry
//            TestUtils.cleanupBefore(collection);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                });
//
//            }
//
//            assertTrue(count.get() > 0);
//        }
//    }
//
//    @Test
//    void preExistingStagedInsertFoundOneFailureTryingToGet() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock1 = new TransactionMock();
//            transactionMock1.beforeAtrCommit = (ctx) -> {
//                return Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
//            };
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(transactionMock1), null, null))) {
//
//                // Create lost txn
//                try {
//                    transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                    });
//                    fail();
//                } catch (TransactionFailed err) {
//                }
//            }
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeGetDocInExistsDuringStagedInsert = (ctx, id) -> {
//                if (count.incrementAndGet() <= 1) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            // Fake that the lost txn has been cleaned up by removing the ATR entry
//            TestUtils.cleanupBefore(collection);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                });
//
//            }
//
//            assertTrue(count.get() > 0);
//        }
//    }
//
//    @Test
//    void preExistingStagedInsertFoundSeveralFailuresTryingToGet() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock1 = new TransactionMock();
//            transactionMock1.beforeAtrCommit = (ctx) -> {
//                return Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
//            };
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(transactionMock1), null, null))) {
//
//                // Create lost txn
//                try {
//                    transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                    });
//                    fail();
//                } catch (TransactionFailed err) {
//                }
//            }
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeGetDocInExistsDuringStagedInsert = (ctx, id) -> {
//                if (count.incrementAndGet() <= 5) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            // Fake that the lost txn has been cleaned up by removing the ATR entry
//            TestUtils.cleanupBefore(collection);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                });
//            }
//
//            assertTrue(count.get() > 0);
//        }
//    }
//
//    @Test
//    void checkATREntryForBlockingDocFailsOnce() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock1 = new TransactionMock();
//            transactionMock1.beforeAtrCommit = (ctx) -> {
//                return Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
//            };
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(transactionMock1), null, null))) {
//
//                // Create lost txn
//                try {
//                    transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                    });
//                    fail();
//                } catch (TransactionFailed err) {
//                }
//            }
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeCheckATREntryForBlockingDoc = (ctx, id) -> {
//                if (count.incrementAndGet() <= 1) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            // Fake that the lost txn has been cleaned up by removing the ATR entry
//            TestUtils.cleanupBefore(collection);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                });
//
//                assertTrue(1 == 1);
//            }
//
//            assertTrue(count.get() > 0);
//        }
//    }
//
//    @Test
//    void checkATREntryForBlockingDocFailsRepeatedly() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock1 = new TransactionMock();
//            transactionMock1.beforeAtrCommit = (ctx) -> {
//                return Mono.error(new AbortedAsRequestedNoRollbackNoCleanup());
//            };
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    .testFactories(new TestAttemptContextFactory(transactionMock1), null, null))) {
//
//                // Create lost txn
//                try {
//                    transactions.run((ctx) -> {
//                        ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                    });
//                    fail();
//                } catch (TransactionFailed err) {
//                }
//            }
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeCheckATREntryForBlockingDoc = (ctx, id) -> {
//                if (count.incrementAndGet() <= 5) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            // Fake that the lost txn has been cleaned up by removing the ATR entry
//            TestUtils.cleanupBefore(collection);
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    ctx.insert(collection, docId, JsonObject.create().put("val", "TXN-1"));
//                });
//            }
//
//            assertTrue(count.get() > 0);
//        }
//    }
//
//
//    @Test
//    void getDocFailsRepeatedly() throws Exception {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            final String docId = TestUtils.docId(collection, 0);
//
//            TransactionMock transactionMock2 = new TransactionMock();
//            AtomicInteger count = new AtomicInteger(0);
//            transactionMock2.beforeDocGet = (ctx, id) -> {
//                if (count.incrementAndGet() <= 5) {
//                    return Mono.error(new TemporaryFailureException(null));
//                } else {
//                    return Mono.just(1);
//                }
//            };
//
//            collection.upsert(docId, JsonObject.create().put("val", "INITIAL"));
//
//            try (Transactions transactions = Transactions.create(cluster,
//                TestUtils.defaultConfig(scope)
//                    // .logDirectly(Event.Severity.VERBOSE)
//                    .testFactories(new TestAttemptContextFactory(transactionMock2), null, null))) {
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    ctx.getOptional(collection, docId);
//                });
//            }
//
//            assertTrue(count.get() > 0);
//        }
//    }
//
//    @Test
//    public void multipleInsertsExpire() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//
//                TransactionResult result = transactions.run((ctx) -> {
//                    for (int i = 0; i < 10; i++) {
//                        String docId = TestUtils.docId(collection, i);
//                        JsonObject initial = JsonObject.create().put("val", 1);
//                        ctx.insert(collection, docId, initial);
//                    }
//
//                    ctx.commit();
//                });
//            }
//        }
//    }
//
//    @Test
//    public void doubleCommit() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                try {
//                    TransactionResult result = transactions.run((ctx) -> {
//                        TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//                        JsonObject content = doc.contentAs(JsonObject.class);
//                        content.put("val", 2);
//                        ctx.replace(doc, content);
//
//                        ctx.commit();
//                        ctx.commit();
//                        fail();
//                    }, TestUtils.defaultPerConfig(scope));
//
//                } catch (TransactionFailed e) {
//                    // The transaction will actually have committed, but the application has a logic bug that it needs
//                    // to fix.  Throwing seems the best course of action.
//                    assertTrue(e.getCause() instanceof AttemptException);
//                }
//
//                assertEquals(2, (int) collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                TestUtils.assertOneAtrEntry(collection, AttemptStates.COMPLETED, transactions.config(), span);
//            }
//        }
//    }
//
//    @Test
//    public void doubleRollback() {
//        try (Scope scope = tracer.buildSpan(TestUtils.testName()).asChildOf(span).startActive(true)) {
//            try (Transactions transactions = Transactions.create(cluster, TestUtils.defaultConfig(scope))) {
//                String docId = TestUtils.docId(collection, 0);
//                JsonObject initial = JsonObject.create().put("val", 1);
//                collection.insert(docId, initial);
//
//                try {
//                    TransactionResult result = transactions.run((ctx) -> {
//                        TransactionGetResult doc = ctx.getOptional(collection, docId).get();
//                        JsonObject content = doc.contentAs(JsonObject.class);
//                        content.put("val", 2);
//                        ctx.replace(doc, content);
//
//                        ctx.rollback();
//                        ctx.rollback();
//                        fail();
//                    }, TestUtils.defaultPerConfig(scope));
//
//                } catch (TransactionFailed e) {
//                    assertTrue(e.getCause() instanceof AttemptException);
//                }
//
//                assertEquals(1, (int) collection.get(docId).contentAs(JsonObject.class).getInt("val"));
//                TestUtils.assertOneAtrEntry(collection, AttemptStates.ROLLED_BACK, transactions.config(), span);
//            }
//        }
//    }
//
//    @Test
//    public void casParsing() {
//        assertEquals(1539336197457L, ActiveTransactionRecord.parseMutationCAS("0x000058a71dd25c15"));
//    }


    // TXNJ-33
    @Test
    public void createTwoTransactionsObjects() {
        TransactionFactoryWrapper wrap1 = TransactionFactoryWrapper.create(shared);
        TransactionFactoryWrapper wrap2 = TransactionFactoryWrapper.create(shared);
        wrap1.commitAndClose();
        wrap2.commitAndClose();
    }


}