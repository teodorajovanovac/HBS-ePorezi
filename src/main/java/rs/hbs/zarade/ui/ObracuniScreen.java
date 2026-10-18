package rs.hbs.zarade.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import rs.hbs.zarade.HbsZaradeApp;
import rs.hbs.zarade.db.AppSettingsDao;
import rs.hbs.zarade.db.DatabaseConnection;
import rs.hbs.zarade.db.PoslodavacDao;
import rs.hbs.zarade.domain.Poslodavac;
import rs.hbs.zarade.reporting.PdfReporter;
import rs.hbs.zarade.reporting.XmlExporter;
// AppLayout used directly (same package)
import rs.hbs.zarade.db.ObracunDao;
import rs.hbs.zarade.db.ObracunDefinicijaDao;
import rs.hbs.zarade.db.ObracunStopeDao;
import rs.hbs.zarade.domain.ObracunStope;
import rs.hbs.zarade.service.ObracunService;
import rs.hbs.zarade.util.NumberFormats;
import rs.hbs.zarade.db.IzvestajPrihodiDao;
import rs.hbs.zarade.db.PrimalacDao;
import rs.hbs.zarade.db.VrstaPrihodaDao;
import rs.hbs.zarade.db.VrstaPrimaocaDao;
import rs.hbs.zarade.domain.Obracun;
import rs.hbs.zarade.domain.ObracunDefinicija;
import rs.hbs.zarade.domain.Primalac;
import rs.hbs.zarade.domain.VrstaPrihoda;
import rs.hbs.zarade.domain.VrstaPrimaoca;

import javafx.collections.transformation.FilteredList;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;

/**
 * Glavni pregled obračuna - Screen 8 iz UI specifikacije.
 */
public class ObracuniScreen {

    private BorderPane root;
    private TableView<Obracun> tableView;
    private ListView<ObracunDefinicija> obracunListView;
    private ComboBox<Object> vrstaIsplateCombo; // Object: "Sve vrste" String ili VrstaPrihoda
    private Label sumaNetoLabel;
    private Label sumaBrutoLabel;
    private Label countLabel;
    private ObservableList<Obracun> obracuniData;
    private ObservableList<ObracunDefinicija> definicijeData;
    private FilteredList<ObracunDefinicija> filteredDefinicije;
    private FilteredList<Obracun> filteredObracuni;
    private Map<Integer, Set<String>> obracunSvp3Map = new HashMap<>();
    private List<VrstaPrimaoca> svePrimaociTypes = new ArrayList<>();
    private List<VrstaPrihoda> svePrihodi = new ArrayList<>();

    public ObracuniScreen() {
        createUI();
        loadObracunDefinicije();
    }

    private void createUI() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #F6F8FA;");

        // Header sa filterom vrste isplate
        VBox header = createHeader();
        root.setTop(header);

        // Center: HBox[ListView(180px) | TableView]
        HBox centerBox = createCenterArea();
        root.setCenter(centerBox);

        // Footer sa sumama i dugmadima
        VBox footer = createFooter();
        root.setBottom(footer);
    }

    private VBox createHeader() {
        VBox header = new VBox(8);
        header.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: transparent transparent #D0D7DE transparent; -fx-border-width: 0 0 1 0;");
        header.setPadding(new Insets(14, 16, 14, 16));

        // Naslov
        Label titleLabel = new Label("Obračuni");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web("#1F2328"));

        // Filter vrste isplate - učitava se iz DB
        HBox filterBox = new HBox(12);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Label vrstaLabel = new Label("Vrsta isplate:");
        vrstaLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        vrstaLabel.setTextFill(Color.web("#57606A"));

        vrstaIsplateCombo = new ComboBox<>();
        vrstaIsplateCombo.setPrefWidth(260);
        vrstaIsplateCombo.setStyle("-fx-font-size: 13px;");
        vrstaIsplateCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setText(null);
                else if (item instanceof VrstaPrihoda vp)
                    setText(vp.getVpz() + " - " + vp.getNaziv());
                else
                    setText(item.toString());
            }
        });
        vrstaIsplateCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setText(null);
                else if (item instanceof VrstaPrihoda vp)
                    setText(vp.getVpz() + " - " + vp.getNaziv());
                else
                    setText(item.toString());
            }
        });

        vrstaIsplateCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            applyFilterVrstaIsplate();
        });
        ComboBoxHelper.makeSearchable(vrstaIsplateCombo);

        filterBox.getChildren().addAll(vrstaLabel, vrstaIsplateCombo);
        header.getChildren().addAll(titleLabel, filterBox);

        return header;
    }

    private HBox createCenterArea() {
        // Lijeva strana: ListView obračuna
        obracunListView = new ListView<>();
        obracunListView.setPrefWidth(340);
        obracunListView.setStyle("-fx-background-color: white;");
        obracunListView.setCellFactory(lv -> new ListCell<>() {
            private final Label nameL = new Label();
            private final Label mesecL = new Label();
            private final Label ovpL = new Label();
            private final VBox cellBox = new VBox(3, nameL, mesecL);
            {
                nameL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                nameL.setWrapText(true);
                mesecL.setFont(Font.font("Segoe UI", 12));
                ovpL.setFont(Font.font("Segoe UI", 10));
                ovpL.setTextFill(Color.web("#1565c0"));
                setPadding(new Insets(7, 12, 7, 12));
                // Tanka linija ispod svake stavke kao razdelnik
                setStyle("-fx-border-color: transparent transparent #E8ECF0 transparent; -fx-border-width: 0 0 1 0;");
                selectedProperty().addListener((obs, was, isNow) -> applyColors(isNow));
            }

            private void applyColors(boolean selected) {
                nameL.setTextFill(Color.web(selected ? "#1F2328" : "#57606A"));
                mesecL.setTextFill(Color.web(selected ? "#424953" : "#848D97"));
            }

            @Override
            protected void updateItem(ObracunDefinicija item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    nameL.setText(item.getNazivObracuna() != null ? item.getNazivObracuna() : "(bez naziva)");
                    mesecL.setText(item.getMesec() != null ? item.getMesec() : "");
                    cellBox.getChildren().setAll(nameL, mesecL);
                    if (item.getOvp() != null) {
                        ovpL.setText("OVP: " + item.getOvp());
                        cellBox.getChildren().add(ovpL);
                    }
                    applyColors(isSelected());
                    setGraphic(cellBox);
                    setText(null);
                }
            }
        });

        // Kad se klikne na stavku u listi → učitaj obračune; dupli klik → edit
        // zaglavlja
        obracunListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        loadObracuniForDefinicija(newVal);
                    }
                });
        obracunListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && getSelectedDefinicija() != null) {
                openObracunDetalji();
            }
        });

        // Header iznad ListView
        VBox leftPanel = new VBox(0);
        leftPanel.setPrefWidth(340);

        HBox listHeader = new HBox(6);
        listHeader.setStyle("-fx-background-color: #343942; -fx-padding: 8 10;");
        listHeader.setAlignment(Pos.CENTER_LEFT);
        Label listTitle = new Label("Lista obračuna");
        listTitle.setTextFill(Color.web("#E6EDF3"));
        listTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        // Edit dugme sa olovkom
        Button editDefBtn = new Button("✏");
        editDefBtn.setStyle(
                "-fx-background-color: #3A4A5E; -fx-text-fill: #E6EDF3; -fx-font-size: 15px; -fx-padding: 3 10; -fx-cursor: hand; -fx-background-radius: 5;");
        editDefBtn.setOnMouseEntered(e -> editDefBtn.setStyle(
                "-fx-background-color: #4493F8; -fx-text-fill: white; -fx-font-size: 15px; -fx-padding: 3 10; -fx-cursor: hand; -fx-background-radius: 5;"));
        editDefBtn.setOnMouseExited(e -> editDefBtn.setStyle(
                "-fx-background-color: #3A4A5E; -fx-text-fill: #E6EDF3; -fx-font-size: 15px; -fx-padding: 3 10; -fx-cursor: hand; -fx-background-radius: 5;"));
        editDefBtn.setTooltip(new Tooltip("Izmeni zaglavlje obračuna"));
        editDefBtn.setOnAction(e -> openObracunDetalji());
        // Delete dugme sa kantom
        Button deleteDefBtn = new Button("🗑");
        deleteDefBtn.setStyle(
                "-fx-background-color: #6E2020; -fx-text-fill: #FFAAAA; -fx-font-size: 15px; -fx-padding: 3 10; -fx-cursor: hand; -fx-background-radius: 5;");
        deleteDefBtn.setOnMouseEntered(e -> deleteDefBtn.setStyle(
                "-fx-background-color: #CF222E; -fx-text-fill: white; -fx-font-size: 15px; -fx-padding: 3 10; -fx-cursor: hand; -fx-background-radius: 5;"));
        deleteDefBtn.setOnMouseExited(e -> deleteDefBtn.setStyle(
                "-fx-background-color: #6E2020; -fx-text-fill: #FFAAAA; -fx-font-size: 15px; -fx-padding: 3 10; -fx-cursor: hand; -fx-background-radius: 5;"));
        deleteDefBtn.setTooltip(new Tooltip("Obriši obračun"));
        deleteDefBtn.setOnAction(e -> deleteObracunDefinicija());
        Region listSpacer = new Region();
        HBox.setHgrow(listSpacer, Priority.ALWAYS);
        listHeader.getChildren().addAll(listTitle, listSpacer, editDefBtn, deleteDefBtn);

        leftPanel.getChildren().addAll(listHeader, obracunListView);
        VBox.setVgrow(obracunListView, Priority.ALWAYS);

        // Desna strana: tabela stavki
        tableView = createTable();
        tableView.setMinWidth(0);

        // Wrapper daje desni razmak i dozvoljava tabeli da se skuplja kad prozor nije maksimiziran
        StackPane tableWrap = new StackPane(tableView);
        tableWrap.setPadding(new Insets(0, 16, 0, 0));
        tableWrap.setMinWidth(0);
        HBox.setHgrow(tableWrap, Priority.ALWAYS);
        leftPanel.setMinWidth(240);

        HBox centerBox = new HBox(0);
        Separator vertSep = new Separator();
        vertSep.setOrientation(javafx.geometry.Orientation.VERTICAL);
        centerBox.getChildren().addAll(leftPanel, vertSep, tableWrap);
        return centerBox;
    }

    private TableView<Obracun> createTable() {
        TableView<Obracun> table = new TableView<>();
        table.setStyle("-fx-background-color: white; -fx-font-size: 14px;");
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Kolone — minWidth osigurava da header tekst stane, CONSTRAINED puni ostatak
        TableColumn<Obracun, String> primalacCol = new TableColumn<>("Primalac");
        primalacCol.setCellValueFactory(data -> {
            Obracun o = data.getValue();
            String name = (o.getPrezime() != null ? o.getPrezime() : "") + " " +
                    (o.getIme() != null ? o.getIme() : "");
            return new SimpleStringProperty(name.trim());
        });
        primalacCol.setMinWidth(120);

        TableColumn<Obracun, String> vrstaPrimaocaCol = new TableColumn<>("Vrsta primaoca");
        vrstaPrimaocaCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getSvp2() != null ? data.getValue().getSvp2() : ""));
        vrstaPrimaocaCol.setMinWidth(100);
        vrstaPrimaocaCol.setMaxWidth(130);

        TableColumn<Obracun, String> vrstaIsplateCol = new TableColumn<>("Vrsta isplate");
        vrstaIsplateCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getSvp3() != null ? data.getValue().getSvp3() : ""));
        vrstaIsplateCol.setMinWidth(85);
        vrstaIsplateCol.setMaxWidth(110);

        TableColumn<Obracun, String> netoCol = new TableColumn<>("Neto iznos");
        netoCol.setCellValueFactory(data -> {
            BigDecimal neto = data.getValue().getNeto();
            return new SimpleStringProperty(neto != null ? NumberFormats.money(neto) : "0,00");
        });
        netoCol.setMinWidth(110);
        netoCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 12 0 0;");

        TableColumn<Obracun, String> brutoCol = new TableColumn<>("Bruto iznos");
        brutoCol.setCellValueFactory(data -> {
            BigDecimal bruto = data.getValue().getBruto();
            return new SimpleStringProperty(bruto != null ? NumberFormats.money(bruto) : "0,00");
        });
        brutoCol.setMinWidth(110);
        brutoCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 18 0 0;");

        table.getColumns().addAll(primalacCol, vrstaPrimaocaCol, vrstaIsplateCol, netoCol, brutoCol);

        // Dupli klik za editovanje + context menu za brisanje
        table.setRowFactory(tv -> {
            TableRow<Obracun> row = new TableRow<>();

            MenuItem editItem = new MenuItem("Izmeni stavku");
            editItem.setOnAction(e -> {
                if (!row.isEmpty())
                    editObracunStavka(row.getItem());
            });

            MenuItem deleteItem = new MenuItem("Ukloni primaoca iz obračuna");
            deleteItem.setStyle("-fx-text-fill: #CF222E;");
            deleteItem.setOnAction(e -> ukloniStavku(row.getItem()));

            MenuItem primalacItem = new MenuItem("Podaci primaoca");
            primalacItem.setOnAction(e -> {
                if (!row.isEmpty())
                    openPodaciPrimaoca(row.getItem());
            });

            ContextMenu contextMenu = new ContextMenu(editItem, new SeparatorMenuItem(), deleteItem, primalacItem);
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu));

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editObracunStavka(row.getItem());
                }
            });
            return row;
        });

        table.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.DELETE) {
                ukloniStavku(table.getSelectionModel().getSelectedItem());
            }
        });

        return table;
    }

    /** Uklanja primaoca iz obračuna (uz potvrdu) i briše stavku iz baze. */
    private void ukloniStavku(Obracun item) {
        if (item == null)
            return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Brisanje stavke");
        confirm.setHeaderText("Ukloni primaoca iz obračuna?");
        confirm.setContentText(item.getPrezime() + " " + item.getIme());
        ButtonType btnObrisi = new ButtonType("Ukloni", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnOtkazi = new ButtonType("Otkaži", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnObrisi, btnOtkazi);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != btnObrisi)
            return;

        try {
            if (item.getIdPobracun() != null) {
                new ObracunDao().delete(DatabaseConnection.getInstance().getMainConnection(), item.getIdPobracun());
            }
        } catch (Exception ex) {
            showError("Greška", "Brisanje stavke nije uspelo: " + ex.getMessage());
            return;
        }
        obracuniData.remove(item);
        refreshObracunSvp3Map();
        updateSume();
    }

    private VBox createFooter() {
        VBox footer = new VBox(10);
        footer.setStyle(
                "-fx-background-color: #F6F8FA; -fx-border-color: #D0D7DE transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        footer.setPadding(new Insets(10, 20, 10, 20));

        // Red sa sumama
        HBox sumeBox = new HBox(20);
        sumeBox.setAlignment(Pos.CENTER_LEFT);

        countLabel = new Label("0 stavki");
        countLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        countLabel.setTextFill(Color.web("#57606A"));

        sumaNetoLabel = new Label("NETO: 0,00 RSD");
        sumaNetoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        sumaNetoLabel.setStyle(
                "-fx-background-color: #DDF4FF; -fx-text-fill: #0969DA; -fx-padding: 5 14; -fx-background-radius: 4;");

        sumaBrutoLabel = new Label("BRUTO: 0,00 RSD");
        sumaBrutoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        sumaBrutoLabel.setStyle(
                "-fx-background-color: #D4EFD9; -fx-text-fill: #1A7F37; -fx-padding: 5 14; -fx-background-radius: 4;");

        sumeBox.getChildren().addAll(countLabel, sumaNetoLabel, sumaBrutoLabel);

        // Red sa dugmadima
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);

        Button autoObradaBtn = new Button("Automatska obrada");
        autoObradaBtn.setStyle(
                "-fx-background-color: #1A7F37; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand;");
        autoObradaBtn.setOnMouseEntered(e -> autoObradaBtn.setStyle(
                "-fx-background-color: #116329; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand;"));
        autoObradaBtn.setOnMouseExited(e -> autoObradaBtn.setStyle(
                "-fx-background-color: #1A7F37; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand;"));
        autoObradaBtn.setOnAction(e -> runAutomatskaObrada());

        // Taster je skriven; Shift+Ctrl+Insert ga prikazuje/sakriva (važi dok je ovaj ekran u sceni)
        autoObradaBtn.setVisible(false);
        autoObradaBtn.setManaged(false);
        javafx.scene.input.KeyCombination toggleCombo = new javafx.scene.input.KeyCodeCombination(
                javafx.scene.input.KeyCode.INSERT,
                javafx.scene.input.KeyCombination.SHIFT_DOWN,
                javafx.scene.input.KeyCombination.CONTROL_DOWN);
        javafx.event.EventHandler<javafx.scene.input.KeyEvent> toggleHandler = ev -> {
            if (toggleCombo.match(ev)) {
                boolean show = !autoObradaBtn.isVisible();
                autoObradaBtn.setVisible(show);
                autoObradaBtn.setManaged(show);
                ev.consume();
            }
        };
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null)
                oldScene.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, toggleHandler);
            if (newScene != null)
                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, toggleHandler);
        });

        // Pojedinačna obrada - zakomentarisano
        // Button pojedinacnaBtn = new Button("Pojedinačna obrada");
        // pojedinacnaBtn.setOnAction(e -> { ... });

        // Izveštaj 1 - zakomentarisano
        // Button izvestaj1Btn = new Button("Izveštaj 1");
        // izvestaj1Btn.setOnAction(e -> generateIzvestaj("zbirni"));

        Button izvestajPojedinacniBtn = new Button("Izveštaj obračun");
        izvestajPojedinacniBtn.setStyle(
                "-fx-background-color: white; -fx-text-fill: #1F2328; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;");
        izvestajPojedinacniBtn.setOnMouseEntered(e -> izvestajPojedinacniBtn.setStyle(
                "-fx-background-color: #EEF2F7; -fx-text-fill: #1F2328; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-border-color: #B3BEC8; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;"));
        izvestajPojedinacniBtn.setOnMouseExited(e -> izvestajPojedinacniBtn.setStyle(
                "-fx-background-color: white; -fx-text-fill: #1F2328; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;"));
        izvestajPojedinacniBtn.setOnAction(e -> generateIzvestaj("zbirni"));

        Button potvrdeBtn = new Button("Potvrde obračuna");
        potvrdeBtn.setStyle(
                "-fx-background-color: white; -fx-text-fill: #1F2328; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;");
        potvrdeBtn.setOnMouseEntered(e -> potvrdeBtn.setStyle(
                "-fx-background-color: #EEF2F7; -fx-text-fill: #1F2328; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-border-color: #B3BEC8; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;"));
        potvrdeBtn.setOnMouseExited(e -> potvrdeBtn.setStyle(
                "-fx-background-color: white; -fx-text-fill: #1F2328; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;"));
        potvrdeBtn.setOnAction(e -> generatePotvrde());

        Button xmlBtn = new Button("Export XML");
        xmlBtn.setStyle(
                "-fx-background-color: #9A6700; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand;");
        xmlBtn.setOnMouseEntered(e -> xmlBtn.setStyle(
                "-fx-background-color: #7C5300; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand;"));
        xmlBtn.setOnMouseExited(e -> xmlBtn.setStyle(
                "-fx-background-color: #9A6700; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand;"));
        xmlBtn.setOnAction(e -> exportXml());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button dodajPrimaocaBtn = new Button("+ Dodaj primaoca");
        dodajPrimaocaBtn.setStyle(
                "-fx-background-color: white; -fx-text-fill: #0969DA; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-border-color: #0969DA; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;");
        dodajPrimaocaBtn.setOnMouseEntered(e -> dodajPrimaocaBtn.setStyle(
                "-fx-background-color: #DDF4FF; -fx-text-fill: #0969DA; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-border-color: #0969DA; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;"));
        dodajPrimaocaBtn.setOnMouseExited(e -> dodajPrimaocaBtn.setStyle(
                "-fx-background-color: white; -fx-text-fill: #0969DA; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-border-color: #0969DA; -fx-border-width: 1; -fx-border-radius: 6; -fx-cursor: hand;"));
        dodajPrimaocaBtn.setOnAction(e -> openDodajPrimaoca());

        Button newObracunBtn = new Button("Novi obračun");
        newObracunBtn.setStyle(
                "-fx-background-color: #0969DA; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand;");
        newObracunBtn.setOnMouseEntered(e -> newObracunBtn.setStyle(
                "-fx-background-color: #0550AE; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand;"));
        newObracunBtn.setOnMouseExited(e -> newObracunBtn.setStyle(
                "-fx-background-color: #0969DA; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand;"));
        newObracunBtn.setOnAction(e -> openNoviObracun());

        Button closeBtn = new Button("Zatvori");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color: #FFEBE9; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6;"));
        closeBtn.setOnAction(e -> AppLayout.showWelcome());

        buttonsBox.getChildren().addAll(
                autoObradaBtn, izvestajPojedinacniBtn, potvrdeBtn, xmlBtn, spacer, dodajPrimaocaBtn, newObracunBtn, closeBtn);

        footer.getChildren().addAll(sumeBox, new Separator(), buttonsBox);
        return footer;
    }

    private void loadObracunDefinicije() {
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            Connection conn = dbConn.getMainConnection();
            if (conn != null) {
                ObracunDefinicijaDao dao = new ObracunDefinicijaDao();
                List<ObracunDefinicija> definicije = dao.findAll(conn);

                definicijeData = FXCollections.observableArrayList(definicije);
                filteredDefinicije = new FilteredList<>(definicijeData, p -> true);
                obracunListView.setItems(filteredDefinicije);

                // Učitaj mapu obracunId -> set svp3
                refreshObracunSvp3Map(conn);

                // Učitaj vrste prihoda iz DB za filter
                loadVrstePrihoda(conn);

                // Primeni inicijalni filter
                applyFilterVrstaIsplate();
            }
        } catch (Exception e) {
            showError("Greška", "Ne mogu učitati definicije obračuna: " + e.getMessage());
        }
    }

    private void loadVrstePrihoda(Connection conn) {
        try {
            VrstaPrihodaDao vpDao = new VrstaPrihodaDao();
            List<VrstaPrihoda> vrstePrihoda = vpDao.findAll(conn);
            svePrihodi = new ArrayList<>(vrstePrihoda);

            vrstaIsplateCombo.getItems().clear();
            vrstaIsplateCombo.getItems().add("Sve vrste");
            vrstaIsplateCombo.getItems().addAll(vrstePrihoda);
            vrstaIsplateCombo.setValue("Sve vrste");
        } catch (Exception e) {
            // Fallback: statičke vrijednosti
            vrstaIsplateCombo.getItems().setAll("Sve vrste");
            vrstaIsplateCombo.setValue("Sve vrste");
        }
        try {
            svePrimaociTypes = new VrstaPrimaocaDao().findAll(conn);
        } catch (Exception e) {
            // nema blokade
        }
    }

    private void refreshObracunSvp3Map() {
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            if (dbConn != null) {
                try (Connection conn = dbConn.getMainConnection()) {
                    refreshObracunSvp3Map(conn);
                }
            }
        } catch (Exception e) {
            System.err.println("Greška pri osvežavanju mape vrsta isplate: " + e.getMessage());
        }
    }

    private void refreshObracunSvp3Map(Connection conn) {
        try {
            ObracunDao dao = new ObracunDao();
            obracunSvp3Map = dao.findAllObracunDefSvp3Map(conn);
        } catch (Exception e) {
            System.err.println("Greška pri učitavanju mape vrsta isplate: " + e.getMessage());
            if (obracunSvp3Map == null) {
                obracunSvp3Map = new HashMap<>();
            }
        }
    }

    private String getCurrentFilterVpz() {
        if (vrstaIsplateCombo == null)
            return null;
        Object val = vrstaIsplateCombo.getValue();
        if (val instanceof VrstaPrihoda vp) {
            return vp.getVpz() != null ? vp.getVpz().trim() : null;
        }
        return null;
    }

    private void applyFilterVrstaIsplate() {
        if (filteredDefinicije == null)
            return;

        String filterVpz = getCurrentFilterVpz();
        refreshObracunSvp3Map();

        ObracunDefinicija previouslySelected = getSelectedDefinicija();

        if (filterVpz == null || filterVpz.isEmpty()) {
            filteredDefinicije.setPredicate(p -> true);
        } else {
            final String targetVpz = filterVpz;
            filteredDefinicije.setPredicate(def -> {
                if (def == null || def.getIdObracun() == null)
                    return false;
                Set<String> svpSet = obracunSvp3Map.get(def.getIdObracun());
                if (svpSet != null && svpSet.contains(targetVpz)) {
                    return true;
                }
                if (def.getOvp() != null && String.valueOf(def.getOvp()).equals(targetVpz)) {
                    return true;
                }
                return false;
            });
        }

        // Održi selekciju ili selektuj prvi dostupni obračun
        if (previouslySelected != null && filteredDefinicije.contains(previouslySelected)) {
            obracunListView.getSelectionModel().select(previouslySelected);
            loadObracuniForDefinicija(previouslySelected);
        } else if (!filteredDefinicije.isEmpty()) {
            obracunListView.getSelectionModel().select(0);
        } else {
            obracunListView.getSelectionModel().clearSelection();
            if (obracuniData != null) {
                obracuniData.clear();
            }
            if (filteredObracuni != null) {
                tableView.setItems(filteredObracuni);
            }
            updateSume();
        }

        applyTableFilter(filterVpz);
    }

    private void applyTableFilter(String filterVpz) {
        if (filteredObracuni == null)
            return;
        if (filterVpz == null || filterVpz.isEmpty()) {
            filteredObracuni.setPredicate(p -> true);
        } else {
            filteredObracuni
                    .setPredicate(o -> o != null && filterVpz.equals(o.getSvp3() != null ? o.getSvp3().trim() : null));
        }
        updateSume();
    }

    private void loadObracuniForDefinicija(ObracunDefinicija selected) {
        if (selected == null)
            return;

        obracuniData = FXCollections.observableArrayList();

        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            Connection conn = dbConn.getMainConnection();
            if (conn != null) {
                ObracunDao dao = new ObracunDao();
                List<Obracun> obracuni = dao.findByLnkObracun(conn, selected.getIdObracun());
                obracuniData.addAll(obracuni);
            }
        } catch (Exception e) {
            showError("Greška", "Ne mogu učitati stavke obračuna: " + e.getMessage());
        }

        filteredObracuni = new FilteredList<>(obracuniData);
        applyTableFilter(getCurrentFilterVpz());
        tableView.setItems(filteredObracuni);
        updateSume();
    }

    /** Vraća trenutno selektovanu definiciju obračuna iz ListView. */
    private ObracunDefinicija getSelectedDefinicija() {
        return obracunListView.getSelectionModel().getSelectedItem();
    }

    private void updateSume() {
        var items = (filteredObracuni != null) ? filteredObracuni : obracuniData;
        int count = items != null ? items.size() : 0;
        BigDecimal sumaNeto = BigDecimal.ZERO;
        BigDecimal sumaBruto = BigDecimal.ZERO;

        if (items != null) {
            for (Obracun o : items) {
                if (o.getNeto() != null)
                    sumaNeto = sumaNeto.add(o.getNeto());
                if (o.getBruto() != null)
                    sumaBruto = sumaBruto.add(o.getBruto());
            }
        }

        countLabel.setText(count + " stavki");
        sumaNetoLabel.setText("NETO: " + NumberFormats.money(sumaNeto) + " RSD");
        sumaBrutoLabel.setText("BRUTO: " + NumberFormats.money(sumaBruto) + " RSD");
    }

    /**
     * Vrsta prihoda (SVP-3) po defaultu: filter u obračunu, a ako nije izabran OVP
     * definicije.
     */
    private String defaultSvp3() {
        String filter = getCurrentFilterVpz();
        if (filter != null)
            return filter;
        ObracunDefinicija def = getSelectedDefinicija();
        return def != null && def.getOvp() != null ? String.valueOf(def.getOvp()) : null;
    }

    /**
     * Računa bruto i ostale vrednosti iz NETO prema stopama za vrstu prihoda
     * (SVP-3).
     * 
     * @return rezultat obračuna ili null ako nema NETO, SVP-3 ili stopa za tu vrstu
     *         prihoda
     */
    private Obracun izracunajStavku(BigDecimal neto, String svp2, String svp3, BigDecimal procenat) {
        if (neto == null || svp3 == null)
            return null;
        try {
            Connection conn = DatabaseConnection.getInstance().getMainConnection();
            ObracunStopeDao stopeDao = new ObracunStopeDao();
            ObracunDefinicija def = getSelectedDefinicija();
            String tip = svp3.trim();
            Optional<ObracunStope> stope = Optional.empty();
            if (def != null && def.getLnkPromenaStope() != null) {
                stope = stopeDao.findByTipAndPromenaStope(conn, tip, def.getLnkPromenaStope());
            }
            if (stope.isEmpty()) {
                stope = stopeDao.findLatestByTip(conn, tip);
            }
            if (stope.isEmpty())
                return null;
            return new ObracunService().izracunaj(neto, stope.get(),
                    procenat != null ? procenat : BigDecimal.ONE, svp2, tip);
        } catch (Exception e) {
            System.err.println("Greška pri obračunu: " + e.getMessage());
            return null;
        }
    }

    /** Preračunava stavku (iz NETO, SVP-2 i SVP-3) i upisuje rezultat u nju. */
    private boolean primeniObracun(Obracun o) {
        Obracun c = izracunajStavku(o.getNeto(), o.getSvp2(), o.getSvp3(), o.getProcenat());
        if (c == null)
            return false;
        o.setBruto(c.getBruto());
        o.setNormTroskovi(c.getNormTroskovi());
        o.setOsnovicaPorez(c.getOsnovicaPorez());
        o.setOsnovicaDoprinosi(c.getOsnovicaDoprinosi());
        o.setPorez(c.getPorez());
        o.setPoreskoOslobodjenje(c.getPoreskoOslobodjenje());
        o.setPio(c.getPio());
        o.setZdr(c.getZdr());
        o.setNez(c.getNez());
        o.setpPio(c.getpPio());
        o.setpZdr(c.getpZdr());
        o.setpNez(c.getpNez());
        o.setzPio(c.getzPio());
        o.setzZdr(c.getzZdr());
        o.setzNez(c.getzNez());
        o.setObracunato(c.getObracunato());
        return true;
    }

    private void runAutomatskaObrada() {
        if (obracuniData == null || obracuniData.isEmpty()) {
            showInfo("Info", "Nema stavki za obradu.");
            return;
        }

        // Potvrda
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Automatska obrada");
        confirm.setHeaderText("Pokrenuti automatsku obradu?");
        confirm.setContentText("Ova akcija će izračunati poreze i doprinose za " +
                obracuniData.size() + " stavki.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int processed = 0;
            for (Obracun o : obracuniData) {
                if (o.getNeto() != null && o.getNeto().compareTo(BigDecimal.ZERO) > 0) {
                    if (o.getSvp3() == null)
                        o.setSvp3(defaultSvp3());
                    if (primeniObracun(o)) {
                        try (Connection conn = DatabaseConnection.getInstance().getMainConnection()) {
                            new ObracunDao().update(conn, o);
                            processed++;
                        } catch (Exception ex) {
                            showError("Greška", "Snimanje stavke nije uspelo: " + ex.getMessage());
                        }
                    }
                }
            }
            tableView.refresh();
            updateSume();
            showInfo("Automatska obrada", "Obrađeno " + processed + " stavki.");
        }
    }

    /** @return true ako je stavka snimljena u bazu */
    private boolean editObracunStavka(Obracun obracun) {
        Dialog<Obracun> dialog = new Dialog<>();
        dialog.setTitle("Pojedinačna obrada");

        ButtonType saveButtonType = new ButtonType("Snimi", ButtonBar.ButtonData.OK_DONE);
        ButtonType potvrdaButtonType = new ButtonType("Potvrda", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(potvrdaButtonType, saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setMinWidth(460);

        String fieldStyle = "-fx-background-color: white; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;";

        // Primalac — label + dugme za promenu
        Label primalacLabel = new Label(obracun.getPrezime() + " " + obracun.getIme());
        primalacLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Button promeniPrimaocaBtn = new Button("Promeni...");
        promeniPrimaocaBtn.setStyle(
                "-fx-background-color: #F6F8FA; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 5; -fx-cursor: hand;");
        HBox primalacBox = new HBox(8, primalacLabel, promeniPrimaocaBtn);
        primalacBox.setAlignment(Pos.CENTER_LEFT);

        // NETO
        TextField netoField = new TextField();
        netoField.setText(obracun.getNeto() != null ? obracun.getNeto().toPlainString().replace('.', ',') : "0");
        netoField.setStyle(fieldStyle);
        netoField.setPrefWidth(180);

        // BRUTO (read-only, calculated)
        Label brutoLabel = new Label(obracun.getBruto() != null ? NumberFormats.money(obracun.getBruto()) : "0,00");
        brutoLabel.setStyle(
                "-fx-background-color: #D4EFD9; -fx-padding: 5 10; -fx-border-color: #A8D5B5; -fx-border-width: 1; -fx-border-radius: 4; -fx-text-fill: #1A7F37; -fx-font-weight: bold; -fx-min-width: 180;");

        // Vrsta primaoca ComboBox
        ComboBox<VrstaPrimaoca> vrstaPrimaocaCombo = new ComboBox<>();
        vrstaPrimaocaCombo.getItems().addAll(svePrimaociTypes);
        vrstaPrimaocaCombo.setPrefWidth(280);
        vrstaPrimaocaCombo.setStyle("-fx-font-size: 13px;");
        vrstaPrimaocaCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(VrstaPrimaoca item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        vrstaPrimaocaCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(VrstaPrimaoca item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        // Selektuj tekuću vrednost po svp2
        if (obracun.getSvp2() != null) {
            for (VrstaPrimaoca vp : svePrimaociTypes) {
                if (String.format("%02d", vp.getId()).equals(obracun.getSvp2())) {
                    vrstaPrimaocaCombo.setValue(vp);
                    break;
                }
            }
        }

        // Vrsta prihoda (isplate) ComboBox
        ComboBox<VrstaPrihoda> vrstaPrihodaCombo = new ComboBox<>();
        vrstaPrihodaCombo.getItems().addAll(svePrihodi);
        vrstaPrihodaCombo.setPrefWidth(280);
        vrstaPrihodaCombo.setStyle("-fx-font-size: 13px;");
        vrstaPrihodaCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(VrstaPrihoda item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getVpz() + " - " + item.getNaziv());
            }
        });
        vrstaPrihodaCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(VrstaPrihoda item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getVpz() + " - " + item.getNaziv());
            }
        });
        // Selektuj tekuću vrednost po svp3; ako nije postavljena, vrsta prihoda
        // izabrana u obračunu
        String svp3Default = obracun.getSvp3() != null ? obracun.getSvp3().trim() : defaultSvp3();
        if (svp3Default != null) {
            for (VrstaPrihoda vp : svePrihodi) {
                if (svp3Default.equals(vp.getVpz() != null ? vp.getVpz().trim() : null)) {
                    vrstaPrihodaCombo.setValue(vp);
                    break;
                }
            }
        }
        ComboBoxHelper.makeSearchable(vrstaPrimaocaCombo, VrstaPrimaoca::getDisplayName);
        ComboBoxHelper.makeSearchable(vrstaPrihodaCombo, VrstaPrihoda::getDisplayName);

        // SVP (samo za prikaz): spoj SVP-1..SVP-5, isto kao što se upisuje u bazu pri snimanju
        Label svpLabel = new Label();
        svpLabel.setStyle(
                "-fx-background-color: #D4EFD9; -fx-padding: 5 10; -fx-border-color: #A8D5B5; -fx-border-width: 1; -fx-border-radius: 4; -fx-text-fill: #1A7F37; -fx-font-weight: bold; -fx-min-width: 180;");
        Runnable refreshSvp = () -> {
            String s2 = vrstaPrimaocaCombo.getValue() != null
                    ? String.format("%02d", vrstaPrimaocaCombo.getValue().getId())
                    : obracun.getSvp2();
            String s3 = vrstaPrihodaCombo.getValue() != null ? vrstaPrihodaCombo.getValue().getVpz()
                    : obracun.getSvp3();
            String svp = Obracun.composeSvp(obracun.getSvp1(), s2, s3, obracun.getSvp4(), obracun.getSvp5());
            svpLabel.setText(svp != null ? svp : "—");
        };
        refreshSvp.run();
        netoField.textProperty().addListener((obs, o, n) -> refreshSvp.run());
        vrstaPrimaocaCombo.valueProperty().addListener((obs, o, n) -> refreshSvp.run());
        vrstaPrihodaCombo.valueProperty().addListener((obs, o, n) -> refreshSvp.run());

        TextField napomenaField = new TextField();
        napomenaField.setPromptText("Napomena za ovaj obračun...");
        napomenaField.setStyle(fieldStyle);

        // Layout
        grid.add(new Label("Primalac:"), 0, 0);
        grid.add(primalacBox, 1, 0);
        grid.add(new Label("NETO iznos:"), 0, 1);
        grid.add(netoField, 1, 1);
        grid.add(new Label("BRUTO iznos:"), 0, 2);
        grid.add(brutoLabel, 1, 2);
        grid.add(new Label("Vrsta primaoca:"), 0, 3);
        grid.add(vrstaPrimaocaCombo, 1, 3);
        grid.add(new Label("Vrsta isplate:"), 0, 4);
        grid.add(vrstaPrihodaCombo, 1, 4);
        grid.add(new Label("SVP:"), 0, 5);
        grid.add(svpLabel, 1, 5);
        grid.add(new Label("Napomena:"), 0, 6);
        grid.add(napomenaField, 1, 6);

        // Stil za labele
        for (int i = 0; i <= 6; i++) {
            Node lbl = getGridNode(grid, 0, i);
            if (lbl instanceof Label l) {
                l.setStyle("-fx-font-weight: bold; -fx-text-fill: #57606A;");
                l.setMinWidth(120);
            }
        }

        // Recalculate bruto helper
        Runnable recalc = () -> {
            try {
                BigDecimal neto = new BigDecimal(netoField.getText().replace(",", "."));
                Obracun calc = izracunajStavku(neto,
                        vrstaPrimaocaCombo.getValue() != null
                                ? String.format("%02d", vrstaPrimaocaCombo.getValue().getId())
                                : obracun.getSvp2(),
                        vrstaPrihodaCombo.getValue() != null ? vrstaPrihodaCombo.getValue().getVpz() : null,
                        obracun.getProcenat());
                brutoLabel.setText(calc != null ? NumberFormats.money(calc.getBruto()) : "—");
            } catch (Exception ex) {
                brutoLabel.setText("0,00");
            }
        };
        recalc.run();

        netoField.textProperty().addListener((obs, o, n) -> recalc.run());
        vrstaPrimaocaCombo.valueProperty().addListener((obs, o, n) -> recalc.run());
        vrstaPrihodaCombo.valueProperty().addListener((obs, o, n) -> recalc.run());

        // Promeni primaoca
        promeniPrimaocaBtn.setOnAction(e -> {
            Primalac izabrani = showPrimaocPicker();
            if (izabrani != null) {
                obracun.setLnkZap(izabrani.getIdZaposleni());
                obracun.setPrezime(izabrani.getPrezime());
                obracun.setIme(izabrani.getIme());
                obracun.setIdentifikatorPrimaoca(izabrani.getMb());
                obracun.setOznakaPrebivalista(izabrani.getOznakaPrebivalista());
                primalacLabel.setText(izabrani.getPrezime() + " " + izabrani.getIme());
                dialog.setHeaderText(null);
                // Auto-set vrsta primaoca if available
                if (izabrani.getVrstaPrimaocaId() != null) {
                    String targetSvp2 = String.format("%02d", izabrani.getVrstaPrimaocaId());
                    for (VrstaPrimaoca vp : svePrimaociTypes) {
                        if (String.format("%02d", vp.getId()).equals(targetSvp2)) {
                            vrstaPrimaocaCombo.setValue(vp);
                            break;
                        }
                    }
                }
            }
        });

        dialog.getDialogPane().setContent(grid);
        dialog.setOnShown(e -> javafx.application.Platform.runLater(netoField::requestFocus));

        // Potvrda: PDF za ovu stavku iz snimljenih podataka; dijalog ostaje otvoren.
        // Dok ima neunetih izmena (ili stavka nije snimljena) taster je onemogućen.
        Node potvrdaBtn = dialog.getDialogPane().lookupButton(potvrdaButtonType);
        potvrdaBtn.setDisable(obracun.getIdPobracun() == null);
        Runnable onChange = () -> potvrdaBtn.setDisable(true);
        netoField.textProperty().addListener((obs, o, n) -> onChange.run());
        vrstaPrimaocaCombo.valueProperty().addListener((obs, o, n) -> onChange.run());
        vrstaPrihodaCombo.valueProperty().addListener((obs, o, n) -> onChange.run());
        potvrdaBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            ev.consume();
            prikaziPotvrde(List.of(obracun));
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (vrstaPrimaocaCombo.getValue() != null) {
                    obracun.setSvp2(String.format("%02d", vrstaPrimaocaCombo.getValue().getId()));
                }
                if (vrstaPrihodaCombo.getValue() != null) {
                    obracun.setSvp3(vrstaPrihodaCombo.getValue().getVpz());
                }
                try {
                    BigDecimal neto = new BigDecimal(netoField.getText().replace(",", "."));
                    obracun.setNeto(neto);
                    if (!primeniObracun(obracun)) {
                        obracun.setBruto(BigDecimal.ZERO);
                    }
                } catch (NumberFormatException e) {
                    // ignoriši nevalidne unose
                }
                return obracun;
            }
            return null;
        });

        Optional<Obracun> res = dialog.showAndWait();
        if (res.isEmpty())
            return false;

        try (Connection conn = DatabaseConnection.getInstance().getMainConnection()) {
            ObracunDao dao = new ObracunDao();
            if (obracun.getIdPobracun() == null) {
                dao.save(conn, obracun);
            }
            dao.update(conn, obracun);
            if (!conn.getAutoCommit()) conn.commit();
        } catch (Exception ex) {
            showError("Greška", "Snimanje stavke nije uspelo: " + ex.getMessage());
            return false;
        }
        tableView.refresh();
        applyFilterVrstaIsplate();
        updateSume();
        return true;
    }

    /** Otvara podatke primaoca stavke u modalnom prozoru (isti ekran kao u listi primalaca). */
    private void openPodaciPrimaoca(Obracun stavka) {
        if (stavka == null || stavka.getLnkZap() == null) {
            showInfo("Info", "Stavka nema povezanog primaoca.");
            return;
        }
        Primalac primalac;
        try {
            Connection conn = DatabaseConnection.getInstance().getMainConnection();
            primalac = new PrimalacDao().findById(conn, stavka.getLnkZap()).orElse(null);
        } catch (Exception e) {
            showError("Greška", "Ne mogu učitati primaoca: " + e.getMessage());
            return;
        }
        if (primalac == null) {
            showInfo("Info", "Primalac nije pronađen u bazi.");
            return;
        }

        showPrimalacStage(new PrimalacDetaljiScreen(primalac), "Podaci primaoca",
                HbsZaradeApp.getPrimaryStage());
        tableView.refresh();
    }

    /** Prikazuje ekran primaoca u modalnom prozoru; "Zatvori" i X zatvaraju samo taj prozor. */
    private void showPrimalacStage(PrimalacDetaljiScreen screen, String title, javafx.stage.Window owner) {
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.initOwner(owner);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
        stage.setTitle(title);
        javafx.scene.Scene scene = new javafx.scene.Scene(screen.getRoot(), 960, 680);
        scene.getStylesheets().add(HbsZaradeApp.class.getResource("/styles/main.css").toExternalForm());
        stage.setScene(scene);
        screen.setOnClose(stage::close);
        stage.setOnCloseRequest(ev -> {
            if (!screen.confirmLeave())
                ev.consume();
        });
        stage.showAndWait();
    }

    /**
     * Red sa poljem za pretragu i tasterom "+" za unos novog primaoca. Ukucani tekst se
     * tumači kao "Prezime Ime" i popunjava formu novog primaoca; po snimanju se novi
     * primalac dodaje u listu i selektuje.
     */
    private HBox createSearchRow(TextField searchField, ObservableList<Primalac> svePrimaoci,
            ListView<Primalac> listView) {
        Button newBtn = new Button("+");
        newBtn.setStyle("-fx-background-color: #0969DA; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand;");
        newBtn.setTooltip(new Tooltip("Unesi novog primaoca"));
        newBtn.setFocusTraversable(true);
        newBtn.setOnAction(e -> {
            String text = searchField.getText() == null ? "" : searchField.getText().trim();
            String[] parts = text.split("\\s+", 2);
            String prezime = parts.length > 0 ? parts[0] : "";
            String ime = parts.length > 1 ? parts[1] : "";
            PrimalacDetaljiScreen screen = new PrimalacDetaljiScreen(prezime, ime);
            showPrimalacStage(screen, "Novi primalac prihoda", searchField.getScene().getWindow());
            Primalac novi = screen.getSavedPrimalac();
            if (novi != null) {
                svePrimaoci.add(novi);
                searchField.clear();
                listView.getSelectionModel().select(novi);
                listView.scrollTo(novi);
                listView.requestFocus();
            }
        });
        HBox row = new HBox(6, searchField, newBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        return row;
    }

    /** Pomoćna metoda za dohvat čvora iz GridPane po koloni i redu. */
    private Node getGridNode(GridPane grid, int col, int row) {
        for (Node node : grid.getChildren()) {
            Integer c = GridPane.getColumnIndex(node);
            Integer r = GridPane.getRowIndex(node);
            if ((c == null ? 0 : c) == col && (r == null ? 0 : r) == row)
                return node;
        }
        return null;
    }

    /** Napomena primaoca za prikaz u listi izbora (prazno ako je nema). */
    private static String napomenaSuffix(Primalac p) {
        String n = p.getNapomena();
        return n == null || n.isBlank() ? "" : " — " + n.trim();
    }

    /** Otvori dijalog za izbor primaoca (isti kao u openDodajPrimaoca). */
    private Primalac showPrimaocPicker() {
        Dialog<Primalac> dialog = new Dialog<>();
        dialog.setTitle("Izbor primaoca");
        dialog.setHeaderText("Izaberite primaoca prihoda");

        ButtonType selectType = new ButtonType("Izaberi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectType, ButtonType.CANCEL);

        ListView<Primalac> listView = new ListView<>();
        listView.setPrefHeight(300);
        listView.setPrefWidth(420);

        ObservableList<Primalac> svePrimaoci = FXCollections.observableArrayList();
        try {
            Connection conn = DatabaseConnection.getInstance().getMainConnection();
            if (conn != null)
                svePrimaoci.addAll(new PrimalacDao().findAll(conn));
        } catch (Exception e) {
            /* ignoriši */ }

        FilteredList<Primalac> filteredPrimaoci = new FilteredList<>(svePrimaoci, p -> true);
        listView.setItems(filteredPrimaoci);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Primalac p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getPrezime() + " " + p.getIme() + " (" + p.getMb() + ")"
                        + napomenaSuffix(p));
            }
        });

        TextField searchField = new TextField();
        searchField.setPromptText("Pretraži...");
        searchField.textProperty().addListener((obs, o, n) -> {
            String lower = n == null ? "" : n.toLowerCase().trim();
            filteredPrimaoci.setPredicate(p -> {
                if (lower.isEmpty())
                    return true;
                return (p.getPrezime() != null && p.getPrezime().toLowerCase().contains(lower))
                        || (p.getIme() != null && p.getIme().toLowerCase().contains(lower))
                        || (p.getMb() != null && p.getMb().contains(n.trim()))
                        || (p.getNapomena() != null && p.getNapomena().toLowerCase().contains(lower));
            });
        });

        dialog.getDialogPane().setContent(new VBox(8, createSearchRow(searchField, svePrimaoci, listView), listView));
        dialog.setOnShown(e -> javafx.application.Platform.runLater(searchField::requestFocus));
        dialog.setResultConverter(btn -> btn == selectType ? listView.getSelectionModel().getSelectedItem() : null);
        return dialog.showAndWait().orElse(null);
    }

    private void openDodajPrimaoca() {
        ObracunDefinicija selectedDef = getSelectedDefinicija();
        if (selectedDef == null) {
            showInfo("Info", "Prvo izaberite obračun.");
            return;
        }

        // Dijalog za izbor primaoca
        Dialog<Primalac> dialog = new Dialog<>();
        dialog.setTitle("Dodaj primaoca u obračun");
        dialog.setHeaderText("Izaberite primaoca prihoda");

        ButtonType addButtonType = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Lista primalaca sa filterom
        ListView<Primalac> listView = new ListView<>();
        listView.setPrefHeight(300);
        listView.setPrefWidth(420);

        ObservableList<Primalac> svePrimaoci = FXCollections.observableArrayList();
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            Connection conn = dbConn.getMainConnection();
            if (conn != null) {
                PrimalacDao dao = new PrimalacDao();
                svePrimaoci.addAll(dao.findAll(conn));
            }
        } catch (Exception e) {
            showError("Greška", "Ne mogu učitati primaoce: " + e.getMessage());
        }
        FilteredList<Primalac> filteredPrimaoci = new FilteredList<>(svePrimaoci);
        listView.setItems(filteredPrimaoci);

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Primalac item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getPrezime() + " " + item.getIme() +
                            " (" + (item.getMb() != null ? item.getMb() : "-") + ")" + napomenaSuffix(item));
                }
            }
        });

        TextField searchField = new TextField();
        searchField.setPromptText("Pretraži po prezimenu, imenu, JMBG, napomeni...");
        searchField.setStyle("-fx-border-color: #D0D7DE; -fx-border-radius: 6; -fx-background-radius: 6;");
        searchField.textProperty().addListener((obs, o, n) -> {
            String lower = n == null ? "" : n.toLowerCase().trim();
            filteredPrimaoci.setPredicate(p -> {
                if (lower.isEmpty())
                    return true;
                return (p.getPrezime() != null && p.getPrezime().toLowerCase().contains(lower))
                        || (p.getIme() != null && p.getIme().toLowerCase().contains(lower))
                        || (p.getMb() != null && p.getMb().contains(n.trim()))
                        || (p.getNapomena() != null && p.getNapomena().toLowerCase().contains(lower));
            });
        });

        VBox content = new VBox(8, createSearchRow(searchField, svePrimaoci, listView), listView);
        dialog.getDialogPane().setContent(content);
        dialog.setOnShown(e -> javafx.application.Platform.runLater(searchField::requestFocus));

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        Optional<Primalac> result = dialog.showAndWait();
        if (result.isPresent()) {
            Primalac primalac = result.get();
            Obracun novaStavka = new Obracun();
            novaStavka.setLnkObracun(selectedDef.getIdObracun());
            novaStavka.setLnkZap(primalac.getIdZaposleni());
            novaStavka.setPrezime(primalac.getPrezime());
            novaStavka.setIme(primalac.getIme());
            novaStavka.setIdentifikatorPrimaoca(primalac.getMb());
            novaStavka.setOznakaPrebivalista(primalac.getOznakaPrebivalista());
            if (primalac.getVrstaIdentifikatoraPrimaoca() != null) {
                novaStavka.setVrstaIdentifikatoraPrimaoca(String.valueOf(primalac.getVrstaIdentifikatoraPrimaoca()));
            }
            novaStavka.setNeto(BigDecimal.ZERO);
            novaStavka.setBruto(BigDecimal.ZERO);
            if (primalac.getVrstaPrimaocaId() != null) {
                novaStavka.setSvp2(String.format("%02d", primalac.getVrstaPrimaocaId()));
            }
            String currentFilter = getCurrentFilterVpz();
            if (currentFilter != null) {
                novaStavka.setSvp3(currentFilter);
            }

            obracuniData.add(novaStavka);
            tableView.getSelectionModel().select(novaStavka);
            updateSume();
            // Odmah otvori editovanje za unos NETO iznosa
            if (!editObracunStavka(novaStavka)) {
                // Otkazano ili neuspelo snimanje: ne ostavljaj nesnimljenu stavku u tabeli
                obracuniData.remove(novaStavka);
                updateSume();
            }
        }
    }

    private void openObracunDetalji() {
        ObracunDefinicija selected = getSelectedDefinicija();
        if (selected == null) {
            showInfo("Info", "Izaberite obračun iz liste.");
            return;
        }
        ZaglavljeObracunaScreen zaglavljeScreen = new ZaglavljeObracunaScreen(selected, false);
        zaglavljeScreen.setOnSaveCallback(updated -> {
            // Osveži listu i vrati se na ObracuniScreen
            AppLayout.setContent(new ObracuniScreen().getRoot());
        });
        AppLayout.setContent(zaglavljeScreen.getRoot());
    }

    private void openNoviObracun() {
        ZaglavljeObracunaScreen zaglavljeScreen = new ZaglavljeObracunaScreen(null, true);
        zaglavljeScreen.setOnSaveCallback(created -> {
            // Vrati se na obračune i osveži
            AppLayout.setContent(new ObracuniScreen().getRoot());
        });
        AppLayout.setContent(zaglavljeScreen.getRoot());
    }

    private void generateIzvestaj(String tip) {
        ObracunDefinicija selectedDef = getSelectedDefinicija();
        if (selectedDef == null) {
            showInfo("Info", "Izaberite obračun iz liste.");
            return;
        }
        if (obracuniData == null || obracuniData.isEmpty()) {
            showInfo("Info", "Nema stavki za izveštaj.");
            return;
        }

        if ("jednotini".equals(tip) || "pojedinacni".equals(tip)) {
            Obracun selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("Info", "Duplim klikom ili selektujte stavku za pojedinačni izveštaj.");
                return;
            }
            try {
                java.io.File tempFile = java.io.File.createTempFile("izvestaj_", ".pdf");
                tempFile.deleteOnExit();
                PdfReporter.generatePojedinacan(tempFile, selected, selectedDef, loadPoslodavac());
                openPdfPreview(tempFile);
            } catch (Exception e) {
                showError("Greška", "Greška pri generisanju PDF: " + e.getMessage());
            }
        } else {
            try {
                java.io.File tempFile = java.io.File.createTempFile("zbirni_", ".pdf");
                tempFile.deleteOnExit();
                PdfReporter.generateZbirni(tempFile, selectedDef, obracuniData, loadPoslodavac());
                openPdfPreview(tempFile);
            } catch (Exception e) {
                showError("Greška", "Greška pri generisanju PDF: " + e.getMessage());
            }
        }
    }

    private void generatePotvrde() {
        ObracunDefinicija selectedDef = getSelectedDefinicija();
        if (selectedDef == null) {
            showInfo("Info", "Izaberite obračun iz liste.");
            return;
        }
        if (obracuniData == null || obracuniData.isEmpty()) {
            showInfo("Info", "Nema stavki za potvrde.");
            return;
        }
        prikaziPotvrde(obracuniData);
    }

    /** Generiše PDF potvrda (jedna strana po stavci) i odmah ga otvara u pregledu. */
    private void prikaziPotvrde(List<Obracun> stavke) {
        try {
            java.util.Map<String, String> nazivPoVpz = new java.util.HashMap<>();
            for (VrstaPrihoda vp : svePrihodi)
                nazivPoVpz.put(vp.getVpz(), vp.getNaziv());

            java.util.Map<Integer, Primalac> primaoci = new java.util.HashMap<>();
            Connection conn = DatabaseConnection.getInstance().getMainConnection();
            if (conn != null) {
                // Naslov iz IzvestajPrihodi (veza OVP3 = SVP-3) ima prednost nad nazivom iz šifarnika
                nazivPoVpz.putAll(new IzvestajPrihodiDao().findNaslovPoOvp3(conn));
                for (Primalac p : new PrimalacDao().findAll(conn))
                    primaoci.put(p.getIdZaposleni(), p);
            }

            java.io.File tempFile = java.io.File.createTempFile("potvrde_", ".pdf");
            tempFile.deleteOnExit();
            // Varijanta potvrde iz tabele settings (PotvrdeReport); prazno/nepoznato = Potvrda_001
            String varijanta = conn != null
                    ? new AppSettingsDao().getSetting(conn, AppSettingsDao.KEY_POTVRDE_REPORT)
                    : null;
            PdfReporter.generatePotvrdeObracuna(tempFile, stavke, nazivPoVpz, primaoci, loadPoslodavac(),
                    varijanta);
            openPdfPreview(tempFile);
        } catch (Exception e) {
            showError("Greška", "Greška pri generisanju PDF: " + e.getMessage());
        }
    }

    /** Odmah otvara PDF u podrazumevanom pregledaču (bez dijaloga za izbor akcije). */
    private void openPdfPreview(java.io.File pdf) {
        try {
            java.awt.Desktop.getDesktop().open(pdf);
        } catch (Exception e) {
            showError("Greška", "Ne mogu otvoriti pregled: " + e.getMessage());
        }
    }

    private void showDocPreview(java.io.File tempPdf, String defaultFileName, String settingsKey) {
        ButtonType previewType = new ButtonType("Otvori pregled");
        ButtonType printType = new ButtonType("Štampaj");
        ButtonType saveType = new ButtonType("Sačuvaj PDF...");
        ButtonType cancelType = new ButtonType("Otkaži", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert dialog = new Alert(Alert.AlertType.NONE);
        dialog.setTitle("Pregled dokumenta");
        dialog.setHeaderText("Dokument je spreman — izaberite akciju");
        dialog.getButtonTypes().setAll(previewType, printType, saveType, cancelType);

        dialog.showAndWait().ifPresent(btn -> {
            try {
                if (btn == previewType) {
                    java.awt.Desktop.getDesktop().open(tempPdf);
                } else if (btn == printType) {
                    java.awt.Desktop.getDesktop().print(tempPdf);
                } else if (btn == saveType) {
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Sačuvaj PDF");
                    java.io.File dir = AppSettingsDao.getExportDir(settingsKey);
                    if (dir != null)
                        fc.setInitialDirectory(dir);
                    fc.setInitialFileName(defaultFileName);
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF fajlovi", "*.pdf"));
                    java.io.File dest = fc.showSaveDialog(HbsZaradeApp.getPrimaryStage());
                    if (dest != null) {
                        java.nio.file.Files.copy(tempPdf.toPath(), dest.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        showPdfSaved("Sačuvano", "PDF je uspešno sačuvan.", dest);
                    }
                }
            } catch (Exception e) {
                showError("Greška", e.getMessage());
            }
        });
    }

    private Poslodavac loadPoslodavac() {
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            Connection conn = dbConn.getMainConnection();
            if (conn != null) {
                PoslodavacDao dao = new PoslodavacDao();
                List<Poslodavac> lista = dao.findAll(conn);
                if (!lista.isEmpty())
                    return lista.get(0);
            }
        } catch (Exception e) {
            // ne blokira operaciju
        }
        return null;
    }

    private void exportXml() {
        ObracunDefinicija selectedDef = getSelectedDefinicija();
        if (selectedDef == null) {
            showInfo("Info", "Izaberite obračun iz liste.");
            return;
        }
        if (obracuniData == null || obracuniData.isEmpty()) {
            showInfo("Info", "Nema stavki za XML export.");
            return;
        }

        Poslodavac poslodavac = loadPoslodavac();

        // Generiši XML preview
        String xmlPreview;
        try {
            xmlPreview = XmlExporter.generateXmlString(selectedDef,
                    obracuniData, poslodavac);
        } catch (Exception e) {
            showError("Greška", "Greška pri generisanju XML: " + e.getMessage());
            return;
        }

        // Ako je u Podešavanjima zadat folder za XML export, fajl se kreira automatski bez pregleda
        java.io.File xmlDir = AppSettingsDao.getExportDir(AppSettingsDao.KEY_XML_EXPORT_PATH);
        if (xmlDir != null) {
            java.io.File file = new java.io.File(xmlDir, XmlExporter.defaultFileName(selectedDef));
            try {
                XmlExporter.exportToFile(file, selectedDef, obracuniData, poslodavac);
            } catch (Exception e) {
                showError("Greška", "Greška pri čuvanju XML: " + e.getMessage());
                return;
            }
            showXmlCreated(file);
            return;
        }

        // Prikaži preview
        Alert preview = new Alert(Alert.AlertType.CONFIRMATION);
        preview.setTitle("XML Export — PPP-PO");
        preview.setHeaderText("Pregled XML za Poresku upravu");

        TextArea textArea = new TextArea(xmlPreview);
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setPrefWidth(650);
        textArea.setPrefHeight(400);
        textArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");

        preview.getDialogPane().setContent(textArea);
        preview.getDialogPane().getButtonTypes().setAll(
                new ButtonType("Sačuvaj XML", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL);

        Optional<ButtonType> result = preview.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sačuvaj XML");
            fileChooser.setInitialFileName(XmlExporter.defaultFileName(selectedDef));
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("XML fajlovi", "*.xml"));
            java.io.File file = fileChooser.showSaveDialog(HbsZaradeApp.getPrimaryStage());
            if (file != null) {
                try {
                    XmlExporter.exportToFile(file, selectedDef, obracuniData, poslodavac);
                    showInfo("XML Export", "XML je uspješno sačuvan:\n" + file.getAbsolutePath());
                } catch (Exception e) {
                    showError("Greška", "Greška pri čuvanju XML: " + e.getMessage());
                }
            }
        }
    }

    /** Poruka da je XML kreiran, sa opcijom da se otvori folder i selektuje fajl. */
    private void showXmlCreated(java.io.File file) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("XML Export");
        alert.setHeaderText("XML je kreiran");
        alert.setContentText(file.getAbsolutePath());
        ButtonType otvoriBtn = new ButtonType("Otvori folder");
        alert.getButtonTypes().setAll(otvoriBtn, ButtonType.OK);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == otvoriBtn) {
                try {
                    if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                        new ProcessBuilder("explorer.exe", "/select," + file.getAbsolutePath()).start();
                    } else {
                        java.awt.Desktop.getDesktop().open(file.getParentFile());
                    }
                } catch (Exception ex) {
                    showError("Greška", "Ne mogu otvoriti folder: " + ex.getMessage());
                }
            }
        });
    }

    private void deleteObracunDefinicija() {
        ObracunDefinicija selected = getSelectedDefinicija();
        if (selected == null) {
            showInfo("Info", "Izaberite obračun za brisanje.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Brisanje obračuna");
        confirm.setHeaderText("Brisanje: " + selected.getNazivObracuna());
        confirm.setContentText("Da li ste sigurni?\nBrišu se i sve stavke ovog obračuna!");
        ButtonType btnBrisi = new ButtonType("Obriši", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnOtkazi = new ButtonType("Otkaži", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnBrisi, btnOtkazi);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == btnBrisi) {
            try {
                Connection conn = DatabaseConnection.getInstance().getMainConnection();
                // Obrisi stavke obracuna (child records)
                try (var stmt = conn.prepareStatement(
                        "DELETE FROM Obracun WHERE lnkObracun = ?")) {
                    stmt.setInt(1, selected.getIdObracun());
                    stmt.executeUpdate();
                }
                // Obrisi definiciju
                new ObracunDefinicijaDao().delete(conn, selected.getIdObracun());
                // Osveži UI
                definicijeData.remove(selected);
                obracunSvp3Map.remove(selected.getIdObracun());
                obracuniData = FXCollections.observableArrayList();
                filteredObracuni = new FilteredList<>(obracuniData);
                tableView.setItems(filteredObracuni);
                updateSume();
            } catch (Exception e) {
                showError("Greška", "Brisanje nije uspelo: " + e.getMessage());
            }
        }
    }

    private void showPdfSaved(String title, String message, java.io.File file) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        ButtonType otvoriBtn = new ButtonType("Otvori PDF");
        alert.getButtonTypes().setAll(otvoriBtn, ButtonType.OK);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == otvoriBtn) {
                try {
                    java.awt.Desktop.getDesktop().open(file);
                } catch (Exception ex) {
                    showError("Greška", "Ne mogu otvoriti fajl: " + ex.getMessage());
                }
            }
        });
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

    public BorderPane getRoot() {
        return root;
    }
}
