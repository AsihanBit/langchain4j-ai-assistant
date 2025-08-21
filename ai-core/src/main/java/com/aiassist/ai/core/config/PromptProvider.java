package com.aiassist.ai.core.config;

import com.aiassist.ai.core.properties.PromptProperties;
import dev.langchain4j.data.message.SystemMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptProvider {
    private final PromptProperties promptProperties;
    private String prompt; // 缓存内容，避免每次 IO

    @PostConstruct
    void init() {
        Resource res = promptProperties.getPath();
        try (InputStream in = res.getInputStream()) {
            this.prompt = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            log.info("Loaded prompt from {}", res);
        } catch (Exception e) {
            log.warn("Failed to load prompt from {}. Using default.", res, e);
            this.prompt = "你是一个专业的助手，能够帮助用户解答相关问题。";
        }
    }

    public SystemMessage systemMessage() {
        return SystemMessage.from(prompt);
    }
}
