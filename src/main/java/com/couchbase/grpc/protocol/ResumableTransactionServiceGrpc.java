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
 * <pre>
 * Used to create resumable transactions and perform operations in them
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.15.0)",
    comments = "Source: txn_client.proto")
public final class ResumableTransactionServiceGrpc {

  private ResumableTransactionServiceGrpc() {}

  public static final String SERVICE_NAME = "txnService.ResumableTransactionService";

  // Static method descriptors that strictly reflect the proto.
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
    if ((getCreateConnMethod = ResumableTransactionServiceGrpc.getCreateConnMethod) == null) {
      synchronized (ResumableTransactionServiceGrpc.class) {
        if ((getCreateConnMethod = ResumableTransactionServiceGrpc.getCreateConnMethod) == null) {
          ResumableTransactionServiceGrpc.getCreateConnMethod = getCreateConnMethod = 
              io.grpc.MethodDescriptor.<com.couchbase.grpc.protocol.TxnClient.conn_info, com.couchbase.grpc.protocol.TxnClient.APIResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "txnService.ResumableTransactionService", "create_conn"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.conn_info.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.APIResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ResumableTransactionServiceMethodDescriptorSupplier("create_conn"))
                  .build();
          }
        }
     }
     return getCreateConnMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateRequest,
      com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateResponse> getTransactionsFactoryCreateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "transactionsFactoryCreate",
      requestType = com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateRequest.class,
      responseType = com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateRequest,
      com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateResponse> getTransactionsFactoryCreateMethod() {
    io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateRequest, com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateResponse> getTransactionsFactoryCreateMethod;
    if ((getTransactionsFactoryCreateMethod = ResumableTransactionServiceGrpc.getTransactionsFactoryCreateMethod) == null) {
      synchronized (ResumableTransactionServiceGrpc.class) {
        if ((getTransactionsFactoryCreateMethod = ResumableTransactionServiceGrpc.getTransactionsFactoryCreateMethod) == null) {
          ResumableTransactionServiceGrpc.getTransactionsFactoryCreateMethod = getTransactionsFactoryCreateMethod = 
              io.grpc.MethodDescriptor.<com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateRequest, com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "txnService.ResumableTransactionService", "transactionsFactoryCreate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ResumableTransactionServiceMethodDescriptorSupplier("transactionsFactoryCreate"))
                  .build();
          }
        }
     }
     return getTransactionsFactoryCreateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCloseRequest,
      com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> getTransactionsFactoryCloseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "transactionsFactoryClose",
      requestType = com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCloseRequest.class,
      responseType = com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCloseRequest,
      com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> getTransactionsFactoryCloseMethod() {
    io.grpc.MethodDescriptor<com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCloseRequest, com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> getTransactionsFactoryCloseMethod;
    if ((getTransactionsFactoryCloseMethod = ResumableTransactionServiceGrpc.getTransactionsFactoryCloseMethod) == null) {
      synchronized (ResumableTransactionServiceGrpc.class) {
        if ((getTransactionsFactoryCloseMethod = ResumableTransactionServiceGrpc.getTransactionsFactoryCloseMethod) == null) {
          ResumableTransactionServiceGrpc.getTransactionsFactoryCloseMethod = getTransactionsFactoryCloseMethod = 
              io.grpc.MethodDescriptor.<com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCloseRequest, com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "txnService.ResumableTransactionService", "transactionsFactoryClose"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCloseRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ResumableTransactionServiceMethodDescriptorSupplier("transactionsFactoryClose"))
                  .build();
          }
        }
     }
     return getTransactionsFactoryCloseMethod;
  }

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
    if ((getTransactionCreateMethod = ResumableTransactionServiceGrpc.getTransactionCreateMethod) == null) {
      synchronized (ResumableTransactionServiceGrpc.class) {
        if ((getTransactionCreateMethod = ResumableTransactionServiceGrpc.getTransactionCreateMethod) == null) {
          ResumableTransactionServiceGrpc.getTransactionCreateMethod = getTransactionCreateMethod = 
              io.grpc.MethodDescriptor.<com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest, com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "txnService.ResumableTransactionService", "transactionCreate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ResumableTransactionServiceMethodDescriptorSupplier("transactionCreate"))
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
    if ((getTransactionInsertMethod = ResumableTransactionServiceGrpc.getTransactionInsertMethod) == null) {
      synchronized (ResumableTransactionServiceGrpc.class) {
        if ((getTransactionInsertMethod = ResumableTransactionServiceGrpc.getTransactionInsertMethod) == null) {
          ResumableTransactionServiceGrpc.getTransactionInsertMethod = getTransactionInsertMethod = 
              io.grpc.MethodDescriptor.<com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest, com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "txnService.ResumableTransactionService", "transactionInsert"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionInsertRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ResumableTransactionServiceMethodDescriptorSupplier("transactionInsert"))
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
    if ((getTransactionCommitMethod = ResumableTransactionServiceGrpc.getTransactionCommitMethod) == null) {
      synchronized (ResumableTransactionServiceGrpc.class) {
        if ((getTransactionCommitMethod = ResumableTransactionServiceGrpc.getTransactionCommitMethod) == null) {
          ResumableTransactionServiceGrpc.getTransactionCommitMethod = getTransactionCommitMethod = 
              io.grpc.MethodDescriptor.<com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest, com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "txnService.ResumableTransactionService", "transactionCommit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionGenericRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ResumableTransactionServiceMethodDescriptorSupplier("transactionCommit"))
                  .build();
          }
        }
     }
     return getTransactionCommitMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ResumableTransactionServiceStub newStub(io.grpc.Channel channel) {
    return new ResumableTransactionServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ResumableTransactionServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ResumableTransactionServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ResumableTransactionServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ResumableTransactionServiceFutureStub(channel);
  }

  /**
   * <pre>
   * Used to create resumable transactions and perform operations in them
   * </pre>
   */
  public static abstract class ResumableTransactionServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Creates a connection from txn_server to cb server
     * </pre>
     */
    public void createConn(com.couchbase.grpc.protocol.TxnClient.conn_info request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.APIResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateConnMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create a Transactions (e.g. a transactions factory), returning a transactionsFactoryRef
     * </pre>
     */
    public void transactionsFactoryCreate(com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTransactionsFactoryCreateMethod(), responseObserver);
    }

    /**
     */
    public void transactionsFactoryClose(com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCloseRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTransactionsFactoryCloseMethod(), responseObserver);
    }

    /**
     * <pre>
     * Creates a transaction, using a transactionsFactoryRef to a previously created Transactions.  Returns a transactionRef
     * </pre>
     */
    public void transactionCreate(com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTransactionCreateMethod(), responseObserver);
    }

    /**
     * <pre>
     * Perform individual operations on a previously created transaction
     * </pre>
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

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateConnMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.couchbase.grpc.protocol.TxnClient.conn_info,
                com.couchbase.grpc.protocol.TxnClient.APIResponse>(
                  this, METHODID_CREATE_CONN)))
          .addMethod(
            getTransactionsFactoryCreateMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateRequest,
                com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateResponse>(
                  this, METHODID_TRANSACTIONS_FACTORY_CREATE)))
          .addMethod(
            getTransactionsFactoryCloseMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCloseRequest,
                com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse>(
                  this, METHODID_TRANSACTIONS_FACTORY_CLOSE)))
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
          .build();
    }
  }

  /**
   * <pre>
   * Used to create resumable transactions and perform operations in them
   * </pre>
   */
  public static final class ResumableTransactionServiceStub extends io.grpc.stub.AbstractStub<ResumableTransactionServiceStub> {
    private ResumableTransactionServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ResumableTransactionServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ResumableTransactionServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ResumableTransactionServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a connection from txn_server to cb server
     * </pre>
     */
    public void createConn(com.couchbase.grpc.protocol.TxnClient.conn_info request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.APIResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateConnMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create a Transactions (e.g. a transactions factory), returning a transactionsFactoryRef
     * </pre>
     */
    public void transactionsFactoryCreate(com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTransactionsFactoryCreateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void transactionsFactoryClose(com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCloseRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTransactionsFactoryCloseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates a transaction, using a transactionsFactoryRef to a previously created Transactions.  Returns a transactionRef
     * </pre>
     */
    public void transactionCreate(com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getTransactionCreateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Perform individual operations on a previously created transaction
     * </pre>
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
  }

  /**
   * <pre>
   * Used to create resumable transactions and perform operations in them
   * </pre>
   */
  public static final class ResumableTransactionServiceBlockingStub extends io.grpc.stub.AbstractStub<ResumableTransactionServiceBlockingStub> {
    private ResumableTransactionServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ResumableTransactionServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ResumableTransactionServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ResumableTransactionServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a connection from txn_server to cb server
     * </pre>
     */
    public com.couchbase.grpc.protocol.TxnClient.APIResponse createConn(com.couchbase.grpc.protocol.TxnClient.conn_info request) {
      return blockingUnaryCall(
          getChannel(), getCreateConnMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a Transactions (e.g. a transactions factory), returning a transactionsFactoryRef
     * </pre>
     */
    public com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateResponse transactionsFactoryCreate(com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateRequest request) {
      return blockingUnaryCall(
          getChannel(), getTransactionsFactoryCreateMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse transactionsFactoryClose(com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCloseRequest request) {
      return blockingUnaryCall(
          getChannel(), getTransactionsFactoryCloseMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates a transaction, using a transactionsFactoryRef to a previously created Transactions.  Returns a transactionRef
     * </pre>
     */
    public com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse transactionCreate(com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest request) {
      return blockingUnaryCall(
          getChannel(), getTransactionCreateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Perform individual operations on a previously created transaction
     * </pre>
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
  }

  /**
   * <pre>
   * Used to create resumable transactions and perform operations in them
   * </pre>
   */
  public static final class ResumableTransactionServiceFutureStub extends io.grpc.stub.AbstractStub<ResumableTransactionServiceFutureStub> {
    private ResumableTransactionServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ResumableTransactionServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ResumableTransactionServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ResumableTransactionServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a connection from txn_server to cb server
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.couchbase.grpc.protocol.TxnClient.APIResponse> createConn(
        com.couchbase.grpc.protocol.TxnClient.conn_info request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateConnMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create a Transactions (e.g. a transactions factory), returning a transactionsFactoryRef
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateResponse> transactionsFactoryCreate(
        com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getTransactionsFactoryCreateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse> transactionsFactoryClose(
        com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCloseRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getTransactionsFactoryCloseMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates a transaction, using a transactionsFactoryRef to a previously created Transactions.  Returns a transactionRef
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.couchbase.grpc.protocol.TxnClient.TransactionCreateResponse> transactionCreate(
        com.couchbase.grpc.protocol.TxnClient.TransactionCreateRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getTransactionCreateMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Perform individual operations on a previously created transaction
     * </pre>
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
  }

  private static final int METHODID_CREATE_CONN = 0;
  private static final int METHODID_TRANSACTIONS_FACTORY_CREATE = 1;
  private static final int METHODID_TRANSACTIONS_FACTORY_CLOSE = 2;
  private static final int METHODID_TRANSACTION_CREATE = 3;
  private static final int METHODID_TRANSACTION_INSERT = 4;
  private static final int METHODID_TRANSACTION_COMMIT = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ResumableTransactionServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ResumableTransactionServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_CONN:
          serviceImpl.createConn((com.couchbase.grpc.protocol.TxnClient.conn_info) request,
              (io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.APIResponse>) responseObserver);
          break;
        case METHODID_TRANSACTIONS_FACTORY_CREATE:
          serviceImpl.transactionsFactoryCreate((com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateRequest) request,
              (io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCreateResponse>) responseObserver);
          break;
        case METHODID_TRANSACTIONS_FACTORY_CLOSE:
          serviceImpl.transactionsFactoryClose((com.couchbase.grpc.protocol.TxnClient.TransactionsFactoryCloseRequest) request,
              (io.grpc.stub.StreamObserver<com.couchbase.grpc.protocol.TxnClient.TransactionGenericResponse>) responseObserver);
          break;
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

  private static abstract class ResumableTransactionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ResumableTransactionServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.couchbase.grpc.protocol.TxnClient.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ResumableTransactionService");
    }
  }

  private static final class ResumableTransactionServiceFileDescriptorSupplier
      extends ResumableTransactionServiceBaseDescriptorSupplier {
    ResumableTransactionServiceFileDescriptorSupplier() {}
  }

  private static final class ResumableTransactionServiceMethodDescriptorSupplier
      extends ResumableTransactionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ResumableTransactionServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (ResumableTransactionServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ResumableTransactionServiceFileDescriptorSupplier())
              .addMethod(getCreateConnMethod())
              .addMethod(getTransactionsFactoryCreateMethod())
              .addMethod(getTransactionsFactoryCloseMethod())
              .addMethod(getTransactionCreateMethod())
              .addMethod(getTransactionInsertMethod())
              .addMethod(getTransactionCommitMethod())
              .build();
        }
      }
    }
    return result;
  }
}
