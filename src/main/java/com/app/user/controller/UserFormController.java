package com.app.user.controller;

import com.app.common.enums.Role;
import com.app.user.model.User;
import com.app.user.service.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserFormController {

    private static final Logger log = LoggerFactory.getLogger(UserFormController.class);

    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private ComboBox<Role> cbRole;
    @FXML
    private Label lblError;

    private final UserService userService;

    private User user;

    public UserFormController(UserService userService) {
        this.userService = userService;
    }

    @FXML
    public void initialize() {
        cbRole.setItems(FXCollections.observableArrayList(Role.values()));
        lblError.setVisible(false);
    }

    public void setUser(User user) {
        this.user = user;

        if (user != null) {
            txtUsername.setText(user.getUsername());
            txtUsername.setDisable(true);
            cbRole.setValue(user.getRole());
        }
    }

    @FXML
    private void onSave() {
        try {
            if (user == null) {
                User newUser = new User();
                newUser.setUsername(txtUsername.getText());
                newUser.setPassword(txtPassword.getText());
                newUser.setRole(cbRole.getValue());

                userService.create(newUser);
                log.info("Created user '{}'", newUser.getUsername());

            } else {
                user.setRole(cbRole.getValue());
                user.setPassword(txtPassword.getText());

                userService.update(user);
                log.info("Updated user '{}'", user.getUsername());
            }

            close();

        } catch (Exception e) {
            log.error("Failed to save user", e);
            lblError.setText(e.getMessage());
            lblError.setVisible(true);
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) txtUsername.getScene().getWindow();
        stage.close();
    }
}