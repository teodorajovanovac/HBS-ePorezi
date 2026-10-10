package rs.hbs.zarade.ui;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * Glavni layout aplikacije sa collapsible sidebarom.
 * Sidebar ostaje vidljiv na svim ekranima (sem login-a).
 */
public class AppLayout {

    /**
     * Interfejs za ekrane koji štite nesačuvane izmene od gubitka pri navigaciji.
     * Ekran setuje root.setUserData(DirtyCheckable) da aktivira zaštitu.
     */
    @FunctionalInterface
    public interface DirtyCheckable {
        /** Vraća true ako je OK navigirati dalje (nema izmena ili korisnik potvrdio). */
        boolean confirmLeave();
    }

    private static AppLayout instance;

    private BorderPane root;
    private SidebarPanel sidebarPanel;

    private AppLayout() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #F6F8FA;");

        sidebarPanel = new SidebarPanel();
        root.setLeft(sidebarPanel.getRoot());

        // Prikaži welcome panel u centru pri pokretanju (direktno, ne kroz getInstance())
        root.setCenter(new WelcomeDashboard().getRoot());
    }

    public static AppLayout getInstance() {
        if (instance == null) {
            instance = new AppLayout();
        }
        return instance;
    }

    /** Resetuje instance pri svakom login-u */
    public static void reset() {
        instance = null;
    }

    public static void setContent(Node content) {
        AppLayout inst = getInstance();
        Node current = inst.root.getCenter();
        if (current != null && current.getUserData() instanceof DirtyCheckable dc) {
            if (!dc.confirmLeave()) return;
        }
        inst.root.setCenter(content);
    }

    public static void showWelcome() {
        setContent(new WelcomeDashboard().getRoot());
    }

    public BorderPane getRoot() {
        return root;
    }
}
