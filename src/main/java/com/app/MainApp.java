package com.app;

import com.app.common.config.AppRuntimeInitializer;
import com.app.common.config.LogContext;
import com.app.common.config.LogbackConfigInitializer;
import com.app.common.helper.SpringContextHolder;
import com.app.file.service.DataFolderManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.bcrypt.BCrypt;

public class MainApp extends Application {

    private ConfigurableApplicationContext springContext;
    private final Logger log = LoggerFactory.getLogger(MainApp.class);
    private DataFolderManager dataFolderManager;
    private static Stage primaryStage;

    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw("admin123", BCrypt.gensalt()));
        LogbackConfigInitializer.initialize();
        launch(args);
    }

    public void init() {
        AppRuntimeInitializer.initialize();

        springContext = SpringApplication.run(SpringBootApp.class);

        dataFolderManager = springContext.getBean(DataFolderManager.class);
        dataFolderManager.init();
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        LogContext.init();
        log.info("🚀 App started");

        showLogin();
    }

    @Override
    public void stop() {
        if (dataFolderManager != null) {
            dataFolderManager.shutdown();
        }
        springContext.close();
    }

    public static void showLogin() {
        loadScene("/fxml/auth/login.fxml", "BDMA", 400, 300);
    }

    public static void showAdmin() {
        loadScene("/fxml/admin/admin_layout.fxml", "BDMA", 1200, 800);
    }

    private static void loadScene(String fxml, String title, int w, int h) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxml));

            loader.setControllerFactory(SpringContextHolder::getBean);

            Scene scene = new Scene(loader.load(), w, h);

            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}