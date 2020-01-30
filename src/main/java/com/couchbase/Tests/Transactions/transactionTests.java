package com.couchbase.Tests.Transactions;

import com.couchbase.Couchbase.Cluster.ClusterConfigure;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.grpc.protocol.txnGrpc;


import java.util.ArrayList;
import java.util.List;

 public class transactionTests {
   public TxnClient.conn_info  conn_info;
   txnGrpc.txnBlockingStub   txnstub;
   TxnClient.APIResponse response;
   String hostname;
   String testname;
    ClusterConfigure clusterConfigure;

   List<transactionTests> txn_tests = new ArrayList<transactionTests>();
    List<String> failed_tests = new ArrayList<>();

    public transactionTests(TxnClient.conn_info  conn_info, txnGrpc.txnBlockingStub   txnstub, String hostname , String testname,ClusterConfigure clusterConfigure){
       this.conn_info=conn_info;
       this.txnstub=txnstub;
       this.hostname= hostname;
       this.testname =  testname;
       this.clusterConfigure = clusterConfigure;
       response = txnstub.createConn(conn_info);
   }


   public void configureTests(){
       txn_tests.add(new sanityTests(conn_info,txnstub,hostname,"sanity",clusterConfigure));
       txn_tests.add(new continuousFailOnSecondInsertMidCommit(conn_info,txnstub,hostname,"continuousFailOnSecondInsertMidCommit",clusterConfigure));
   }


   public void execute(){
        configureTests();
       for(transactionTests test : txn_tests){
           test.runTests();
           System.out.println("Completed test:"+test.testname);
       }
   }

    void runTests(){};
}
