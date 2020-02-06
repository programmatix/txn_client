package com.couchbase;

import com.couchbase.Constants.Strings;
import com.couchbase.Couchbase.Cluster.ClusterConfigure;
import com.couchbase.Couchbase.Couchbase.CouchbaseInstaller;
import com.couchbase.InputParameters.inputParameters;
import com.couchbase.Logging.LogUtil;
import com.couchbase.Tests.Transactions.transactionTests;
import com.couchbase.grpc.protocol.ResumableTransactionServiceGrpc;
import com.couchbase.grpc.protocol.TxnClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import java.io.IOException;
import java.util.concurrent.ExecutionException;



public class clientService {
    private static ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub txnstub = null;
    static Logger logger;
    static CouchbaseInstaller couchbaseInstaller;
    static ClusterConfigure clusterConfigure;
    static String clusterHost;
    static inputParameters inputParameters;

    public static void main(String args[]) throws IOException, ParseException, ExecutionException {
        setup(args);

        logger.info("Connecting to GRPC Server");
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost",8050).usePlaintext().build();
        txnstub = ResumableTransactionServiceGrpc.newBlockingStub(channel);

        TxnClient.conn_info  conn_create_req =
                TxnClient.conn_info.newBuilder()
                        .setHandleHostname(clusterHost)
                        .setHandleBucket(inputParameters.getbucketname())
                        .setHandlePort(inputParameters.getclusterPort())
                        .setHandleUsername(Strings.ADMIN_USER)
                        .setHandlePassword(Strings.PASSWORD)
                        .setHandleAutofailoverMs(inputParameters.getAutoFailoverTimeout())
                        .build();
        TxnClient.APIResponse response = txnstub.createConn(conn_create_req);

        logger.info("Test server established connection with Couchbase Server: "+response.getAPISuccessStatus());
        try {
            logger.info("Type of tests being run are  \"{}\" and the test suite for those tests are  \"{}\". We will execute  \"{}\" tests for this testsuite ",inputParameters.gettesttype(),inputParameters.gettestsuite(),inputParameters.gettestname() );
            if(inputParameters.gettesttype().equals("txn") || inputParameters.gettesttype().equals("")){
                transactionTests txn_tests = new transactionTests(conn_create_req,txnstub,clusterHost,inputParameters.gettestsuite(),inputParameters.gettestname() ,clusterConfigure);
                txn_tests.execute();
            }
            else {
                logger.error("Invalid testtype selected");
            }
        } catch(Exception e) {
            logger.error("Few tests have failed : "+e.getStackTrace());
        }
        channel.shutdown();
    }

    public static void setup(String[] params) throws IOException, ParseException, ExecutionException{

        inputParameters = new inputParameters(params[0]);
        inputParameters.readandStoreParams();

        logger = LogUtil.getLogger(clientService.class);
        logger.info("Starting the framework");

        couchbaseInstaller = new CouchbaseInstaller(inputParameters);
        if (inputParameters.getinstallcouchbase()) {
            couchbaseInstaller.installcouchbase();
            logger.info("Completed Couchbase installation on individual nodes. Proceeding with Cluster Creation");
        } else {
            logger.info("Not Installing couchbase Since the user input for installcouchbase is set to : " + inputParameters.getinstallcouchbase());
        }


        clusterConfigure = new ClusterConfigure(inputParameters);
         clusterHost = clusterConfigure.initializecluster(true);
        logger.info("Completed Cluster Creation. Proceeding with Test Execution");

    }

}
