package com.couchbase.Tests.Transactions.BasicTests;

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

import static org.junit.Assert.assertTrue;

public class simpleUpdate extends transactionTests {
    String docId ;
    JsonObject docContent = null;
    JsonObject updateContent = null;
    List<String> docKeys;

    public simpleUpdate(TxnClient.conn_info  conn_info, ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub txnstub, String hostname, String testname, ClusterConfigure clusterConfigure){
        super(conn_info,txnstub,hostname,null,testname,clusterConfigure);
    }

    public void createData(){
        docId = UUID.randomUUID().toString();
        docKeys = new ArrayList<>();
        docKeys.add(docId);
        docContent = JsonObject.create().put(Strings.CONTENT_NAME, Strings.DEFAULT_CONTENT_VALUE);
        updateContent = JsonObject.create().put(Strings.CONTENT_NAME, Strings.UPDATED_CONTENT_VALUE);
    }


    public void runTests(){
        clusterConfigure.initializecluster(true);
        for(int i =0;i<txncommit.length;i++){
            executeTests(txncommit[i]);
        }

    }

    public void executeTests(boolean txncommit){
        createData();

        TxnClient.TransactionsFactoryCreateResponse factory =
                txnstub.transactionsFactoryCreate(createDefaultTransactionsFactory()
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

        TxnClient.TransactionGenericResponse update =
                txnstub.transactionUpdate(TxnClient.TransactionUpdateRequest.newBuilder()
                        .setTransactionRef(transactionRef)
                        .setDocId(docId)
                        .setContentJson(updateContent.toString())
                        .build());
        logger.info("Verify for updateContent : "+updateContent);
        txnUtils.verifyDocuments(docKeys,null,true,hostname);

        if(txncommit){
            TxnClient.TransactionGenericResponse commit =
                    txnstub.transactionCommit(TxnClient.TransactionGenericRequest.newBuilder()
                            .setTransactionRef(transactionRef)
                            .build());
            assertTrue(commit.getSuccess());
            txnUtils.verifyDocuments(docKeys,updateContent,true,hostname);
        }

        assertTrue(txnstub.transactionsFactoryClose(TxnClient.TransactionsFactoryCloseRequest.newBuilder()
                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                .build()).getSuccess());
        logger.info("Completed Simple Update with commit : "+txncommit);
    }
}

