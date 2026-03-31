package com.app.common.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.util.List;

public abstract class BaseLayoutController {

    private final ViewLoader viewLoader;

    protected BaseLayoutController(ViewLoader viewLoader) {
        this.viewLoader = viewLoader;
    }

    // ── Subclass cung cấp ──────────────────────────────────────────────────

    protected abstract StackPane getContentArea();

    protected abstract List<Button> getMenuButtons();

    // ── Content ─────────────────────────────────────────────────────────────

    public void setContent(Node node) {
        if (node != null) {
            getContentArea().getChildren().setAll(node);
        }
    }

    // ── View loading ────────────────────────────────────────────────────────

    protected Node loadView(String fxml) {
        var result = viewLoader.loadWithController(fxml, this);
        return result != null ? result.node() : null;
    }

    protected <T> ViewLoader.LoadResult<T> loadViewWithController(String fxml) {
        return viewLoader.loadWithController(fxml, this);
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    protected void setActiveMenu(Button active) {
        getMenuButtons().forEach(b -> b.getStyleClass().remove("active-menu"));

        if (!active.getStyleClass().contains("active-menu")) {
            active.getStyleClass().add("active-menu");
        }
    }
}