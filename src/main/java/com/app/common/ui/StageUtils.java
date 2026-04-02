package com.app.common.ui;

import javafx.scene.image.Image;
import javafx.stage.Stage;

public class StageUtils {

    private static final String ICON_PATH = "/image/logo.png";

    public static void applyAppIcon(Stage stage) {
        if (stage.getIcons().isEmpty()) {
            stage.getIcons().add(
                    new Image(StageUtils.class.getResourceAsStream(ICON_PATH))
            );
        }
    }
}