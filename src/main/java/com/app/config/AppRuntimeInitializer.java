package com.app.config;

import java.io.File;

public final class AppRuntimeInitializer {

    private AppRuntimeInitializer() {
    }

    public static String initialize() {
        String appDir = AppPaths.appDir();

        ensureBaseDirectories();
        ensureDatabaseFile();
        configureSqlite();
        forceLoadSqliteDriver();
        initializeAppContext(appDir);

        return appDir;
    }

    private static void ensureBaseDirectories() {
        new File(AppPaths.appDir()).mkdirs();
        new File(AppPaths.configDir()).mkdirs();
        new File(AppPaths.tmpDir()).mkdirs();
    }

    private static void ensureDatabaseFile() {
        File dbFile = AppPaths.dataFile();
        try {
            if (!dbFile.exists()) dbFile.createNewFile();
        } catch (Exception e) {
            System.err.println("Failed to create DB file: " + e.getMessage());
        }
    }

    private static void configureSqlite() {
        System.setProperty("org.sqlite.tmpdir", AppPaths.tmpDir());
    }

    private static void forceLoadSqliteDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            System.err.println("Failed to load SQLite driver: " + e.getMessage());
        }
    }

    private static void initializeAppContext(String appDir) {
        DeviceIdManager deviceIdManager = new DeviceIdManager(appDir);
        AppContext.DEVICE_ID = deviceIdManager.getDeviceId();

        String version = AppRuntimeInitializer.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = System.getProperty("app.version", "dev");
        }
        AppContext.VERSION = version;

        System.out.println("AppDir: " + appDir);
        System.out.println("DeviceId: " + AppContext.DEVICE_ID);
        System.out.println("Version: " + AppContext.VERSION);

        LogContext.init();
    }
}