package com.aiassist.mediassist.service;

import java.util.List;

public interface EmbeddingService {
    
    /**
     * 获取单个文本的嵌入向量
     * @param text 文本内容
     * @return 嵌入向量
     */
    List<Float> getEmbedding(String text);
    
    /**
     * 批量获取文本的嵌入向量
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    List<List<Float>> getBatchEmbeddings(List<String> texts);
    
    /**
     * 计算两个向量的余弦相似度
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 相似度分数 (0-1之间)
     */
    float calculateCosineSimilarity(List<Float> vector1, List<Float> vector2);
}
