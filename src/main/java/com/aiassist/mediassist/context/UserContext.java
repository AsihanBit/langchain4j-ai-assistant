package com.aiassist.mediassist.context;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserContext {
    private static final ThreadLocal<String> currentUserIp = new ThreadLocal<>();
    private static final ThreadLocal<String> currentMemoryId = new ThreadLocal<>();

    public static void setCurrentUserIp(String ip) {
        currentUserIp.set(ip);
//        log.info("🔧 [UserContext] 设置ThreadLocal IP: {} , 线程: {}", ip, Thread.currentThread().getName());
    }

    public static String getCurrentUserIp() {
        String ip = currentUserIp.get();
//        log.info("🔧 [UserContext] 获取ThreadLocal IP: {} , 线程: {}", ip, Thread.currentThread().getName());
        return ip;
    }

    public static void setCurrentMemoryId(String memoryId) {
        currentMemoryId.set(memoryId);
    }

    public static String getCurrentMemoryId() {
        return currentMemoryId.get();
    }

    public static void clear() {
        currentUserIp.remove();
        currentMemoryId.remove();
    }

    // 调试方法：获取当前上下文信息
    public static String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("当前线程: ").append(Thread.currentThread().getName()).append("\n");
        sb.append("ThreadLocal IP: ").append(currentUserIp.get()).append("\n");
        sb.append("ThreadLocal MemoryId: ").append(currentMemoryId.get()).append("\n");
        return sb.toString();
    }
}
