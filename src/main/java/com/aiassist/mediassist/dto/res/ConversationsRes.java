package com.aiassist.mediassist.dto.res;

import com.aiassist.mediassist.dto.entity.Conversation;
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

//    private int currentPage;
//    private int pageSize;
//    private int totalPages;
}
