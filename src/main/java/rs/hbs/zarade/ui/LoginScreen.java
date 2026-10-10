package rs.hbs.zarade.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import rs.hbs.zarade.HbsZaradeApp;
import rs.hbs.zarade.db.DatabaseConnection;
import rs.hbs.zarade.db.LogDao;
import rs.hbs.zarade.db.StaffDao;
import rs.hbs.zarade.domain.Staff;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Login ekran - Screen 1 iz UI specifikacije.
 */
public class LoginScreen {

    private BorderPane root;
    private ComboBox<String> userCombo;
    private PasswordField passField;
    private Label errorLabel;

    public LoginScreen() {
        createUI();
        loadUsers();
    }

    private void createUI() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #F6F8FA;");

        HBox header = createHeader();
        root.setTop(header);

        HBox center = createCenterContent();
        root.setCenter(center);

        HBox footer = createFooter();
        root.setBottom(footer);
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #0D1117;");
        header.setPadding(new Insets(12, 24, 12, 24));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox leftInfo = new VBox(2);
        Label appLabel = new Label("HBS Zarade");
        appLabel.setTextFill(Color.web("#E6EDF3"));
        appLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));

        Label versionLabel = new Label("v1.0.0");
        versionLabel.setTextFill(Color.web("#7D8590"));
        versionLabel.setFont(Font.font("Segoe UI", 11));

        leftInfo.getChildren().addAll(appLabel, versionLabel);

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label titleLabel = new Label("ePorezi");
        titleLabel.setTextFill(Color.web("#4493F8"));
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy. HH:mm");
        Label dateLabel = new Label(LocalDateTime.now().format(formatter));
        dateLabel.setTextFill(Color.web("#7D8590"));
        dateLabel.setFont(Font.font("Segoe UI", 12));

        header.getChildren().addAll(leftInfo, spacer1, titleLabel, spacer2, dateLabel);
        return header;
    }

    private HBox createCenterContent() {
        HBox center = new HBox(50);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(50));

        Region leftPanel = new Region();
        leftPanel.setPrefWidth(300);

        VBox formPanel = createLoginForm();

        center.getChildren().addAll(leftPanel, formPanel);
        return center;
    }

    private VBox createLoginForm() {
        VBox form = new VBox(18);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(32));
        form.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 8; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(27,31,35,0.08), 12, 0, 0, 4);");
        form.setPrefWidth(400);

        Label formTitle = new Label("Prijava u sistem");
        formTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        formTitle.setTextFill(Color.web("#1F2328"));

        Label langLabel = new Label("Jezik programa");
        langLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        langLabel.setTextFill(Color.web("#57606A"));

        ComboBox<String> langCombo = new ComboBox<>();
        langCombo.getItems().addAll("Latinica", "Ćirilica", "English", "Deutsch");
        langCombo.setValue("Latinica");
        langCombo.setMaxWidth(Double.MAX_VALUE);
        langCombo.setStyle("-fx-background-color: white; -fx-font-size: 13px; " +
                "-fx-border-color: #D0D7DE; -fx-border-radius: 6;");

        Label userLabel = new Label("Korisničko ime");
        userLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        userLabel.setTextFill(Color.web("#57606A"));

        userCombo = new ComboBox<>();
        userCombo.setMaxWidth(Double.MAX_VALUE);
        userCombo.setStyle("-fx-background-color: white; -fx-font-size: 13px; " +
                "-fx-border-color: #D0D7DE; -fx-border-radius: 6;");

        Label passLabel = new Label("Lozinka");
        passLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        passLabel.setTextFill(Color.web("#57606A"));

        passField = new PasswordField();
        passField.setStyle("-fx-background-color: white; -fx-font-size: 13px; " +
                "-fx-border-color: #D0D7DE; -fx-border-radius: 6; -fx-border-width: 1;");
        passField.setPromptText("Unesite lozinku");

        errorLabel = new Label();
        errorLabel.setFont(Font.font("Segoe UI", 12));
        errorLabel.setStyle("-fx-text-fill: #CF222E; -fx-background-color: #FFEBE9; " +
                "-fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #F7C5C5; " +
                "-fx-border-width: 1; -fx-border-radius: 6;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(12, 0, 0, 0));

        Button loginBtn = new Button("Prijava");
        loginBtn.setStyle("-fx-background-color: #0969DA; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-padding: 10 32; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle("-fx-background-color: #0550AE; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-padding: 10 32; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;"));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle("-fx-background-color: #0969DA; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-padding: 10 32; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;"));

        Button exitBtn = new Button("Kraj rada");
        exitBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CF222E; " +
                "-fx-border-color: #D0D7DE; -fx-border-radius: 6; " +
                "-fx-background-radius: 6; -fx-padding: 10 20; -fx-font-size: 13px; -fx-cursor: hand;");
        exitBtn.setOnMouseEntered(e -> exitBtn.setStyle("-fx-background-color: #FFEBE9; -fx-text-fill: #CF222E; " +
                "-fx-border-color: #CF222E; -fx-border-radius: 6; " +
                "-fx-background-radius: 6; -fx-padding: 10 20; -fx-font-size: 13px; -fx-cursor: hand;"));
        exitBtn.setOnMouseExited(e -> exitBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CF222E; " +
                "-fx-border-color: #D0D7DE; -fx-border-radius: 6; " +
                "-fx-background-radius: 6; -fx-padding: 10 20; -fx-font-size: 13px; -fx-cursor: hand;"));

        loginBtn.setOnAction(event -> doLogin());
        passField.setOnAction(event -> doLogin());
        userCombo.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                doLogin();
            }
        });

        exitBtn.setOnAction(event -> System.exit(0));

        buttonBox.getChildren().addAll(loginBtn, exitBtn);

        form.getChildren().addAll(
                formTitle,
                new Separator(),
                langLabel, langCombo,
                userLabel, userCombo,
                passLabel, passField,
                errorLabel,
                buttonBox
        );

        return form;
    }

    private void loadUsers() {
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            if (dbConn == null) {
                userCombo.getItems().add("Administrator");
                userCombo.setValue("Administrator");
                return;
            }
            try (Connection conn = dbConn.getMainConnection()) {
                StaffDao dao = new StaffDao();
                List<Staff> staffList = dao.findAll(conn);
                for (Staff s : staffList) {
                    userCombo.getItems().add(s.getUser());
                }
                if (!userCombo.getItems().isEmpty()) {
                    String lastUser = Preferences.userNodeForPackage(LoginScreen.class)
                            .get("last.user", null);
                    if (lastUser != null && userCombo.getItems().contains(lastUser)) {
                        userCombo.setValue(lastUser);
                    } else {
                        userCombo.setValue(userCombo.getItems().get(0));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Greška pri učitavanju korisnika: " + e.getMessage());
            userCombo.getItems().add("Administrator");
            userCombo.setValue("Administrator");
        }
    }

    private void doLogin() {
        String username = userCombo.getValue();
        String password = passField.getText();

        if (username == null || username.isEmpty()) {
            showError("Izaberite korisničko ime.");
            return;
        }

        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            if (dbConn == null) {
                showError("Nije moguće uspostaviti vezu sa bazom podataka.");
                return;
            }
            try (Connection conn = dbConn.getMainConnection()) {
                StaffDao dao = new StaffDao();
                Staff staff = dao.authenticate(conn, username, password);
                if (staff != null) {
                    hideError();
                    Preferences.userNodeForPackage(LoginScreen.class).put("last.user", username);
                    HbsZaradeApp.setCurrentUser(staff.getUser(), staff.getLevel() != null ? staff.getLevel() : 0);
                    // Log prijavu
                    LogDao.log(conn, staff.getIdUser(), staff.getUser(),
                            "Login", "LOGIN", 0, "LOGIN",
                            "Prijava korisnika: " + staff.getUser());
                    HbsZaradeApp.showAppLayout();
                } else {
                    showError("Pogrešno korisničko ime ili lozinka.");
                    passField.clear();
                    passField.requestFocus();
                }
            }
        } catch (Exception e) {
            showError("Greška pri prijavi: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setStyle("-fx-background-color: #F6F8FA; -fx-border-color: #D0D7DE transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        footer.setPadding(new Insets(8, 20, 8, 20));
        footer.setAlignment(Pos.CENTER_LEFT);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy. HH:mm:ss");
        Label statusLabel = new Label("Poslednji pristup: " + LocalDateTime.now().format(formatter));
        statusLabel.setTextFill(Color.web("#7D8590"));
        statusLabel.setFont(Font.font("Segoe UI", 11));

        footer.getChildren().add(statusLabel);
        return footer;
    }

    public BorderPane getRoot() {
        return root;
    }
}
