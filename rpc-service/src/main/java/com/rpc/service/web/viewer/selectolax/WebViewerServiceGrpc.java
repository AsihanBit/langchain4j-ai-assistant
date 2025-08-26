package com.rpc.service.web.viewer.selectolax;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * 简易网页浏览 gRPC 服务定义
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.57.2)",
    comments = "Source: web_viewer.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class WebViewerServiceGrpc {

  private WebViewerServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.rpc.service.web.viewer.selectolax.WebViewerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.rpc.service.web.viewer.selectolax.ViewRequest,
      com.rpc.service.web.viewer.selectolax.ViewResponse> getViewWebPageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ViewWebPage",
      requestType = com.rpc.service.web.viewer.selectolax.ViewRequest.class,
      responseType = com.rpc.service.web.viewer.selectolax.ViewResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.rpc.service.web.viewer.selectolax.ViewRequest,
      com.rpc.service.web.viewer.selectolax.ViewResponse> getViewWebPageMethod() {
    io.grpc.MethodDescriptor<com.rpc.service.web.viewer.selectolax.ViewRequest, com.rpc.service.web.viewer.selectolax.ViewResponse> getViewWebPageMethod;
    if ((getViewWebPageMethod = WebViewerServiceGrpc.getViewWebPageMethod) == null) {
      synchronized (WebViewerServiceGrpc.class) {
        if ((getViewWebPageMethod = WebViewerServiceGrpc.getViewWebPageMethod) == null) {
          WebViewerServiceGrpc.getViewWebPageMethod = getViewWebPageMethod =
              io.grpc.MethodDescriptor.<com.rpc.service.web.viewer.selectolax.ViewRequest, com.rpc.service.web.viewer.selectolax.ViewResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ViewWebPage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.rpc.service.web.viewer.selectolax.ViewRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.rpc.service.web.viewer.selectolax.ViewResponse.getDefaultInstance()))
              .setSchemaDescriptor(new WebViewerServiceMethodDescriptorSupplier("ViewWebPage"))
              .build();
        }
      }
    }
    return getViewWebPageMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static WebViewerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WebViewerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WebViewerServiceStub>() {
        @java.lang.Override
        public WebViewerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WebViewerServiceStub(channel, callOptions);
        }
      };
    return WebViewerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static WebViewerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WebViewerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WebViewerServiceBlockingStub>() {
        @java.lang.Override
        public WebViewerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WebViewerServiceBlockingStub(channel, callOptions);
        }
      };
    return WebViewerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static WebViewerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<WebViewerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<WebViewerServiceFutureStub>() {
        @java.lang.Override
        public WebViewerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new WebViewerServiceFutureStub(channel, callOptions);
        }
      };
    return WebViewerServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * 简易网页浏览 gRPC 服务定义
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * 从URL提取网页内容
     * </pre>
     */
    default void viewWebPage(com.rpc.service.web.viewer.selectolax.ViewRequest request,
        io.grpc.stub.StreamObserver<com.rpc.service.web.viewer.selectolax.ViewResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getViewWebPageMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service WebViewerService.
   * <pre>
   * 简易网页浏览 gRPC 服务定义
   * </pre>
   */
  public static abstract class WebViewerServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return WebViewerServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service WebViewerService.
   * <pre>
   * 简易网页浏览 gRPC 服务定义
   * </pre>
   */
  public static final class WebViewerServiceStub
      extends io.grpc.stub.AbstractAsyncStub<WebViewerServiceStub> {
    private WebViewerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WebViewerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WebViewerServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 从URL提取网页内容
     * </pre>
     */
    public void viewWebPage(com.rpc.service.web.viewer.selectolax.ViewRequest request,
        io.grpc.stub.StreamObserver<com.rpc.service.web.viewer.selectolax.ViewResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getViewWebPageMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service WebViewerService.
   * <pre>
   * 简易网页浏览 gRPC 服务定义
   * </pre>
   */
  public static final class WebViewerServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<WebViewerServiceBlockingStub> {
    private WebViewerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WebViewerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WebViewerServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 从URL提取网页内容
     * </pre>
     */
    public com.rpc.service.web.viewer.selectolax.ViewResponse viewWebPage(com.rpc.service.web.viewer.selectolax.ViewRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getViewWebPageMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service WebViewerService.
   * <pre>
   * 简易网页浏览 gRPC 服务定义
   * </pre>
   */
  public static final class WebViewerServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<WebViewerServiceFutureStub> {
    private WebViewerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected WebViewerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new WebViewerServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 从URL提取网页内容
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.rpc.service.web.viewer.selectolax.ViewResponse> viewWebPage(
        com.rpc.service.web.viewer.selectolax.ViewRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getViewWebPageMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_VIEW_WEB_PAGE = 0;

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
        case METHODID_VIEW_WEB_PAGE:
          serviceImpl.viewWebPage((com.rpc.service.web.viewer.selectolax.ViewRequest) request,
              (io.grpc.stub.StreamObserver<com.rpc.service.web.viewer.selectolax.ViewResponse>) responseObserver);
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
          getViewWebPageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.rpc.service.web.viewer.selectolax.ViewRequest,
              com.rpc.service.web.viewer.selectolax.ViewResponse>(
                service, METHODID_VIEW_WEB_PAGE)))
        .build();
  }

  private static abstract class WebViewerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    WebViewerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.rpc.service.web.viewer.selectolax.SelectolaxProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("WebViewerService");
    }
  }

  private static final class WebViewerServiceFileDescriptorSupplier
      extends WebViewerServiceBaseDescriptorSupplier {
    WebViewerServiceFileDescriptorSupplier() {}
  }

  private static final class WebViewerServiceMethodDescriptorSupplier
      extends WebViewerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    WebViewerServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (WebViewerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new WebViewerServiceFileDescriptorSupplier())
              .addMethod(getViewWebPageMethod())
              .build();
        }
      }
    }
    return result;
  }
}
