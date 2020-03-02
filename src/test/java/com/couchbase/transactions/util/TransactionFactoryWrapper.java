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

import static org.junit.Assert.assertTrue;

/**
 * A simple wrapper to take care of:
 * 1. Creating a Transactions factory on the server
 * 2. Creating a ResumableTransaction on the server
 * 3. Closing the Transactions factory on the server when it's finished with
 */
public class TransactionFactoryWrapper implements AutoCloseable {
    private final ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub;
    private final TxnClient.TransactionsFactoryCreateResponse factory;
    private final TxnClient.TransactionCreateResponse create;

    public static TransactionFactoryWrapper create(ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub) {
        return new TransactionFactoryWrapper(stub);
    }

    private TransactionFactoryWrapper(ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub) {
        this.stub = stub;

        TxnClient.TransactionsFactoryCreateRequest.Builder defConfig =
            TestUtils.createDefaultTransactionsFactory();

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
     * Tell the server to insert a document in this resumable transaction.
     */
    public TxnClient.TransactionGenericResponse insert(String docId, String content) {
        TxnClient.TransactionGenericResponse response =
            stub.transactionInsert(TxnClient.TransactionInsertRequest.newBuilder()
                .setTransactionRef(transactionRef())
                .setDocId(docId)
                .setContentJson(content)
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
     * Tell the server to commit a resumable transaction, then close it.
     */
    public TxnClient.TransactionResultObject commitAndClose() {
        commit();

        return stub.transactionClose(TxnClient.TransactionGenericRequest.newBuilder()
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
