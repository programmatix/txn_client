/*
 * Copyright (c) 2020 Couchbase, Inc.
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

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.codec.Transcoder;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.transactions.TestUtils;
import com.couchbase.transactions.TransactionAttempt;
import com.couchbase.transactions.TransactionResult;
import com.couchbase.transactions.Transactions;
import com.couchbase.transactions.atr.ATRIds;
import com.couchbase.transactions.components.ATREntry;
import com.couchbase.transactions.components.ActiveTransactionRecord;
import com.couchbase.transactions.config.TransactionConfig;
import com.couchbase.transactions.support.AttemptStates;
import io.opentracing.Span;
import org.junit.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Utility methods to validate that ATRs are in expected states.
 */
public class ATRValidator {
    private ATRValidator() {}

    public static ATREntry assertCompleted(Collection collection, String attemptId) {
        ATREntry entry = findAtrEntryForAttempt(collection, attemptId);
        Assert.assertEquals(AttemptStates.COMPLETED, entry.state());

        entry.insertedIds().get().stream().forEach(doc -> {
            DocValidator.assertDocIsCommitted(collection, doc.id());
        });
        entry.replacedIds().get().stream().forEach(doc -> {
            DocValidator.assertDocIsCommitted(collection, doc.id());
        });
        entry.removedIds().get().stream().forEach(doc ->
            assertThrows(DocumentNotFoundException.class, () -> collection.get(doc.id())));
        return entry;
    }


    public static Flux<ATREntry> allAtrEntries(Collection collection) {
        return Flux.fromIterable(ATRIds.allAtrs(ATRIds.NUM_ATRS_DEFAULT))

            .flatMap(atrId -> ActiveTransactionRecord.getAndTouchAtr(collection.reactive(), atrId, null, null, null)

                .flatMap(v -> {
                    if (v.isPresent()) return Mono.just(v.get());
                    else return Mono.empty();
                })
            )

            .flatMap(v -> Flux.fromIterable(v.entries()));
    }

    public static ATREntry findAtrEntryForAttempt(Collection collection, String attemptId) {
        return allAtrEntries(collection)
            .filter(v -> v.attemptId().equals(attemptId))
            .blockFirst();
    }

}
