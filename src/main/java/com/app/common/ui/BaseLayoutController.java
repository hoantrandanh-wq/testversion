package com.app.common.ui;

import com.app.MainApp;
import com.app.common.css.CssLoader;
import com.app.common.theme.ThemeManager;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.Setter;

import java.util.List;

public abstract class BaseLayoutController {

    private final ViewLoader viewLoader;

    // fxml của layout shell (admin_layout.fxml) — chỉ set 1 lần từ ViewLoader
    @Setter
    private String fxmlPath;

    // fxml của module đang hiển thị trong content area
    private String currentModuleFxml;

    protected BaseLayoutController(ViewLoader viewLoader) {
        this.viewLoader = viewLoader;
    }

    // ── Subclass cung cấp ──────────────────────────────────────────────────

    protected abstract StackPane getContentArea();

    // ── Content ─────────────────────────────────────────────────────────────

    public void setContent(Node node) {
        if (node != null) {
            getContentArea().getChildren().setAll(node);
        }
    }

    // ── View loading ────────────────────────────────────────────────────────

    protected Node loadView(String fxml) {
        var result = viewLoader.loadWithController(fxml, this);
        if (result != null) {
            // KHÔNG ghi đè fxmlPath — đó là layout path, do ViewLoader set
            currentModuleFxml = fxml;
            CssLoader.applyModule(MainApp.getScene(), fxml);
            return result.node();
        }
        return null;
    }

    protected <T> ViewLoader.LoadResult<T> loadViewWithController(String fxml) {
        ViewLoader.LoadResult<T> result =
                (ViewLoader.LoadResult<T>) viewLoader.loadWithController(fxml, this);
        if (result != null) {
            // KHÔNG ghi đè fxmlPath
            currentModuleFxml = fxml;
            CssLoader.applyModule(MainApp.getScene(), fxml);
        }
        return result;
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    protected void setActiveButton(List<Button> buttons, Button active) {
        buttons.forEach(b -> b.getStyleClass().remove("active-button"));
        if (!active.getStyleClass().contains("active-button")) {
            active.getStyleClass().add("active-button");
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────

    protected void reloadUI() {
        if (fxmlPath == null) {
            throw new IllegalStateException("FXML path not set for controller");
        }

        // Snapshot trước khi reload
        String moduleToRestore = currentModuleFxml;

        // Reload layout shell
        var result = viewLoader.loadWithController(fxmlPath, null);
        if (result == null) return;

        Scene scene = MainApp.getScene();
        scene.setRoot((javafx.scene.Parent) result.node());
        CssLoader.applyAdmin(scene);
        ThemeManager.apply(scene);

        // initialize() của AdminLayoutController đã load dashboard mặc định.
        // Nếu user đang ở module khác thì restore lại đúng module.
        if (moduleToRestore != null
                && !moduleToRestore.equals("/fxml/admin/dashboard.fxml")
                && result.controller() instanceof BaseLayoutController layoutCtrl) {
            Node moduleNode = layoutCtrl.loadView(moduleToRestore);
            layoutCtrl.setContent(moduleNode);
        }
    }
}