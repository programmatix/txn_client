package com.couchbase.Tests.Transactions.Hooks;

import com.couchbase.Constants.Strings;
import com.couchbase.Couchbase.Cluster.ClusterConfigure;
import com.couchbase.Tests.Transactions.Utils.txnUtils;
import com.couchbase.Tests.Transactions.transactionTests;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.grpc.protocol.ResumableTransactionServiceGrpc;
import com.couchbase.grpc.protocol.TxnClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class failsbeforeCommit extends transactionTests {
    String docId ;
    JsonObject docContent = null;
    List<String> docKeys = new ArrayList<>();


    public failsbeforeCommit(TxnClient.conn_info  conn_info, ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub txnstub, String hostname, String testname, ClusterConfigure clusterConfigure){
        super(conn_info,txnstub,hostname,null,testname,clusterConfigure);
    }

    public void configureTests(){
       clusterConfigure.initializecluster(true);
        docId = UUID.randomUUID().toString();
        docKeys.add(docId);
        docContent = JsonObject.create().put(Strings.CONTENT_NAME, Strings.DEFAULT_CONTENT_VALUE);
    }


    public void runTests(){
        configureTests();
        executeTests();
    }

    public void executeTests(){
        TxnClient.TransactionsFactoryCreateResponse factory =
                txnstub.transactionsFactoryCreate(createDefaultTransactionsFactory("MAJORITY")
                    .addHook(TxnClient.Hook.newBuilder()
                        .setHookPoint(TxnClient.HookPoint.BEFORE_ATR_COMMIT)
                        .setHookCondition(TxnClient.HookCondition.ALWAYS)
                        .setHookAction(TxnClient.HookAction.FAIL_NO_ROLLBACK)
                        .build())
                .build());

        assertTrue(factory.getSuccess());

        TxnClient.TransactionCreateResponse create =
                txnstub.transactionCreate(TxnClient.TransactionCreateRequest.newBuilder()
                        .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                        .build());

        assertTrue(create.getSuccess());
        String transactionRef = create.getTransactionRef();

        TxnClient.TransactionGenericResponse insert =
                txnstub.transactionInsert(TxnClient.TransactionInsertRequest.newBuilder()
                        .setTransactionRef(transactionRef)
                        .setDocId(docId)
                        .setContentJson(docContent.toString())
                        .build());

        txnUtils.verifyDocuments(docKeys,null,true,hostname);

        TxnClient.TransactionGenericResponse commit =
                txnstub.transactionCommit(TxnClient.TransactionGenericRequest.newBuilder()
                        .setTransactionRef(transactionRef)
                        .build());

        assertFalse(commit.getSuccess());
        txnUtils.verifyDocuments(docKeys,null,true,hostname);


        assertTrue(txnstub.transactionsFactoryClose(TxnClient.TransactionsFactoryCloseRequest.newBuilder()
                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                .build()).getSuccess());

}
}

