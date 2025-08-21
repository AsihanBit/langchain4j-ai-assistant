package com.aiassist.chat.server.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CreateConversationRes {
    private String memoryId;
    private String message;
}
