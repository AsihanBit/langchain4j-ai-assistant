package com.aiassist.rpcservice.utils;

import com.aiassist.rpcservice.client.SearchServiceClient;
import com.aiassist.rpcservice.dto.SearchResultItemDTO;
import com.rpc.service.web.searcher.searxng.SearchResultItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Component
public class SearchWebUtils {
    @Autowired
    private SearchServiceClient searchServiceClient;

    public List<SearchResultItemDTO> searchWeb(String keyWord) {
        log.info("SearchWebUtils 工具类, 搜索关键字: {}", keyWord);
        List<SearchResultItem> results = searchServiceClient.search(keyWord);
        printResults(results);

        return results.stream()
                .map(this::toDto)
                .toList();
    }

    private SearchResultItemDTO toDto(SearchResultItem it) { // todo 提高 Bean 拷贝性能
        return new SearchResultItemDTO(it.getTitle(), it.getUrl(), it.getContent());
    }

    /**
     * 打印搜索结果的辅助方法
     *
     * @param results 搜索结果列表
     */
    public void printResults(List<SearchResultItem> results) {
        if (results == null || results.isEmpty()) {
            System.out.println("未找到任何结果。");
            return;
        }
        System.out.println("=== 搜索结果 ===");
        for (int i = 0; i < results.size(); i++) {
            SearchResultItem item = results.get(i);
            System.out.printf("%d. 标题: %s%n", i + 1, item.getTitle());
            System.out.printf("   链接: %s%n", item.getUrl());
            System.out.printf("   内容: %s%n", item.getContent());
            System.out.println("   ---");
        }
    }
}
