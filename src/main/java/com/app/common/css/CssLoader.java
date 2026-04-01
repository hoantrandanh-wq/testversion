package com.app.common.css;

import com.app.MainApp;
import com.app.common.theme.ThemeManager;
import javafx.scene.Scene;

import java.net.URL;

public class CssLoader {

    public static void applyBase(Scene scene) {
        addIfAbsent(scene, "/css/base.css");
    }

    public static void applyAdmin(Scene scene) {
        scene.getStylesheets().removeIf(s -> s.contains("login.css"));
        addIfAbsent(scene, "/css/admin.css");
    }

    public static void applyLogin(Scene scene) {
        scene.getStylesheets().removeIf(s ->
                s.contains("admin.css")
                        || s.contains("/css/admin/")
                        || s.contains("/css/user/")
        );
        addIfAbsent(scene, "/css/auth/login.css");
    }

    public static void applyModule(Scene scene, String fxml) {
        scene.getStylesheets().removeIf(s ->
                s.contains("/css/admin/") || s.contains("/css/user/")
        );
        addIfAbsent(scene, resolveCssPath(fxml));
    }

    public static void applyDialog(Scene scene, String fxml) {
        applyBase(scene);
        addIfAbsent(scene, ThemeManager.cssPathForTheme(ThemeManager.getTheme()));
        addIfAbsent(scene, resolveCssPath(fxml));
    }

    private static void addIfAbsent(Scene scene, String path) {
        if (path == null) return;
        URL url = MainApp.class.getResource(path);
        if (url == null) {
            System.err.println("[CssLoader] CSS not found: " + path);
            return;
        }
        String ext = url.toExternalForm();
        if (!scene.getStylesheets().contains(ext)) {
            scene.getStylesheets().add(ext);
        }
    }

    static String resolveCssPath(String fxml) {
        if (fxml == null) return null;
        return fxml
                .replace("/fxml/", "/css/")
                .replace(".fxml", ".css");
    }
}