package com.couchbase.sdkdclient.workload;

import com.couchbase.grpc.protocol.TxnClient;
import com.couchbase.grpc.protocol.txnGrpc;

abstract class transactionTests {
    public TxnClient.conn_info  conn_info;
    txnGrpc.txnBlockingStub   txnstub;
    TxnClient.APIResponse response;
    String hostname;

    transactionTests(TxnClient.conn_info  conn_info,  txnGrpc.txnBlockingStub   txnstub,String hostname ){
        this.conn_info=conn_info;
        this.txnstub=txnstub;
        this.hostname= hostname;
        response = txnstub.createConn(conn_info);
    }

    abstract boolean execute();
}
