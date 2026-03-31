package com.app.auth.controller;

import com.app.MainApp;
import com.app.common.session.Session;
import com.app.update.controller.UpdateController;
import com.app.user.model.User;
import com.app.user.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private Label message;

    private final UserService userService;
    private final UpdateController updateController;

    public LoginController(UserService userService, UpdateController updateController) {
        this.userService = userService;
        this.updateController = updateController;
    }

    @FXML
    public void initialize() {
        updateController.setOnStatusChange(null);
        updateController.checkOnStartup();
        message.setVisible(false);
        message.setManaged(false);
    }

    @FXML
    private void handleLogin() {
        String usernameText = username.getText();

        User user = userService.login(usernameText, password.getText());

        if (user != null) {
            log.info("User '{}' logged in successfully", usernameText);
            Session.setUser(user);
            MainApp.showAdmin();
        } else {
            log.warn("Failed login attempt for username '{}'", usernameText);
            message.setText("Sai username hoặc password");
            message.setVisible(true);
            message.setManaged(true);
        }
    }
}