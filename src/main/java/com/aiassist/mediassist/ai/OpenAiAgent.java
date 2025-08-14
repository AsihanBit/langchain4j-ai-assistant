package com.aiassist.mediassist.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(wiringMode = AiServiceWiringMode.AUTOMATIC,
        chatMemoryProvider = "chatMemoryProviderOpenAi",
        tools = {"textTools"})
public interface OpenAiAgent {

    @SystemMessage(fromResource = "prompts/system-prompt.txt")

        // 非流式
    String chat(@MemoryId String memoryId, @UserMessage String input);

    // 流式：直接返回 Flux（LangChain4j 会自动适配）
    Flux<String> chatStream(@MemoryId String memoryId, @UserMessage String input);

    // 便捷方法：直接返回 Flux（适合前端调用）
    default Flux<String> chatFlux(String memoryId, String input) {
        return chatStream(memoryId, input);
    }
}
