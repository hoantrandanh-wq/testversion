package com.app;

import java.io.File;

public class AppLauncher {

    public static void main(String[] args) {
        // ✅ Set APP_DIR TRƯỚC KHI MainApp class được load
        String appDir = System.getProperty("user.home") + "/.helloworld-app";

        new File(appDir + "/logs").mkdirs();
        new File(appDir + "/config").mkdirs();
        new File(appDir + "/tmp").mkdirs();

        System.setProperty("APP_DIR", appDir);
        System.setProperty("org.sqlite.tmpdir", appDir + "/tmp");

        // Bây giờ mới gọi JavaFX launch → lúc này MainApp class mới được load
        MainApp.main(args);
    }
}