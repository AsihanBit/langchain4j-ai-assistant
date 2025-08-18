package com.aiassist.mediassist.dto.res.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatStringRes {
    private String memoryId;
    private String message;
}
