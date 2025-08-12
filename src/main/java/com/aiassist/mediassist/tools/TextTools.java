package com.aiassist.mediassist.tools;

import com.aiassist.mediassist.context.UserContext;
import com.aiassist.mediassist.dto.entity.ChunkDocs;
import com.aiassist.mediassist.dto.entity.User;
import com.aiassist.mediassist.service.UserService;
import com.aiassist.mediassist.util.WeaviateUtils;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TextTools {

    @Autowired
    private UserService userService;

    @Autowired
    private WeaviateUtils weaviateUtils;

    @Tool(name="encrypt_password", value="Encrypt password using encryption algorithm")
    public String crypt(int password) {
        int num=0;
        while(password>0){
            num=num*10+password%10;
            password=password/10;
        }
        String str = "Crypt超级加密: ";
        return str + num;
    }

    @Tool(name = "get_user_ip_address", value = "Obtain the current user's IP address")
    public String getClientIp() {
        String clientIp = UserContext.getCurrentUserIp();
        log.info("AI使用了IP工具类");
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return "无法获取用户IP地址，请确保通过HTTP接口访问";
        }
        return "用户IP地址: " + clientIp;
    }
    
    @Tool(name = "remember_user_name", value = "Remember and bind user's name with their IP address")
    public String rememberUserName(String userName) {
        String currentIp = UserContext.getCurrentUserIp();
        if (currentIp == null || currentIp.isEmpty()) {
            return "无法获取用户IP，无法绑定用户名";
        }
        
        if (userName == null || userName.trim().isEmpty()) {
            return "用户名不能为空";
        }
        
        try {
            User user = userService.recordUserVisit(userName.trim(), currentIp);
            if (user.getVisitCount() == 1) {
                return String.format("好的，我已经记住你是 %s！下次你访问时我就能认出你了。", userName.trim());
            } else {
                return String.format("我已经认识你了，%s！这是你第 %d 次访问，很高兴再次见到你。", 
                    userName.trim(), user.getVisitCount());
            }
        } catch (Exception e) {
            log.error("保存用户信息时出错", e);
            return "保存用户信息时出错：" + e.getMessage();
        }
    }
    
    @Tool(name = "check_user_identity", value = "Check if current user is already known by their IP address")
    public String checkUserIdentity() {
        String currentIp = UserContext.getCurrentUserIp();
        if (currentIp == null || currentIp.isEmpty()) {
            return "无法获取用户IP地址";
        }
        
        User user = userService.getUserByIpAddress(currentIp);
        if (user != null) {
            // 更新访问信息
            userService.updateUserVisit(user.getId());
            return String.format("我认识你！你是 %s，这是你第 %d 次访问，上次见面是 %s。欢迎回来！", 
                user.getUserName(), 
                user.getVisitCount() + 1,  // +1 因为刚刚更新了访问次数
                user.getLastSeen().toString());
        } else {
            return "这是我第一次见到来自 " + currentIp + " 的用户，请告诉我你的名字，我会记住你的。";
        }
    }
    
    @Tool(name = "get_all_known_users", value = "Get a list of all users I have met before")
    public String getAllKnownUsers() {
        var allUsers = userService.getAllUsers();
        if (allUsers.isEmpty()) {
            return "我还没有认识任何用户。";
        }
        
        StringBuilder sb = new StringBuilder("我认识的用户有：\n");
        for (User user : allUsers) {
            sb.append(String.format("- %s (IP: %s, 访问次数: %d, 最后访问: %s)\n", 
                user.getUserName(),
                user.getIpAddress(),
                user.getVisitCount(),
                user.getLastSeen().toString()));
        }
        
        return sb.toString();
    }
    
    @Tool(name = "get_user_visit_ranking", value = "Get user ranking by visit count")
    public String getUserVisitRanking() {
        var users = userService.getUsersByVisitCount();
        if (users.isEmpty()) {
            return "暂无用户访问记录。";
        }
        
        StringBuilder sb = new StringBuilder("用户访问排行榜：\n");
        for (int i = 0; i < users.size() && i < 10; i++) {
            User user = users.get(i);
            sb.append(String.format("%d. %s - %d次访问\n", 
                i + 1, user.getUserName(), user.getVisitCount()));
        }
        
        return sb.toString();
    }
    
    @Tool(name = "search_knowledge_base", value = "Search for relevant information from the knowledge base using semantic similarity")
    public String searchKnowledgeBase(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "查询内容不能为空";
        }
        
        try {
            log.info("AI使用知识库搜索工具，查询: {}", query);
            
            // 搜索相似文档，相似度阈值0.7，最多返回5个结果
            List<ChunkDocs> results = weaviateUtils.searchSimilarDocuments(query.trim(), 5, 0.3f);
            
            if (results.isEmpty()) {
                return "很抱歉，在知识库中没有找到与\"" + query + "\"相关的信息。";
            }
            
            StringBuilder response = new StringBuilder();
            response.append("根据你的查询\"").append(query).append("\"，我在知识库中找到了以下相关信息：\n\n");
            
            for (int i = 0; i < results.size(); i++) {
                ChunkDocs doc = results.get(i);
                response.append("📄 **").append(i + 1).append(". ").append(doc.getShortDescription()).append("**\n");
                response.append("📍 相似度: ").append(String.format("%.2f", doc.getSimilarity() * 100)).append("%\n");
                response.append("🔑 关键词: ").append(doc.getKeywordsString()).append("\n");
                response.append("📝 内容摘要: ").append(doc.getTextSummary()).append("\n");
                response.append("📂 来源: ").append(doc.getSourcePath()).append("\n\n");
            }
            
            response.append("💡 如需查看完整内容，请告诉我你感兴趣的文档编号。");
            return response.toString();
            
        } catch (Exception e) {
            log.error("知识库搜索失败", e);
            return "知识库搜索时出现错误: " + e.getMessage();
        }
    }
    
    @Tool(name = "get_document_content", value = "Get the full content of a specific document by doc_id")
    public String getDocumentContent(String docId) {
        if (docId == null || docId.trim().isEmpty()) {
            return "文档ID不能为空";
        }
        
        try {
            log.info("AI使用文档获取工具，文档ID: {}", docId);
            
            List<ChunkDocs> documents = weaviateUtils.getDocumentById(docId.trim());
            
            if (documents.isEmpty()) {
                return "未找到ID为\"" + docId + "\"的文档。";
            }
            
            // 按chunk_index排序
            documents.sort((a, b) -> {
                Integer indexA = a.getChunkIndex() != null ? a.getChunkIndex() : 0;
                Integer indexB = b.getChunkIndex() != null ? b.getChunkIndex() : 0;
                return indexA.compareTo(indexB);
            });
            
            StringBuilder content = new StringBuilder();
            content.append("📄 **文档内容**\n");
            content.append("🆔 文档ID: ").append(docId).append("\n");
            content.append("📝 标题: ").append(documents.get(0).getTitle()).append("\n");
            content.append("📂 来源: ").append(documents.get(0).getSourcePath()).append("\n");
            content.append("🔑 关键词: ").append(documents.get(0).getKeywordsString()).append("\n\n");
            
            content.append("📖 **完整内容:**\n");
            for (ChunkDocs doc : documents) {
                if (doc.getSectionTitle() != null && !doc.getSectionTitle().isEmpty()) {
                    content.append("\n## ").append(doc.getSectionTitle()).append("\n");
                }
                content.append(doc.getText()).append("\n");
            }
            
            return content.toString();
            
        } catch (Exception e) {
            log.error("获取文档内容失败", e);
            return "获取文档内容时出现错误: " + e.getMessage();
        }
    }
    
    @Tool(name = "search_by_keywords", value = "Search documents by specific keywords")
    public String searchByKeywords(String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return "关键词不能为空";
        }
        
        try {
            log.info("AI使用关键词搜索工具，关键词: {}", keywords);
            
            // 分割关键词
            List<String> keywordList = List.of(keywords.trim().split("[,，\\s]+"))
                .stream()
                .filter(kw -> !kw.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toList());
            
            if (keywordList.isEmpty()) {
                return "请提供有效的关键词";
            }
            
            List<ChunkDocs> results = weaviateUtils.searchByKeywords(keywordList, 10);
            
            if (results.isEmpty()) {
                return "没有找到包含关键词\"" + keywords + "\"的文档。";
            }
            
            StringBuilder response = new StringBuilder();
            response.append("🔍 根据关键词\"").append(keywords).append("\"找到了 ").append(results.size()).append(" 个相关文档：\n\n");
            
            for (int i = 0; i < results.size(); i++) {
                ChunkDocs doc = results.get(i);
                response.append("📄 ").append(i + 1).append(". ").append(doc.getShortDescription()).append("\n");
                response.append("🆔 文档ID: ").append(doc.getDocId()).append("\n");
                response.append("🔑 匹配关键词: ").append(doc.getKeywordsString()).append("\n");
                response.append("📝 内容预览: ").append(doc.getTextSummary()).append("\n\n");
            }
            
            return response.toString();
            
        } catch (Exception e) {
            log.error("关键词搜索失败", e);
            return "关键词搜索时出现错误: " + e.getMessage();
        }
    }
    
    @Tool(name = "get_knowledge_base_stats", value = "Get statistics about the knowledge base")
    public String getKnowledgeBaseStats() {
        try {
            log.info("AI使用知识库统计工具");
            
            var stats = weaviateUtils.getDocumentStats();
            boolean connectionOk = weaviateUtils.testConnection();
            
            StringBuilder response = new StringBuilder();
            response.append("📊 **知识库状态报告**\n\n");
            response.append("🔗 连接状态: ").append(connectionOk ? "✅ 正常" : "❌ 连接失败").append("\n");
            response.append("📚 文档总数: ").append(stats.get("total")).append(" 个文档块\n");
            
            if (stats.containsKey("error")) {
                response.append("⚠️ 错误信息: ").append(stats.get("error")).append("\n");
            }
            
            response.append("\n💡 可以使用以下功能:\n");
            response.append("- 🔍 语义搜索: search_knowledge_base\n");
            response.append("- 🔑 关键词搜索: search_by_keywords\n");
            response.append("- 📄 获取完整文档: get_document_content\n");
            
            return response.toString();
            
        } catch (Exception e) {
            log.error("获取知识库统计失败", e);
            return "获取知识库统计时出现错误: " + e.getMessage();
        }
    }
}
