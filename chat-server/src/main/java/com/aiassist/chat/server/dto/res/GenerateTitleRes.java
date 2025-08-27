package com.aiassist.chat.server.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerateTitleRes {
    private String memoryId;
    private String title;
}
