package com.aiassist.chat.core.tools;

import com.aiassist.rpcservice.utils.SearchWebUtils;
import com.aiassist.rpcservice.utils.ViewWebUtils;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Grpc远程调用的工具类
 */
@Slf4j
@Component
public class GrpcTools {
    @Autowired
    private SearchWebUtils searchWebUtils;
    @Autowired
    private ViewWebUtils viewWebUtils;

    @Tool(name = "web_search",
            value = "This method performs a web search function. It takes a 'keyWord' as input, retrieves the corresponding search results, and returns a list of search result items, each item is formatted in a readable format with title, URL, and content for each search result, which can be used to fetch and display web search results.")
    public String searchWeb(String keyWord) {
        String res = searchWebUtils.searchWebFormattedDirect(keyWord);
        log.info("TOOL_CALL searchWeb 结果: {}", res);
        return res;
    }

    @Tool(name = "get_web_content",
            value = "Retrieve and parse static web page content from the specified URL. Returns the structured content as a string.")
    public String viewWeb(String url) {
        String res = viewWebUtils.getWebContent(url, 8); // TODO 超时处理
        log.info("TOOL_CALL viewWeb 结果: {}", res);
        return res;
    }
}
