package com.app.update.ui;

import com.app.update.model.UpdateInfo;
import com.app.update.service.UpdateStateService;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.URI;
import java.util.Optional;

@Component
public class UpdateDialog {

    @Autowired
    private UpdateStateService stateService;

    public void show(UpdateInfo info) {

        Platform.runLater(() -> {

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Update Available");
            alert.setHeaderText("Có phiên bản mới: " + info.getVersion());
            alert.setContentText("Bạn có muốn cập nhật không?");

            ButtonType updateBtn = new ButtonType("Update");
            ButtonType skipBtn = new ButtonType("Bỏ qua");
            ButtonType laterBtn = new ButtonType("Để sau", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(updateBtn, skipBtn, laterBtn);

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent()) {
                if (result.get() == updateBtn) {
                    openDownload(info.getDownloadUrl());
                } else if (result.get() == skipBtn) {
                    stateService.saveSkippedVersion(info.getVersion());
                }
            }
        });
    }

    private void openDownload(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}