package com.aiassist.rpcservice.utils;

import com.aiassist.rpcservice.client.SearchServiceClient;
import com.rpc.service.web.searcher.searxng.SearchResultItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SearchWebUtils {
    @Autowired
    private SearchServiceClient searchServiceClient;

    public List<SearchResultItem> searchWeb(String keyWord) {
        log.info("SearchWebUtils 工具类, 搜索关键字: {}", keyWord);
        return searchServiceClient.search(keyWord);
    }

    /**
     * 搜索网页并返回格式化字符串
     *
     * @param keyWord 搜索关键字
     * @return 格式化后的搜索结果字符串
     */
    public String searchWebFormattedDirect(String keyWord) {
        log.info("SearchWebUtils 工具类, 搜索关键字: {}", keyWord);
        List<SearchResultItem> results = searchServiceClient.search(keyWord);
        return SearchResponseFormatter.toFormattedStringDirect(results);
    }

    /**
     * 打印搜索结果的辅助方法
     *
     * @param results 搜索结果列表
     */
    public void printResults(List<SearchResultItem> results) {
        if (results == null || results.isEmpty()) {
            log.info("未找到任何结果。");
            return;
        }
        System.out.println("=== 搜索结果 ===");
        for (int i = 0; i < results.size(); i++) {
            SearchResultItem item = results.get(i);
            log.info("{}. 标题: {}", i + 1, item.getTitle());
            log.info("   链接: {}", item.getUrl());
            log.info("   内容: {}", item.getContent());
            log.info("   ---");
        }
    }
}
