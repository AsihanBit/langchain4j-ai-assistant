package com.aiassist.ai.core.utils;

import com.aiassist.ai.core.entity.ChunkDocs;
import com.aiassist.ai.core.service.EmbeddingService;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
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

/**
 * Weaviate 实用工具类
 * <p>
 * 功能概览：
 * - 提供基于向量的相似检索（nearVector）与简化的关键词检索
 * - 提供按 doc_id 聚合整篇文档片段、统计信息与连通性检测
 * - 统一解析 GraphQL 响应为领域对象 {@link com.aiassist.ai.core.entity.ChunkDocs}
 * <p>
 * 重要参数与调优建议：
 * - collectionName：类名（集合名），默认 "ChunkDocs"，可在 application.yml 的 weaviate.collection-name 覆盖
 * - limit：返回的候选数量。RAG 召回建议 20~50，随后在应用层做排序/裁剪为 TopK（如 5）
 * - certainty：相似度阈值（Weaviate v1 GraphQL 中的 _additional.certainty，范围 0.0~1.0）。
 * 常见中文问答场景可取 0.15~0.35 之间，值越低召回越多、噪声也更多；值越高更精但可能漏召回。
 * - 字段：本类默认取回 doc_id、chunk_index、title、section_title、tags、keywords、source_path、text
 * 以及 _additional 中的 vector 与 certainty，便于后续调试与重排。
 */
@Slf4j
@Component
public class WeaviateUtils {

    @Autowired
    private WeaviateClient weaviateClient;

    @Autowired
    private EmbeddingService embeddingService;

    /**
     * Weaviate 中的类名（集合名）。
     * 默认使用 "ChunkDocs"，可在 application.yml 通过 weaviate.collection-name 调整。
     */
    @Value("${weaviate.collection-name:ChunkDocs}")
    private String collectionName;

    /**
     * 基于文本（语义向量）查询相似文档。
     * <p>
     * 参数说明与建议：
     * - queryText：用户查询文本，将通过 EmbeddingService 转为向量
     * - limit：返回结果数量上限。用于“召回”阶段建议设置为 20~50，再在应用层筛选 TopK 注入模型
     * - certainty：相似度阈值（0.0~1.0）。中文场景常用 0.2 左右；过高会漏召回，过低会引入噪声
     * <p>
     * 返回：按 Weaviate 的近邻排序返回的候选列表，并映射为 {@link ChunkDocs}
     */
    public List<ChunkDocs> searchSimilarDocuments(String queryText, int limit, float certainty) {
        try {
            log.info("开始查询相似文档，查询文本: {}, 限制: {}, 阈值: {}", queryText, limit, certainty);

            // 获取查询文本的嵌入向量
            List<Float> queryVector = embeddingService.getEmbedding(queryText);

            // 构建 Weaviate GraphQL 查询：Get -> withNearVector + withLimit
            // 说明：certainty 是 Weaviate 旧版 GraphQL 的语义相似度分值，并不等同于余弦相似度；
            // 值越大越相似。此处取回 _additional.certainty 便于排序/调试。
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
     * 根据关键词查询文档（简化版）。
     * <p>
     * 实现方式：将关键词拼接为查询文本，再走向量检索。
     * 若要精细匹配（AND/OR、字段过滤、BM25），建议改为使用 GraphQL where/bm25 API。
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
     * 根据文档 ID 查询所有相关的分块。
     * <p>
     * 注意：此处为演示实现，直接拉取集合中最多 100 条后在内存中过滤 doc_id。
     * 若数据量较大，请改为 where 过滤（eq: doc_id）以减少传输量。
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
     * 获取集合统计信息（当前仅返回 count）。
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
     * 解析 Weaviate GraphQL 响应为业务对象列表。
     * <p>
     * 字段映射：
     * - doc_id, chunk_index, title, section_title, tags, keywords, source_path, text
     * - _additional.certainty -> {@link ChunkDocs#setSimilarity(Float)}
     * - _additional.vector    -> {@link ChunkDocs#setVector(List)}（用于调试/可视化）
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
     * 测试 Weaviate 连接可用性（/v1/.well-known/ready 类似的存活检查）。
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
