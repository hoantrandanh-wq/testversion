package com.app.update.controller;

import com.app.update.model.UpdateInfo;
import com.app.update.service.UpdateService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class UpdateController {

    @Autowired
    private UpdateService updateService;

    // Nhận từ MainController
    private Label labelUpdateStatus;
    private Button btnCheckUpdate;

    public void setLabelUpdateStatus(Label label) {
        this.labelUpdateStatus = label;
    }

    public void setBtnCheckUpdate(Button btn) {
        this.btnCheckUpdate = btn;
    }

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
            System.out.println("Giá trị của Update Info là: " + info.toString());
            String skipped = updateService.getSkippedVersion();
            if (info.getLatestVersion().equals(skipped)) return;

            Platform.runLater(() -> showUpdateDialog(info));
        });

        new Thread(task).start();
    }

    public void onCheckUpdateManual() {
        System.out.println("Kiểm tra phiên bản:");
        btnCheckUpdate.setDisable(true);
        labelUpdateStatus.setText("Đang kiểm tra...");

        Task<UpdateInfo> task = new Task<>() {
            @Override
            protected UpdateInfo call() {
                return updateService.checkLatestVersion();
            }
        };

        task.setOnSucceeded(e -> {
            UpdateInfo info = task.getValue();
            Platform.runLater(() -> {
                btnCheckUpdate.setDisable(false);
                if (!info.isHasUpdate()) {
                    labelUpdateStatus.setText("✅ Bạn đang dùng phiên bản mới nhất!");
                } else {
                    labelUpdateStatus.setText("Có phiên bản mới: " + info.getLatestVersion());
                    showUpdateDialog(info);
                }
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            btnCheckUpdate.setDisable(false);
            labelUpdateStatus.setText("❌ Không thể kiểm tra. Vui lòng thử lại.");
        }));

        new Thread(task).start();
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
                System.out.println("Thực hiện update");
                downloadAndInstall(info);
            } else if (result == btnSkip) {
                System.out.println("Bỏ qua phien ban này");
                updateService.saveSkippedVersion(info.getLatestVersion());
                labelUpdateStatus.setText("Đã bỏ qua phiên bản " + info.getLatestVersion());
            }
        });
    }

    private void downloadAndInstall(UpdateInfo info) {
        System.out.println("Đang tải về");
        labelUpdateStatus.setText("Đang tải về...");
        btnCheckUpdate.setDisable(true);

        Task<java.io.File> task = new Task<>() {
            @Override
            protected java.io.File call() throws Exception {
                return updateService.downloadInstaller(info);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            try {
                System.out.println("Vào cài đặt");
                File installer = task.getValue();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Sẵn sàng cài đặt");
                alert.setHeaderText("Tải về thành công!");
                alert.setContentText("Installer sẽ mở ra. Sau khi cài xong, vui lòng mở lại app.");
                alert.showAndWait();
                new ProcessBuilder(installer.getAbsolutePath())
                        .start();
                Platform.exit();
            } catch (Exception ex) {
                labelUpdateStatus.setText("❌ Lỗi khi mở file cài đặt.");
                System.out.println("Lỗi cài đặt: " + ex.getClass().getName() + ": " + ex.getMessage());
                btnCheckUpdate.setDisable(false);
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            labelUpdateStatus.setText("❌ Tải về thất bại. Vui lòng thử lại.");
            btnCheckUpdate.setDisable(false);
        }));

        new Thread(task).start();
    }
}