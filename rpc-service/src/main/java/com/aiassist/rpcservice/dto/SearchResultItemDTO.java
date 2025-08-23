package com.aiassist.rpcservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchResultItemDTO {
    private String title;
    private String url;
    private String content;
}
