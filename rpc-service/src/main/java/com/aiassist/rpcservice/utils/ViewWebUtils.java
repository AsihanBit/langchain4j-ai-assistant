package com.aiassist.rpcservice.utils;

import com.aiassist.rpcservice.client.SelectolaxViewerClient;
import com.rpc.service.web.viewer.selectolax.ViewResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ViewWebUtils {
    @Autowired
    private SelectolaxViewerClient selectolaxViewerClient;

    /**
     * 抓取网页内容 - 返回简洁格式的字符串
     *
     * @param url            要抓取的网页URL
     * @param timeoutSeconds 超时时间（秒）
     * @return 简洁格式的网页内容响应
     */
    public String getWebContent(String url, int timeoutSeconds) {
        ViewResponse viewResponse = selectolaxViewerClient.viewWebPage(url, timeoutSeconds, null, false, false, true, null, null);
        log.info("抓取内容长度: {}", viewResponse.getTextContent().length());

        // 使用格式化工具返回简洁的字符串
        return ViewResponseFormatter.toSimpleString(viewResponse);
    }

    /**
     * 抓取网页内容 - 返回详细格式的字符串
     *
     * @param url            要抓取的网页URL
     * @param timeoutSeconds 超时时间（秒）
     * @return 详细格式的网页内容响应
     */
    public String getWebContentDetailed(String url, int timeoutSeconds) {
        ViewResponse viewResponse = selectolaxViewerClient.viewWebPage(url, timeoutSeconds, null, false, false, true, null, null);
        log.info("抓取内容长度: {}", viewResponse.getTextContent().length());

        // 使用格式化工具返回详细的字符串
        return ViewResponseFormatter.toDetailedString(viewResponse);
    }

    /**
     * 抓取网页内容 - 返回包装对象（推荐）
     *
     * @param url            要抓取的网页URL
     * @param timeoutSeconds 超时时间（秒）
     * @return 包装后的网页内容响应，支持自定义toString
     */
    public String getWebContentWrapped(String url, int timeoutSeconds) {
        ViewResponse viewResponse = selectolaxViewerClient.viewWebPage(url, timeoutSeconds, null, false, false, true, null, null);
        log.info("抓取内容长度: {}", viewResponse.getTextContent().length());

        // 返回包装对象
        return ViewResponseFormatter.toSimpleString(viewResponse);
    }

    /**
     * 抓取网页内容 - 返回JSON格式字符串
     *
     * @param url            要抓取的网页URL
     * @param timeoutSeconds 超时时间（秒）
     * @return JSON格式的网页内容响应
     */
    public String getWebContentJson(String url, int timeoutSeconds) {
        ViewResponse viewResponse = selectolaxViewerClient.viewWebPage(url, timeoutSeconds, null, false, false, true, null, null);
        log.info("抓取内容长度: {}", viewResponse.getTextContent().length());

        // 使用格式化工具返回JSON字符串
        return ViewResponseFormatter.toJsonString(viewResponse);
    }

    /**
     * 抓取网页内容 - 带超时 截断
     *
     * @param url            要抓取的网页URL
     * @param timeoutSeconds 超时时间（秒）
     * @return 网页内容响应
     */
    public ViewResponse getWebContent(String url, int timeoutSeconds, int textStartPos, int textEndPos) {
        return selectolaxViewerClient.viewWebPage(url, timeoutSeconds, null, false, false, true, textStartPos, textEndPos);
    }

}
