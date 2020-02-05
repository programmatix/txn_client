package com.couchbase.Tests.Transactions;

import com.couchbase.Couchbase.Cluster.ClusterConfigure;
import com.couchbase.Logging.LogUtil;
import com.couchbase.Tests.Transactions.BasicTests.simpleInsert;
import com.couchbase.Tests.Transactions.BasicTests.simplecommit;
import com.couchbase.Tests.Transactions.Hooks.failsbeforeCommit;
import com.couchbase.Tests.Transactions.Utils.txnUtils;
import com.couchbase.clientService;
import com.couchbase.grpc.protocol.ResumableTransactionServiceGrpc;
import com.couchbase.grpc.protocol.TxnClient;
import org.slf4j.Logger;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class transactionTests {
   public TxnClient.conn_info  conn_info;
   protected ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub  txnstub;
   TxnClient.APIResponse response;
   protected String hostname;
   protected String testname;
   protected String testsuite;
   protected ClusterConfigure clusterConfigure;
   protected Logger logger = LogUtil.getLogger(transactionTests.class);

   List<transactionTests> txn_tests = new ArrayList<>();
   Map<String,transactionTests> basicTests = new HashMap<>();
    Map<String,transactionTests> hookTests = new HashMap<>();

    Map<String,Map<String,transactionTests>> allTests = new HashMap<>();
    List<String> failed_tests = new ArrayList<>();

    public transactionTests(TxnClient.conn_info  conn_info, ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub txnstub, String hostname ,String testsuite, String testname, ClusterConfigure clusterConfigure){
         this.conn_info=conn_info;
         this.txnstub=txnstub;
         this.hostname= hostname;
         this.testname =  testname;
         this.testsuite = testsuite;
         this.clusterConfigure = clusterConfigure;
         response = txnstub.createConn(conn_info);
     }

   public void configureTests(){
       loadalltests();
       getrequiredtests();
   }


   public void execute(){
        configureTests();
       for(transactionTests test : txn_tests){
           test.runTests();
           logger.info("Success for Completed test:"+test.testname);
       }
   }

   protected static TxnClient.TransactionsFactoryCreateRequest.Builder createDefaultTransactionsFactory() {
         return TxnClient.TransactionsFactoryCreateRequest.newBuilder()

                 // Disable these threads so can run multiple Transactions (and hence hooks)
                 .setCleanupClientAttempts(false)
                 .setCleanupLostAttempts(false)

                 // This is default durability for txns library
                 .setDurability(TxnClient.Durability.MAJORITY)

                 // Plenty of time for manual debugging
                 .setExpirationSeconds(120);
     }

    protected void runTests(){};

     private  void getrequiredtests(){
         Map<String,transactionTests> reqTests = allTests.get(testsuite);
         if(testname.equalsIgnoreCase("all")){
            for( transactionTests tests: reqTests.values()){
                txn_tests.add(tests);
            }
         }else
         {
             txn_tests.add(reqTests.get(testname.toLowerCase()));
         }

     }

     private void loadalltests(){
         basicTests.put("simplecommit",new simplecommit(conn_info,txnstub,hostname,"simplecommit",clusterConfigure));
         basicTests.put("simpleinsert",new simpleInsert(conn_info,txnstub,hostname,"simpleInsert",clusterConfigure));


         hookTests.put("failsbeforecommit",new simpleInsert(conn_info,txnstub,hostname,"failsbeforeCommit",clusterConfigure));

         allTests.put("basic",basicTests);
         allTests.put("hook",hookTests);
     }

}
