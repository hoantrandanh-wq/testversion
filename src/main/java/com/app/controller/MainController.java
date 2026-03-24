package com.app.controller;

import com.app.service.HelloService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    @Autowired
    private HelloService helloService;

    @FXML
    private Label labelResult;

    @FXML
    public void onButtonClick() {
        labelResult.setText(helloService.greet());
    }
}