package com.aiassist.rpcservice.utils;

import com.rpc.service.web.searcher.searxng.SearchResultItem;

import java.util.List;

/**
 * 搜索结果格式化工具类
 * 提供多种格式化方法，将搜索结果列表转换为不同格式的字符串
 */
public class SearchResponseFormatter {

    // ========== 直接处理gRPC对象的方法 ==========

    /**
     * 直接格式化gRPC搜索结果为标准格式
     *
     * @param results gRPC搜索结果列表
     * @return 格式化后的字符串
     */
    public static String toFormattedStringDirect(List<SearchResultItem> results) {
        if (results == null || results.isEmpty()) {
            return "Search results: no results found";
        }

        StringBuilder sb = new StringBuilder(results.size() * 200);
        sb.append("=== Search Results ===\n");

        // 使用增强for循环避免get(i)调用
        int index = 1;
        for (SearchResultItem item : results) {
            sb.append("Result ").append(index).append("：\n")
                    .append("Title: ").append(item.getTitle()).append('\n')
                    .append("Link: ").append(item.getUrl()).append('\n')
                    .append("Content: ").append(item.getContent()).append("\n");
            sb.append("   ---\n");
            index++;
        }
        return sb.toString();
    }

    /**
     * 最少 append 版本
     *
     * @param results gRPC搜索结果列表
     * @return 格式化后的字符串
     */
    public static String toFormattedStringUltraFast(List<SearchResultItem> results) {
        if (results == null || results.isEmpty()) {
            return "=== 搜索结果 ===\n未找到任何结果。";
        }

        // 预估容量：每个结果大约200字符
        StringBuilder sb = new StringBuilder(results.size() * 200 + 50);
        sb.append("=== 搜索结果 ===\n");

        int index = 1;
        for (SearchResultItem item : results) {
            sb.append(index)
                    .append(". 标题: ")
                    .append(item.getTitle())
                    .append("\n   链接: ")
                    .append(item.getUrl())
                    .append("\n   内容: ")
                    .append(item.getContent())
                    .append("\n   ---\n");
            index++;
        }

        return sb.toString();
    }

    /**
     * 直接格式化gRPC搜索结果为单行格式
     *
     * @param results gRPC搜索结果列表
     * @return 单行格式的字符串
     */
    public static String toOneLineStringDirect(List<SearchResultItem> results) {
        if (results == null || results.isEmpty()) {
            return "搜索结果: 0条";
        }

        StringBuilder sb = new StringBuilder(100);
        sb.append("搜索结果: ").append(results.size()).append("条 - ");

        int limit = Math.min(3, results.size());
        int index = 0;
        for (SearchResultItem item : results) {
            if (index >= limit) break;
            if (index > 0) sb.append(", ");
            sb.append(item.getTitle());
            index++;
        }

        if (results.size() > 3) {
            sb.append(" 等").append(results.size()).append("条结果");
        }

        return sb.toString();
    }
}
