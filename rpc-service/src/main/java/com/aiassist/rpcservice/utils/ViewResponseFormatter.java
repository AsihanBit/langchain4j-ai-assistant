package com.aiassist.rpcservice.utils;

import com.rpc.service.web.viewer.selectolax.ViewResponse;

/**
 * ViewResponse格式化工具类
 * 提供多种格式化方法，避免修改自动生成的gRPC代码
 */
public class ViewResponseFormatter {

    /**
     * 格式化为简洁的字符串
     */
    public static String toSimpleString(ViewResponse response) {
        if (response == null) {
            return "ViewResponse{null}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ViewResponse{");
        sb.append("url='").append(response.getUrl()).append('\'');
        sb.append(", title='").append(response.getTitle()).append('\'');
        sb.append(", content='").append(response.getTextContent()).append('\'');
        sb.append(", statusCode=").append(response.getStatusCode());
        sb.append(", processingTime=").append(response.getProcessingTime()).append("s");
        sb.append(", contentLength=").append(response.getTextContent().length());
        sb.append(", linksCount=").append(response.getLinksCount());
        sb.append(", imagesCount=").append(response.getImagesCount());

        if (!response.getErrorMessage().isEmpty()) {
            sb.append(", error='").append(response.getErrorMessage()).append('\'');
        }

        sb.append('}');
        return sb.toString();
    }

    /**
     * 格式化为详细的字符串
     */
    public static String toDetailedString(ViewResponse response) {
        if (response == null) {
            return "ViewResponse: null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 网页抓取结果 ===\n");
        sb.append("URL: ").append(response.getUrl()).append("\n");
        sb.append("标题: ").append(response.getTitle()).append("\n");
        sb.append("内容： ").append(response.getTextContent()).append('\n');
        sb.append("状态码: ").append(response.getStatusCode()).append("\n");
        sb.append("处理时间: ").append(response.getProcessingTime()).append("秒\n");
        sb.append("内容长度: ").append(response.getTextContent().length()).append("字符\n");
        sb.append("链接数量: ").append(response.getLinksCount()).append("\n");
        sb.append("图片数量: ").append(response.getImagesCount()).append("\n");

        if (!response.getErrorMessage().isEmpty()) {
            sb.append("错误信息: ").append(response.getErrorMessage()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 格式化为JSON字符串
     */
    public static String toJsonString(ViewResponse response) {
        if (response == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"url\":\"").append(escapeJson(response.getUrl())).append("\",");
        sb.append("\"title\":\"").append(escapeJson(response.getTitle())).append("\",");
        sb.append("\"content\":\"").append(escapeJson(response.getTextContent())).append("\",");
        sb.append("\"statusCode\":").append(response.getStatusCode()).append(",");
        sb.append("\"processingTime\":").append(response.getProcessingTime()).append(",");
        sb.append("\"contentLength\":").append(response.getTextContent().length()).append(",");
        sb.append("\"linksCount\":").append(response.getLinksCount()).append(",");
        sb.append("\"imagesCount\":").append(response.getImagesCount());

        if (!response.getErrorMessage().isEmpty()) {
            sb.append(",\"error\":\"").append(escapeJson(response.getErrorMessage())).append("\"");
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * 格式化为单行摘要
     */
    public static String toSummary(ViewResponse response) {
        if (response == null) {
            return "null";
        }

        return String.format("[%d] %s (%d chars, %.2fs)",
                response.getStatusCode(),
                response.getTitle().isEmpty() ? response.getUrl() : response.getTitle(),
                response.getTextContent().length(),
                response.getProcessingTime());
    }

    /**
     * 格式化内容预览
     */
    public static String toContentPreview(ViewResponse response, int maxLength) {
        if (response == null) {
            return "null";
        }

        String content = response.getTextContent();
        if (content.length() <= maxLength) {
            return content;
        }

        return content.substring(0, maxLength) + "...";
    }

    /**
     * 转义JSON字符串
     */
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 格式化为表格行（用于批量显示）
     */
    public static String toTableRow(ViewResponse response) {
        if (response == null) {
            return "| null | null | null | null |";
        }

        return String.format("| %-30s | %-20s | %3d | %6.2fs |",
                truncate(response.getUrl(), 30),
                truncate(response.getTitle(), 20),
                response.getStatusCode(),
                response.getProcessingTime());
    }

    /**
     * 截断字符串
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * 格式化为Markdown格式
     */
    public static String toMarkdown(ViewResponse response) {
        if (response == null) {
            return "**ViewResponse**: null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 网页抓取结果\n\n");
        sb.append("- **URL**: ").append(response.getUrl()).append("\n");
        sb.append("- **标题**: ").append(response.getTitle()).append("\n");
        sb.append("- **状态码**: ").append(response.getStatusCode()).append("\n");
        sb.append("- **处理时间**: ").append(response.getProcessingTime()).append("秒\n");
        sb.append("- **内容长度**: ").append(response.getTextContent().length()).append("字符\n");
        sb.append("- **链接数量**: ").append(response.getLinksCount()).append("\n");
        sb.append("- **图片数量**: ").append(response.getImagesCount()).append("\n");

        if (!response.getErrorMessage().isEmpty()) {
            sb.append("- **错误信息**: ").append(response.getErrorMessage()).append("\n");
        }

        return sb.toString();
    }
}
