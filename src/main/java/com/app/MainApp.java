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

    // ✅ Lúc này APP_DIR đã được set bởi AppLauncher rồi
    private static final Logger logger = LogManager.getLogger(MainApp.class);

    @Override
    public void init() {
        String appDir = System.getProperty("APP_DIR");
        AppContext.APP_DIR = appDir;

        try { Class.forName("org.sqlite.JDBC"); } catch (Exception e) { e.printStackTrace(); }

        File dbFile = new File(appDir + "/data.db");
        try {
            if (!dbFile.exists()) dbFile.createNewFile();
        } catch (Exception e) { e.printStackTrace(); }

        DeviceIdManager deviceIdManager = new DeviceIdManager(appDir);
        AppContext.DEVICE_ID = deviceIdManager.getDeviceId();

        String version = MainApp.class.getPackage().getImplementationVersion();
        AppContext.VERSION = (version != null) ? version : System.getProperty("app.version", "dev");

        springContext = SpringApplication.run(SpringBootApp.class);
    }

    @Override
    public void start(Stage stage) throws Exception {
        LogContext.init();
        logger.info("App started - version: {}", AppContext.VERSION);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.setControllerFactory(springContext::getBean);

        stage.setTitle("Hello World App");
        var url = getClass().getResource("/image/icon.png");
        if (url != null) stage.getIcons().add(new javafx.scene.image.Image(url.toExternalForm()));
        stage.setScene(new Scene(loader.load(), 600, 300));
        stage.show();
    }

    @Override
    public void stop() {
        LogContext.clear();
        springContext.close();
    }

    // ✅ Chỉ còn launch(), KHÔNG setup gì ở đây
    public static void main(String[] args) {
        launch(args);
    }
}