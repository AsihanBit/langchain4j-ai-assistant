package com.aiassist.chat.core.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Integer id;               // 主键，int unsigned
    private String userName;          // 用户名，varchar(50)
    private String ipAddress;         // IP地址，varchar(50)
    private LocalDateTime firstSeen;  // 首次见面，datetime
    private LocalDateTime lastSeen;   // 最后见面，datetime
    private Integer visitCount;       // 访问次数，int unsigned
    private Long tokenUsage;          // token使用数量，bigint unsigned

    // 构造方法 - 新用户
    public User(String userName, String ipAddress) {
        this.userName = userName;
        this.ipAddress = ipAddress;
        this.firstSeen = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.visitCount = 1;
        this.tokenUsage = 0L;
    }

    // 更新访问信息
    public void updateVisit() {
        this.lastSeen = LocalDateTime.now();
        this.visitCount = (this.visitCount == null ? 0 : this.visitCount) + 1;
    }

    // 增加token使用量
    public void addTokenUsage(long tokens) {
        this.tokenUsage = (this.tokenUsage == null ? 0L : this.tokenUsage) + tokens;
    }
}