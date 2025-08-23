package com.aiassist.ai.core.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chat.prompt")
public class PromptProperties {
    //    @Value("${chat.prompt.path}")
    private Resource path; // 支持 classpath:/、file:/、http:/ 等
}