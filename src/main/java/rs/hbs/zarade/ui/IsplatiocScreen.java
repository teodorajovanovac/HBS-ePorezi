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
import rs.hbs.zarade.db.OpstinaDao;
import rs.hbs.zarade.db.PoslodavacDao;
import rs.hbs.zarade.db.VrstaIsplatiocaDao;
import rs.hbs.zarade.domain.Opstina;
import rs.hbs.zarade.domain.Poslodavac;
import rs.hbs.zarade.domain.VrstaIsplatioca;
import rs.hbs.zarade.validation.PibValidator;

import javafx.application.Platform;
import javafx.stage.Window;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ekran isplatioca - Screen 7 iz UI specifikacije.
 */
public class IsplatiocScreen {

    private static final String COLOR_VIEW = "#343942"; // tamno siva: pogled
    private static final String COLOR_DIRTY = "#9A6700"; // amber: nesačuvane izmene
    private static final String COLOR_SAVED = "#1A7F37"; // zelena: sačuvano

    private BorderPane root;
    private HBox header;
    private Button saveBtn;
    private Button cancelBtn;

    private Poslodavac poslodavac;
    private boolean hasUnsavedChanges = false;
    private boolean loading = false;

    // Form fields
    private TextField idField;
    private ComboBox<VrstaIsplatioca> tipIsplatiocaCombo;
    private TextField pibField;
    private ComboBox<Opstina> sifraOpstineCombo;
    private ComboBox<Opstina> nazivOpstineCombo;

    // Lookup data
    private List<Opstina> opstine = new ArrayList<>();
    private List<VrstaIsplatioca> vrsteIsplatioca = new ArrayList<>();
    private TextField brojZaposlenihField;
    private TextField mbField;
    private TextField nazivField;
    private TextField telefonField;
    private TextField ulicaField;
    private TextField emailField;
    private TextField odgovornoLiceField;
    private Label pibValidationLabel;

    public IsplatiocScreen() {
        loadPoslodavac();
        createUI();
        populateFields();
    }

    private void loadPoslodavac() {
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            Connection conn = dbConn.getMainConnection();
            if (conn != null) {
                PoslodavacDao dao = new PoslodavacDao();
                List<Poslodavac> lista = dao.findAll(conn);
                if (!lista.isEmpty())
                    poslodavac = lista.get(0);

                OpstinaDao oDao = new OpstinaDao();
                opstine = oDao.findAll(conn);

                VrstaIsplatiocaDao viDao = new VrstaIsplatiocaDao();
                vrsteIsplatioca = viDao.findAll(conn);
            }
        } catch (Exception e) {
            poslodavac = new Poslodavac();
        }
        if (poslodavac == null)
            poslodavac = new Poslodavac();
    }

    private void createUI() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #F6F8FA;");

        header = createHeader();
        root.setTop(header);

        VBox form = createForm();
        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        root.setCenter(scrollPane);

        HBox footer = createFooter();
        root.setBottom(footer);
    }

    private HBox createHeader() {
        HBox hdr = new HBox();
        hdr.setStyle("-fx-background-color: " + COLOR_VIEW + ";");
        hdr.setPadding(new Insets(15));
        hdr.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Podaci o isplatiocu");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.WHITE);

        hdr.getChildren().add(titleLabel);
        return hdr;
    }

    private void setHeaderColor(String color) {
        header.setStyle("-fx-background-color: " + color + ";");
    }

    private VBox createForm() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(30));
        form.setAlignment(Pos.TOP_CENTER);
        form.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 8;");
        form.setMaxWidth(760);
        form.setStyle(form.getStyle() + " -fx-font-size: 15px;");

        idField = new TextField();
        idField.setEditable(false);
        idField.setStyle(
                "-fx-background-color: #F6F8FA; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #848D97;");

        tipIsplatiocaCombo = new ComboBox<>();
        for (VrstaIsplatioca vi : vrsteIsplatioca)
            tipIsplatiocaCombo.getItems().add(vi);
        tipIsplatiocaCombo.setMaxWidth(Double.MAX_VALUE);
        tipIsplatiocaCombo.setStyle("-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-radius: 6;");

        VBox pibBox = new VBox(4);
        pibField = new TextField();
        pibField.setPromptText("9 cifara");
        pibField.setStyle(
                "-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");
        pibValidationLabel = new Label();
        pibValidationLabel.setFont(Font.font("Segoe UI", 11));
        pibField.textProperty().addListener((obs, oldVal, newVal) -> validatePib(newVal));
        pibBox.getChildren().addAll(pibField, pibValidationLabel);

        brojZaposlenihField = new TextField();
        brojZaposlenihField.setStyle(
                "-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        mbField = new TextField();
        mbField.setStyle(
                "-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        nazivField = new TextField();
        nazivField.setStyle(
                "-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        sifraOpstineCombo = new ComboBox<>();
        sifraOpstineCombo.setPrefWidth(100);
        sifraOpstineCombo.setStyle(
                "-fx-background-color: white; -fx-font-size: 15px; -fx-border-color: #D0D7DE; -fx-border-radius: 6;");
        for (Opstina o : opstine)
            sifraOpstineCombo.getItems().add(o);
        sifraOpstineCombo.setCellFactory(lv -> new javafx.scene.control.ListCell<Opstina>() {
            @Override
            protected void updateItem(Opstina item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getSifra());
            }
        });
        sifraOpstineCombo.setButtonCell(new javafx.scene.control.ListCell<Opstina>() {
            @Override
            protected void updateItem(Opstina item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getSifra());
            }
        });

        nazivOpstineCombo = new ComboBox<>();
        nazivOpstineCombo.setPrefWidth(250);
        nazivOpstineCombo.setStyle(
                "-fx-background-color: white; -fx-font-size: 15px; -fx-border-color: #D0D7DE; -fx-border-radius: 6;");
        for (Opstina o : opstine)
            nazivOpstineCombo.getItems().add(o);
        nazivOpstineCombo.setCellFactory(lv -> new javafx.scene.control.ListCell<Opstina>() {
            @Override
            protected void updateItem(Opstina item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNaziv());
            }
        });
        nazivOpstineCombo.setButtonCell(new javafx.scene.control.ListCell<Opstina>() {
            @Override
            protected void updateItem(Opstina item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNaziv());
            }
        });

        sifraOpstineCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(nazivOpstineCombo.getValue()))
                nazivOpstineCombo.setValue(newVal);
        });
        nazivOpstineCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(sifraOpstineCombo.getValue()))
                sifraOpstineCombo.setValue(newVal);
        });

        // Brzo biranje kucanjem početnog slova (type-ahead)
        ComboBoxHelper.makeSearchable(nazivOpstineCombo, Opstina::getNaziv);
        ComboBoxHelper.makeSearchable(sifraOpstineCombo, Opstina::getSifra);
        ComboBoxHelper.makeSearchable(tipIsplatiocaCombo, VrstaIsplatioca::getNaziv);

        HBox sedisteRow = new HBox(10);
        sedisteRow.setAlignment(Pos.CENTER_LEFT);
        sedisteRow.getChildren().addAll(sifraOpstineCombo, nazivOpstineCombo);

        telefonField = new TextField();
        telefonField.setStyle(
                "-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        ulicaField = new TextField();
        ulicaField.setStyle(
                "-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        emailField = new TextField();
        emailField.setStyle(
                "-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        odgovornoLiceField = new TextField();
        odgovornoLiceField.setStyle(
                "-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        form.getChildren().addAll(
                createFieldRow("ID Poslodavac:", idField),
                createFieldRow("Naziv: *", nazivField),
                createFieldRow("Tip isplatioca: *", tipIsplatiocaCombo),
                createFieldRow("PIB: *", pibBox),
                createFieldRow("MB isplatioca: *", mbField),
                createFieldRow("Ulica i broj: *", ulicaField),
                createFieldRow("SedištePrebivalište: *", sedisteRow),

                createFieldRow("Broj zaposlenih: *", brojZaposlenihField),

                createFieldRow("Telefon: *", telefonField),
                createFieldRow("eMail: *", emailField),

                createFieldRow("Odgovorno lice:", odgovornoLiceField));

        // Dirty tracking
        tipIsplatiocaCombo.valueProperty().addListener((obs, o, n) -> markDirty());
        pibField.textProperty().addListener((obs, o, n) -> markDirty());
        brojZaposlenihField.textProperty().addListener((obs, o, n) -> markDirty());
        mbField.textProperty().addListener((obs, o, n) -> markDirty());
        nazivField.textProperty().addListener((obs, o, n) -> markDirty());
        sifraOpstineCombo.valueProperty().addListener((obs, o, n) -> markDirty());
        telefonField.textProperty().addListener((obs, o, n) -> markDirty());
        ulicaField.textProperty().addListener((obs, o, n) -> markDirty());
        emailField.textProperty().addListener((obs, o, n) -> markDirty());
        odgovornoLiceField.textProperty().addListener((obs, o, n) -> markDirty());

        return form;
    }

    private Separator createSeparator() {
        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 4 0 4 0;");
        sep.skinProperty().addListener((obs, o, n) -> {
            javafx.scene.Node line = sep.lookup(".line");
            if (line != null)
                line.setStyle("-fx-border-width: 0 0 0 0; -fx-background-color: #57606A; -fx-padding: 0;");
        });
        return sep;
    }

    private HBox createFieldRow(String labelText, javafx.scene.Node field) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.setPrefWidth(160);
        label.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        label.setStyle("-fx-text-fill: #57606A;");
        HBox.setHgrow(field, Priority.ALWAYS);
        row.getChildren().addAll(label, field);
        return row;
    }

    private void validatePib(String value) {
        if (value == null || value.isEmpty()) {
            pibField.setStyle(
                    "-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");
            pibValidationLabel.setText("");
            return;
        }
        var result = PibValidator.validate(value);
        if (result.isValid()) {
            pibField.setStyle(
                    "-fx-background-color: white; -fx-border-color: #1A7F37; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6;");
            pibValidationLabel.setTextFill(Color.web("#1A7F37"));
            pibValidationLabel.setText("✓ PIB validan");
        } else {
            pibField.setStyle(
                    "-fx-background-color: white; -fx-border-color: #CF222E; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6;");
            pibValidationLabel.setTextFill(Color.web("#CF222E"));
            pibValidationLabel.setText("✗ " + result.getMessage());
        }
    }

    private HBox createFooter() {
        HBox footer = new HBox(10);
        footer.setStyle(
                "-fx-background-color: #F6F8FA; -fx-border-color: #D0D7DE transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        footer.setPadding(new Insets(10, 20, 10, 20));
        footer.setAlignment(Pos.CENTER_LEFT);

        saveBtn = new Button("Snimi");
        saveBtn.setStyle(
                "-fx-background-color: #1A7F37; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 7 18; -fx-background-radius: 6; -fx-cursor: hand;");
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(
                "-fx-background-color: #116329; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 7 18; -fx-background-radius: 6; -fx-cursor: hand;"));
        saveBtn.setOnMouseExited(e -> saveBtn.setStyle(
                "-fx-background-color: #1A7F37; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 7 18; -fx-background-radius: 6; -fx-cursor: hand;"));
        saveBtn.setOnAction(e -> saveData());
        saveBtn.setDisable(true);

        cancelBtn = new Button("Poništi");
        cancelBtn.setStyle(
                "-fx-background-color: white; -fx-text-fill: #57606A; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> populateFields());
        cancelBtn.setDisable(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("Zatvori");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color: #FFEBE9; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6;"));
        closeBtn.setOnAction(e -> {
            if (hasUnsavedChanges) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Nesačuvane izmene");
                confirm.setHeaderText("Imate nesačuvane izmene.");
                confirm.setContentText("Da li ste sigurni da želite da izađete bez čuvanja?");
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK)
                    return;
            }
            HbsZaradeApp.navigateTo(new WelcomeDashboard().getRoot());
        });

        footer.getChildren().addAll(saveBtn, cancelBtn, spacer, closeBtn);
        return footer;
    }

    private void populateFields() {
        loading = true;
        try {
            if (poslodavac != null) {
                idField.setText(
                        poslodavac.getIdPoslodavac() != null ? String.valueOf(poslodavac.getIdPoslodavac()) : "");

                if (poslodavac.getTipIsplatioca() != null) {
                    int tip = poslodavac.getTipIsplatioca();
                    for (VrstaIsplatioca vi : tipIsplatiocaCombo.getItems()) {
                        if (vi.getId() != null && vi.getId() == tip) {
                            tipIsplatiocaCombo.setValue(vi);
                            break;
                        }
                    }
                }

                pibField.setText(
                        poslodavac.getPoreskiIdentifikacioniBroj() != null ? poslodavac.getPoreskiIdentifikacioniBroj()
                                : "");
                brojZaposlenihField.setText(
                        poslodavac.getBrojZaposlenih() != null ? String.valueOf(poslodavac.getBrojZaposlenih()) : "");
                mbField.setText(
                        poslodavac.getMaticniBrojIsplatioca() != null ? poslodavac.getMaticniBrojIsplatioca() : "");
                nazivField.setText(poslodavac.getNazivPrezimeIme() != null ? poslodavac.getNazivPrezimeIme() : "");

                String sifraOpstine = poslodavac.getSedistePrebivaliste();
                if (sifraOpstine != null && !sifraOpstine.isEmpty()) {
                    for (Opstina o : opstine) {
                        if (sifraOpstine.equals(o.getSifra())) {
                            sifraOpstineCombo.setValue(o);
                            nazivOpstineCombo.setValue(o);
                            break;
                        }
                    }
                }

                telefonField.setText(poslodavac.getTelefon() != null ? poslodavac.getTelefon() : "");
                ulicaField.setText(poslodavac.getUlicaIBroj() != null ? poslodavac.getUlicaIBroj() : "");
                emailField.setText(poslodavac.getEmail() != null ? poslodavac.getEmail() : "");
                odgovornoLiceField.setText(poslodavac.getOdgovornoLice() != null ? poslodavac.getOdgovornoLice() : "");
            }
        } finally {
            loading = false;
        }
        hasUnsavedChanges = false;
        saveBtn.setDisable(true);
        cancelBtn.setDisable(true);
        setHeaderColor(COLOR_VIEW);
    }

    private void saveData() {
        StringBuilder errors = new StringBuilder();
        if (tipIsplatiocaCombo.getValue() == null)
            errors.append("- Tip isplatioca je obavezan\n");
        if (pibField.getText().isEmpty())
            errors.append("- PIB je obavezan\n");
        else if (!PibValidator.isValid(pibField.getText()))
            errors.append("- PIB nije validan\n");
        if (brojZaposlenihField.getText().isEmpty())
            errors.append("- Broj zaposlenih je obavezan\n");
        if (mbField.getText().isEmpty())
            errors.append("- MB isplatioca je obavezan\n");
        if (nazivField.getText().isEmpty())
            errors.append("- Naziv je obavezan\n");
        if (sifraOpstineCombo.getValue() == null)
            errors.append("- Sedište (opština) je obavezno\n");
        if (telefonField.getText().isEmpty())
            errors.append("- Telefon je obavezan\n");
        if (ulicaField.getText().isEmpty())
            errors.append("- Ulica i broj je obavezno\n");
        if (emailField.getText().isEmpty())
            errors.append("- eMail je obavezan\n");

        if (errors.length() > 0) {
            showError("Validacija", "Popunite sva obavezna polja:\n\n" + errors.toString());
            return;
        }

        VrstaIsplatioca odabraniTip = tipIsplatiocaCombo.getValue();
        poslodavac.setTipIsplatioca(odabraniTip != null ? odabraniTip.getId() : null);
        poslodavac.setPoreskiIdentifikacioniBroj(pibField.getText().trim());
        poslodavac.setMaticniBrojIsplatioca(mbField.getText().trim());
        poslodavac.setNazivPrezimeIme(nazivField.getText().trim());
        try {
            String bz = brojZaposlenihField.getText().trim();
            poslodavac.setBrojZaposlenih(bz.isEmpty() ? null : Integer.parseInt(bz));
        } catch (NumberFormatException e) {
            showError("Validacija", "Broj zaposlenih mora biti broj!");
            return;
        }
        Opstina odabranaOpstina = sifraOpstineCombo.getValue();
        poslodavac.setSedistePrebivaliste(odabranaOpstina != null ? odabranaOpstina.getSifra() : null);
        poslodavac.setUlicaIBroj(ulicaField.getText().trim());
        poslodavac.setTelefon(telefonField.getText().trim());
        poslodavac.setEmail(emailField.getText().trim());
        poslodavac.setOdgovornoLice(odgovornoLiceField.getText().trim());

        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            Connection conn = dbConn.getMainConnection();
            PoslodavacDao dao = new PoslodavacDao();
            dao.update(conn, poslodavac);
            markSaved();
        } catch (SQLException e) {
            showError("Greška pri čuvanju", e.getMessage());
        }
    }

    private void markDirty() {
        if (loading)
            return;
        if (!hasUnsavedChanges) {
            hasUnsavedChanges = true;
            setHeaderColor(COLOR_DIRTY);
            saveBtn.setDisable(false);
            cancelBtn.setDisable(false);
        }
    }

    private void markSaved() {
        hasUnsavedChanges = false;
        setHeaderColor(COLOR_SAVED);
        saveBtn.setDisable(true);
        cancelBtn.setDisable(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Greška");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Skroluje popup listu comboya do selektovane stavke. */
    private static <T> void scrollComboToSelected(ComboBox<T> combo) {
        T sel = combo.getValue();
        if (sel == null)
            return;
        int idx = combo.getItems().indexOf(sel);
        if (idx < 0)
            return;
        javafx.stage.Stage mainStage = HbsZaradeApp.getPrimaryStage();
        for (Window w : Window.getWindows()) {
            if (w != mainStage && w.isShowing() && w.getScene() != null) {
                javafx.scene.Node node = w.getScene().getRoot().lookup(".list-view");
                if (node instanceof ListView<?> lv && lv.getItems().size() == combo.getItems().size()) {
                    lv.scrollTo(idx);
                    return;
                }
            }
        }
    }

    public BorderPane getRoot() {
        return root;
    }
}
