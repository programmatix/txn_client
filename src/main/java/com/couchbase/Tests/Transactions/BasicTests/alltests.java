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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

public class alltests   extends transactionTests {
    boolean[] transactionCommit;
    String[] durability;
    List<List<String>> op_type;
    String docId ;
    JsonObject docContent = null;
    JsonObject updateContent = null;
    List<String> docKeys;

    public alltests(TxnClient.conn_info  conn_info, ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub txnstub, String hostname, String testname, ClusterConfigure clusterConfigure){
        super(conn_info,txnstub,hostname,null,testname,clusterConfigure);
        transactionCommit = new boolean[]{true, false};
        durability = new String[]{"NONE","MAJORITY","MAJORITY_AND_PERSIST_TO_ACTIVE","PERSIST_TO_MAJORITY"};
        op_type =  asList(
               // asList("create")
              //  asList("create","update")
                asList("create","update","delete")
               /* asList("create","update","general_delete"),
                asList("create","general_update","delete"),
                asList("create","general_update","general_delete"),
                asList("general_create","update"),
                asList("general_create","update","delete"),
                asList("general_create","update","general_delete")
                */
        );
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
        for (int i = 0; i < durability.length; i++) {
            executeTests(durability[i]);
        }

    }

    public void executeTests(String durability) {
        logger.info("Executing test:" +testname);
            TxnClient.TransactionsFactoryCreateResponse factory =
                    txnstub.transactionsFactoryCreate(createDefaultTransactionsFactory(durability)
                            .build());
            assertTrue(factory.getSuccess());

            for(int i=0;i<transactionCommit.length;i++){
                boolean txncommit;
                for(int j=0;j<op_type.size();j++){
                    createData();
                    for(int k=0; k<op_type.get(j).size();k++){
                        if(op_type.get(j).size()!=1){
                            txncommit = transactionCommit[i];
                        }else{
                            txncommit = true;
                        }
                        logger.info("Will be executing:"+op_type.get(j).get(k));
                        switch (op_type.get(j).get(k)) {
                            case "create":
                                TxnClient.TransactionCreateResponse createtxn =
                                        txnstub.transactionCreate(TxnClient.TransactionCreateRequest.newBuilder()
                                                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                                                .build());
                                assertTrue(createtxn.getSuccess());

                                String transactionRef = createtxn.getTransactionRef();
                                TxnClient.TransactionGenericResponse insert =
                                        txnstub.transactionInsert(TxnClient.TransactionInsertRequest.newBuilder()
                                                .setTransactionRef(transactionRef)
                                                .setDocId(docId)
                                                .setContentJson(docContent.toString())
                                                .build());
                                txnUtils.verifyDocuments(docKeys,null,true,hostname);

                                if(txncommit){
                                    TxnClient.TransactionGenericResponse commit =
                                            txnstub.transactionCommit(TxnClient.TransactionGenericRequest.newBuilder()
                                                    .setTransactionRef(transactionRef)
                                                    .build());
                                    assertTrue(commit.getSuccess());
                                    txnUtils.verifyDocuments(docKeys,docContent,true,hostname);
                                }
                                break;
                            case "update":
                                TxnClient.TransactionCreateResponse updatetxn =
                                        txnstub.transactionCreate(TxnClient.TransactionCreateRequest.newBuilder()
                                                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                                                .build());
                                assertTrue(updatetxn.getSuccess());

                                String updatetransactionRef = updatetxn.getTransactionRef();
                                TxnClient.TransactionGenericResponse update =
                                        txnstub.transactionUpdate(TxnClient.TransactionUpdateRequest.newBuilder()
                                                .setTransactionRef(updatetransactionRef)
                                                .setDocId(docId)
                                                .setContentJson(updateContent.toString())
                                                .build());
                                if(txncommit){
                                    txnUtils.verifyDocuments(docKeys,docContent,true,hostname);
                                }
                                else{
                                    txnUtils.verifyDocuments(docKeys,null,true,hostname);
                                }


                                if(txncommit){
                                    TxnClient.TransactionGenericResponse commit =
                                            txnstub.transactionCommit(TxnClient.TransactionGenericRequest.newBuilder()
                                                    .setTransactionRef(updatetransactionRef)
                                                    .build());
                                    assertTrue(commit.getSuccess());
                                    txnUtils.verifyDocuments(docKeys,updateContent,true,hostname);
                                }
                                break;
                            case "delete":
                                TxnClient.TransactionCreateResponse deletetxn =
                                        txnstub.transactionCreate(TxnClient.TransactionCreateRequest.newBuilder()
                                                .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                                                .build());
                                assertTrue(deletetxn.getSuccess());

                                String deletetransactionRef = deletetxn.getTransactionRef();
                                TxnClient.TransactionGenericResponse delete =
                                        txnstub.transactionDelete(TxnClient.TransactionDeleteRequest.newBuilder()
                                                .setTransactionRef(deletetransactionRef)
                                                .setDocId(docId)
                                                .build());
                                if(txncommit){
                                    txnUtils.verifyDocuments(docKeys,updateContent,true,hostname);
                                }
                                else{
                                    txnUtils.verifyDocuments(docKeys,null,true,hostname);
                                }


                                if(txncommit){
                                    TxnClient.TransactionGenericResponse deletecommit =
                                            txnstub.transactionCommit(TxnClient.TransactionGenericRequest.newBuilder()
                                                    .setTransactionRef(deletetransactionRef)
                                                    .build());
                                    assertTrue(deletecommit.getSuccess());
                                    txnUtils.verifyDocuments(docKeys,null,false,hostname);
                                }
                                break;
                            default:
                                throw new IllegalStateException("Cannot handle this optType" + op_type.get(i).get(j));
                        }
                    }
                    logger.info("Completed Testcase:"+j);
                }
            }

            assertTrue(txnstub.transactionsFactoryClose(TxnClient.TransactionsFactoryCloseRequest.newBuilder()
                    .setTransactionsFactoryRef(factory.getTransactionsFactoryRef())
                    .build()).getSuccess());
            logger.info("Completed Simple Update with commit : "+txncommit);
        }
    }

