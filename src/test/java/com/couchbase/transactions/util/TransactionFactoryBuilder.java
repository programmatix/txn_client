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

import java.util.ArrayList;

/**
 * Allows customizing the Transactions factory before building it.  E.g., to add hooks.
 */
public class TransactionFactoryBuilder {
    private final ArrayList<TxnClient.Hook> hooks = new ArrayList<>();
    private final ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub;

    public TransactionFactoryBuilder addHook(TxnClient.HookPoint hookPoint,
                                             TxnClient.HookCondition condition,
                                             int conditionParam,
                                             TxnClient.HookAction action) {
        hooks.add(TxnClient.Hook.newBuilder()
            .setHookPoint(hookPoint)
            .setHookCondition(condition)
            .setHookConditionValue(conditionParam)
            .setHookAction(action)
            .build());
        return this;
    }

    public TransactionFactoryBuilder addHook(TxnClient.Hook hook) {
        hooks.add(hook);
        return this;
    }

    private TransactionFactoryBuilder(ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub) {
        this.stub = stub;
    }

    static public TransactionFactoryBuilder create(ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub stub) {
        return new TransactionFactoryBuilder(stub);
    }

    static public TransactionFactoryBuilder create(SharedTestState shared) {
        return new TransactionFactoryBuilder(shared.stub());
    }

    public TransactionFactoryWrapper build() {
        return new TransactionFactoryWrapper(stub, hooks);
    }
}
