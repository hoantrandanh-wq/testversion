package com.app;

import com.app.config.AppContext;
import com.app.config.DeviceIdManager;
import com.app.config.LogContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;

public class MainApp extends Application {

    private ConfigurableApplicationContext springContext;
    private final Logger log = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        setupLoggingConfig();
        launch(args);
    }

    private static void setupLoggingConfig() {
        String appDir = System.getProperty("user.home") + "/.helloworld-app";
        String configDir = appDir + "/config";
        File configFile = new File(configDir + "/logback.xml");

        ensureDirectoriesExist(appDir, configDir);
        
        if (shouldUpdateLogbackConfig(configFile)) {
            copyLogbackConfigFromResource(configFile);
        } else {
            System.out.println("✅ Using existing logback config at: " + configFile.getAbsolutePath());
        }

        System.setProperty("logging.config", configFile.getAbsolutePath());
        System.out.println("✅ Set logging.config to: " + configFile.getAbsolutePath());
        
        reloadLogbackConfiguration(configFile);
    }

    private static void ensureDirectoriesExist(String appDir, String configDir) {
        new File(appDir).mkdirs();
        new File(appDir + "/logs").mkdirs();
        new File(configDir).mkdirs();
    }

    private static boolean shouldUpdateLogbackConfig(File configFile) {
        if (!configFile.exists()) return true;
        
        try {
            String content = new String(Files.readAllBytes(configFile.toPath()));
            if (content.contains("C:/temp/test.log") || content.contains("C:\\temp\\test.log")) {
                System.out.println("⚠️ Detected old test logback config, will replace with new one");
                Files.delete(configFile.toPath());
                return true;
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static void copyLogbackConfigFromResource(File configFile) {
        InputStream is = tryGetResource("logback-spring.xml", MainApp.class.getClassLoader());
        
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

    public void init() {
        String appDir = System.getProperty("user.home") + "/.helloworld-app";

        ensureDirectoriesExist(appDir, appDir + "/config");

        // 🔥 tạo file DB (QUAN TRỌNG)
        File dbFile = new File(appDir + "/data.db");
        try {
            if (!dbFile.exists()) dbFile.createNewFile();
        } catch (Exception e) {
            System.err.println("Failed to create DB file: " + e.getMessage());
        }

        // fix SQLite native
        System.setProperty("org.sqlite.tmpdir", appDir + "/tmp");
        new File(appDir + "/tmp").mkdirs();

        // 🔥 (QUAN TRỌNG) force load driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            System.err.println("Failed to load SQLite driver: " + e.getMessage());
        }

        initializeAppContext(appDir);
        
        // start Spring
        springContext = SpringApplication.run(SpringBootApp.class);
    }

    private void initializeAppContext(String appDir) {
        DeviceIdManager deviceIdManager = new DeviceIdManager(appDir);
        AppContext.DEVICE_ID = deviceIdManager.getDeviceId();

        String version = MainApp.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = System.getProperty("app.version", "dev");
        }
        AppContext.VERSION = version;

        System.out.println("AppDir: " + appDir);
        System.out.println("DeviceId: " + AppContext.DEVICE_ID);
        System.out.println("Version: " + AppContext.VERSION);

        LogContext.init();
    }

    @Override
    public void start(Stage stage) throws Exception {
        LogContext.init();
        log.info("🚀 App started");
        log.warn("Warning test");
        log.error("Something went wrong");

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/main.fxml")
        );
        loader.setControllerFactory(springContext::getBean);

        stage.setTitle("Hello World App");


        // Debug tìm icon
        var url = getClass().getResource("/image/icon.png");
        if (url != null) {
            stage.getIcons().add(new javafx.scene.image.Image(url.toExternalForm()));
        }
        stage.setScene(new Scene(loader.load(), 600, 300));
        stage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }
}