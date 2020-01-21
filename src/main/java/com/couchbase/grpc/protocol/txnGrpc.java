package com.couchbase.grpc.protocol;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.15.0)",
    comments = "Source: txn_client.proto")
public final class txnGrpc {

  private txnGrpc() {}

  public static final String SERVICE_NAME = "txnService.txn";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest,
      com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse> getTransactionCreateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "transactionCreate",
      requestType = com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest.class,
      responseType = com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest,
      com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse> getTransactionCreateMethod() {
    io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest, com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse> getTransactionCreateMethod;
    if ((getTransactionCreateMethod = txnGrpc.getTransactionCreateMethod) == null) {
      synchronized (txnGrpc.class) {
        if ((getTransactionCreateMethod = txnGrpc.getTransactionCreateMethod) == null) {
          txnGrpc.getTransactionCreateMethod = getTransactionCreateMethod = 
              io.grpc.MethodDescriptor.<com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest, com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "txnService.txn", "transactionCreate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new txnMethodDescriptorSupplier("transactionCreate"))
                  .build();
          }
        }
     }
     return getTransactionCreateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest,
      com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> getTransactionInsertMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "transactionInsert",
      requestType = com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest.class,
      responseType = com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest,
      com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> getTransactionInsertMethod() {
    io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest, com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> getTransactionInsertMethod;
    if ((getTransactionInsertMethod = txnGrpc.getTransactionInsertMethod) == null) {
      synchronized (txnGrpc.class) {
        if ((getTransactionInsertMethod = txnGrpc.getTransactionInsertMethod) == null) {
          txnGrpc.getTransactionInsertMethod = getTransactionInsertMethod = 
              io.grpc.MethodDescriptor.<com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest, com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "txnService.txn", "transactionInsert"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new txnMethodDescriptorSupplier("transactionInsert"))
                  .build();
          }
        }
     }
     return getTransactionInsertMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest,
      com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> getTransactionCommitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "transactionCommit",
      requestType = com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest.class,
      responseType = com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest,
      com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> getTransactionCommitMethod() {
    io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest, com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> getTransactionCommitMethod;
    if ((getTransactionCommitMethod = txnGrpc.getTransactionCommitMethod) == null) {
      synchronized (txnGrpc.class) {
        if ((getTransactionCommitMethod = txnGrpc.getTransactionCommitMethod) == null) {
          txnGrpc.getTransactionCommitMethod = getTransactionCommitMethod = 
              io.grpc.MethodDescriptor.<com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest, com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "txnService.txn", "transactionCommit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new txnMethodDescriptorSupplier("transactionCommit"))
                  .build();
          }
        }
     }
     return getTransactionCommitMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.txn_req,
      com.couchbase.grpc.protocol.TxnClient.APIResponse> getCreateTxnFactoryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "create_TxnFactory",
      requestType = com.couchbase.grpc.protocol.TxnClient.txn_req.class,
      responseType = com.couchbase.grpc.protocol.TxnClient.APIResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.txn_req,
      com.couchbase.grpc.protocol.TxnClient.APIResponse> getCreateTxnFactoryMethod() {
    io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.txn_req, com.couchbase.grpc.protocol.TxnClient.APIResponse> getCreateTxnFactoryMethod;
    if ((getCreateTxnFactoryMethod = txnGrpc.getCreateTxnFactoryMethod) == null) {
      synchronized (txnGrpc.class) {
        if ((getCreateTxnFactoryMethod = txnGrpc.getCreateTxnFactoryMethod) == null) {
          txnGrpc.getCreateTxnFactoryMethod = getCreateTxnFactoryMethod = 
              io.grpc.MethodDescriptor.<com.couchbase.grpc.protocol.TxnClient.txn_req, com.couchbase.grpc.protocol.TxnClient.APIResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "txnService.txn", "create_TxnFactory"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.txn_req.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.APIResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new txnMethodDescriptorSupplier("create_TxnFactory"))
                  .build();
          }
        }
     }
     return getCreateTxnFactoryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.conn_info,
      com.couchbase.grpc.protocol.TxnClient.APIResponse> getCreateConnMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "create_conn",
      requestType = com.couchbase.grpc.protocol.TxnClient.conn_info.class,
      responseType = com.couchbase.grpc.protocol.TxnClient.APIResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.conn_info,
      com.couchbase.grpc.protocol.TxnClient.APIResponse> getCreateConnMethod() {
    io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.conn_info, com.couchbase.grpc.protocol.TxnClient.APIResponse> getCreateConnMethod;
    if ((getCreateConnMethod = txnGrpc.getCreateConnMethod) == null) {
      synchronized (txnGrpc.class) {
        if ((getCreateConnMethod = txnGrpc.getCreateConnMethod) == null) {
          txnGrpc.getCreateConnMethod = getCreateConnMethod = 
              io.grpc.MethodDescriptor.<com.couchbase.grpc.protocol.TxnClient.conn_info, com.couchbase.grpc.protocol.TxnClient.APIResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "txnService.txn", "create_conn"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.conn_info.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.APIResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new txnMethodDescriptorSupplier("create_conn"))
                  .build();
          }
        }
     }
     return getCreateConnMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.txn_req,
      com.couchbase.grpc.protocol.TxnClient.APIResponse> getExecuteTxnMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "execute_txn",
      requestType = com.couchbase.grpc.protocol.TxnClient.txn_req.class,
      responseType = com.couchbase.grpc.protocol.TxnClient.APIResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.txn_req,
      com.couchbase.grpc.protocol.TxnClient.APIResponse> getExecuteTxnMethod() {
    io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.txn_req, com.couchbase.grpc.protocol.TxnClient.APIResponse> getExecuteTxnMethod;
    if ((getExecuteTxnMethod = txnGrpc.getExecuteTxnMethod) == null) {
      synchronized (txnGrpc.class) {
        if ((getExecuteTxnMethod = txnGrpc.getExecuteTxnMethod) == null) {
          txnGrpc.getExecuteTxnMethod = getExecuteTxnMethod = 
              io.grpc.MethodDescriptor.<com.couchbase.grpc.protocol.TxnClient.txn_req, com.couchbase.grpc.protocol.TxnClient.APIResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "txnService.txn", "execute_txn"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.txn_req.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.APIResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new txnMethodDescriptorSupplier("execute_txn"))
                  .build();
          }
        }
     }
     return getExecuteTxnMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static txnStub newStub(io.grpc.Channel channel) {
    return new txnStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static txnBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new txnBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static txnFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new txnFutureStub(channel);
  }

  /**
   */
  public static abstract class txnImplBase implements io.grpc.BindableService {

    /**
     */
    public void transactionCreate(com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTransactionCreateMethod(), responseObserver);
    }

    /**
     */
    public void transactionInsert(com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTransactionInsertMethod(), responseObserver);
    }

    /**
     */
    public void transactionCommit(com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTransactionCommitMethod(), responseObserver);
    }

    /**
     */
    public void createTxnFactory(com.couchbase.grpc.protocol.TxnClient.txn_req request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.APIResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateTxnFactoryMethod(), responseObserver);
    }

    /**
     */
    public void createConn(com.couchbase.grpc.protocol.TxnClient.conn_info request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.APIResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateConnMethod(), responseObserver);
    }

    /**
     */
    public void executeTxn(com.couchbase.grpc.protocol.TxnClient.txn_req request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.APIResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteTxnMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getTransactionCreateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest,
                com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse>(
                  this, METHODID_TRANSACTION_CREATE)))
          .addMethod(
            getTransactionInsertMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest,
                com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse>(
                  this, METHODID_TRANSACTION_INSERT)))
          .addMethod(
            getTransactionCommitMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest,
                com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse>(
                  this, METHODID_TRANSACTION_COMMIT)))
          .addMethod(
            getCreateTxnFactoryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.couchbase.grpc.protocol.TxnClient.txn_req,
                com.couchbase.grpc.protocol.TxnClient.APIResponse>(
                  this, METHODID_CREATE_TXN_FACTORY)))
          .addMethod(
            getCreateConnMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.couchbase.grpc.protocol.TxnClient.conn_info,
                com.couchbase.grpc.protocol.TxnClient.APIResponse>(
                  this, METHODID_CREATE_CONN)))
          .addMethod(
            getExecuteTxnMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.couchbase.grpc.protocol.TxnClient.txn_req,
                com.couchbase.grpc.protocol.TxnClient.APIResponse>(
                  this, METHODID_EXECUTE_TXN)))
          .build();
    }
  }

  /**
   */
  public static final class txnStub extends io.grpc.stub.AbstractStub<txnStub> {
    private txnStub(io.grpc.Channel channel) {
      super(channel);
    }

    private txnStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected txnStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new txnStub(channel, callOptions);
    }

    /**
     */
    public void transactionCreate(com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTransactionCreateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void transactionInsert(com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTransactionInsertMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void transactionCommit(com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTransactionCommitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void createTxnFactory(com.couchbase.grpc.protocol.TxnClient.txn_req request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.APIResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateTxnFactoryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void createConn(com.couchbase.grpc.protocol.TxnClient.conn_info request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.APIResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateConnMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void executeTxn(com.couchbase.grpc.protocol.TxnClient.txn_req request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.APIResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExecuteTxnMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class txnBlockingStub extends io.grpc.stub.AbstractStub<txnBlockingStub> {
    private txnBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private txnBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected txnBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new txnBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse transactionCreate(com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest request) {
      return blockingUnaryCall(
          getChannel(), getTransactionCreateMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse transactionInsert(com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest request) {
      return blockingUnaryCall(
          getChannel(), getTransactionInsertMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse transactionCommit(com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest request) {
      return blockingUnaryCall(
          getChannel(), getTransactionCommitMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.couchbase.grpc.protocol.TxnClient.APIResponse createTxnFactory(com.couchbase.grpc.protocol.TxnClient.txn_req request) {
      return blockingUnaryCall(
          getChannel(), getCreateTxnFactoryMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.couchbase.grpc.protocol.TxnClient.APIResponse createConn(com.couchbase.grpc.protocol.TxnClient.conn_info request) {
      return blockingUnaryCall(
          getChannel(), getCreateConnMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.couchbase.grpc.protocol.TxnClient.APIResponse executeTxn(com.couchbase.grpc.protocol.TxnClient.txn_req request) {
      return blockingUnaryCall(
          getChannel(), getExecuteTxnMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class txnFutureStub extends io.grpc.stub.AbstractStub<txnFutureStub> {
    private txnFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private txnFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected txnFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new txnFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse> transactionCreate(
        com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getTransactionCreateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> transactionInsert(
        com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getTransactionInsertMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> transactionCommit(
        com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getTransactionCommitMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.couchbase.grpc.protocol.TxnClient.APIResponse> createTxnFactory(
        com.couchbase.grpc.protocol.TxnClient.txn_req request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateTxnFactoryMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.couchbase.grpc.protocol.TxnClient.APIResponse> createConn(
        com.couchbase.grpc.protocol.TxnClient.conn_info request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateConnMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.couchbase.grpc.protocol.TxnClient.APIResponse> executeTxn(
        com.couchbase.grpc.protocol.TxnClient.txn_req request) {
      return futureUnaryCall(
          getChannel().newCall(getExecuteTxnMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_TRANSACTION_CREATE = 0;
  private static final int METHODID_TRANSACTION_INSERT = 1;
  private static final int METHODID_TRANSACTION_COMMIT = 2;
  private static final int METHODID_CREATE_TXN_FACTORY = 3;
  private static final int METHODID_CREATE_CONN = 4;
  private static final int METHODID_EXECUTE_TXN = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final txnImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(txnImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_TRANSACTION_CREATE:
          serviceImpl.transactionCreate((com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest) request,
              (io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse>) responseObserver);
          break;
        case METHODID_TRANSACTION_INSERT:
          serviceImpl.transactionInsert((com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest) request,
              (io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse>) responseObserver);
          break;
        case METHODID_TRANSACTION_COMMIT:
          serviceImpl.transactionCommit((com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest) request,
              (io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse>) responseObserver);
          break;
        case METHODID_CREATE_TXN_FACTORY:
          serviceImpl.createTxnFactory((com.couchbase.grpc.protocol.TxnClient.txn_req) request,
              (io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.APIResponse>) responseObserver);
          break;
        case METHODID_CREATE_CONN:
          serviceImpl.createConn((com.couchbase.grpc.protocol.TxnClient.conn_info) request,
              (io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.APIResponse>) responseObserver);
          break;
        case METHODID_EXECUTE_TXN:
          serviceImpl.executeTxn((com.couchbase.grpc.protocol.TxnClient.txn_req) request,
              (io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.APIResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class txnBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    txnBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.couchbase.grpc.protocol.TxnClient.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("txn");
    }
  }

  private static final class txnFileDescriptorSupplier
      extends txnBaseDescriptorSupplier {
    txnFileDescriptorSupplier() {}
  }

  private static final class txnMethodDescriptorSupplier
      extends txnBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    txnMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (txnGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new txnFileDescriptorSupplier())
              .addMethod(getTransactionCreateMethod())
              .addMethod(getTransactionInsertMethod())
              .addMethod(getTransactionCommitMethod())
              .addMethod(getCreateTxnFactoryMethod())
              .addMethod(getCreateConnMethod())
              .addMethod(getExecuteTxnMethod())
              .build();
        }
      }
    }
    return result;
  }
}
