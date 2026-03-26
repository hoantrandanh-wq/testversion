package com.app;

import com.app.config.AppContext;
import com.app.config.DeviceIdManager;
import com.app.config.LogContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;

public class MainApp extends Application {

    private ConfigurableApplicationContext springContext;

    public void init() {

        String appDir = System.getProperty("user.home") + "/.helloworld-app";

        // tạo folder
        File dir = new File(appDir);
        if (!dir.exists()) dir.mkdirs();

        File logFolder = new File(appDir + "/logs");
        if (!logFolder.exists()) logFolder.mkdirs();

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

        // start Spring
        springContext = SpringApplication.run(SpringBootApp.class);
    }

    @Override
    public void start(Stage stage) throws Exception {

        Logger logger = LogManager.getLogger(MainApp.class); // ← khai báo local

        // 🔥 4. inject log context vào thread JavaFX
        LogContext.init();
        logger.info("🚀 App started");
        logger.warn("Warning test");
        logger.error("Something went wrong");
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

    public static void main(String[] args) {
        launch(args);
    }
}