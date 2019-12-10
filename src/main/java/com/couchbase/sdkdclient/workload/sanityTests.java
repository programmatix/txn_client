package com.couchbase.sdkdclient.workload;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.grpc.protocol.txnGrpc;
import com.couchbase.sdkdclient.protocol.Strings;
import com.couchbase.sdkdclient.util.txnUtils;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class sanityTests extends transactionTests {

    private static List<String> txnKeys = new LinkedList<>();
    private static List<String> updateKeys = new LinkedList<>();
    private static List<String> deleteKeys = new LinkedList<>();
    private static JsonObject docContent = null;

    public sanityTests(TxnClient.conn_info  conn_info, txnGrpc.txnBlockingStub   txnstub,String hostname){
        super(conn_info,txnstub,hostname);
    }

    public void initializeTest(){
        for (int i = 0; i < 100; i++) {
            txnKeys.add("Test" + i);

            if(i>0 && i<25){
                updateKeys.add("Test" + i);
            }

            if(i>25 && i<50){
                deleteKeys.add("Test" + i);
            }
        }
        docContent = JsonObject.create().put("mutated", 0);
        JsonObject body = JsonObject.create().put("firstname", "James");
        body = body.put("age", 22);
        docContent = docContent.put("body", body);
    }


    public boolean execute(){
        initializeTest();
        try{
            TxnClient.txn_req txn_create_req = TxnClient.txn_req.newBuilder().setTxnTimeout(10).setTxnDurability(1).setCommand("TXN_CREATE").build();
            assertTrue(txnstub.createTxnFactory(txn_create_req).getAPISuccessStatus());
        }catch(Exception e){
            System.out.println(response.getAPIStatusInfo());
        }

        try{
            TxnClient.txn_req txn_insert_req = TxnClient.txn_req.newBuilder().setNumDocs(100).setCommand("TXN_DATA_INSERT").build();
            assertTrue(txnstub.executeTxn(txn_insert_req).getAPISuccessStatus());
        }
        catch(Exception e){
                System.out.println(response.getAPIStatusInfo());
        }

        try{
            TxnClient.txn_req txn_commit_req = TxnClient.txn_req.newBuilder().setCommand("TXN_COMMIT").build();
            assertTrue(txnstub.executeTxn(txn_commit_req).getAPISuccessStatus());
            txnUtils.verifyDocuments(txnKeys,docContent,true,hostname);
        }catch(Exception e){
            System.out.println(response.getAPIStatusInfo());
        }


        try{
            TxnClient.txn_req txn_update_req = TxnClient.txn_req.newBuilder().setNumDocs(25).setCommand("TXN_DATA_UPDATE").build();
            assertTrue(txnstub.executeTxn(txn_update_req).getAPISuccessStatus());
            JsonObject newContent = docContent.put("mutated", 1);
            txnUtils.verifyDocuments(updateKeys,newContent,true,hostname);
        }catch(Exception e){
            System.out.println(response.getAPIStatusInfo());
        }

        try{
            TxnClient.txn_req txn_update_req = TxnClient.txn_req.newBuilder().setNumDocs(25).setCommand("TXN_DATA_DELETE").build();
            assertTrue(txnstub.executeTxn(txn_update_req).getAPISuccessStatus());
            txnUtils.verifyDocuments(deleteKeys,null,false,hostname);
        }catch(Exception e){
            System.out.println(response.getAPIStatusInfo());
        }

        try{
            TxnClient.txn_req txn_rollback_req = TxnClient.txn_req.newBuilder().setCommand("TXN_ROLLBACK").build();
            assertTrue(txnstub.executeTxn(txn_rollback_req).getAPISuccessStatus());
            txnUtils.verifyDocuments(txnKeys,docContent,true,hostname);
        }catch(Exception e){
            System.out.println(response.getAPIStatusInfo());
        }

        return true;
    }



}
