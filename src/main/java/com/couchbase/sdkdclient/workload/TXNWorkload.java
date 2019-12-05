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


        System.out.println("Asking the txn_framework server to establish connection with Couchbase Server");
        TxnClient.APIResponse response = txnstub.createConn(conn_create_req);
        System.out.println("Did txn_framework server establish connection with Couchbase Server:"+response.getAPISuccessStatus());

        TxnClient.txn_req txn_create_req = TxnClient.txn_req.newBuilder().setTxnTimeout(10).setTxnDurability(1).setCommand("TXN_CREATE").build();
        response = txnstub.createTxn(txn_create_req);
        assertTrue(response.getAPISuccessStatus());
        System.out.println(response.getAPIStatusInfo());

        TxnClient.txn_req txn_load_data_req = TxnClient.txn_req.newBuilder().setNumThreads(10).setNumDocs(100).setCommit(true).setSync(true).setCommand("TXN_DATA_LOAD").build();
        response = txnstub.executeTxn(txn_load_data_req);
        assertTrue(response.getAPISuccessStatus());
        System.out.println(response.getAPIStatusInfo());

        try{
            TxnClient.txn_req txn_update_data_req = TxnClient.txn_req.newBuilder().setNumThreads(10).setNumDocs(25).setCommit(true).setSync(true).setCommand("TXN_DATA_UPDATE").build();
            response = txnstub.executeTxn(txn_update_data_req);
            assertTrue(response.getAPISuccessStatus());
            System.out.println(response.getAPIStatusInfo());
        }catch(Exception e){
            System.out.println(response.getAPIStatusInfo());
        }

        try{
            TxnClient.txn_req txn_delete_data_req = TxnClient.txn_req.newBuilder().setNumThreads(10).setNumDocs(25).setCommit(true).setSync(true).setCommand("TXN_DATA_DELETE").build();
            response = txnstub.executeTxn(txn_delete_data_req);
            assertTrue(response.getAPISuccessStatus());
            System.out.println(response.getAPIStatusInfo());
        }catch(Exception e){
            System.out.println(response.getAPIStatusInfo());
        }


        try{
            TxnClient.txn_req txn_continuousFailOnSecondInsertMidCommit_req = TxnClient.txn_req.newBuilder().setNumThreads(1).setNumDocs(3).setCommit(true).setSync(true).setCommand("continuousFailOnSecondInsertMidCommit").build();
            response = txnstub.executeTxn(txn_continuousFailOnSecondInsertMidCommit_req);
            assertTrue(response.getAPISuccessStatus());
            System.out.println(response.getAPIStatusInfo());
        }catch(Exception e){
            System.out.println(response.getAPIStatusInfo());
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