import com.couchbase.Constants.Strings;
import com.couchbase.Couchbase.Cluster.ClusterConfigure;
import com.couchbase.Couchbase.Couchbase.CouchbaseInstaller;
import com.couchbase.InputParameters.inputParameters;
import com.couchbase.Tests.Transactions.transactionTests;
import com.couchbase.grpc.protocol.ResumableTransactionServiceGrpc;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.grpc.protocol.txnGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;



public class clientService {
    private static ResumableTransactionServiceGrpc.ResumableTransactionServiceBlockingStub txnstub = null;
    static Logger logger;


    public static void main(String args[]) throws IOException, ParseException, ExecutionException, ParserConfigurationException, SAXException {
        setup(args);
    }

    public static void setup(String[] params) throws IOException, ParseException, ExecutionException, ParserConfigurationException, SAXException {

        inputParameters inputParameters  =  new inputParameters(params[0]);
        inputParameters.readandStoreParams();

        logger =  LoggerFactory.getLogger(clientService.class);
        logger.info("Starting the framework");

        CouchbaseInstaller couchbaseInstaller = new CouchbaseInstaller(inputParameters);
        couchbaseInstaller.installcouchbase();
        logger.info("Completed Couchbase installation on individual nodes. Proceeding with Cluster Creation");

        ClusterConfigure clusterConfigure = new ClusterConfigure(inputParameters);
        String clusterHost = clusterConfigure.initializecluster(true);
        logger.info("Completed Cluster Creation. Proceeding with Test Execution");


        logger.info("Connecting to GRPC Server");
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost",8050).usePlaintext().build();
        txnstub = ResumableTransactionServiceGrpc.newBlockingStub(channel);
        logger.info("Created connection between txn_framework client and server");

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

        logger.info("Did txn_framework server establish connection with Couchbase Server: "+response.getAPISuccessStatus());
        try {
            transactionTests txn_tests = new transactionTests(conn_create_req,txnstub,clusterHost,"",clusterConfigure);
            txn_tests.execute();
        } catch(Exception e) {
            logger.error("Few tests have failed Test failed: "+e);
        }

        channel.shutdown();
    }
}
