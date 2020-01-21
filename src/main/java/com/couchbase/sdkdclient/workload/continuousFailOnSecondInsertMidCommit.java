package com.couchbase.sdkdclient.workload;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.grpc.protocol.txnGrpc;
import com.couchbase.sdkdclient.protocol.Strings;
import com.couchbase.sdkdclient.util.txnUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Deprecated
public class continuousFailOnSecondInsertMidCommit  extends transactionTests {
    private static List<String> txnKeys = new LinkedList<>();
    private static JsonObject docContent;

    public continuousFailOnSecondInsertMidCommit(TxnClient.conn_info  conn_info, txnGrpc.txnBlockingStub   txnstub,String hostname ,String testname ){
        super(conn_info,txnstub,hostname,testname);

    }

    public void initializeTest(){
        for (int i = 0; i < 3; i++) {
            txnKeys.add(Strings.DEFAULT_KEY+i);
        }
        docContent = JsonObject.create().put(Strings.CONTENT_NAME, Strings.DEFAULT_CONTENT_VALUE);
    }

    public void execute(){
        initializeTest();
        System.out.println("Executing continuousFailOnSecondInsertMidCommit");
        try{
            TxnClient.txn_req txn_create_req = TxnClient.txn_req.newBuilder()
                                                .setTxnTimeout(10)
                                                .setTxnDurability(1).setCommand("TXN_CREATE")
                                                .setMock(true)
                                                .setDocNum(3)
                                                .setMockOperation("beforeDocCommitted")
                                                .build();
            assertTrue(txnstub.createTxnFactory(txn_create_req).getAPISuccessStatus());
        }catch(Exception e){
            System.out.println(response.getAPIStatusInfo());
            failed_tests.add(this.testname);
        }

        try{
            TxnClient.txn_req txn_create_req = TxnClient.txn_req.newBuilder()
                                                .setTxnTimeout(10)
                                                .setTxnDurability(1)
                                                .setNumDocs(3)
                                                .setCommand("TXN_DATA_INSERT")
                                                .build();
            assertTrue(txnstub.executeTxn(txn_create_req).getAPISuccessStatus());
            verifyDocuments(txnKeys,hostname);

        }catch(Exception e){
            failed_tests.add(this.testname);
            System.out.println(response.getAPIStatusInfo());
        }

        try{
            TxnClient.txn_req txn_commit_req = TxnClient.txn_req.newBuilder().setCommand("TXN_COMMIT").build();
            assertTrue(txnstub.executeTxn(txn_commit_req).getAPISuccessStatus());
            txnUtils.verifyDocuments(txnKeys,docContent,true,hostname);
        }catch(Exception e){
            System.out.println(response.getAPIStatusInfo());
            failed_tests.add(this.testname);
        }
    }



    public  boolean verifyDocuments(List<String> keys,String hostname) {
        Cluster cluster = Cluster.connect(hostname, Strings.ADMIN_USER, Strings.PASSWORD);
        Collection defaultCollection= cluster.bucket("default").defaultCollection();
        assertEquals(Strings.DEFAULT_CONTENT_VALUE, defaultCollection.get(keys.get(0)).contentAs(JsonObject.class).getString(Strings.CONTENT_NAME));
        assertEquals(Strings.DEFAULT_CONTENT_VALUE, defaultCollection.get(keys.get(1)).contentAs(JsonObject.class).getString(Strings.CONTENT_NAME));
        assertEquals(Strings.DEFAULT_CONTENT_VALUE, defaultCollection.get(keys.get(1)).contentAs(JsonObject.class).getString(Strings.CONTENT_NAME));
        return true;
    }
}
