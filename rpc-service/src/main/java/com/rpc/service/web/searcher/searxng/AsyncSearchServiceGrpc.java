package com.rpc.service.web.searcher.searxng;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * 定义异步搜索服务
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.57.2)",
    comments = "Source: search_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class AsyncSearchServiceGrpc {

  private AsyncSearchServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.rpc.service.web.searcher.searxng.AsyncSearchService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.rpc.service.web.searcher.searxng.SearchRequest,
      com.rpc.service.web.searcher.searxng.SearchResponse> getSearchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Search",
      requestType = com.rpc.service.web.searcher.searxng.SearchRequest.class,
      responseType = com.rpc.service.web.searcher.searxng.SearchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.rpc.service.web.searcher.searxng.SearchRequest,
      com.rpc.service.web.searcher.searxng.SearchResponse> getSearchMethod() {
    io.grpc.MethodDescriptor<com.rpc.service.web.searcher.searxng.SearchRequest, com.rpc.service.web.searcher.searxng.SearchResponse> getSearchMethod;
    if ((getSearchMethod = AsyncSearchServiceGrpc.getSearchMethod) == null) {
      synchronized (AsyncSearchServiceGrpc.class) {
        if ((getSearchMethod = AsyncSearchServiceGrpc.getSearchMethod) == null) {
          AsyncSearchServiceGrpc.getSearchMethod = getSearchMethod =
              io.grpc.MethodDescriptor.<com.rpc.service.web.searcher.searxng.SearchRequest, com.rpc.service.web.searcher.searxng.SearchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Search"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.rpc.service.web.searcher.searxng.SearchRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.rpc.service.web.searcher.searxng.SearchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AsyncSearchServiceMethodDescriptorSupplier("Search"))
              .build();
        }
      }
    }
    return getSearchMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AsyncSearchServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AsyncSearchServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AsyncSearchServiceStub>() {
        @java.lang.Override
        public AsyncSearchServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AsyncSearchServiceStub(channel, callOptions);
        }
      };
    return AsyncSearchServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AsyncSearchServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AsyncSearchServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AsyncSearchServiceBlockingStub>() {
        @java.lang.Override
        public AsyncSearchServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AsyncSearchServiceBlockingStub(channel, callOptions);
        }
      };
    return AsyncSearchServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AsyncSearchServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AsyncSearchServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AsyncSearchServiceFutureStub>() {
        @java.lang.Override
        public AsyncSearchServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AsyncSearchServiceFutureStub(channel, callOptions);
        }
      };
    return AsyncSearchServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * 定义异步搜索服务
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * 执行一次搜索
     * </pre>
     */
    default void search(com.rpc.service.web.searcher.searxng.SearchRequest request,
        io.grpc.stub.StreamObserver<com.rpc.service.web.searcher.searxng.SearchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSearchMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service AsyncSearchService.
   * <pre>
   * 定义异步搜索服务
   * </pre>
   */
  public static abstract class AsyncSearchServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AsyncSearchServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service AsyncSearchService.
   * <pre>
   * 定义异步搜索服务
   * </pre>
   */
  public static final class AsyncSearchServiceStub
      extends io.grpc.stub.AbstractAsyncStub<AsyncSearchServiceStub> {
    private AsyncSearchServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AsyncSearchServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AsyncSearchServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 执行一次搜索
     * </pre>
     */
    public void search(com.rpc.service.web.searcher.searxng.SearchRequest request,
        io.grpc.stub.StreamObserver<com.rpc.service.web.searcher.searxng.SearchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSearchMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service AsyncSearchService.
   * <pre>
   * 定义异步搜索服务
   * </pre>
   */
  public static final class AsyncSearchServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AsyncSearchServiceBlockingStub> {
    private AsyncSearchServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AsyncSearchServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AsyncSearchServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 执行一次搜索
     * </pre>
     */
    public com.rpc.service.web.searcher.searxng.SearchResponse search(com.rpc.service.web.searcher.searxng.SearchRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSearchMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service AsyncSearchService.
   * <pre>
   * 定义异步搜索服务
   * </pre>
   */
  public static final class AsyncSearchServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<AsyncSearchServiceFutureStub> {
    private AsyncSearchServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AsyncSearchServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AsyncSearchServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 执行一次搜索
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.rpc.service.web.searcher.searxng.SearchResponse> search(
        com.rpc.service.web.searcher.searxng.SearchRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSearchMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEARCH = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEARCH:
          serviceImpl.search((com.rpc.service.web.searcher.searxng.SearchRequest) request,
              (io.grpc.stub.StreamObserver<com.rpc.service.web.searcher.searxng.SearchResponse>) responseObserver);
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

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSearchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.rpc.service.web.searcher.searxng.SearchRequest,
              com.rpc.service.web.searcher.searxng.SearchResponse>(
                service, METHODID_SEARCH)))
        .build();
  }

  private static abstract class AsyncSearchServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AsyncSearchServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.rpc.service.web.searcher.searxng.SearXNGProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AsyncSearchService");
    }
  }

  private static final class AsyncSearchServiceFileDescriptorSupplier
      extends AsyncSearchServiceBaseDescriptorSupplier {
    AsyncSearchServiceFileDescriptorSupplier() {}
  }

  private static final class AsyncSearchServiceMethodDescriptorSupplier
      extends AsyncSearchServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    AsyncSearchServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (AsyncSearchServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AsyncSearchServiceFileDescriptorSupplier())
              .addMethod(getSearchMethod())
              .build();
        }
      }
    }
    return result;
  }
}
