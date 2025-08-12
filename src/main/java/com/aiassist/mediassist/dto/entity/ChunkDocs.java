package com.aiassist.mediassist.dto.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkDocs {
    
    /**
     * 文档ID
     */
    private String docId;
    
    /**
     * 块索引
     */
    private Integer chunkIndex;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 章节标题
     */
    private String sectionTitle;
    
    /**
     * 标签列表
     */
    private List<String> tags;
    
    /**
     * 关键词列表
     */
    private List<String> keywords;
    
    /**
     * 源文件路径
     */
    private String sourcePath;
    
    /**
     * 文本内容
     */
    private String text;
    
    /**
     * 向量表示（用于相似度计算）
     */
    private List<Float> vector;
    
    /**
     * 相似度分数（查询时使用）
     */
    private Float similarity;
    
    /**
     * 构造方法 - 用于创建查询结果
     */
    public ChunkDocs(String docId, Integer chunkIndex, String title, String text, Float similarity) {
        this.docId = docId;
        this.chunkIndex = chunkIndex;
        this.title = title;
        this.text = text;
        this.similarity = similarity;
    }
    
    /**
     * 获取文档的简短描述
     */
    public String getShortDescription() {
        String desc = title != null ? title : "无标题文档";
        if (sectionTitle != null && !sectionTitle.isEmpty()) {
            desc += " - " + sectionTitle;
        }
        return desc;
    }
    
    /**
     * 获取关键词字符串
     */
    public String getKeywordsString() {
        if (keywords == null || keywords.isEmpty()) {
            return "";
        }
        return String.join(", ", keywords);
    }
    
    /**
     * 获取标签字符串
     */
    public String getTagsString() {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(", ", tags);
    }
    
    /**
     * 获取文本摘要（前100个字符）
     */
    public String getTextSummary() {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
