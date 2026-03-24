package com.app.controller;

import com.app.service.HelloService;
import com.app.update.controller.UpdateController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    @Autowired
    private HelloService helloService;

    @Autowired
    private UpdateController updateController;

    @FXML
    private Label labelResult;

    @FXML
    private Label labelUpdateStatus;

    @FXML
    private Button btnCheckUpdate;

    @FXML
    public void initialize() {
        // Inject các FXML field vào UpdateController
        updateController.setLabelUpdateStatus(labelUpdateStatus);
        updateController.setBtnCheckUpdate(btnCheckUpdate);

        // Tự động check update khi khởi động
        updateController.checkOnStartup();
    }

    @FXML
    public void onButtonClick() {
        labelResult.setText(helloService.greet());
    }

    // Delegate sang UpdateController
    @FXML
    public void onCheckUpdateManual() {
        updateController.onCheckUpdateManual();
    }
}