package com.aiassist.mediassist.util;

import com.aiassist.mediassist.dto.entity.ChunkDocs;
import com.aiassist.mediassist.service.EmbeddingService;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.graphql.query.argument.NearVectorArgument;
import io.weaviate.client.v1.graphql.query.fields.Field;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WeaviateUtils {

    @Autowired
    private WeaviateClient weaviateClient;

    @Autowired
    private EmbeddingService embeddingService;

    @Value("${weaviate.collection-name:ChunkDocs}")
    private String collectionName;

    /**
     * 基于文本查询相似文档
     * @param queryText 查询文本
     * @param limit 返回结果数量限制
     * @param certainty 相似度阈值 (0.0-1.0)
     * @return 相似文档列表
     */
    public List<ChunkDocs> searchSimilarDocuments(String queryText, int limit, float certainty) {
        try {
            log.info("开始查询相似文档，查询文本: {}, 限制: {}, 阈值: {}", queryText, limit, certainty);
            
            // 获取查询文本的嵌入向量
            List<Float> queryVector = embeddingService.getEmbedding(queryText);
            
            // 构建Weaviate查询
            Result<GraphQLResponse> result = weaviateClient.graphQL().get()
                .withClassName(collectionName)
                .withFields(
                    Field.builder().name("doc_id").build(),
                    Field.builder().name("chunk_index").build(),
                    Field.builder().name("title").build(),
                    Field.builder().name("section_title").build(),
                    Field.builder().name("tags").build(),
                    Field.builder().name("keywords").build(),
                    Field.builder().name("source_path").build(),
                    Field.builder().name("text").build(),
                    Field.builder().name("_additional").fields(
                        Field.builder().name("vector").build(),
                        Field.builder().name("certainty").build()
                    ).build()
                )
                .withNearVector(NearVectorArgument.builder()
                    .vector(queryVector.toArray(new Float[0]))
                    .certainty(certainty)
                    .build())
                .withLimit(limit)
                .run();

            if (result.hasErrors()) {
                log.error("Weaviate查询出错: {}", result.getError().getMessages());
                return new ArrayList<>();
            }

            return parseGraphQLResponse(result.getResult());

        } catch (Exception e) {
            log.error("搜索相似文档失败", e);
            throw new RuntimeException("搜索相似文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据关键词查询文档（简化版，不使用Where条件）
     * @param keywords 关键词列表
     * @param limit 返回结果数量限制
     * @return 相关文档列表
     */
    public List<ChunkDocs> searchByKeywords(List<String> keywords, int limit) {
        try {
            log.info("根据关键词查询文档: {}", keywords);
            
            if (keywords.isEmpty()) {
                return new ArrayList<>();
            }

            // 简化实现：将关键词组合成查询文本，使用向量搜索
            String queryText = String.join(" ", keywords);
            return searchSimilarDocuments(queryText, limit, 0.7f);

        } catch (Exception e) {
            log.error("根据关键词查询文档失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据文档ID查询文档
     * @param docId 文档ID
     * @return 文档列表
     */
    public List<ChunkDocs> getDocumentById(String docId) {
        try {
            log.info("根据文档ID查询: {}", docId);

            // 简化查询，直接获取所有文档
            Result<GraphQLResponse> result = weaviateClient.graphQL().get()
                .withClassName(collectionName)
                .withFields(
                    Field.builder().name("doc_id").build(),
                    Field.builder().name("chunk_index").build(),
                    Field.builder().name("title").build(),
                    Field.builder().name("section_title").build(),
                    Field.builder().name("tags").build(),
                    Field.builder().name("keywords").build(),
                    Field.builder().name("source_path").build(),
                    Field.builder().name("text").build()
                )
                .withLimit(100)  // 一个文档可能有多个chunk
                .run();

            if (result.hasErrors()) {
                log.error("文档ID查询出错: {}", result.getError().getMessages());
                return new ArrayList<>();
            }

            // 过滤结果，只返回匹配的文档ID
            List<ChunkDocs> allResults = parseGraphQLResponse(result.getResult());
            return allResults.stream()
                .filter(doc -> docId.equals(doc.getDocId()))
                .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("根据文档ID查询失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取文档统计信息
     * @return 统计信息
     */
    public Map<String, Object> getDocumentStats() {
        try {
            Result<GraphQLResponse> result = weaviateClient.graphQL().aggregate()
                .withClassName(collectionName)
                .withFields(
                    Field.builder().name("meta").fields(
                        Field.builder().name("count").build()
                    ).build()
                )
                .run();

            if (result.hasErrors()) {
                log.error("获取统计信息出错: {}", result.getError().getMessages());
                return Map.of("total", 0);
            }

            GraphQLResponse response = result.getResult();
            Object dataObj = response.getData();
            if (!(dataObj instanceof Map)) {
                return Map.of("total", 0);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            if (data != null && data.containsKey("Aggregate")) {
                Object aggregateObj = data.get("Aggregate");
                if (aggregateObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> aggregate = (Map<String, Object>) aggregateObj;
                    if (aggregate.containsKey(collectionName)) {
                        Object collectionsObj = aggregate.get(collectionName);
                        if (collectionsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> collections = (List<Map<String, Object>>) collectionsObj;
                            if (!collections.isEmpty()) {
                                Object metaObj = collections.get(0).get("meta");
                                if (metaObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> meta = (Map<String, Object>) metaObj;
                                    return Map.of("total", meta.get("count"));
                                }
                            }
                        }
                    }
                }
            }

            return Map.of("total", 0);

        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return Map.of("total", 0, "error", e.getMessage());
        }
    }

    /**
     * 解析GraphQL响应
     */
    private List<ChunkDocs> parseGraphQLResponse(GraphQLResponse response) {
        List<ChunkDocs> results = new ArrayList<>();
        
        try {
            Object dataObj = response.getData();
            if (!(dataObj instanceof Map)) {
                return results;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dataObj;
            if (data != null && data.containsKey("Get")) {
                Object getObj = data.get("Get");
                if (getObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> get = (Map<String, Object>) getObj;
                    if (get.containsKey(collectionName)) {
                        Object documentsObj = get.get(collectionName);
                        if (documentsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> documents = (List<Map<String, Object>>) documentsObj;
                    
                            for (Map<String, Object> doc : documents) {
                                ChunkDocs chunkDoc = new ChunkDocs();
                                
                                chunkDoc.setDocId((String) doc.get("doc_id"));
                                Object chunkIndexObj = doc.get("chunk_index");
                                if (chunkIndexObj instanceof Number) {
                                    chunkDoc.setChunkIndex(((Number) chunkIndexObj).intValue());
                                }
                                chunkDoc.setTitle((String) doc.get("title"));
                                chunkDoc.setSectionTitle((String) doc.get("section_title"));
                                chunkDoc.setTags((List<String>) doc.get("tags"));
                                chunkDoc.setKeywords((List<String>) doc.get("keywords"));
                                chunkDoc.setSourcePath((String) doc.get("source_path"));
                                chunkDoc.setText((String) doc.get("text"));
                                
                                // 处理_additional字段
                                if (doc.containsKey("_additional")) {
                                    Object additionalObj = doc.get("_additional");
                                    if (additionalObj instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> additional = (Map<String, Object>) additionalObj;
                                        if (additional.containsKey("certainty")) {
                                            chunkDoc.setSimilarity(((Number) additional.get("certainty")).floatValue());
                                        }
                                        if (additional.containsKey("vector")) {
                                            Object vectorObj = additional.get("vector");
                                            if (vectorObj instanceof List) {
                                                @SuppressWarnings("unchecked")
                                                List<Number> vectorNumbers = (List<Number>) vectorObj;
                                                List<Float> vector = vectorNumbers.stream()
                                                    .map(Number::floatValue)
                                                    .toList();
                                                chunkDoc.setVector(vector);
                                            }
                                        }
                                    }
                                }
                                
                                results.add(chunkDoc);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析GraphQL响应失败", e);
        }
        
        log.info("解析到 {} 个文档结果", results.size());
        return results;
    }

    /**
     * 测试Weaviate连接
     */
    public boolean testConnection() {
        try {
            Result<Boolean> result = weaviateClient.misc().liveChecker().run();
            boolean isAlive = result.getResult();
            log.info("Weaviate连接测试: {}", isAlive ? "成功" : "失败");
            return isAlive;
        } catch (Exception e) {
            log.error("Weaviate连接测试失败", e);
            return false;
        }
    }
}
