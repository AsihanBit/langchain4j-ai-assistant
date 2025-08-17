package com.aiassist.mediassist.cache;

import dev.langchain4j.data.message.ChatMessage;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;
import java.util.Optional;

/**
 * 请求作用域的聊天记忆缓存。
 * 每个HTTP请求都会拥有一个独立的实例。
 * 请求结束后，这个实例和它里面的数据都会被销毁。
 */
@Component
@RequestScope
@Getter
@Setter
public class RequestScopeChatMemoryCache {

    // 使用 Optional 来区分 “缓存了但内容为空” 和 “尚未缓存”
    private Optional<List<ChatMessage>> messages = Optional.empty();

    public boolean isCached() {
        return messages.isPresent();
    }

    public List<ChatMessage> get() {
        return messages.orElse(null); // 或者 orElseThrow
    }

    public void cache(List<ChatMessage> messagesToCache) {
        this.messages = Optional.ofNullable(messagesToCache);
    }
}
