package com.app;

import com.app.common.config.AppRuntimeInitializer;
import com.app.common.config.LogContext;
import com.app.common.config.LogbackConfigInitializer;
import com.app.common.css.CssLoader;
import com.app.common.helper.SpringContextHolder;
import com.app.common.i18n.I18n;
import com.app.common.theme.ThemeManager;
import com.app.common.ui.NavigationService;
import com.app.common.ui.ViewLoader;
import com.app.file.service.DataFolderManager;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    private ConfigurableApplicationContext springContext;
    private DataFolderManager dataFolderManager;

    @Getter
    private static Stage primaryStage;
    @Getter
    private static Scene scene;

    public static void main(String[] args) {
        LogbackConfigInitializer.initialize();
        launch(args);
    }

    @Override
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
        I18n.loadSavedLocale();

        scene = new Scene(new StackPane());
        primaryStage.setScene(scene);

        CssLoader.applyBase(scene);
        ThemeManager.apply(scene);

        log.info("App started");
        showLogin();
    }

    @Override
    public void stop() {
        if (dataFolderManager != null) dataFolderManager.shutdown();
        if (springContext != null) springContext.close();
        log.info("App stopped");
    }

    public static void showLogin() {
        loadAndNavigate("/fxml/auth/login.fxml", "BDMA", 400, 300);
    }

    public static void showAdmin() {
        loadAndNavigate("/fxml/admin/admin_layout.fxml", "BDMA", 1200, 800);
    }

    private static void loadAndNavigate(String fxml, String title, int w, int h) {
        try {
            ViewLoader viewLoader = SpringContextHolder.getBean(ViewLoader.class);
            var result = viewLoader.loadWithController(fxml, null);

            if (result == null || result.node() == null) {
                log.error("View loading returned null for: {}", fxml);
                return;
            }

            Parent root = (Parent) result.node();

            if (fxml.contains("login")) {
                NavigationService.goToLogin(root);
            } else {
                NavigationService.goToAdmin(root);
            }

            primaryStage.setTitle(title);
            primaryStage.setWidth(w);
            primaryStage.setHeight(h);

            if (primaryStage.getScene() == null) {
                primaryStage.setScene(scene);
            }
            primaryStage.show();

        } catch (Exception e) {
            log.error("Failed to load scene: {}", fxml, e);
        }
    }
}