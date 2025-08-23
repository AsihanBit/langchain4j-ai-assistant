package com.aiassist.chat.core.tools;

import com.aiassist.rpcservice.dto.SearchResultItemDTO;
import com.aiassist.rpcservice.utils.SearchWebUtils;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Grpc远程调用的工具类
 */
@Slf4j
@Component
public class GrpcTools {
    @Autowired
    private SearchWebUtils searchWebUtils;

    @Tool(name = "web_content_search", value = "This method performs a web search function. It takes a 'keyWord' as input, retrieves the corresponding search results, and returns a list of search result items. Each result item includes a title (title), a URL (url), and content (content), which can be used to fetch and display web search results.")
    public List<SearchResultItemDTO> crypt(String keyWord) {
        List<SearchResultItemDTO> res = searchWebUtils.searchWeb(keyWord);
        return res;
    }
}
