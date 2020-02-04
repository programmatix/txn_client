package com.couchbase.Tests.Transactions;

import com.couchbase.Couchbase.Cluster.ClusterConfigure;
import com.couchbase.grpc.protocol.ResumableTransactionServiceGrpc;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.grpc.protocol.txnGrpc;


import java.util.ArrayList;
import java.util.List;

 public class transactionTests {
   public TxnClient.conn_info  conn_info;
   ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub  txnstub;
   TxnClient.APIResponse response;
   String hostname;
   String testname;
    ClusterConfigure clusterConfigure;

   List<transactionTests> txn_tests = new ArrayList<transactionTests>();
    List<String> failed_tests = new ArrayList<>();

    public transactionTests(TxnClient.conn_info  conn_info, ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub txnstub, String hostname , String testname, ClusterConfigure clusterConfigure){
       this.conn_info=conn_info;
       this.txnstub=txnstub;
       this.hostname= hostname;
       this.testname =  testname;
       this.clusterConfigure = clusterConfigure;
       response = txnstub.createConn(conn_info);
   }


   public void configureTests(){
       txn_tests.add(new simplecommit(conn_info,txnstub,hostname,"simplecommit",clusterConfigure));
       txn_tests.add(new simpleInsert(conn_info,txnstub,hostname,"simpleInsert",clusterConfigure));
       txn_tests.add(new failsbeforeCommit(conn_info,txnstub,hostname,"failsbeforeCommit",clusterConfigure));

   }


   public void execute(){
        configureTests();
       for(transactionTests test : txn_tests){
           test.runTests();
           System.out.println("Success for Completed test:"+test.testname);
       }
   }

   static TxnClient.TransactionsFactoryCreateRequest.Builder createDefaultTransactionsFactory() {
         return TxnClient.TransactionsFactoryCreateRequest.newBuilder()

                 // Disable these threads so can run multiple Transactions (and hence hooks)
                 .setCleanupClientAttempts(false)
                 .setCleanupLostAttempts(false)

                 // This is default durability for txns library
                 .setDurability(TxnClient.Durability.MAJORITY)

                 // Plenty of time for manual debugging
                 .setExpirationSeconds(120);
     }

    void runTests(){};
}
