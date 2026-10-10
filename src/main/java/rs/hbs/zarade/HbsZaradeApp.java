package rs.hbs.zarade;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import rs.hbs.zarade.ui.AppLayout;
import rs.hbs.zarade.ui.LoginScreen;

/**
 * Glavna JavaFX aplikacija ePorezi.
 *
 * Ovo je ulazna tačka za GUI aplikaciju.
 */
public class HbsZaradeApp extends Application {

    private static Stage primaryStage;
    private static String currentUser = null;
    private static int userLevel = 0;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("ePorezi");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);

        // Ikona aplikacije — prvo iz resources/icon.png, zatim programmatska
        Image icon = loadIconFromResources();
        if (icon == null) icon = createAppIcon();
        if (icon != null) primaryStage.getIcons().add(icon);

        // Pokaži login ekran
        showLoginScreen();

        primaryStage.show();
    }

    /** Učitava ikonu iz src/main/resources/icon.png ako postoji. */
    private static Image loadIconFromResources() {
        try {
            var stream = HbsZaradeApp.class.getResourceAsStream("/icon.png");
            if (stream != null) return new Image(stream);
        } catch (Exception ignored) {}
        return null;
    }

    /** Kreira programmatsku ikonu aplikacije (grafikon sa plavom pozadinom). */
    private static Image createAppIcon() {
        try {
            Canvas canvas = new Canvas(64, 64);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            // Pozadina
            gc.setFill(Color.web("#0969DA"));
            gc.fillRoundRect(0, 0, 64, 64, 14, 14);

            // Bar chart stupci (beli)
            gc.setFill(Color.WHITE);
            gc.fillRoundRect(6,  38, 12, 20, 3, 3);
            gc.fillRoundRect(22, 22, 12, 36, 3, 3);
            gc.fillRoundRect(38, 10, 12, 48, 3, 3);

            // Linija ispod grafikona
            gc.setStroke(Color.web("#FFFFFF", 0.5));
            gc.setLineWidth(1.5);
            gc.strokeLine(4, 60, 60, 60);

            WritableImage img = new WritableImage(64, 64);
            canvas.snapshot(null, img);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    public static void showLoginScreen() {
        LoginScreen loginScreen = new LoginScreen();
        boolean wasMaximized = primaryStage.isMaximized();
        Scene scene = new Scene(loginScreen.getRoot(), 1200, 800);
        scene.getStylesheets().add(HbsZaradeApp.class.getResource("/styles/main.css").toExternalForm());
        primaryStage.setScene(scene);
        if (wasMaximized) Platform.runLater(() -> primaryStage.setMaximized(true));
    }

    /** Otvara glavni layout sa sidebarom (koristi se umjesto showMainMenu). */
    public static void showAppLayout() {
        AppLayout.reset(); // svježa instanca
        AppLayout layout = AppLayout.getInstance();
        boolean wasMaximized = primaryStage.isMaximized();
        Scene scene = new Scene(layout.getRoot(), 1200, 800);
        scene.getStylesheets().add(HbsZaradeApp.class.getResource("/styles/main.css").toExternalForm());
        primaryStage.setScene(scene);
        if (wasMaximized) Platform.runLater(() -> primaryStage.setMaximized(true));
    }

    /** Zamjenjuje content u centru AppLayout-a. */
    public static void navigateTo(Node content) {
        AppLayout.setContent(content);
    }

    /** @deprecated Koristiti showAppLayout() */
    @Deprecated
    public static void showMainMenu() {
        showAppLayout();
    }

    public static void setCurrentUser(String username, int level) {
        currentUser = username;
        userLevel = level;
    }

    public static String getCurrentUser() {
        return currentUser;
    }

    public static int getUserLevel() {
        return userLevel;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
