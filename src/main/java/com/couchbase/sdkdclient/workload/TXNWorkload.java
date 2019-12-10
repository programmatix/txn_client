package com.couchbase.sdkdclient.workload;
import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.grpc.protocol.txnGrpc;
import com.couchbase.sdkdclient.handle.HandleFactory;
import com.couchbase.sdkdclient.handle.HandleOptions;
import com.couchbase.sdkdclient.options.OptionConsumer;
import com.couchbase.sdkdclient.options.OptionTree;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import static org.junit.Assert.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 *
 */
public class TXNWorkload extends Workload implements OptionConsumer {
  private final TXNOptions options = new TXNOptions();


  public TXNWorkload(String id) {
    super(id);
  }

  @Override
  public void configure(HandleFactory hf) {
    super.configure(hf);
  }

  @Override
  public void load() throws IOException {}

    @Override
  public void start() throws IOException {

        System.out.println("Connecting to GRPC Server");
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost",8050).usePlaintext().build();
        txnGrpc.txnBlockingStub   txnstub = txnGrpc.newBlockingStub(channel);
        System.out.println("Created connection between txn_framework client and server");

        HandleOptions options=  handleMaker.getHandleOptions();

        URI ept = options.getEntryPoint();
        TxnClient.conn_info  conn_create_req =
                TxnClient.conn_info.newBuilder()
                        .setHandleHostname(ept.getHost())
                        .setHandleBucket(options.getBucketName())
                        .setHandlePort(ept.getPort())
                        .setHandlePassword(options.getBucketPassword())
                        .setHandleUsername(options.getBucketName())
                        .setHandleSsl(options.getUseSSL())
                        .setHandleAutofailoverMs(options.getAutoFailoverSetting())
                        .setHandleCert(options.getClusterCertificate())
                .build();
        TxnClient.APIResponse response = txnstub.createConn(conn_create_req);
        System.out.println("Did txn_framework server establish connection with Couchbase Server: "+response.getAPISuccessStatus());



        try{
            transactionTests sanity_tests = new sanityTests(conn_create_req,txnstub,ept.getHost());
            assertTrue(sanity_tests.execute());
        }catch(Exception e){
            System.out.println("Basic Test failed. hence not proceeding further");
            System.exit(-1);
        }


        try{
            transactionTests continuousFailOnSecondInsertMidCommit = new continuousFailOnSecondInsertMidCommit(conn_create_req,txnstub,ept.getHost());
            assertTrue(continuousFailOnSecondInsertMidCommit.execute());
        }catch(Exception e){
            System.out.println("continuousFailOnSecondInsertMidCommit Test failed");
        }


        channel.shutdown();
        System.exit(-1);

  }


  @Override
  public void beginRebound() throws IOException {

  }

  @Override
  public OptionTree getOptionTree() {
    return options.getOptionTree();
  }
}