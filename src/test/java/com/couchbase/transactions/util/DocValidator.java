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

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.codec.Transcoder;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.transactions.TransactionGetResult;
import com.couchbase.transactions.components.DocumentGetter;
import com.couchbase.transactions.config.TransactionConfig;
import io.opentracing.Span;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Utility methods to validate that documents are in expected states.
 */
public class DocValidator {
    private DocValidator() {}

    public static void assertInsertedDocIsStaged(Collection collection,
                                                 String docId) {
        // TODO add all checks from TXNJ-125 branch
        // TODO check all metadata is in expected format
        GetResult get = collection.get(docId);
        // TXNJ-125: inserted doc will be there, but should be empty
        assertEquals(0, get.contentAsObject().size());
    }

    public static GetResult assertDocExistsAndNotInTransaction(Collection collection,
                                                          String docId) {
        // TODO check all metadata is in expected format
        GetResult get = collection.get(docId);
        assertNotEquals(0, get.contentAsObject().size());
        return get;
    }

    public static void assertDocExistsAndNotInTransactionAndContentEquals(Collection collection,
                                                                               String docId,
                                                                               JsonObject content) {
        GetResult result = assertDocExistsAndNotInTransaction(collection, docId);
        JsonObject fetchedContent = result.contentAsObject();
        assertEquals(fetchedContent, content);
    }

    public static boolean isDocInTxn(Collection collection, String id) {
        TransactionGetResult doc = DocumentGetter.getAsync(collection.reactive(), null, id, null,
            null, null).block().get();
        return doc.links().isDocumentInTransaction();
    }

}
