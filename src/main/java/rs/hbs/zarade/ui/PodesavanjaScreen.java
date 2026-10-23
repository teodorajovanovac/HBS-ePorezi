package rs.hbs.zarade.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import rs.hbs.zarade.HbsZaradeApp;
import rs.hbs.zarade.db.AppSettingsDao;
import rs.hbs.zarade.db.DatabaseConfig;
import rs.hbs.zarade.db.DatabaseConnection;
import rs.hbs.zarade.db.StaffDao;
import rs.hbs.zarade.domain.AppSetting;
import rs.hbs.zarade.domain.Staff;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Podešavanja sistema - Screen 18 iz UI specifikacije.
 */
public class PodesavanjaScreen {

    private BorderPane root;

    // Putanje za export
    private TextField exportXmlPathField;
    private TextField exportPrimaocPdfField;
    private TextField exportObracunPdfField;
    private TextField dbMainPathField;

    // Konstante obračuna
    private TextField minCenaRadaField;
    private TextField poreskoOslobodjenjeField;

    // Korisnici
    private TableView<Staff> korisniciTable;

    // Settings tabela
    private TextField settingsSearchField;
    private TableView<AppSetting> settingsTable;
    private final ObservableList<AppSetting> settingsData = FXCollections.observableArrayList();

    // Footer i praćenje nesačuvanih izmena
    private Button saveBtn;
    private Button cancelBtn;
    private boolean hasUnsavedChanges = false;
    private boolean loading = false;

    public PodesavanjaScreen() {
        createUI();
        loadSettings();
    }

    private void createUI() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #F6F8FA;");

        HBox header = createHeader();
        root.setTop(header);

        TabPane tabPane = createTabs();
        tabPane.setStyle("-fx-background-color: white;");
        root.setCenter(tabPane);

        HBox footer = createFooter();
        root.setBottom(footer);
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: transparent transparent #D0D7DE transparent; -fx-border-width: 0 0 1 0;");
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Podešavanja");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web("#1F2328"));

        header.getChildren().add(titleLabel);
        return header;
    }

    private TabPane createTabs() {
        TabPane tabPane = new TabPane();

        Tab putanjeTab = new Tab("Putanje");
        putanjeTab.setClosable(false);
        putanjeTab.setContent(createPutanjeTab());

        Tab konstanteTab = new Tab("Konstante obračuna");
        konstanteTab.setClosable(false);
        konstanteTab.setContent(createKonstanteTab());

        Tab korisniciTab = new Tab("Korisnici");
        korisniciTab.setClosable(false);
        korisniciTab.setContent(createKorisniciTab());

        Tab settingsTab = new Tab("Settings");
        settingsTab.setClosable(false);
        settingsTab.setContent(createSettingsTab());

        tabPane.getTabs().addAll(putanjeTab, konstanteTab, korisniciTab, settingsTab);
        return tabPane;
    }

    private VBox createSettingsTab() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Tabela Settings");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#1F2328"));

        settingsSearchField = new TextField();
        settingsSearchField.setPromptText("Pretraga (naziv, vrednost, opis, kategorija, MFF)...");
        settingsSearchField.setStyle("-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 5 10;");

        Button addBtn = new Button("Novi red");
        addBtn.setStyle("-fx-background-color: #0969DA; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showSettingDialog(null));

        Button refreshBtn = new Button("Osveži");
        refreshBtn.setStyle("-fx-background-color: white; -fx-text-fill: #57606A; -fx-font-size: 13px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> loadSettingsTable());

        HBox toolbar = new HBox(10, settingsSearchField, addBtn, refreshBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(settingsSearchField, Priority.ALWAYS);

        settingsTable = new TableView<>();
        settingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        settingsTable.setPlaceholder(new Label("Nema podataka"));
        // ID kolona je Integer da bi sortiranje bilo numeričko, a ne tekstualno
        TableColumn<AppSetting, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getIdSettings()));
        idCol.setPrefWidth(50);
        idCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        settingsTable.getColumns().addAll(List.of(
                idCol,
                settingsColumn("SettingName", 180, AppSetting::getSettingName),
                settingsColumn("SettingVal", 220, AppSetting::getSettingVal),
                settingsColumn("Description", 220, AppSetting::getDescription),
                settingsColumn("Category", 110, AppSetting::getCategory),
                settingsColumn("MFF", 80, AppSetting::getMff)
        ));

        FilteredList<AppSetting> filtered = new FilteredList<>(settingsData, s -> true);
        settingsSearchField.textProperty().addListener((obs, o, n) ->
                filtered.setPredicate(s -> s.matches(n)));
        SortedList<AppSetting> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(settingsTable.comparatorProperty());
        settingsTable.setItems(sorted);

        settingsTable.setRowFactory(tv -> {
            TableRow<AppSetting> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showSettingDialog(row.getItem());
                }
            });
            return row;
        });
        VBox.setVgrow(settingsTable, Priority.ALWAYS);

        Label infoLabel = new Label("Dupli klik na red otvara izmenu.");
        infoLabel.setStyle("-fx-text-fill: #666;");

        content.getChildren().addAll(titleLabel, toolbar, settingsTable, infoLabel);
        return content;
    }

    private TableColumn<AppSetting, String> settingsColumn(String title, double width,
                                                           Function<AppSetting, String> getter) {
        TableColumn<AppSetting, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(getter.apply(data.getValue())));
        col.setPrefWidth(width);
        return col;
    }

    private void loadSettingsTable() {
        if (settingsTable == null) return;
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            if (dbConn == null) return;
            try (Connection conn = dbConn.getMainConnection()) {
                settingsData.setAll(new AppSettingsDao().findAll(conn));
            }
        } catch (Exception e) {
            System.err.println("Greška pri učitavanju Settings tabele: " + e.getMessage());
            showInfo("Greška", "Nije moguće učitati tabelu Settings: " + e.getMessage());
        }
    }

    /** Popup za unos (existing == null) ili izmenu reda iz tabele Settings. */
    private void showSettingDialog(AppSetting existing) {
        boolean isNew = existing == null;

        Dialog<AppSetting> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Novi red" : "Izmena podešavanja");
        dialog.setHeaderText(isNew ? "Unos novog reda u tabelu Settings"
                                   : "IDSettings: " + existing.getIdSettings());

        ButtonType saveType = new ButtonType("Snimi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField nameField = new TextField(isNew ? "" : nvl(existing.getSettingName()));
        TextArea valArea = new TextArea(isNew ? "" : nvl(existing.getSettingVal()));
        valArea.setPrefRowCount(3);
        valArea.setWrapText(true);
        TextArea descArea = new TextArea(isNew ? "" : nvl(existing.getDescription()));
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        TextField categoryField = new TextField(isNew ? "" : nvl(existing.getCategory()));
        TextField mffField = new TextField(isNew ? "" : nvl(existing.getMff()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.setPrefWidth(520);
        ColumnConstraints c0 = new ColumnConstraints();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        int r = 0;
        if (!isNew) {
            TextField idField = new TextField(String.valueOf(existing.getIdSettings()));
            idField.setEditable(false);
            idField.setStyle("-fx-background-color: #F6F8FA; -fx-text-fill: #848D97;");
            grid.addRow(r++, new Label("IDSettings:"), idField);
        }
        grid.addRow(r++, new Label("SettingName:"), nameField);
        grid.addRow(r++, new Label("SettingVal:"), valArea);
        grid.addRow(r++, new Label("Description:"), descArea);
        grid.addRow(r++, new Label("Category:"), categoryField);
        grid.addRow(r, new Label("MFF:"), mffField);
        dialog.getDialogPane().setContent(grid);

        // SettingName je obavezno polje
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.disableProperty().bind(nameField.textProperty().isEmpty());
        javafx.application.Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(bt -> {
            if (bt != saveType) return null;
            AppSetting s = isNew ? new AppSetting() : existing;
            s.setSettingName(nameField.getText().trim());
            s.setSettingVal(valArea.getText());
            s.setDescription(descArea.getText());
            s.setCategory(categoryField.getText().trim());
            s.setMff(mffField.getText().trim());
            return s;
        });

        Optional<AppSetting> result = dialog.showAndWait();
        result.ifPresent(s -> {
            try {
                DatabaseConnection dbConn = DatabaseConnection.getInstance();
                if (dbConn == null) { showInfo("Greška", "Baza nije dostupna."); return; }
                try (Connection conn = dbConn.getMainConnection()) {
                    AppSettingsDao dao = new AppSettingsDao();
                    if (isNew) dao.insert(conn, s); else dao.update(conn, s);
                }
            } catch (Exception e) {
                showInfo("Greška", "Greška pri snimanju: " + e.getMessage());
            }
            // Uvek ponovo učitaj (i posle greške, da tabela ne prikazuje nesnimljene izmene).
            // Putanje u tabu "Putanje" se čitaju iz iste tabele, pa i njih osveži ako nisu menjane.
            if (hasUnsavedChanges) loadSettingsTable(); else loadSettings();
        });
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private VBox createPutanjeTab() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Putanje za export fajlova");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#1F2328"));

        exportXmlPathField   = createExportPathRow(content, "Folder za XML export (Poreska uprava):");
        exportPrimaocPdfField = createExportPathRow(content, "Folder za PDF primaoci prihoda:");
        exportObracunPdfField = createExportPathRow(content, "Folder za PDF zbirni obračun:");

        Separator sep1 = new Separator();

        Label dbLabel = new Label("Baza podataka");
        dbLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        dbLabel.setTextFill(Color.web("#1F2328"));

        VBox mainBox = new VBox(5);
        Label mainLabel = new Label("Glavna baza (data_zarade.mdb):");
        mainLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        mainLabel.setStyle("-fx-text-fill: #57606A;");
        dbMainPathField = new TextField();
        dbMainPathField.setEditable(false);
        dbMainPathField.setStyle("-fx-background-color: #F6F8FA; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-text-fill: #848D97;");
        mainBox.getChildren().addAll(mainLabel, dbMainPathField);

        Label infoLabel = new Label("Putanja do baze se podešava u fajlu app.local.properties");
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        content.getChildren().addAll(sep1, dbLabel, mainBox, infoLabel);

        return content;
    }

    /** Kreira red sa labelom, TextField-om i dugmetom za browse. Vraća TextField. */
    private TextField createExportPathRow(VBox parent, String labelText) {
        VBox box = new VBox(5);
        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        lbl.setStyle("-fx-text-fill: #57606A;");
        TextField field = new TextField();
        field.setStyle("-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 5 10;");
        Button btn = new Button("...");
        btn.setStyle("-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand; -fx-padding: 5 10;");
        btn.setOnAction(e -> browseFolder(field));
        HBox row = new HBox(10, field, btn);
        HBox.setHgrow(field, Priority.ALWAYS);
        box.getChildren().addAll(lbl, row);
        parent.getChildren().add(box);
        return field;
    }

    private VBox createKonstanteTab() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Konstante za obračun");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#1F2328"));

        Label warningLabel = new Label(
                "Upozorenje: Promene ovih vrednosti utiču na sve buduće obračune!"
        );
        warningLabel.setStyle("-fx-background-color: #FFEBE9; -fx-padding: 10 14; -fx-text-fill: #A40E26; " +
                "-fx-border-color: transparent transparent transparent #CF222E; -fx-border-width: 0 0 0 3; -fx-font-size: 12px;");

        VBox minCenaBox = new VBox(5);
        Label minCenaLabel = new Label("Minimalna cena rada (RSD/sat):");
        minCenaLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        minCenaRadaField = new TextField();
        minCenaRadaField.setPromptText("npr. 271.00");
        minCenaBox.getChildren().addAll(minCenaLabel, minCenaRadaField);

        VBox poreskoBox = new VBox(5);
        Label poreskoLabel = new Label("Poresko oslobođenje (RSD):");
        poreskoLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        poreskoOslobodjenjeField = new TextField();
        poreskoOslobodjenjeField.setPromptText("npr. 25000.00");
        poreskoBox.getChildren().addAll(poreskoLabel, poreskoOslobodjenjeField);

        Label stopeLabel = new Label("Poreske stope i doprinosi");
        stopeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        stopeLabel.setTextFill(Color.web("#1F2328"));

        TableView<String[]> stopeTable = new TableView<>();
        stopeTable.setPrefHeight(200);

        TableColumn<String[], String> tipCol = new TableColumn<>("Tip");
        tipCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        tipCol.setPrefWidth(100);

        TableColumn<String[], String> porezCol = new TableColumn<>("Porez %");
        porezCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        porezCol.setPrefWidth(80);

        TableColumn<String[], String> pioCol = new TableColumn<>("PIO %");
        pioCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        pioCol.setPrefWidth(80);

        TableColumn<String[], String> zdrCol = new TableColumn<>("ZDR %");
        zdrCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));
        zdrCol.setPrefWidth(80);

        TableColumn<String[], String> nezCol = new TableColumn<>("NEZ %");
        nezCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[4]));
        nezCol.setPrefWidth(80);

        stopeTable.getColumns().addAll(tipCol, porezCol, pioCol, zdrCol, nezCol);
        stopeTable.getItems().addAll(
                new String[]{"101 - Zarada", "10.00", "14.00 + 10.00", "5.15 + 5.15", "0.75"},
                new String[]{"405 - Autorski", "20.00", "-", "-", "-"},
                new String[]{"601 - Ugovor", "20.00", "14.00 + 10.00", "5.15 + 5.15", "-"}
        );

        Label stopeInfoLabel = new Label("Napomena: Stope se učitavaju iz tabele Obracun_Stope u bazi.");
        stopeInfoLabel.setStyle("-fx-text-fill: #666;");

        content.getChildren().addAll(
                titleLabel, warningLabel,
                minCenaBox, poreskoBox,
                new Separator(),
                stopeLabel, stopeTable, stopeInfoLabel
        );

        return content;
    }

    @SuppressWarnings("unchecked")
    private VBox createKorisniciTab() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Korisnički nalozi");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#1F2328"));

        // Napomena o pristupnim nivoima
        Label nivoiLabel = new Label("Nivoi pristupa:  4 = Ogranič. pristup (samo Primaoci prihoda)  |  8 = Moderator  |  9 = Administrator");
        nivoiLabel.setStyle("-fx-text-fill: #57606A; -fx-font-size: 11px;");

        korisniciTable = new TableView<>();
        korisniciTable.setPrefHeight(220);

        TableColumn<Staff, String> usernameCol = new TableColumn<>("Korisničko ime");
        usernameCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getUser()));
        usernameCol.setPrefWidth(200);

        TableColumn<Staff, String> levelCol = new TableColumn<>("Nivo");
        levelCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getLevel() != null ? String.valueOf(data.getValue().getLevel()) : ""));
        levelCol.setPrefWidth(60);

        TableColumn<Staff, String> levelNazivCol = new TableColumn<>("Opis nivoa");
        levelNazivCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getLevelNaziv()));
        levelNazivCol.setPrefWidth(140);

        korisniciTable.getColumns().addAll(usernameCol, levelCol, levelNazivCol);

        HBox buttonsBox = new HBox(10);
        Button addBtn = new Button("Dodaj korisnika");
        addBtn.setStyle("-fx-background-color: #0969DA; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");

        Button editBtn = new Button("Izmeni");
        editBtn.setStyle("-fx-background-color: white; -fx-text-fill: #57606A; -fx-font-size: 13px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;");

        Button deleteBtn = new Button("Obriši");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CF222E; -fx-font-size: 13px; -fx-padding: 6 14; -fx-cursor: hand;");

        Button refreshBtn = new Button("Osveži");
        refreshBtn.setStyle("-fx-background-color: white; -fx-text-fill: #57606A; -fx-font-size: 13px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;");

        addBtn.setOnAction(e -> showAddUserDialog());
        editBtn.setOnAction(e -> {
            Staff selected = korisniciTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("Korisnici", "Izaberite korisnika za izmenu.");
                return;
            }
            showEditUserDialog(selected);
        });
        deleteBtn.setOnAction(e -> {
            Staff selected = korisniciTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("Korisnici", "Izaberite korisnika za brisanje.");
                return;
            }
            deleteUser(selected);
        });
        refreshBtn.setOnAction(e -> loadKorisnici());

        buttonsBox.getChildren().addAll(addBtn, editBtn, deleteBtn, refreshBtn);

        Label infoLabel = new Label(
                "Napomena: Korisnički nalozi se čuvaju u tabeli Staff u data_zarade.mdb."
        );
        infoLabel.setStyle("-fx-text-fill: #666;");

        content.getChildren().addAll(titleLabel, nivoiLabel, korisniciTable, buttonsBox, infoLabel);

        return content;
    }

    private HBox createFooter() {
        HBox footer = new HBox(10);
        footer.setStyle("-fx-background-color: #F6F8FA; -fx-border-color: #D0D7DE transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        footer.setPadding(new Insets(10, 20, 10, 20));
        footer.setAlignment(Pos.CENTER_LEFT);

        saveBtn = new Button("Snimi");
        saveBtn.setStyle("-fx-background-color: #1A7F37; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;");
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle("-fx-background-color: #116329; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;"));
        saveBtn.setOnMouseExited(e -> saveBtn.setStyle("-fx-background-color: #1A7F37; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;"));
        saveBtn.setOnAction(e -> saveSettings());
        saveBtn.setDisable(true);

        cancelBtn = new Button("Poništi");
        cancelBtn.setStyle("-fx-background-color: white; -fx-text-fill: #57606A; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> loadSettings());
        cancelBtn.setDisable(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("Zatvori");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #FFEBE9; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;"));
        closeBtn.setOnAction(e -> {
            if (hasUnsavedChanges) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Nesačuvane izmene");
                confirm.setHeaderText("Imate nesačuvane izmene.");
                confirm.setContentText("Da li ste sigurni da želite da izađete bez čuvanja?");
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) return;
            }
            HbsZaradeApp.showMainMenu();
        });

        // Praćenje izmena putanja
        for (TextField f : List.of(exportXmlPathField, exportPrimaocPdfField, exportObracunPdfField)) {
            f.textProperty().addListener((obs, o, n) -> markDirty());
        }

        footer.getChildren().addAll(saveBtn, cancelBtn, spacer, closeBtn);
        return footer;
    }

    private void markDirty() {
        if (loading) return;
        hasUnsavedChanges = true;
        saveBtn.setDisable(false);
        cancelBtn.setDisable(false);
    }

    private void markClean() {
        hasUnsavedChanges = false;
        saveBtn.setDisable(true);
        cancelBtn.setDisable(true);
    }

    private void loadSettings() {
        loading = true;
        try {
            loadSettingsValues();
        } finally {
            loading = false;
        }
        markClean();
    }

    private void loadSettingsValues() {
        // Učitaj putanju do DB
        try {
            var dbConfig = DatabaseConfig.loadFromProperties();
            if (dbConfig != null) {
                dbMainPathField.setText(dbConfig.getMainDbPath());
            }
        } catch (Exception e) {
            System.err.println("Greška pri učitavanju konfiguracije: " + e.getMessage());
        }

        // Učitaj export putanje iz DB
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            if (dbConn != null) {
                try (java.sql.Connection conn = dbConn.getMainConnection()) {
                    AppSettingsDao dao = new AppSettingsDao();
                    String xml  = dao.getSetting(conn, AppSettingsDao.KEY_XML_EXPORT_PATH);
                    String pri  = dao.getSetting(conn, AppSettingsDao.KEY_PRIMALAC_PDF_PATH);
                    String obc  = dao.getSetting(conn, AppSettingsDao.KEY_OBRACUN_PDF_PATH);
                    if (xml  != null) exportXmlPathField.setText(xml);
                    if (pri  != null) exportPrimaocPdfField.setText(pri);
                    if (obc  != null) exportObracunPdfField.setText(obc);
                }
            }
        } catch (Exception e) {
            System.err.println("Greška pri učitavanju podešavanja: " + e.getMessage());
        }

        minCenaRadaField.setText("271.00");
        poreskoOslobodjenjeField.setText("25000.00");

        loadKorisnici();
        loadSettingsTable();
    }

    private void loadKorisnici() {
        if (korisniciTable == null) return;
        korisniciTable.getItems().clear();
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            if (dbConn == null) return;
            try (Connection conn = dbConn.getMainConnection()) {
                StaffDao dao = new StaffDao();
                List<Staff> list = dao.findAll(conn);
                korisniciTable.getItems().addAll(list);
            }
        } catch (Exception e) {
            System.err.println("Greška pri učitavanju korisnika: " + e.getMessage());
            showInfo("Greška", "Nije moguće učitati korisnike: " + e.getMessage());
        }
    }

    private void showAddUserDialog() {
        Dialog<Staff> dialog = new Dialog<>();
        dialog.setTitle("Dodaj korisnika");
        dialog.setHeaderText("Novi korisnički nalog");

        ButtonType saveType = new ButtonType("Snimi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        VBox form = buildUserForm(null);
        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                return extractFromForm(form, null);
            }
            return null;
        });

        Optional<Staff> result = dialog.showAndWait();
        result.ifPresent(staff -> {
            try {
                DatabaseConnection dbConn = DatabaseConnection.getInstance();
                if (dbConn == null) { showInfo("Greška", "Baza nije dostupna."); return; }
                try (Connection conn = dbConn.getMainConnection()) {
                    new StaffDao().save(conn, staff);
                }
                loadKorisnici();
            } catch (Exception e) {
                showInfo("Greška", "Greška pri dodavanju korisnika: " + e.getMessage());
            }
        });
    }

    private void showEditUserDialog(Staff staff) {
        int myLevel = HbsZaradeApp.getUserLevel();
        // Moderator (level 8) ne može da menja podatke i lozinku administratora (level 9)
        if (myLevel < 9 && staff.getLevel() != null && staff.getLevel() >= 9) {
            showInfo("Zabranjeno", "Moderator ne može da menja podatke ni lozinku administratorskog naloga.");
            return;
        }

        Dialog<Staff> dialog = new Dialog<>();
        dialog.setTitle("Izmeni korisnika");
        dialog.setHeaderText("Izmena korisničkog naloga: " + staff.getUser());

        ButtonType saveType = new ButtonType("Snimi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        VBox form = buildUserForm(staff);
        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                return extractFromForm(form, staff);
            }
            return null;
        });

        Optional<Staff> result = dialog.showAndWait();
        result.ifPresent(updatedStaff -> {
            try {
                DatabaseConnection dbConn = DatabaseConnection.getInstance();
                if (dbConn == null) { showInfo("Greška", "Baza nije dostupna."); return; }
                try (Connection conn = dbConn.getMainConnection()) {
                    String newPass = updatedStaff.getStaffLogin();
                    boolean changePass = newPass != null && !newPass.isEmpty();
                    new StaffDao().update(conn, updatedStaff, changePass);
                }
                loadKorisnici();
            } catch (Exception e) {
                showInfo("Greška", "Greška pri izmeni korisnika: " + e.getMessage());
            }
        });
    }

    private void deleteUser(Staff staff) {
        // Zaštita od brisanja administratora (level 9) od strane ne-admina
        int myLevel = HbsZaradeApp.getUserLevel();
        if (myLevel < 9 && staff.getLevel() != null && staff.getLevel() >= 9) {
            showInfo("Zabranjeno", "Nije dozvoljeno brisanje administratorskog naloga.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potvrda brisanja");
        confirm.setHeaderText("Obriši korisnika: " + staff.getUser() + "?");
        confirm.setContentText("Ova akcija je nepovratna.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            if (dbConn == null) { showInfo("Greška", "Baza nije dostupna."); return; }
            try (Connection conn = dbConn.getMainConnection()) {
                new StaffDao().delete(conn, staff.getIdUser());
            }
            loadKorisnici();
        } catch (Exception e) {
            showInfo("Greška", "Greška pri brisanju: " + e.getMessage());
        }
    }

    private VBox buildUserForm(Staff existing) {
        VBox form = new VBox(12);
        form.setPadding(new Insets(16));
        form.setPrefWidth(360);

        TextField userField = new TextField(existing != null ? existing.getUser() : "");
        userField.setPromptText("Korisničko ime");
        userField.setId("userField");

        PasswordField passField = new PasswordField();
        passField.setId("passField");
        passField.setPromptText(existing != null ? "(ostavite prazno da ne menjate lozinku)" : "Lozinka");

        ComboBox<Integer> levelCombo = new ComboBox<>();
        ComboBoxHelper.openOnSpace(levelCombo);
        int myLevel = HbsZaradeApp.getUserLevel();
        // Administrator može da dodeli sve nivoe; Moderator ne može da dodeli nivo 9
        if (myLevel >= 9) {
            levelCombo.getItems().addAll(4, 8, 9);
        } else {
            levelCombo.getItems().addAll(4, 8);
        }
        levelCombo.setId("levelCombo");
        levelCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); return; }
                setText(item + " — " + new Staff(null, null, null, item, null).getLevelNaziv());
            }
        });
        levelCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); return; }
                setText(item + " — " + new Staff(null, null, null, item, null).getLevelNaziv());
            }
        });
        levelCombo.setValue(existing != null && existing.getLevel() != null ? existing.getLevel() : 4);

        form.getChildren().addAll(
                new Label("Korisničko ime:"), userField,
                new Label(existing != null ? "Nova lozinka (ostavite prazno za bez promene):" : "Lozinka:"), passField,
                new Label("Nivo pristupa:"), levelCombo
        );

        return form;
    }

    private Staff extractFromForm(VBox form, Staff existing) {
        TextField userField = (TextField) form.lookup("#userField");
        PasswordField passField = (PasswordField) form.lookup("#passField");
        ComboBox<?> levelCombo = (ComboBox<?>) form.lookup("#levelCombo");

        Staff s = existing != null ? existing : new Staff();
        if (userField != null) s.setUser(userField.getText().trim());
        if (passField != null) s.setStaffLogin(passField.getText());
        if (levelCombo != null && levelCombo.getValue() instanceof Integer) {
            s.setLevel((Integer) levelCombo.getValue());
        }
        return s;
    }

    private void saveSettings() {
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            if (dbConn == null) { showInfo("Greška", "Baza nije dostupna."); return; }
            try (java.sql.Connection conn = dbConn.getMainConnection()) {
                AppSettingsDao dao = new AppSettingsDao();
                dao.saveSetting(conn, AppSettingsDao.KEY_XML_EXPORT_PATH,   exportXmlPathField.getText().trim());
                dao.saveSetting(conn, AppSettingsDao.KEY_PRIMALAC_PDF_PATH, exportPrimaocPdfField.getText().trim());
                dao.saveSetting(conn, AppSettingsDao.KEY_OBRACUN_PDF_PATH,  exportObracunPdfField.getText().trim());
            }
            markClean();
            showInfo("Sačuvano", "Podešavanja su uspješno sačuvana.");
        } catch (Exception e) {
            showInfo("Greška", "Nije moguće snimiti podešavanja: " + e.getMessage());
        }
    }

    private void browseFolder(TextField targetField) {
        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("Izaberite folder");

        java.io.File selectedDir = chooser.showDialog(HbsZaradeApp.getPrimaryStage());
        if (selectedDir != null) {
            targetField.setText(selectedDir.getAbsolutePath());
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }
}
