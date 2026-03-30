package com.app;

import com.app.config.AppContext;
import com.app.config.AppRuntimeInitializer;
import com.app.config.LogContext;
import com.app.config.LogbackConfigInitializer;
import com.app.service.DataFolderManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class MainApp extends Application {

    private ConfigurableApplicationContext springContext;
    private final Logger log = LoggerFactory.getLogger(MainApp.class);
    private DataFolderManager dataFolderManager;

    public static void main(String[] args) {
        LogbackConfigInitializer.initialize();
        launch(args);
    }

    public void init() {
        AppRuntimeInitializer.initialize();

        // start Spring
        springContext = SpringApplication.run(SpringBootApp.class);

        // Lấy bean từ Spring context sau khi start xong
        dataFolderManager = springContext.getBean(DataFolderManager.class);
        dataFolderManager.init();
    }

    @Override
    public void start(Stage stage) throws Exception {
        LogContext.init();
        log.info("🚀 App started");

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
        if (dataFolderManager != null) {
            dataFolderManager.shutdown();
        }
        springContext.close();
    }
}