package com.app;

import com.app.common.config.AppRuntimeInitializer;
import com.app.common.config.LogContext;
import com.app.common.config.LogbackConfigInitializer;
import com.app.common.css.CssLoader;
import com.app.common.helper.SpringContextHolder;
import com.app.common.i18n.I18n;
import com.app.common.theme.ThemeManager;
import com.app.common.ui.NavigationService;
import com.app.common.ui.StageUtils;
import com.app.common.ui.ViewLoader;
import com.app.file.service.DataFolderManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);
    public static final int SINGLE_INSTANCE_PORT = 54321;

    private ConfigurableApplicationContext springContext;
    private DataFolderManager dataFolderManager;
    private static ServerSocket instanceSocket;

    @Getter private static Stage primaryStage;
    @Getter private static Scene scene;

    public static void main(String[] args) {
        LogbackConfigInitializer.initialize();

        if (!acquireSingleInstanceLock()) {
            log.warn("App is already running. Bringing existing window to front.");
            signalExistingInstance();
            System.exit(0);
        }

        launch(args);
    }

    private static boolean acquireSingleInstanceLock() {
        try {
            instanceSocket = new ServerSocket(SINGLE_INSTANCE_PORT, 1,
                    InetAddress.getByName("127.0.0.1"));

            Thread listenerThread = new Thread(() -> {
                while (!instanceSocket.isClosed()) {
                    try {
                        Socket incoming = instanceSocket.accept();
                        incoming.close();
                        Platform.runLater(MainApp::bringToFront);
                    } catch (IOException e) {
                        if (!instanceSocket.isClosed()) {
                            log.warn("Single instance listener error", e);
                        }
                    }
                }
            }, "single-instance-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void signalExistingInstance() {
        try (Socket socket = new Socket("127.0.0.1", SINGLE_INSTANCE_PORT)) {
            log.info("Signal sent to existing instance.");
        } catch (IOException e) {
            log.warn("Could not signal existing instance", e);
        }
    }

    private static void bringToFront() {
        if (primaryStage != null) {
            primaryStage.setIconified(false);
            primaryStage.toFront();
            primaryStage.requestFocus();
            log.info("Brought existing window to front.");
        }
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

        StageUtils.applyAppIcon(primaryStage);

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
        if (instanceSocket != null && !instanceSocket.isClosed()) {
            try {
                instanceSocket.close();
            } catch (IOException e) {
                log.warn("Failed to close instance socket", e);
            }
        }
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