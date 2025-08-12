package com.aiassist.mediassist;

import com.aiassist.mediassist.ai.OpenAiAgent;
import com.aiassist.mediassist.context.UserContext;
import com.aiassist.mediassist.dto.entity.ChunkDocs;
import com.aiassist.mediassist.service.EmbeddingService;
import com.aiassist.mediassist.util.WeaviateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("dev")
class RagIntegrationTest {

    @Autowired
    private WeaviateUtils weaviateUtils;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private OpenAiAgent openAiAgent;

    @Test
    void testWeaviateConnection() {
        System.out.println("=== 测试Weaviate连接 ===");
        
        boolean connected = weaviateUtils.testConnection();
        System.out.println("连接状态: " + (connected ? "✅ 成功" : "❌ 失败"));
        
        if (connected) {
            var stats = weaviateUtils.getDocumentStats();
            System.out.println("文档统计: " + stats);
        }
    }

    @Test
    void testEmbeddingService() {
        System.out.println("=== 测试嵌入服务 ===");
        
        try {
            String testText = "测试文本嵌入功能";
            List<Float> embedding = embeddingService.getEmbedding(testText);
            
            System.out.println("输入文本: " + testText);
            System.out.println("嵌入向量维度: " + embedding.size());
            System.out.println("前5个维度值: " + embedding.subList(0, Math.min(5, embedding.size())));
            
            // 测试批量嵌入
            List<String> texts = List.of("文本1", "文本2", "文本3");
            List<List<Float>> batchEmbeddings = embeddingService.getBatchEmbeddings(texts);
            System.out.println("批量嵌入结果数量: " + batchEmbeddings.size());
            
        } catch (Exception e) {
            System.err.println("嵌入服务测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void testKnowledgeBaseSearch() {
        System.out.println("=== 测试知识库搜索 ===");
        
        try {
            // 测试语义搜索
            String query = "良子";
            List<ChunkDocs> results = weaviateUtils.searchSimilarDocuments(query, 3, 0.7f);
            
            System.out.println("查询: " + query);
            System.out.println("找到 " + results.size() + " 个相关文档:");
            
            for (int i = 0; i < results.size(); i++) {
                ChunkDocs doc = results.get(i);
                System.out.println((i + 1) + ". " + doc.getShortDescription());
                System.out.println("   相似度: " + String.format("%.2f", doc.getSimilarity() * 100) + "%");
                System.out.println("   关键词: " + doc.getKeywordsString());
                System.out.println("   摘要: " + doc.getTextSummary());
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("知识库搜索测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void testKeywordSearch() {
        System.out.println("=== 测试关键词搜索 ===");
        
        try {
            List<String> keywords = List.of("梁子", "良子");
            List<ChunkDocs> results = weaviateUtils.searchByKeywords(keywords, 5);
            
            System.out.println("关键词: " + keywords);
            System.out.println("找到 " + results.size() + " 个相关文档:");
            
            for (ChunkDocs doc : results) {
                System.out.println("- " + doc.getShortDescription());
                System.out.println("  文档ID: " + doc.getDocId());
                System.out.println("  关键词: " + doc.getKeywordsString());
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("关键词搜索测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void testGetDocumentById() {
        System.out.println("=== 测试文档ID查询 ===");
        
        try {
            String docId = "良子介绍-b263f61e";  // 使用示例中的文档ID
            List<ChunkDocs> documents = weaviateUtils.getDocumentById(docId);
            
            System.out.println("文档ID: " + docId);
            System.out.println("找到 " + documents.size() + " 个文档块:");
            
            for (ChunkDocs doc : documents) {
                System.out.println("块 " + doc.getChunkIndex() + ":");
                System.out.println("标题: " + doc.getTitle());
                System.out.println("内容: " + doc.getTextSummary());
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("文档ID查询测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void testRAGWithAI() {
        System.out.println("=== 测试AI + RAG集成 ===");
        
        // 设置用户上下文
        UserContext.setCurrentUserIp("127.0.0.1");
        
        try {
            // 测试知识库搜索功能
            String response1 = openAiAgent.chat("rag-test-1", "你好，请帮我查询一下良子的相关信息");
            System.out.println("问题1: 查询良子信息");
            System.out.println("AI回答: " + response1);
            System.out.println();
            
            // 测试知识库统计
            String response2 = openAiAgent.chat("rag-test-1", "知识库里有多少文档？");
            System.out.println("问题2: 知识库统计");
            System.out.println("AI回答: " + response2);
            System.out.println();
            
            // 测试关键词搜索
            String response3 = openAiAgent.chat("rag-test-1", "请搜索包含'梁子'关键词的文档");
            System.out.println("问题3: 关键词搜索");
            System.out.println("AI回答: " + response3);
            System.out.println();
            
        } finally {
            UserContext.clear();
        }
    }

    @Test
    void testRAGStreamingWithAI() {
        System.out.println("=== 测试AI + RAG流式集成 ===");
        
        // 设置用户上下文
        UserContext.setCurrentUserIp("127.0.0.1");
        
        try {
            System.out.println("问题: 流式查询良子相关信息");
            System.out.print("AI流式回答: ");
            
            openAiAgent.chatStream("rag-stream-test", "请详细介绍一下良子这个人物")
                .doOnNext(System.out::print)
                .doOnComplete(() -> System.out.println("\n=== 流式完成 ==="))
                .doOnError(error -> System.err.println("流式错误: " + error.getMessage()))
                .blockLast();
                
        } finally {
            UserContext.clear();
        }
    }

    @Test
    void testCosineSimilarity() {
        System.out.println("=== 测试余弦相似度计算 ===");
        
        try {
            List<Float> vector1 = embeddingService.getEmbedding("良子是谁");
            List<Float> vector2 = embeddingService.getEmbedding("梁子介绍");
            List<Float> vector3 = embeddingService.getEmbedding("天气怎么样");
            
            float similarity12 = embeddingService.calculateCosineSimilarity(vector1, vector2);
            float similarity13 = embeddingService.calculateCosineSimilarity(vector1, vector3);
            
            System.out.println("'良子是谁' vs '梁子介绍' 相似度: " + String.format("%.4f", similarity12));
            System.out.println("'良子是谁' vs '天气怎么样' 相似度: " + String.format("%.4f", similarity13));
            
            System.out.println("预期: 前者相似度应该高于后者");
            
        } catch (Exception e) {
            System.err.println("相似度测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
