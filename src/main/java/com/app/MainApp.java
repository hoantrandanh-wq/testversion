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

        // Tạo thư mục và file log trước khi Spring Boot khởi tạo logging
        File appDirFile = new File(appDir);
        if (!appDirFile.exists()) appDirFile.mkdirs();

        File logFolder = new File(appDir + "/logs");
        if (!logFolder.exists()) logFolder.mkdirs();

        File configFolder = new File(configDir);
        if (!configFolder.exists()) configFolder.mkdirs();

        File configFile = new File(configDir + "/logback.xml");
        
        // Luôn force copy từ resource nếu detect file cũ (có content khác)
        boolean needsUpdate = true;
        if (configFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(configFile.toPath()));
                // Nếu file cũ là test file (C:/temp/test.log), xóa nó để copy lại
                if (content.contains("C:/temp/test.log") || content.contains("C:\\temp\\test.log")) {
                    System.out.println("⚠️ Detected old test logback config, will replace with new one");
                    Files.delete(configFile.toPath());
                    needsUpdate = true;
                } else {
                    needsUpdate = false;
                }
            } catch (Exception e) {
                needsUpdate = true;
            }
        }
        
        if (needsUpdate) {
            try (InputStream is = MainApp.class.getClassLoader().getResourceAsStream("logback-spring.xml")) {
                if (is != null) {
                    Files.copy(is, configFile.toPath());
                    System.out.println("✅ Created default logback config at: " + configFile.getAbsolutePath());
                } else {
                    System.err.println("⚠️ logback-spring.xml not found via getResourceAsStream, trying alternative method");
                    // Fallback: copy từ classpath resource loader
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    try (InputStream is2 = loader.getResourceAsStream("logback-spring.xml")) {
                        if (is2 != null) {
                            Files.copy(is2, configFile.toPath());
                            System.out.println("✅ Created logback config (via context classloader)");
                        } else {
                            System.err.println("❌ logback-spring.xml not found in any classpath");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to copy logback config:");
                e.printStackTrace();
            }
        } else {
            System.out.println("✅ Using existing logback config at: " + configFile.getAbsolutePath());
        }

        // Set property TRƯỚC khi logger được sử dụng
        System.setProperty("logging.config", configFile.getAbsolutePath());
        System.out.println("✅ Set logging.config to: " + configFile.getAbsolutePath());

        // Force logback reload config từ file
        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            
            // Reset config hiện tại
            loggerContext.reset();
            System.out.println("✅ Reset LoggerContext");
            
            // Load config từ file
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(configFile);
            System.out.println("✅ Loaded logback config from file");
            
            // Print status nếu có lỗi
            StatusPrinter.print(loggerContext);
        } catch (Exception e) {
            System.err.println("❌ Failed to configure logback:");
            e.printStackTrace();
        }
    }

    public void init() {

        String appDir = System.getProperty("user.home") + "/.helloworld-app";
        String configDir = appDir + "/config";

        // tạo folder
        File dir = new File(appDir);
        if (!dir.exists()) dir.mkdirs();

        File logFolder = new File(appDir + "/logs");
        if (!logFolder.exists()) logFolder.mkdirs();

        File configFolder = new File(configDir);
        if (!configFolder.exists()) configFolder.mkdirs();

        // 🔥 tạo file DB (QUAN TRỌNG)
        File dbFile = new File(appDir + "/data.db");
        try {
            if (!dbFile.exists()) {
                dbFile.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // fix SQLite native
        System.setProperty("org.sqlite.tmpdir", appDir + "/tmp");
        new File(appDir + "/tmp").mkdirs();

        // 🔥 (QUAN TRỌNG) force load driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            e.printStackTrace();
        }

        DeviceIdManager deviceIdManager = new DeviceIdManager(appDir);
        AppContext.DEVICE_ID = deviceIdManager.getDeviceId();

        // 🔥 3. VERSION (từ manifest)
        String version = MainApp.class
                .getPackage()
                .getImplementationVersion();

        if (version == null) {
            version = System.getProperty("app.version", "dev");
        }

        AppContext.VERSION = version;

        // Debug
        System.out.println("AppDir: " + appDir);
        System.out.println("DeviceId: " + AppContext.DEVICE_ID);
        System.out.println("Version: " + AppContext.VERSION);

        LogContext.init();
        // start Spring
        springContext = SpringApplication.run(SpringBootApp.class);
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