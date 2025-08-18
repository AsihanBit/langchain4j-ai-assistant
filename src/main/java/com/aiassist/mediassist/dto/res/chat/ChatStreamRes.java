package com.aiassist.mediassist.dto.res.chat;

import lombok.Builder;
import lombok.Data;
import reactor.core.publisher.Flux;

@Data
@Builder
public class ChatStreamRes {
    private String memoryId;
    private Flux<String> contentStream;
}
