package com.aiassist.chat.server.dto.res;

import com.aiassist.ai.core.entity.Conversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationsRes {
    private List<Conversation> conversations;
    private Integer totalCount;

    // TODO 待添加

//    private int currentPage;
//    private int pageSize;
//    private int totalPages;

//        private int messageCount;
//        private int totalTokensInput;
//        private int totalTokensOutput;
//        private int totalTokens;

}
