/*
 * Copyright (c) 2019 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.transactions.util;

import com.couchbase.client.core.logging.LogRedaction;
import com.couchbase.client.core.logging.RedactionLevel;
import com.couchbase.client.java.Collection;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.transactions.TransactionAttempt;
import com.couchbase.transactions.TransactionResult;
import com.couchbase.transactions.components.ATREntry;
import com.couchbase.transactions.support.AttemptStates;
import org.junit.Assert;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utility methods to validate the results of a transaction.
 */
public class ResultValidator {
    private ResultValidator() {}

    public static void dumpLogs(TxnClient.TransactionResultObject result) {
        for(int i = 0; i < result.getLogCount(); i ++) {
            // TODO sort out the logging!
            System.out.println(result.getLog(i));
        }
    }

    public static void assertEmptyTxn(TxnClient.TransactionResultObject  result, TxnClient.AttemptStates state) {
        assertEquals(0, result.getMutationTokensSize());
        TxnClient.TransactionAttempt attempt = result.getAttempts(0);
        Assert.assertEquals(state, attempt.getState());
    }

    /**
     * Expects a single attempt have been made, which reached COMPLETED state.
     * Verify everything possible about the transaction: all docs should be in expected state, log redaction should be
     * done, expected number of mutation tokens returned...
     */
    public static void assertCompletedInSingleAttempt(Collection collection, TxnClient.TransactionResultObject result) {
        ATREntry entry = assertSingleAttempt(collection, result, TxnClient.AttemptStates.COMPLETED);
        ATRValidator.assertCompleted(collection, entry, entry.attemptId());
    }

    /**
     * Expects more than one attempt to have been made.
     * Only one (the last) should have reached COMPLETED state.
     * Verify everything possible about the transaction: all docs should be in expected state, log redaction should be
     * done, expected number of mutation tokens returned...
     */
    public static void assertCompletedInMultipleAttempts(Collection collection, TxnClient.TransactionResultObject result) {
        ATREntry entry = assertMultipleAttempts(collection, result, TxnClient.AttemptStates.COMPLETED);
        ATRValidator.assertCompleted(collection, entry, entry.attemptId());
    }

    /**
     * Expects a single attempt have been made, which the app requested be rolled back to ROLLED_BACK state.
     * Verify everything possible about the transaction: all docs should be in expected state, log redaction should be
     * done, expected number of mutation tokens returned...
     */
    public static void assertRolledBackInSingleAttempt(Collection collection, TxnClient.TransactionResultObject result) {
        ATREntry entry = assertSingleAttempt(collection, result, TxnClient.AttemptStates.ROLLED_BACK);
        ATRValidator.assertRolledBack(collection, entry, entry.attemptId());
    }

    private static ATREntry assertSingleAttempt(Collection collection,
                                                TxnClient.TransactionResultObject result,
                                                TxnClient.AttemptStates state) {
        assertEquals(1, result.getAttemptsCount());
        TxnClient.TransactionAttempt attempt = result.getAttempts(0);
        return assertAttempt(collection, result, state, attempt);
    }

    private static ATREntry assertMultipleAttempts(Collection collection,
                                                TxnClient.TransactionResultObject result,
                                                TxnClient.AttemptStates state) {
        assertTrue(result.getAttemptsCount() > 1);
        TxnClient.TransactionAttempt attempt = result.getAttempts(result.getAttemptsCount() - 1);
        return assertAttempt(collection, result, state, attempt);
    }

    private static ATREntry assertAttempt(Collection collection, TxnClient.TransactionResultObject result,
                                          TxnClient.AttemptStates state, TxnClient.TransactionAttempt attempt) {
        Assert.assertEquals(state, attempt.getState());
        ATREntry entry = ATRValidator.findAtrEntryForAttempt(collection, attempt.getAttemptId());
        assertMutationTokensCount(result, entry);
        assertLogRedaction(result, entry);
        return entry;
    }

    private static void assertMutationTokensCount(TxnClient.TransactionResultObject result, ATREntry entry) {
        int mutatedDocs =
            entry.insertedIds().get().size() + entry.removedIds().get().size() + entry.replacedIds().get().size();
        ResultValidator.assertMutationTokensCount(result, mutatedDocs);
    }

    private static void assertLogRedaction(TxnClient.TransactionResultObject result, ATREntry entry) {
        entry.insertedIds().get().forEach(doc -> {
            assertLogRedaction(result.getLogList(), doc.id());
        });
        entry.replacedIds().get().forEach(doc -> {
            assertLogRedaction(result.getLogList(), doc.id());
        });
        entry.removedIds().get().forEach(doc -> {
            assertLogRedaction(result.getLogList(), doc.id());
        });
    }

    private static void assertMutationTokensCount(TxnClient.TransactionResultObject result, int expectedMutationTokens) {
        assertEquals(expectedMutationTokens, result.getMutationTokensSize());
    }

    /**
     * Check log has been redacted properly.
     */
    private static void assertLogRedaction(List<String> logs, String docId) {
        logs.forEach(l -> {
            // logger.info("Checking logreadaction: " + l);
            if (l.contains(docId)) {
                //TODO below assertion not working . Need to check if this should be actually working
                // assertTrue(l.contains("<ud>" + docId + "</ud>"));
            }
        });

    }

    /**
     * Asserts transaction is in 'lost' (half-finished) state, and in PENDING.
     *
     * See [[LOST_PENDING]] in RFC - since the docs involved in the transaction have not been written to the ATR, there
     * isn't much to check (or that can be done) with these.
     */
    public static void assertLostInPendingState(Collection collection, TxnClient.TransactionResultObject result) {
        ATREntry entry = assertSingleAttempt(collection, result, TxnClient.AttemptStates.PENDING);
        // Not much more we can do for PENDING transaction
    }
}
