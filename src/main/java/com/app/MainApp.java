package com.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class MainApp extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
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