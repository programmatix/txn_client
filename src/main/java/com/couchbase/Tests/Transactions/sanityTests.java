package com.couchbase.Tests.Transactions;

import com.couchbase.Constants.Strings;
import com.couchbase.Couchbase.Cluster.ClusterConfigure;
import com.couchbase.Tests.Transactions.Utils.txnUtils;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.grpc.protocol.txnGrpc;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class sanityTests extends transactionTests {

    private static List<String> txnKeys = new LinkedList<>();
    private static List<String> updateKeys = new LinkedList<>();
    private static List<String> deleteKeys = new LinkedList<>();
    private static JsonObject docContent = null;

    public sanityTests(TxnClient.conn_info  conn_info, txnGrpc.txnBlockingStub   txnstub, String hostname, String testname, ClusterConfigure clusterConfigure){
        super(conn_info,txnstub,hostname,testname,clusterConfigure);
    }

    public void configureTests(){
        String masterHost = clusterConfigure.initializecluster(true);
        System.out.println("Completed Cluster configuring for testscase: "+ testname);

        for (int i = 0; i < 100; i++) {
            txnKeys.add(Strings.DEFAULT_KEY+i);
            if(i>0 && i<25){
                updateKeys.add(Strings.DEFAULT_KEY+i);
            }
            if(i>=75){
                deleteKeys.add(Strings.DEFAULT_KEY+i);
            }
        }
        docContent = JsonObject.create().put(Strings.CONTENT_NAME, Strings.DEFAULT_CONTENT_VALUE);
    }


    public void runTests(){
        configureTests();
        System.out.println("Executing SanityTests");
        executeTests();
    }

    public void executeTests(){
        try{
            TxnClient.txn_req txn_create_req = TxnClient.txn_req.newBuilder()
                    .setTxnTimeout(10)
                    .setTxnDurability(1)
                    .setCommand("TXN_CREATE")
                    .build();
            assertTrue(txnstub.createTxnFactory(txn_create_req).getAPISuccessStatus());
        }catch(Exception e){
            failed_tests.add(this.testname);
            System.out.println(response.getAPIStatusInfo());
        }

        try{
            TxnClient.txn_req txn_insert_req = TxnClient.txn_req.newBuilder()
                    .setNumDocs(100)
                    .setCommand("TXN_DATA_INSERT")
                    .build();
            assertTrue(txnstub.executeTxn(txn_insert_req).getAPISuccessStatus());
        }
        catch(Exception e){
            failed_tests.add(this.testname);
            System.out.println(response.getAPIStatusInfo());
        }

        try{
            TxnClient.txn_req txn_commit_req = TxnClient.txn_req.newBuilder()
                    .setCommand("TXN_COMMIT")
                    .build();
            assertTrue(txnstub.executeTxn(txn_commit_req).getAPISuccessStatus());
            txnUtils.verifyDocuments(txnKeys,docContent,true,hostname);
        }catch(Exception e){
            failed_tests.add(this.testname);
            System.out.println(response.getAPIStatusInfo());
        }


        try{
            TxnClient.txn_req txn_update_req = TxnClient.txn_req.newBuilder()
                    .setNumDocs(25)
                    .setCommand("TXN_DATA_UPDATE")
                    .build();
            assertTrue(txnstub.executeTxn(txn_update_req).getAPISuccessStatus());
            JsonObject newContent =JsonObject.create().put(Strings.CONTENT_NAME, Strings.UPDATED_CONTENT_VALUE);
            txnUtils.verifyDocuments(updateKeys,newContent,true,hostname);
        }catch(Exception e){
            failed_tests.add(this.testname);
            System.out.println(response.getAPIStatusInfo());
        }

        try{
            TxnClient.txn_req txn_update_req = TxnClient.txn_req.newBuilder()
                    .setNumDocs(25)
                    .setCommand("TXN_DATA_DELETE")
                    .build();
            assertTrue(txnstub.executeTxn(txn_update_req).getAPISuccessStatus());


            TxnClient.txn_req txn_commit_req = TxnClient.txn_req.newBuilder()
                    .setCommand("TXN_COMMIT")
                    .build();
            assertTrue(txnstub.executeTxn(txn_commit_req).getAPISuccessStatus());

            txnUtils.verifyDocuments(deleteKeys,null,false,hostname);
        }catch(Exception e){
            failed_tests.add(this.testname);
            System.out.println("Exception during TXN_DATA_DELETE: "+e);
        }

/*

        try{
            TxnClient.txn_req txn_rollback_req = TxnClient.txn_req.newBuilder().setCommand("TXN_ROLLBACK").build();
            assertTrue(txnstub.executeTxn(txn_rollback_req).getAPISuccessStatus());

            TxnClient.txn_req txn_close_req = TxnClient.txn_req.newBuilder().setCommand("TXN_CLOSE").build();
            assertTrue(txnstub.executeTxn(txn_close_req).getAPISuccessStatus());

            txnUtils.verifyDocuments(txnKeys,docContent,true,hostname);
        }catch(Exception e){
            System.out.println(response.getAPIStatusInfo());
        }
*/
    }

}

