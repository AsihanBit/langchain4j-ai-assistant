package com.aiassist.mediassist.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CreateNewConversationRes {
    private String memoryId;
    private String message;
}
