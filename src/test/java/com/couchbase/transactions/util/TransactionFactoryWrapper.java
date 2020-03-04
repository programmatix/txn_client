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

import com.couchbase.grpc.protocol.ResumableTransactionServiceGrpc;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.transactions.TestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

class Hook {

}

/**
 * A simple wrapper to take care of:
 * <p>
 * 1. Creating a Transactions factory on the server
 * 2. Creating a ResumableTransaction on the server
 * 3. Closing the Transactions factory on the server when it's finished with
 * <p>
 * All operations are blocking, and may throw.
 */
public class TransactionFactoryWrapper implements AutoCloseable {
    private final ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub;
    private final TxnClient.TransactionsFactoryCreateResponse factory;
    private final TxnClient.TransactionCreateResponse create;

    public static TransactionFactoryWrapper create(ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub) {
        return new TransactionFactoryWrapper(stub, Collections.EMPTY_LIST);
    }

    public static TransactionFactoryWrapper create(SharedTestState shared) {
        return new TransactionFactoryWrapper(shared.stub(), Collections.EMPTY_LIST);
    }

    TransactionFactoryWrapper(ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub,
                              List<TxnClient.Hook> hooks) {
        this.stub = stub;

        // It's cleaner (though not essential) to create one Transactions per test. Can do the debug hooks without
        // accidentally interfering with other tests.
        TxnClient.TransactionsFactoryCreateRequest.Builder defConfig =
            TestUtils.createDefaultTransactionsFactory()
                .addAllHook(hooks);

        factory = stub.transactionsFactoryCreate(defConfig.build());
        assertTrue(factory.getSuccess());

        create = stub.transactionCreate(TxnClient.TransactionCreateRequest.newBuilder()
            .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
            .build());
        assertTrue(create.getSuccess());
    }

    public String transactionRef() {
        return create.getTransactionRef();
    }

    /**
     * Tell the server to create an empty transaction. Empty transaction does not perform any actions but justs
     * commits after its creation
     */
    public TxnClient.TransactionGenericResponse empty() {
        TxnClient.TransactionGenericResponse response =
            stub.transactionEmpty(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef())
                .build());
        assertTrue(response.getSuccess());

        return response;
    }


    /**
     * Tell the server to insert a document in this resumable transaction.
     */
    public TxnClient.TransactionGenericResponse insert(String docId, String content) {
        TxnClient.TransactionGenericResponse response =
            stub.transactionInsert(TxnClient.TransactionInsertRequest.newBuilder()
                .setTransactionRef(transactionRef())
                .setDocId(docId)
                .setContentJson(content)
                .setExpectedResult(TxnClient.ExpectedResult.SUCCESS)
                .build());

        assertTrue(response.getSuccess());

        return response;
    }

    /**
     * Tell the server to replace a document in this resumable transaction.
     * <p>
     * Assert that the operation succeeds.
     */
    public TxnClient.TransactionGenericResponse replace(String docId, String content) {
        TxnClient.TransactionGenericResponse response =
            stub.transactionUpdate(TxnClient.TransactionUpdateRequest.newBuilder()
                .setTransactionRef(transactionRef())
                .setDocId(docId)
                .setContentJson(content)
                .setExpectedResult(TxnClient.ExpectedResult.SUCCESS)
                .build());

        assertTrue(response.getSuccess());

        return response;
    }

    /**
     * Tell the server to replace a document in this resumable transaction.
     * <p>
     * Assert that the operation fails, e.g. throws.  The transaction may not may not
     * retry or fail as a result, that is not checked here.
     */
    public TxnClient.TransactionGenericResponse replaceExpectFailure(String docId, String content) {
        TxnClient.TransactionGenericResponse response =
            stub.transactionUpdate(TxnClient.TransactionUpdateRequest.newBuilder()
                .setTransactionRef(transactionRef())
                .setDocId(docId)
                .setContentJson(content)
                .setExpectedResult(TxnClient.ExpectedResult.THROWS)
                .build());

        assertFalse(response.getSuccess());

        return response;
    }

    /**
     * Tell the server to delete a document in this resumable transaction.
     */
    public TxnClient.TransactionGenericResponse remove(String docId) {
        TxnClient.TransactionGenericResponse response =
            stub.transactionDelete(TxnClient.TransactionDeleteRequest.newBuilder()
                .setTransactionRef(transactionRef())
                .setDocId(docId)
                .setExpectedResult(TxnClient.ExpectedResult.SUCCESS)
                .build());

        assertTrue(response.getSuccess());

        return response;
    }


    /**
     * Tell the server to commit a resumable transaction.
     */
    public TxnClient.TransactionGenericResponse commit() {
        TxnClient.TransactionGenericResponse response =
            stub.transactionCommit(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef())
                .build());

        assertTrue(response.getSuccess());

        return response;
    }

    /**
     * Tell the server to rollback a resumable transaction.
     */
    public TxnClient.TransactionGenericResponse rollback() {
        TxnClient.TransactionGenericResponse response =
            stub.transactionRollback(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef())
                .build());

        assertTrue(response.getSuccess());

        return response;
    }

    /**
     * Tell the server to rollback a resumable transaction.
     */
    public TxnClient.TransactionGenericResponse rollbackWithFailure() {
        TxnClient.TransactionGenericResponse response =
            stub.transactionRollback(TxnClient.TransactionGenericRequest.newBuilder()
                .setTransactionRef(transactionRef())
                .build());

        assertFalse(response.getSuccess());

        return response;
    }

    /**
     * Tell the server to commit a resumable transaction, then close it.
     */
    public TxnClient.TransactionResultObject commitAndClose() {
        commit();

        return stub.transactionClose(TxnClient.TransactionGenericRequest.newBuilder()
            .setTransactionRef(transactionRef())
            .build());
    }

    /**
     * Tell the server to rollback a resumable transaction, then close it.
     */
    public TxnClient.TransactionResultObject rollbackAndClose() {
        rollback();

        return stub.transactionClose(TxnClient.TransactionGenericRequest.newBuilder()
            .setTransactionRef(transactionRef())
            .build());
    }

    /**
     * Tell the server to rollback a resumable transaction, then close it.
     */
    public TxnClient.TransactionResultObject rollbackExpectingFailurendClose() {
        rollbackWithFailure();

        return stub.transactionClose(TxnClient.TransactionGenericRequest.newBuilder()
            .setTransactionRef(transactionRef())
            .build());
    }

    /**
     * Just close the resumable transaction.
     */
    public TxnClient.TransactionResultObject txnClose() {
        return stub.transactionClose(TxnClient.TransactionGenericRequest.newBuilder()
            .setTransactionRef(transactionRef())
            .build());
    }

    /**
     * Get the state of the transaction
     */
    public TxnClient.TransactionState state() {
        return stub.getTransactionState(TxnClient.TransactionGenericRequest.newBuilder()
            .setTransactionRef(transactionRef())
            .build());
    }


    @Override
    public void close() {
        assertTrue(stub.transactionsFactoryClose(TxnClient.TransactionsFactoryCloseRequest.newBuilder()
            .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
            .build()).getSuccess());
    }
}
