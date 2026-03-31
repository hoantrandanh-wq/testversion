package com.app.update.controller;

import com.app.update.model.UpdateInfo;
import com.app.update.service.UpdateService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.function.Consumer;

@Component
public class UpdateController {

    private static final Logger log = LoggerFactory.getLogger(UpdateController.class);

    private final UpdateService updateService;

    @Setter
    private Runnable onCheckStart;
    @Setter
    private Runnable onCheckEnd;
    @Setter
    private Consumer<String> onStatusChange;

    public UpdateController(UpdateService updateService) {
        this.updateService = updateService;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public void checkOnStartup() {
        if (!updateService.shouldCheckThisWeek()) return;
        updateService.saveCheckDate();

        Task<UpdateInfo> task = new Task<>() {
            @Override
            protected UpdateInfo call() {
                return updateService.checkLatestVersion();
            }
        };

        task.setOnSucceeded(e -> {
            UpdateInfo info = task.getValue();
            if (!info.isHasUpdate()) return;

            String skipped = updateService.getSkippedVersion();
            if (info.getLatestVersion().equals(skipped)) return;

            Platform.runLater(() -> showUpdateDialog(info));
        });

        new Thread(task).start();
    }

    public void onCheckUpdateManual() {
        if (onCheckStart != null) onCheckStart.run();
        updateStatus("Đang kiểm tra...");

        Task<UpdateInfo> task = new Task<>() {
            @Override
            protected UpdateInfo call() {
                return updateService.checkLatestVersion();
            }
        };

        task.setOnSucceeded(e -> {
            UpdateInfo info = task.getValue();
            Platform.runLater(() -> {
                if (onCheckEnd != null) onCheckEnd.run();
                if (!info.isHasUpdate()) {
                    updateStatus("Bạn đang dùng phiên bản mới nhất!");
                } else {
                    updateStatus("Có phiên bản mới: " + info.getLatestVersion());
                    showUpdateDialog(info);
                }
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            if (onCheckEnd != null) onCheckEnd.run();
            updateStatus("Không thể kiểm tra. Vui lòng thử lại.");
            log.warn("Update check failed", task.getException());
        }));

        new Thread(task).start();
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private void updateStatus(String msg) {
        if (onStatusChange != null) onStatusChange.accept(msg);
    }

    private void showUpdateDialog(UpdateInfo info) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Có phiên bản mới!");
        alert.setHeaderText("Phiên bản " + info.getLatestVersion() + " đã có sẵn");
        alert.setContentText("Bạn có muốn tải về và cài đặt không?");

        ButtonType btnUpdate = new ButtonType("Cập nhật ngay");
        ButtonType btnSkip = new ButtonType("Bỏ qua phiên bản này");
        ButtonType btnLater = new ButtonType("Để sau", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnUpdate, btnSkip, btnLater);

        alert.showAndWait().ifPresent(result -> {
            if (result == btnUpdate) {
                log.info("User chose to update to version {}", info.getLatestVersion());
                downloadAndInstall(info);
            } else if (result == btnSkip) {
                log.info("User skipped version {}", info.getLatestVersion());
                updateService.saveSkippedVersion(info.getLatestVersion());
            }
        });
    }

    private void downloadAndInstall(UpdateInfo info) {
        log.info("Downloading installer for version {}", info.getLatestVersion());

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                return updateService.downloadInstaller(info);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            try {
                File installer = task.getValue();
                log.info("Installer downloaded: {}", installer.getAbsolutePath());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Sẵn sàng cài đặt");
                alert.setHeaderText("Tải về thành công!");
                alert.setContentText("Installer sẽ mở ra. Sau khi cài xong, vui lòng mở lại app.");
                alert.showAndWait();

                new ProcessBuilder(installer.getAbsolutePath()).start();
                Platform.exit();

            } catch (Exception ex) {
                log.error("Failed to launch installer", ex);
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() ->
                log.error("Failed to download installer", task.getException())
        ));

        new Thread(task).start();
    }
}