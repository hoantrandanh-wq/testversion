package com.app.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public final class LogbackConfigInitializer {

    private LogbackConfigInitializer() {
    }

    public static void initialize() {
        File configFile = AppPaths.logConfigFile();

        ensureDirectoriesExist();

        if (shouldUpdateLogbackConfig(configFile)) {
            copyLogbackConfigFromResource(configFile);
        } else {
            System.out.println("✅ Using existing logback config at: " + configFile.getAbsolutePath());
        }

        System.setProperty("logging.config", configFile.getAbsolutePath());
        System.out.println("✅ Set logging.config to: " + configFile.getAbsolutePath());

        reloadLogbackConfiguration(configFile);
    }

    private static void ensureDirectoriesExist() {
        new File(AppPaths.appDir()).mkdirs();
        new File(AppPaths.logsDir()).mkdirs();
        new File(AppPaths.configDir()).mkdirs();
    }

    private static boolean shouldUpdateLogbackConfig(File configFile) {
        if (!configFile.exists()) return true;

        try {
            String content = new String(Files.readAllBytes(configFile.toPath()));
            if (content.contains("C:/temp/test.log") || content.contains("C:\\temp\\test.log")) {
                System.out.println("⚠️ Detected old test logback config, will replace");
                Files.delete(configFile.toPath());
                return true;
            }
            if (content.contains("LOGGLY_TOKEN") || content.contains("LogglyAppender")) {
                System.out.println("⚠️ Detected old real-time Loggly config, will replace");
                Files.delete(configFile.toPath());
                return true;
            }
            if (content.contains("<file>${LOG_DIR}/error.log</file>")) {
                System.out.println("⚠️ Detected legacy fixed error.log config, will replace");
                Files.delete(configFile.toPath());
                return true;
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static void copyLogbackConfigFromResource(File configFile) {
        InputStream is = tryGetResource("logback-spring.xml", LogbackConfigInitializer.class.getClassLoader());

        if (is == null) {
            is = tryGetResource("logback-spring.xml", Thread.currentThread().getContextClassLoader());
        }

        if (is == null) {
            System.err.println("❌ logback-spring.xml not found in any classpath");
            return;
        }

        try (InputStream resource = is) {
            Files.copy(resource, configFile.toPath());
            System.out.println("✅ Created logback config at: " + configFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Failed to copy logback config: " + e.getMessage());
        }
    }

    private static InputStream tryGetResource(String resourceName, ClassLoader classLoader) {
        try {
            return classLoader.getResourceAsStream(resourceName);
        } catch (Exception e) {
            return null;
        }
    }

    private static void reloadLogbackConfiguration(File configFile) {
        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.reset();

            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(configFile);

            System.out.println("✅ Logback config loaded successfully");
            StatusPrinter.print(loggerContext);
        } catch (Exception e) {
            System.err.println("❌ Failed to configure logback: " + e.getMessage());
        }
    }
}