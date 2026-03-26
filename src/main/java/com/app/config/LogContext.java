package com.app.config;

import org.apache.logging.log4j.ThreadContext;

public class LogContext {

    public static void init() {
        ThreadContext.put("deviceId", AppContext.DEVICE_ID);
        ThreadContext.put("appVersion", AppContext.VERSION);
    }

    public static void clear() {
        ThreadContext.clearAll();
    }
}