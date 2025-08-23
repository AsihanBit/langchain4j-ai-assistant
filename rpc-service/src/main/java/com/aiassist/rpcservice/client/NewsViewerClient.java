package com.aiassist.rpcservice.client;

/**
 * 引用嵌套在 NewsViewerProto 里的消息类型
 */

import com.rpc.service.web.viewer.gne.Content;
import com.rpc.service.web.viewer.gne.ContentListResponse;
import com.rpc.service.web.viewer.gne.NewsViewerServiceGrpc;
import com.rpc.service.web.viewer.gne.WebListRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class NewsViewerClient {

    private final ManagedChannel channel;
    private final NewsViewerServiceGrpc.NewsViewerServiceBlockingStub blockingStub;

    public NewsViewerClient(String host, int port, boolean plaintext) {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(host, port);
        if (plaintext) {
            builder.usePlaintext(); // Python 示例常用明文
        }
        this.channel = builder.build();
        this.blockingStub = NewsViewerServiceGrpc.newBlockingStub(channel);
    }

    public ContentListResponse viewPage(List<String> urls,
                                        int timeoutSeconds,
                                        Map<String, String> headers) {
        WebListRequest req = WebListRequest.newBuilder()
                .addAllUrls(urls)
                .setTimeoutSeconds(timeoutSeconds)
                .putAllCustomHeaders(headers)
                .build();
        try {
            // 设置调用超时，避免阻塞太久
            return blockingStub
                    .withDeadlineAfter(timeoutSeconds > 0 ? timeoutSeconds : 5, TimeUnit.SECONDS)
                    .viewPage(req);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("调用 gRPC 失败: " + e.getStatus(), e);
        }
    }

    public void shutdown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    // 测试
    public static void main(String[] args) throws Exception {
        NewsViewerClient client = new NewsViewerClient("localhost", 50051, true);
        try {
            List<String> urls = Arrays.asList(
                    "https://www.guancha.cn/TaimurRahman/2025_08_22_787370.shtml",
                    "https://www.guancha.cn/ShenYi/2025_08_22_787411.shtml",
                    "https://www.guancha.cn/IserlohnFortress/2025_08_22_787419.shtml"
            );
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "MyCrawler/1.0");

            ContentListResponse resp = client.viewPage(urls, 5, headers);

            for (Content c : resp.getContentsList()) {
                System.out.println("标题: " + c.getTitle());
                System.out.println("作者: " + c.getAuthor());
                System.out.println("时间: " + c.getPublishTime());
//                System.out.println("正文: " + c.getContent());
                System.out.println("图片数: " + c.getImagesCount());
                System.out.println("meta: " + c.getMetaMap());
                System.out.println("------");
            }
        } finally {
            client.shutdown();
        }
    }
}
