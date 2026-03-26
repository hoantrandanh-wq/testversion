package com.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class MainApp extends Application {

    private ConfigurableApplicationContext springContext;

    public void init() {

        String appDir = System.getProperty("user.home") + "/.helloworld-app";

        // tạo folder
        java.io.File dir = new java.io.File(appDir);
        if (!dir.exists()) dir.mkdirs();

        // 🔥 tạo file DB (QUAN TRỌNG)
        java.io.File dbFile = new java.io.File(appDir + "/data.db");
        try {
            if (!dbFile.exists()) {
                dbFile.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // fix SQLite native
        System.setProperty("org.sqlite.tmpdir", appDir + "/tmp");
        new java.io.File(appDir + "/tmp").mkdirs();

        // start Spring
        springContext = SpringApplication.run(SpringBootApp.class);
    }

    @Override
    public void start(Stage stage) throws Exception {
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