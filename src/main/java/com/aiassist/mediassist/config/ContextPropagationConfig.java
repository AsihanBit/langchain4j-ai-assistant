package com.aiassist.mediassist.config;

import com.aiassist.mediassist.context.UserIpThreadLocalAccessor;
import io.micrometer.context.ContextRegistry;
import reactor.core.publisher.Hooks;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContextPropagationConfig {

    @PostConstruct
    public void init() {
        Hooks.enableAutomaticContextPropagation(); // 关键：启用 Reactor 自动传播
        ContextRegistry.getInstance().registerThreadLocalAccessor(new UserIpThreadLocalAccessor());
        // 可选：如果需要 memoryId，也注册对应 accessor
        // ContextRegistry.getInstance().registerThreadLocalAccessor(new MemoryIdThreadLocalAccessor());
    }
}
