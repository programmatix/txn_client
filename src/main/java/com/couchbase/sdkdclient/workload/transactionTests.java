package com.couchbase.sdkdclient.workload;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.grpc.protocol.txnGrpc;
import com.couchbase.sdkdclient.protocol.Strings;

import java.util.ArrayList;
import java.util.List;

 class transactionTests {
    public TxnClient.conn_info  conn_info;
    txnGrpc.txnBlockingStub   txnstub;
    TxnClient.APIResponse response;
    String hostname;
    String testname;

    List<transactionTests> txn_tests = new ArrayList<transactionTests>();
     List<String> failed_tests = new ArrayList<>();

     transactionTests(TxnClient.conn_info  conn_info,  txnGrpc.txnBlockingStub   txnstub,String hostname ,String testname){
        this.conn_info=conn_info;
        this.txnstub=txnstub;
        this.hostname= hostname;
        this.testname =  testname;
        response = txnstub.createConn(conn_info);
    }


    public void configureTests(){
        txn_tests.add(new sanityTests(conn_info,txnstub,hostname,"sanity"));
        txn_tests.add(new continuousFailOnSecondInsertMidCommit(conn_info,txnstub,hostname,"continuousFailOnSecondInsertMidCommit"));
    }


     void execute(){
         configureTests();
        for(transactionTests test : txn_tests){
           // test.execute();
            System.out.println("Completed test:"+test.testname);
        }
    }


}
