package rs.hbs.zarade.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import rs.hbs.zarade.db.PrimalacDao;
import rs.hbs.zarade.domain.Opstina;
import rs.hbs.zarade.domain.Primalac;

import javafx.application.Platform;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lista primalaca prihoda - Screen 4 iz UI specifikacije.
 */
public class PrimaociListaScreen {

    // Stanje koje se čuva između navigacija
    private static String savedFilter = "";
    private static Integer savedSelectedId = null;

    private BorderPane root;
    private TableView<Primalac> tableView;
    private ObservableList<Primalac> primaociData;
    private FilteredList<Primalac> filteredData;
    private TextField filterField;
    private Map<String, String> opstineMap = new HashMap<>(); // sifra → naziv

    public PrimaociListaScreen() {
        createUI();
        loadData();
    }

    private void createUI() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #F6F8FA;");

        VBox header = createHeader();
        root.setTop(header);

        tableView = createTable();
        root.setCenter(tableView);

        HBox footer = createFooter();
        root.setBottom(footer);
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: transparent transparent #D0D7DE transparent; -fx-border-width: 0 0 1 0;");
        header.setPadding(new Insets(14, 16, 14, 16));

        Label titleLabel = new Label("Primaoci prihoda");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web("#1F2328"));

        HBox filterBox = new HBox(12);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("Pretraga:");
        filterLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        filterLabel.setTextFill(Color.web("#57606A"));

        filterField = new TextField();
        filterField.setPrefWidth(300);
        filterField.setPromptText("Ime, prezime ili email...");
        filterField.setStyle("-fx-background-color: white; -fx-font-size: 13px; " +
                "-fx-border-color: #D0D7DE; -fx-border-radius: 6; -fx-border-width: 1; -fx-background-radius: 6; -fx-padding: 6 10;");

        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredData != null) {
                filteredData.setPredicate(primalac -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    String f = newVal.toLowerCase();
                    if (primalac.getPrezime() != null && primalac.getPrezime().toLowerCase().contains(f)) return true;
                    if (primalac.getIme() != null && primalac.getIme().toLowerCase().contains(f)) return true;
                    if (primalac.getEmail() != null && primalac.getEmail().toLowerCase().contains(f)) return true;
                    return false;
                });
            }
        });

        Button clearHeaderBtn = new Button("Ukloni filter");
        clearHeaderBtn.setStyle("-fx-background-color: #EEF2F7; -fx-text-fill: #57606A; -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 4; -fx-border-color: #D0D7DE; -fx-border-width: 1; -fx-border-radius: 4; -fx-cursor: hand;");
        clearHeaderBtn.setDisable(true);
        clearHeaderBtn.setOnAction(e -> filterField.clear());
        filterField.textProperty().addListener((obs, o, n) -> clearHeaderBtn.setDisable(n == null || n.isEmpty()));

        filterBox.getChildren().addAll(filterLabel, filterField, clearHeaderBtn);
        header.getChildren().addAll(titleLabel, filterBox);
        return header;
    }

    private TableView<Primalac> createTable() {
        TableView<Primalac> table = new TableView<>();
        table.setStyle("-fx-background-color: white; -fx-font-size: 14px;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Primalac, String> imeCol = new TableColumn<>("Ime");
        imeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getIme() != null ? data.getValue().getIme() : ""));
        imeCol.setMinWidth(100);

        TableColumn<Primalac, String> prezimeCol = new TableColumn<>("Prezime");
        prezimeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPrezime() != null ? data.getValue().getPrezime() : ""));
        prezimeCol.setMinWidth(120);

        TableColumn<Primalac, String> opstinaCol = new TableColumn<>("Opština");
        opstinaCol.setCellValueFactory(data -> {
            String sifra = data.getValue().getOznakaPrebivalista();
            if (sifra == null || sifra.isEmpty()) return new SimpleStringProperty("");
            String naziv = opstineMap.getOrDefault(sifra, sifra);
            return new SimpleStringProperty(naziv);
        });
        opstinaCol.setMinWidth(120);

        TableColumn<Primalac, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEmail() != null ? data.getValue().getEmail() : ""));
        emailCol.setMinWidth(150);

        TableColumn<Primalac, String> mobilniCol = new TableColumn<>("Mobilni");
        mobilniCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getMobilni() != null ? data.getValue().getMobilni() : ""));
        mobilniCol.setMinWidth(90);

        table.getColumns().addAll(imeCol, prezimeCol, opstinaCol, emailCol, mobilniCol);

        // Dupli klik otvara detalje
        table.setRowFactory(tv -> {
            TableRow<Primalac> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openPrimalacDetalji(row.getItem());
                }
            });
            return row;
        });

        return table;
    }

    private HBox createFooter() {
        HBox footer = new HBox(12);
        footer.setStyle("-fx-background-color: #F6F8FA; -fx-border-color: #D0D7DE transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        footer.setPadding(new Insets(10, 20, 10, 20));
        footer.setAlignment(Pos.CENTER_LEFT);

        Button newBtn = new Button("Novi primalac");
        newBtn.setStyle("-fx-background-color: #0969DA; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;");
        newBtn.setOnMouseEntered(e -> newBtn.setStyle("-fx-background-color: #0550AE; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;"));
        newBtn.setOnMouseExited(e -> newBtn.setStyle("-fx-background-color: #0969DA; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;"));
        newBtn.setOnAction(e -> openPrimalacDetalji(null));

        Button closeBtn = new Button("Zatvori");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #FFEBE9; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CF222E; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 7 16; -fx-background-radius: 6; -fx-cursor: hand;"));
        closeBtn.setOnAction(e -> AppLayout.showWelcome());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footer.getChildren().addAll(spacer, newBtn, closeBtn);
        return footer;
    }

    private void loadData() {
        primaociData = FXCollections.observableArrayList();

        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            Connection conn = dbConn.getMainConnection();
            if (conn != null) {
                // Učitaj opstine za prikaz naziva
                OpstinaDao opsDao = new OpstinaDao();
                List<Opstina> opstine = opsDao.findAll(conn);
                for (Opstina o : opstine) {
                    if (o.getSifra() != null) opstineMap.put(o.getSifra(), o.getNaziv());
                }

                PrimalacDao dao = new PrimalacDao();
                List<Primalac> primaoci = dao.findAll(conn);
                primaociData.addAll(primaoci);
            }
        } catch (Exception e) {
            showError("Greška pri učitavanju podataka", e.getMessage());
        }

        filteredData = new FilteredList<>(primaociData, p -> true);
        tableView.setItems(filteredData);

        // Vrati filter
        if (!savedFilter.isEmpty()) {
            filterField.setText(savedFilter);
        }

        // Vrati selekciju + skroluj — koristimo sceneProperty listener jer loadData()
        // se poziva iz konstruktora, pre nego što je tabela dodata u scenu.
        if (savedSelectedId != null) {
            for (Primalac p : filteredData) {
                if (savedSelectedId.equals(p.getIdZaposleni())) {
                    tableView.getSelectionModel().select(p);
                    int idx = tableView.getSelectionModel().getSelectedIndex();
                    if (idx >= 0) {
                        tableView.sceneProperty().addListener(new javafx.beans.value.ChangeListener<>() {
                            @Override
                            public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.Scene> obs,
                                                javafx.scene.Scene old, javafx.scene.Scene n) {
                                if (n != null) {
                                    tableView.sceneProperty().removeListener(this);
                                    Platform.runLater(() -> tableView.scrollTo(idx));
                                }
                            }
                        });
                    }
                    break;
                }
            }
        }
    }

    private void openPrimalacDetalji(Primalac primalac) {
        // Sačuvaj stanje liste pre navigacije
        savedFilter = filterField.getText();
        savedSelectedId = primalac != null ? primalac.getIdZaposleni() : null;
        PrimalacDetaljiScreen detaljiScreen = new PrimalacDetaljiScreen(primalac);
        AppLayout.setContent(detaljiScreen.getRoot());
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
