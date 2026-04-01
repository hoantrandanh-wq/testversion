package com.app.admin.controller;

import com.app.MainApp;
import com.app.common.i18n.I18n;
import com.app.common.session.Session;
import com.app.common.theme.ThemeManager;
import com.app.common.ui.BaseLayoutController;
import com.app.common.ui.ViewLoader;
import com.app.update.controller.UpdateController;
import com.app.user.controller.UserInfoController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminLayoutController extends BaseLayoutController {

    private static final Logger log = LoggerFactory.getLogger(AdminLayoutController.class);

    private final UpdateController updateController;

    @FXML
    private StackPane contentArea;
    @FXML
    private Button btnCheckUpdate;
    @FXML
    private Label labelUpdateStatus;
    @FXML
    private MenuButton userMenu;
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnUser;
    @FXML
    private Button btnEnglish;
    @FXML
    private Button btnVietnamese;
    @FXML
    private Button btnLightTheme;
    @FXML
    private Button btnDarkTheme;

    public AdminLayoutController(ViewLoader viewLoader, UpdateController updateController) {
        super(viewLoader);
        this.updateController = updateController;
    }

    // ── BaseLayoutController impl ───────────────────────────────────────────

    @Override
    protected StackPane getContentArea() {
        return contentArea;
    }

    protected List<Button> getMenuButtons() {
        return List.of(btnDashboard, btnUser);
    }

    protected List<Button> getLangButtons() {
        return List.of(btnEnglish, btnVietnamese);
    }

    // ── Init ────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        updateController.setOnCheckStart(() -> btnCheckUpdate.setDisable(true));
        updateController.setOnCheckEnd(() -> btnCheckUpdate.setDisable(false));
        updateController.setOnStatusChange(msg -> labelUpdateStatus.setText(msg));

        userMenu.setText(I18n.get("top.hello", Session.getUser().getUsername()));

        setContent(loadView("/fxml/admin/dashboard.fxml"));
        setActiveButton(getMenuButtons(), btnDashboard);

        String lang = I18n.getLocale().getLanguage();
        setActiveButton(getLangButtons(), "vi".equals(lang) ? btnVietnamese : btnEnglish);

        updateThemeButtons();
    }

    // ── Menu handlers ───────────────────────────────────────────────────────

    @FXML
    public void goDashboard() {
        setContent(loadView("/fxml/admin/dashboard.fxml"));
        setActiveButton(getMenuButtons(), btnDashboard);
    }

    @FXML
    private void goUser() {
        setActiveButton(getMenuButtons(), btnUser);
        if (Session.isAdmin()) {
            setContent(loadView("/fxml/user/user.fxml"));
        } else {
            openMyProfile();
        }
    }

    @FXML
    private void onUserInfo() {
        setActiveButton(getMenuButtons(), btnUser);
        openMyProfile();
    }

    @FXML
    public void logout() {
        Session.clear();
        MainApp.showLogin();
    }

    @FXML
    public void onCheckUpdateManual() {
        updateController.onCheckUpdateManual();
    }

    // ── Private ─────────────────────────────────────────────────────────────

    private void openMyProfile() {
        var result = loadViewWithController("/fxml/user/user-info.fxml");
        if (result == null) {
            log.error("Failed to load user-info view");
            return;
        }

        UserInfoController controller = (UserInfoController) result.controller();
        controller.setShowBack(false);
        controller.setUser(Session.getUser());

        setContent(result.node());
    }

    @FXML
    private void switchToEnglish() {
        I18n.setLocale(java.util.Locale.forLanguageTag("en"));
        reloadUI();
        setActiveButton(getLangButtons(), btnEnglish);
    }

    @FXML
    private void switchToVietnamese() {
        I18n.setLocale(java.util.Locale.forLanguageTag("vi"));
        reloadUI();
        setActiveButton(getLangButtons(), btnVietnamese);
    }

    @FXML
    private void switchToLightTheme() {
        ThemeManager.setTheme(ThemeManager.THEME_LIGHT);
        ThemeManager.apply(MainApp.getScene());
        updateThemeButtons();
    }

    @FXML
    private void switchToDarkTheme() {
        ThemeManager.setTheme(ThemeManager.THEME_DARK);
        ThemeManager.apply(MainApp.getScene());
        updateThemeButtons();
    }

    private void updateThemeButtons() {
        boolean isDark = ThemeManager.THEME_DARK.equals(ThemeManager.getTheme());
        btnDarkTheme.getStyleClass().removeAll("theme-btn-active");
        btnLightTheme.getStyleClass().removeAll("theme-btn-active");
        if (isDark) {
            btnDarkTheme.getStyleClass().add("theme-btn-active");
        } else {
            btnLightTheme.getStyleClass().add("theme-btn-active");
        }
    }
}