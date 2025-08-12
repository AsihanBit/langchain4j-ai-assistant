package com.aiassist.mediassist.context;

public class UserContext {
    private static final ThreadLocal<String> currentUserIp = new ThreadLocal<>();
    private static final ThreadLocal<String> currentMemoryId = new ThreadLocal<>();

    public static void setCurrentUserIp(String ip) {
        currentUserIp.set(ip);
    }

    public static String getCurrentUserIp() {
        return currentUserIp.get();
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
}
