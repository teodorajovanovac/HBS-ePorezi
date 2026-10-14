package rs.hbs.zarade.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import rs.hbs.zarade.HbsZaradeApp;
import rs.hbs.zarade.db.DatabaseConnection;
import rs.hbs.zarade.db.ObracunDefinicijaDao;
import rs.hbs.zarade.db.PoslodavacDao;
import rs.hbs.zarade.db.VrstaPrihodaDao;
import rs.hbs.zarade.domain.ObracunDefinicija;
import rs.hbs.zarade.domain.Poslodavac;
import rs.hbs.zarade.domain.VrstaPrihoda;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Zaglavlje obračuna - Screen 9 (edit mode) i Screen 10 (create mode) iz UI specifikacije.
 */
public class ZaglavljeObracunaScreen {

    private static final String COLOR_VIEW  = "#343942"; // tamno siva: pogled
    private static final String COLOR_NEW   = "#0969DA"; // plava: novi
    private static final String COLOR_DIRTY = "#9A6700"; // amber: nesačuvane izmene
    private static final String COLOR_SAVED = "#1A7F37"; // zelena: sačuvano

    private BorderPane root;
    private HBox header;
    private Button saveBtn;
    private Button cancelBtn;

    private ObracunDefinicija obracunDef;
    private boolean createMode;
    private Consumer<ObracunDefinicija> onSaveCallback;
    private boolean hasUnsavedChanges = false;
    private boolean loading = false;
    private Label unsavedBanner;

    // Form fields
    private TextField sifraField;
    private TextField nazivField;
    private ComboBox<VrstaPrihoda> tipIsplateCombo;
    private TextField mesecField;
    private DatePicker datumObracunaPicker;
    private DatePicker datumSlanjaPicker;
    private ComboBox<String> oznakaKonacnaCombo;
    private TextField brojDanaField;
    private TextField brojSatiField;
    private TextField brojPrijaveField;
    private ComboBox<Poslodavac> poslodavacCombo;

    public ZaglavljeObracunaScreen(ObracunDefinicija obracunDef, boolean createMode) {
        this.obracunDef = obracunDef != null ? obracunDef : new ObracunDefinicija();
        this.createMode = createMode;
        createUI();
        // Poslodavci moraju biti učitani pre popunjavanja polja, inače se poslodavac ne selektuje
        loadPoslodavci();
        if (!createMode) {
            populateFields();
        }
        // Zaštita od gubitka izmena pri sidebar navigaciji
        root.setUserData((AppLayout.DirtyCheckable) this::confirmLeave);
    }

    private boolean confirmLeave() {
        if (!hasUnsavedChanges) return true;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Nesačuvane izmene");
        confirm.setHeaderText("Imate nesačuvane izmene.");
        confirm.setContentText("Da li ste sigurni da želite da napustite ekran bez čuvanja?");
        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public void setOnSaveCallback(Consumer<ObracunDefinicija> callback) {
        this.onSaveCallback = callback;
    }

    private void createUI() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #F6F8FA;");

        // Header
        HBox header = createHeader();
        root.setTop(header);

        // Unsaved changes banner (hidden initially)
        unsavedBanner = new Label("⚠  Nesačuvane izmene");
        unsavedBanner.setStyle("-fx-background-color: #FFF8C5; -fx-text-fill: #9A6700; " +
                "-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 20; " +
                "-fx-border-color: transparent transparent transparent #9A6700; -fx-border-width: 0 0 0 3;");
        unsavedBanner.setVisible(false);
        unsavedBanner.setManaged(false);
        unsavedBanner.setMaxWidth(Double.MAX_VALUE);

        // Forma
        VBox form = createForm();
        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        VBox centerBox = new VBox(0, unsavedBanner, scrollPane);
        root.setCenter(centerBox);

        // Footer
        HBox footer = createFooter();
        root.setBottom(footer);

        // Novi obračun: fokus odmah na naziv obračuna
        if (createMode) {
            root.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    javafx.application.Platform.runLater(() -> nazivField.requestFocus());
                }
            });
            javafx.application.Platform.runLater(() -> nazivField.requestFocus());
        }
    }

    private HBox createHeader() {
        header = new HBox();
        String bgColor = createMode ? COLOR_NEW : COLOR_VIEW;
        header.setStyle("-fx-background-color: " + bgColor + ";");
        header.setPadding(new Insets(15));
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(createMode ? "Novi obračun" : "Zaglavlje obračuna");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.WHITE);

        header.getChildren().add(titleLabel);
        return header;
    }

    private void setHeaderColor(String color) {
        if (header != null) {
            header.setStyle("-fx-background-color: " + color + ";");
        }
    }

    private VBox createForm() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(30));
        form.setAlignment(Pos.TOP_CENTER);
        form.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 8;");
        form.setMaxWidth(700);

        // Šifra (ID)
        sifraField = new TextField();
        sifraField.setEditable(false);
        sifraField.setStyle("-fx-background-color: #F6F8FA; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #848D97;");
        if (createMode) {
            sifraField.setText("(automatski)");
        }

        // Naziv obračuna
        nazivField = new TextField();
        nazivField.setStyle("-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");
        nazivField.setPromptText("Unesite naziv obračuna");

        // Tip isplate (OVP)
        tipIsplateCombo = new ComboBox<>();
        loadVrstePrihoda();
        tipIsplateCombo.setMaxWidth(Double.MAX_VALUE);
        tipIsplateCombo.setStyle("-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-radius: 6;");

        // Mesec (YYYY-MM)
        mesecField = new TextField();
        mesecField.setPromptText("YYYY-MM (npr. 2025-11)");
        mesecField.setStyle("-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        // Datum obračuna
        datumObracunaPicker = new DatePicker();
        datumObracunaPicker.setStyle("-fx-background-color: white;");
        datumObracunaPicker.setMaxWidth(Double.MAX_VALUE);

        // Datum slanja
        datumSlanjaPicker = new DatePicker();
        datumSlanjaPicker.setMaxWidth(Double.MAX_VALUE);
        rs.hbs.zarade.util.DateFormats.install(datumObracunaPicker);
        rs.hbs.zarade.util.DateFormats.install(datumSlanjaPicker);

        // Oznaka (Konačna/Akontacija)
        oznakaKonacnaCombo = new ComboBox<>();
        oznakaKonacnaCombo.getItems().addAll("K - Konačna", "A - Akontacija");
        oznakaKonacnaCombo.setValue("K - Konačna");
        oznakaKonacnaCombo.setMaxWidth(Double.MAX_VALUE);

        // Broj dana
        brojDanaField = new TextField();
        brojDanaField.setPromptText("Broj kalendarskih dana");

        // Broj sati (Fond sati)
        brojSatiField = new TextField();
        brojSatiField.setPromptText("Mesečni fond sati");

        // Broj prijave UID
        brojPrijaveField = new TextField();
        brojPrijaveField.setPromptText("Jedinstveni ID prijave");

        // Poslodavac
        poslodavacCombo = new ComboBox<>();
        poslodavacCombo.setMaxWidth(Double.MAX_VALUE);
        poslodavacCombo.setStyle("-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-radius: 6;");

        // Brzo biranje kucanjem početnog slova
        ComboBoxHelper.makeSearchable(tipIsplateCombo);
        ComboBoxHelper.makeSearchable(oznakaKonacnaCombo);
        ComboBoxHelper.makeSearchable(poslodavacCombo);

        // Dodaj polja u formu
        form.getChildren().addAll(
                createFieldRow("Šifra:", sifraField),
                createFieldRow("Naziv obračuna: *", nazivField),
                createFieldRow("Tip isplate: *", tipIsplateCombo),
                createFieldRow("Mesec: *", mesecField),
                createFieldRow("Datum obračuna: *", datumObracunaPicker),
                createFieldRow("Datum slanja: *", datumSlanjaPicker),
                createFieldRow("Oznaka:", oznakaKonacnaCombo),
                new Separator(),
                createFieldRow("Broj dana:", brojDanaField),
                createFieldRow("Fond sati:", brojSatiField),
                createFieldRow("Broj prijave UID:", brojPrijaveField),
                new Separator(),
                createFieldRow("Poslodavac: *", poslodavacCombo)
        );

        // Dirty tracking
        nazivField.textProperty().addListener((obs, o, n) -> markDirty());
        tipIsplateCombo.valueProperty().addListener((obs, o, n) -> markDirty());
        mesecField.textProperty().addListener((obs, o, n) -> markDirty());
        datumObracunaPicker.valueProperty().addListener((obs, o, n) -> markDirty());
        datumSlanjaPicker.valueProperty().addListener((obs, o, n) -> markDirty());
        oznakaKonacnaCombo.valueProperty().addListener((obs, o, n) -> markDirty());
        brojDanaField.textProperty().addListener((obs, o, n) -> markDirty());
        brojSatiField.textProperty().addListener((obs, o, n) -> markDirty());
        brojPrijaveField.textProperty().addListener((obs, o, n) -> markDirty());
        poslodavacCombo.valueProperty().addListener((obs, o, n) -> markDirty());

        return form;
    }

    private HBox createFieldRow(String labelText, javafx.scene.Node field) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.setPrefWidth(160);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        if (labelText.contains("*")) {
            label.setStyle("-fx-text-fill: #0969DA;");
        } else {
            label.setStyle("-fx-text-fill: #57606A;");
        }

        HBox.setHgrow(field, Priority.ALWAYS);

        row.getChildren().addAll(label, field);
        return row;
    }

    private HBox createFooter() {
        HBox footer = new HBox(10);
        footer.setStyle("-fx-background-color: #F6F8FA; -fx-border-color: #D0D7DE transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        footer.setPadding(new Insets(10, 20, 10, 20));
        footer.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("Zatvori");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #FFEBE9; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6;"));
        closeBtn.setOnAction(e -> close());

        cancelBtn = new Button("Poništi");
        cancelBtn.setStyle("-fx-background-color: white; -fx-text-fill: #57606A; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> {
            if (createMode) {
                clearFields();
            } else {
                populateFields();
            }
            markClean();
        });
        cancelBtn.setDisable(true);

        saveBtn = new Button("Snimi");
        saveBtn.setStyle("-fx-background-color: #1A7F37; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;");
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle("-fx-background-color: #116329; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;"));
        saveBtn.setOnMouseExited(e -> saveBtn.setStyle("-fx-background-color: #1A7F37; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;"));
        saveBtn.setOnAction(e -> saveData());
        saveBtn.setDisable(!createMode);

        footer.getChildren().addAll(saveBtn, cancelBtn, spacer, closeBtn);

        return footer;
    }

    private void loadVrstePrihoda() {
        try {
            Connection conn = DatabaseConnection.getInstance().getMainConnection();
            if (conn != null) {
                tipIsplateCombo.getItems().setAll(new VrstaPrihodaDao().findAll(conn));
            }
        } catch (Exception e) {
            System.err.println("Greška pri učitavanju vrsta prihoda: " + e.getMessage());
        }
    }

    private void loadPoslodavci() {
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            Connection conn = dbConn.getMainConnection();
            if (conn != null) {
                PoslodavacDao dao = new PoslodavacDao();
                List<Poslodavac> lista = dao.findAll(conn);

                poslodavacCombo.getItems().clear();
                poslodavacCombo.getItems().addAll(lista);

                // Custom cell factory
                poslodavacCombo.setCellFactory(lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(Poslodavac item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getIdPoslodavac() + " - " + item.getNazivPrezimeIme());
                        }
                    }
                });

                poslodavacCombo.setButtonCell(new ListCell<>() {
                    @Override
                    protected void updateItem(Poslodavac item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("Izaberite poslodavca...");
                        } else {
                            setText(item.getNazivPrezimeIme());
                        }
                    }
                });

                // Selektuj prvog ako postoji (u create mode)
                if (!lista.isEmpty() && createMode) {
                    loading = true;
                    poslodavacCombo.setValue(lista.get(0));
                    loading = false;
                }
            }
        } catch (Exception e) {
            System.err.println("Greška pri učitavanju poslodavaca: " + e.getMessage());
        }
    }

    private void populateFields() {
        if (obracunDef == null) return;
        loading = true;
        try {

        sifraField.setText(obracunDef.getIdObracun() != null ?
                String.valueOf(obracunDef.getIdObracun()) : "");
        nazivField.setText(obracunDef.getNazivObracuna() != null ?
                obracunDef.getNazivObracuna() : "");

        // Tip isplate
        if (obracunDef.getOvp() != null) {
            String ovp = String.valueOf(obracunDef.getOvp());
            for (VrstaPrihoda item : tipIsplateCombo.getItems()) {
                if (item.getVpz() != null && item.getVpz().trim().equals(ovp)) {
                    tipIsplateCombo.setValue(item);
                    break;
                }
            }
        }

        mesecField.setText(obracunDef.getMesec() != null ? obracunDef.getMesec() : "");

        if (obracunDef.getDatumObracuna() != null) {
            datumObracunaPicker.setValue(obracunDef.getDatumObracuna());
        }
        if (obracunDef.getDatumSlanja() != null) {
            datumSlanjaPicker.setValue(obracunDef.getDatumSlanja());
        }

        // Oznaka
        if ("A".equalsIgnoreCase(obracunDef.getOznakaZaKonacnu())) {
            oznakaKonacnaCombo.setValue("A - Akontacija");
        } else {
            oznakaKonacnaCombo.setValue("K - Konačna");
        }

        brojDanaField.setText(obracunDef.getBrojDana() != null ?
                String.valueOf(obracunDef.getBrojDana()) : "");
        brojSatiField.setText(obracunDef.getFondSati() != null ?
                String.valueOf(obracunDef.getFondSati()) : "");
        brojPrijaveField.setText(obracunDef.getPuId() != null ?
                String.valueOf(obracunDef.getPuId()) : "");

        // Poslodavac
        if (obracunDef.getLnkPoslodavac() != null) {
            for (Poslodavac p : poslodavacCombo.getItems()) {
                if (p.getIdPoslodavac() != null &&
                        p.getIdPoslodavac().equals(obracunDef.getLnkPoslodavac())) {
                    poslodavacCombo.setValue(p);
                    break;
                }
            }
        }

        } finally {
            loading = false;
        }
        markClean();
    }

    private void clearFields() {
        sifraField.setText("(automatski)");
        nazivField.clear();
        tipIsplateCombo.setValue(null);
        mesecField.clear();
        datumObracunaPicker.setValue(null);
        datumSlanjaPicker.setValue(null);
        oznakaKonacnaCombo.setValue("K - Konačna");
        brojDanaField.clear();
        brojSatiField.clear();
        brojPrijaveField.clear();
    }

    private void saveData() {
        // Validacija
        StringBuilder errors = new StringBuilder();

        if (nazivField.getText().isEmpty()) {
            errors.append("- Naziv obračuna je obavezan\n");
        }
        if (tipIsplateCombo.getValue() == null) {
            errors.append("- Tip isplate je obavezan\n");
        }
        if (mesecField.getText().isEmpty()) {
            errors.append("- Mesec je obavezan\n");
        } else if (!mesecField.getText().matches("\\d{4}-(0[1-9]|1[0-2])")) {
            errors.append("- Mesec mora biti u formatu YYYY-MM (npr. 2025-11)\n");
        }
        if (datumObracunaPicker.getValue() == null) {
            errors.append("- Datum obračuna je obavezan\n");
        }
        if (datumSlanjaPicker.getValue() == null) {
            errors.append("- Datum slanja je obavezan\n");
        }
        if (poslodavacCombo.getValue() == null) {
            errors.append("- Poslodavac je obavezan\n");
        }

        if (errors.length() > 0) {
            showError("Validacija", "Popunite obavezna polja:\n\n" + errors.toString());
            return;
        }

        // Popuni obracunDef sa podacima iz forme
        obracunDef.setNazivObracuna(nazivField.getText());

        // Izvuci OVP iz combo
        VrstaPrihoda tipValue = tipIsplateCombo.getValue();
        if (tipValue != null && tipValue.getVpz() != null) {
            try {
                obracunDef.setOvp(Integer.parseInt(tipValue.getVpz().trim()));
            } catch (NumberFormatException e) {
                showError("Validacija", "Šifra vrste isplate nije numerička: " + tipValue.getVpz());
                return;
            }
        }

        obracunDef.setMesec(mesecField.getText());
        obracunDef.setDatumObracuna(datumObracunaPicker.getValue());
        obracunDef.setDatumSlanja(datumSlanjaPicker.getValue());

        // Oznaka
        String oznakaValue = oznakaKonacnaCombo.getValue();
        obracunDef.setOznakaZaKonacnu(oznakaValue != null && oznakaValue.startsWith("A") ? "A" : "K");

        // Brojevi
        if (!brojDanaField.getText().isEmpty()) {
            try {
                obracunDef.setBrojDana(Integer.parseInt(brojDanaField.getText()));
            } catch (NumberFormatException ignored) {}
        }
        if (!brojSatiField.getText().isEmpty()) {
            try {
                obracunDef.setFondSati(Integer.parseInt(brojSatiField.getText()));
            } catch (NumberFormatException ignored) {}
        }
        if (!brojPrijaveField.getText().isEmpty()) {
            try {
                obracunDef.setPuId(Integer.parseInt(brojPrijaveField.getText()));
            } catch (NumberFormatException ignored) {}
        }

        // Poslodavac
        Poslodavac selectedPoslodavac = poslodavacCombo.getValue();
        if (selectedPoslodavac != null) {
            obracunDef.setLnkPoslodavac(selectedPoslodavac.getIdPoslodavac());
        }

        // Snimi u bazu
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            Connection conn = dbConn.getMainConnection();
            ObracunDefinicijaDao dao = new ObracunDefinicijaDao();

            if (createMode) {
                dao.save(conn, obracunDef);
                createMode = false; // prebaci u edit mode
                sifraField.setText(obracunDef.getIdObracun() != null ?
                        String.valueOf(obracunDef.getIdObracun()) : "");
            } else {
                dao.update(conn, obracunDef);
            }

            markSaved();

            if (onSaveCallback != null) {
                onSaveCallback.accept(obracunDef);
            }
        } catch (SQLException e) {
            showError("Greška pri čuvanju", e.getMessage());
        }
    }

    private void markDirty() {
        if (loading) return;
        if (!hasUnsavedChanges) {
            hasUnsavedChanges = true;
            if (unsavedBanner != null) {
                unsavedBanner.setVisible(true);
                unsavedBanner.setManaged(true);
            }
            setHeaderColor(COLOR_DIRTY);
            if (saveBtn != null) saveBtn.setDisable(false);
            if (cancelBtn != null) cancelBtn.setDisable(false);
        }
    }

    private void markClean() {
        hasUnsavedChanges = false;
        if (unsavedBanner != null) {
            unsavedBanner.setVisible(false);
            unsavedBanner.setManaged(false);
        }
        setHeaderColor(createMode ? COLOR_NEW : COLOR_VIEW);
        if (saveBtn != null) saveBtn.setDisable(!createMode);
        if (cancelBtn != null) cancelBtn.setDisable(true);
    }

    private void markSaved() {
        hasUnsavedChanges = false;
        if (unsavedBanner != null) {
            unsavedBanner.setVisible(false);
            unsavedBanner.setManaged(false);
        }
        setHeaderColor(COLOR_SAVED);
        if (saveBtn != null) saveBtn.setDisable(true);
        if (cancelBtn != null) cancelBtn.setDisable(true);
    }

    private void close() {
        if (hasUnsavedChanges) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Nesačuvane izmene");
            confirm.setHeaderText("Imate nesačuvane izmene.");
            confirm.setContentText("Da li ste sigurni da želite da izađete bez čuvanja?");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }
        AppLayout.setContent(new ObracuniScreen().getRoot());
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Greška");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }
}
