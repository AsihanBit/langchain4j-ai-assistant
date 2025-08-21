package com.aiassist.chat.core.context;

import io.micrometer.context.ThreadLocalAccessor;

public class UserIpThreadLocalAccessor implements ThreadLocalAccessor<String> {
    public static final String KEY = "USER_CONTEXT";

    @Override
    public Object key() { // 新版本返回 String，老版本返回 Object，根据你用的版本调整签名
        return KEY;
    }

    @Override
    public String getValue() {
        return UserContext.getCurrentUserIp();
    }

    @Override
    public void setValue(String value) {
        UserContext.setCurrentUserIp(value);
    }

    @Override
    public void reset() {
        UserContext.clear();
    }
}
