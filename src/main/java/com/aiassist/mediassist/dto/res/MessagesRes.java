package com.aiassist.mediassist.dto.res;

import com.aiassist.mediassist.dto.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessagesRes {
    private String memoryId;
    private List<Message> messages;
    private int totalCount;

//    private int currentPage;
//    private int pageSize;
//    private int totalPages;
}
