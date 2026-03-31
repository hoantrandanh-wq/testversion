package com.app.user.controller;

import com.app.common.helper.SpringContextHolder;
import com.app.common.session.Session;
import com.app.common.ui.BaseLayoutController;
import com.app.common.ui.LayoutAware;
import com.app.common.ui.ViewLoader;
import com.app.user.model.User;
import com.app.user.service.UserService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserController implements LayoutAware {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @FXML
    private TableView<User> table;
    @FXML
    private TableColumn<User, Number> colSTT;
    @FXML
    private TableColumn<User, String> colUsername;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, Void> colAction;
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cbRole;
    @FXML
    private VBox root;
    @FXML
    private Button btnPrev;
    @FXML
    private Button btnNext;
    @FXML
    private Label lblPageInfo;

    private final UserService userService;
    private final ViewLoader viewLoader;

    private List<User> allUsers = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();

    private static final int PAGE_SIZE = 10;
    private int currentPageIndex = 0;

    private BaseLayoutController layoutController;
    private Node currentView;

    public UserController(UserService userService, ViewLoader viewLoader) {
        this.userService = userService;
        this.viewLoader = viewLoader;
    }

    // ── LayoutAware ─────────────────────────────────────────────────────────

    @Override
    public void setLayoutController(BaseLayoutController layoutController) {
        this.layoutController = layoutController;
    }

    // ── Init ────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        if (!Session.isAdmin()) {
            return;
        }

        currentView = root;

        cbRole.getItems().addAll("ALL", "ADMIN", "USER");
        cbRole.setValue("ALL");

        colSTT.setCellValueFactory(c ->
                new SimpleIntegerProperty(
                        currentPageIndex * PAGE_SIZE
                                + table.getItems().indexOf(c.getValue()) + 1
                )
        );

        colUsername.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsername())
        );
        colUsername.setCellFactory(col -> new TableCell<>() {

            private final Hyperlink link = new Hyperlink();

            {
                link.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    openUserInfo(user);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    link.setText(item);
                    setGraphic(link);
                }
            }
        });

        colRole.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRole().name())
        );

        addActionColumn();
        loadData();
    }

    // ── Data ────────────────────────────────────────────────────────────────

    private void loadData() {
        allUsers = userService.findAll();
        filteredUsers = new ArrayList<>(allUsers);
        setupPagination();
    }

    // ── Handlers ────────────────────────────────────────────────────────────

    @FXML
    private void onAdd() {
        openForm(null);
    }

    @FXML
    private void onSearch() {
        String keyword = txtSearch.getText().toLowerCase().trim();
        String role = cbRole.getValue();

        filteredUsers = allUsers.stream()
                .filter(u -> {
                    boolean matchUsername = u.getUsername().toLowerCase().contains(keyword);
                    boolean matchRole = role.equals("ALL") ||
                            u.getRole().name().equalsIgnoreCase(role);
                    return matchUsername && matchRole;
                })
                .toList();

        currentPageIndex = 0;
        setupPagination();
    }

    @FXML
    private void onReset() {
        txtSearch.clear();
        cbRole.setValue("ALL");
        filteredUsers = new ArrayList<>(allUsers);
        currentPageIndex = 0;
        setupPagination();
    }

    @FXML
    private void onPrevPage() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            setupPagination();
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPageIndex < getPageCount() - 1) {
            currentPageIndex++;
            setupPagination();
        }
    }

    // ── Table ────────────────────────────────────────────────────────────────

    private void addActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button btnEdit = new Button("Sửa");
            private final Button btnDelete = new Button("Xóa");

            {
                btnEdit.setStyle("-fx-background-color:#2980b9; -fx-text-fill:white;");
                btnDelete.setStyle("-fx-background-color:#c0392b; -fx-text-fill:white;");

                btnEdit.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    openForm(user);
                });

                btnDelete.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Xóa user này?", ButtonType.YES, ButtonType.NO);

                    confirm.showAndWait().ifPresent(type -> {
                        if (type == ButtonType.YES) {
                            userService.delete(user.getId());
                            log.info("Deleted user '{}'", user.getUsername());
                            loadData();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(10, btnEdit, btnDelete));
            }
        });
    }

    private void setupPagination() {
        int pageCount = getPageCount();
        if (currentPageIndex >= pageCount) {
            currentPageIndex = pageCount - 1;
        }
        updateTablePage();
        updatePagerControls(pageCount);
        table.refresh();
    }

    private void updateTablePage() {
        int from = currentPageIndex * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filteredUsers.size());

        table.setItems(FXCollections.observableArrayList(
                from < to ? filteredUsers.subList(from, to) : List.of()
        ));
    }

    private void updatePagerControls(int pageCount) {
        btnPrev.setDisable(currentPageIndex <= 0);
        btnNext.setDisable(currentPageIndex >= pageCount - 1);
        lblPageInfo.setText("Page " + (currentPageIndex + 1) + " / " + pageCount);
    }

    private int getPageCount() {
        return Math.max((int) Math.ceil((double) filteredUsers.size() / PAGE_SIZE), 1);
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private void openForm(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/user/user-form.fxml")
            );
            loader.setControllerFactory(SpringContextHolder::getBean);

            Scene scene = new Scene(loader.load(), 460, 360);

            UserFormController controller = loader.getController();
            controller.setUser(user);

            Stage stage = new Stage();
            stage.setTitle(user == null ? "Thêm User" : "Sửa User");
            stage.setScene(scene);
            stage.setMinWidth(460);
            stage.setMinHeight(360);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            loadData();

        } catch (Exception e) {
            log.error("Failed to open user form", e);
        }
    }

    private void openUserInfo(User user) {
        var result = viewLoader.loadWithController("/fxml/user/user-info.fxml", layoutController);
        if (result == null) {
            log.error("Failed to load user-info view");
            return;
        }

        UserInfoController controller = (UserInfoController) result.controller();
        controller.setUser(user);

        Node previousView = currentView;
        controller.setOnBack(() -> layoutController.setContent(previousView));

        layoutController.setContent(result.node());
    }
}