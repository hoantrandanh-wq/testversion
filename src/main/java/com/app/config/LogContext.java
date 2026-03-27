package com.app.config;

import org.slf4j.MDC;

public class LogContext {

    public static void init() {
        MDC.put("deviceId", AppContext.DEVICE_ID);
        MDC.put("appVersion", AppContext.VERSION);
    }

    public static void clear() {
        MDC.clear();
    }
}