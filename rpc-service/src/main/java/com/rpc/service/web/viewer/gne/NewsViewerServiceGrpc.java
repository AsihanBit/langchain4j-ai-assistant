package com.rpc.service.web.viewer.gne;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.57.2)",
    comments = "Source: web_grpc.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class NewsViewerServiceGrpc {

  private NewsViewerServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.rpc.service.web.viewer.gne.NewsViewerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.rpc.service.web.viewer.gne.WebListRequest,
      com.rpc.service.web.viewer.gne.ContentListResponse> getViewPageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ViewPage",
      requestType = com.rpc.service.web.viewer.gne.WebListRequest.class,
      responseType = com.rpc.service.web.viewer.gne.ContentListResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.rpc.service.web.viewer.gne.WebListRequest,
      com.rpc.service.web.viewer.gne.ContentListResponse> getViewPageMethod() {
    io.grpc.MethodDescriptor<com.rpc.service.web.viewer.gne.WebListRequest, com.rpc.service.web.viewer.gne.ContentListResponse> getViewPageMethod;
    if ((getViewPageMethod = NewsViewerServiceGrpc.getViewPageMethod) == null) {
      synchronized (NewsViewerServiceGrpc.class) {
        if ((getViewPageMethod = NewsViewerServiceGrpc.getViewPageMethod) == null) {
          NewsViewerServiceGrpc.getViewPageMethod = getViewPageMethod =
              io.grpc.MethodDescriptor.<com.rpc.service.web.viewer.gne.WebListRequest, com.rpc.service.web.viewer.gne.ContentListResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ViewPage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.rpc.service.web.viewer.gne.WebListRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.rpc.service.web.viewer.gne.ContentListResponse.getDefaultInstance()))
              .setSchemaDescriptor(new NewsViewerServiceMethodDescriptorSupplier("ViewPage"))
              .build();
        }
      }
    }
    return getViewPageMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static NewsViewerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NewsViewerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NewsViewerServiceStub>() {
        @java.lang.Override
        public NewsViewerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NewsViewerServiceStub(channel, callOptions);
        }
      };
    return NewsViewerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static NewsViewerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NewsViewerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NewsViewerServiceBlockingStub>() {
        @java.lang.Override
        public NewsViewerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NewsViewerServiceBlockingStub(channel, callOptions);
        }
      };
    return NewsViewerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static NewsViewerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NewsViewerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NewsViewerServiceFutureStub>() {
        @java.lang.Override
        public NewsViewerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NewsViewerServiceFutureStub(channel, callOptions);
        }
      };
    return NewsViewerServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void viewPage(com.rpc.service.web.viewer.gne.WebListRequest request,
        io.grpc.stub.StreamObserver<com.rpc.service.web.viewer.gne.ContentListResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getViewPageMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service NewsViewerService.
   */
  public static abstract class NewsViewerServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return NewsViewerServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service NewsViewerService.
   */
  public static final class NewsViewerServiceStub
      extends io.grpc.stub.AbstractAsyncStub<NewsViewerServiceStub> {
    private NewsViewerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NewsViewerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NewsViewerServiceStub(channel, callOptions);
    }

    /**
     */
    public void viewPage(com.rpc.service.web.viewer.gne.WebListRequest request,
        io.grpc.stub.StreamObserver<com.rpc.service.web.viewer.gne.ContentListResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getViewPageMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service NewsViewerService.
   */
  public static final class NewsViewerServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<NewsViewerServiceBlockingStub> {
    private NewsViewerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NewsViewerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NewsViewerServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.rpc.service.web.viewer.gne.ContentListResponse viewPage(com.rpc.service.web.viewer.gne.WebListRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getViewPageMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service NewsViewerService.
   */
  public static final class NewsViewerServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<NewsViewerServiceFutureStub> {
    private NewsViewerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NewsViewerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NewsViewerServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.rpc.service.web.viewer.gne.ContentListResponse> viewPage(
        com.rpc.service.web.viewer.gne.WebListRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getViewPageMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_VIEW_PAGE = 0;

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
        case METHODID_VIEW_PAGE:
          serviceImpl.viewPage((com.rpc.service.web.viewer.gne.WebListRequest) request,
              (io.grpc.stub.StreamObserver<com.rpc.service.web.viewer.gne.ContentListResponse>) responseObserver);
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
          getViewPageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.rpc.service.web.viewer.gne.WebListRequest,
              com.rpc.service.web.viewer.gne.ContentListResponse>(
                service, METHODID_VIEW_PAGE)))
        .build();
  }

  private static abstract class NewsViewerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    NewsViewerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.rpc.service.web.viewer.gne.GNEProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("NewsViewerService");
    }
  }

  private static final class NewsViewerServiceFileDescriptorSupplier
      extends NewsViewerServiceBaseDescriptorSupplier {
    NewsViewerServiceFileDescriptorSupplier() {}
  }

  private static final class NewsViewerServiceMethodDescriptorSupplier
      extends NewsViewerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    NewsViewerServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (NewsViewerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new NewsViewerServiceFileDescriptorSupplier())
              .addMethod(getViewPageMethod())
              .build();
        }
      }
    }
    return result;
  }
}
