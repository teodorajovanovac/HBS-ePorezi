package rs.hbs.zarade.ui;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;

import java.util.function.Function;

/**
 * Pomoćna klasa za JavaFX ComboBox kontrole koja omogućava brzo biranje
 * stavki kucanjem početnog slova ili prefiksa (type-ahead search).
 *
 * Podržava:
 * - Pritisak prvog slova: odmah selektuje prvu stavku koja počinje tim slovom
 * - Višestruki pritisak istog slova: kružno prolazi kroz sve stavke koje počinju tim slovom
 * - Brzo kucanje više slova (npr. "beo" -> "Beočin") sa resetom posle 1 sekunde neaktivnosti
 * - Naša slova sa dijakritikom i bez (č/ć/c, š/s, ž/z, đ/dj)
 * - Automatsko skrolovanje padajuće liste do selektovane stavke (i kad je otvorena)
 */
public class ComboBoxHelper {

    private static final long RESET_DELAY_MS = 1000;

    /**
     * Omogućava brzo selektovanje za ComboBox koristeći toString() metodu stavki.
     */
    public static <T> void makeSearchable(ComboBox<T> combo) {
        makeSearchable(combo, item -> item != null ? item.toString() : "");
    }

    /**
     * Omogućava brzo selektovanje za ComboBox koristeći prilagođeni ekstraktor teksta.
     *
     * @param combo ComboBox koji se unapređuje
     * @param textExtractor funkcija koja pretvara stavku u tekst koji korisnik traži (npr. Opstina::getNaziv)
     */
    public static <T> void makeSearchable(ComboBox<T> combo, Function<T, String> textExtractor) {
        if (combo == null) return;

        final StringBuilder buffer = new StringBuilder();
        final long[] lastKeyPressTime = {0};

        EventHandler<KeyEvent> keyHandler = event -> {
            if (event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
                return;
            }

            KeyCode code = event.getCode();
            if (code == KeyCode.UP || code == KeyCode.DOWN || code == KeyCode.LEFT || code == KeyCode.RIGHT ||
                code == KeyCode.ENTER || code == KeyCode.ESCAPE || code == KeyCode.TAB ||
                code == KeyCode.SHIFT || code == KeyCode.CONTROL || code == KeyCode.ALT) {
                return;
            }

            String ch = event.getText();
            if (ch == null || ch.isEmpty()) {
                return;
            }

            char c = ch.charAt(0);
            if (Character.isISOControl(c)) {
                return;
            }

            ObservableList<T> items = combo.getItems();
            if (items == null || items.isEmpty()) {
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastKeyPressTime[0] > RESET_DELAY_MS) {
                buffer.setLength(0);
            }
            lastKeyPressTime[0] = now;

            int currentIdx = combo.getSelectionModel().getSelectedIndex();

            // Ako je pritisnuto isto slovo ponovo dok je dužina bafera 1 (npr. 'b' pa 'b')
            boolean isSameSingleChar = buffer.length() == 1 &&
                    Character.toLowerCase(buffer.charAt(0)) == Character.toLowerCase(c);

            if (isSameSingleChar) {
                String prefix = String.valueOf(c).toLowerCase();
                int nextStart = (currentIdx >= 0) ? (currentIdx + 1) : 0;
                int matchIdx = findNextMatch(items, textExtractor, prefix, nextStart);
                if (matchIdx < 0 && nextStart > 0) {
                    matchIdx = findNextMatch(items, textExtractor, prefix, 0);
                }
                if (matchIdx >= 0) {
                    selectIndex(combo, matchIdx);
                    event.consume();
                }
                return;
            }

            buffer.append(c);
            String searchStr = buffer.toString().toLowerCase();

            int matchIdx = findNextMatch(items, textExtractor, searchStr, 0);

            // Ako nema poklapanja za ceo bafer, probaj samo za ovo jedno poslednje slovo
            if (matchIdx < 0 && buffer.length() > 1) {
                buffer.setLength(0);
                buffer.append(c);
                searchStr = buffer.toString().toLowerCase();
                matchIdx = findNextMatch(items, textExtractor, searchStr, 0);
            }

            if (matchIdx >= 0) {
                selectIndex(combo, matchIdx);
                event.consume();
            }
        };

        // Filter na nivou ComboBox-a (dok je zatvoren)
        combo.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);

        // Filter i skrolovanje na nivou popup ListView-a (kad se otvori)
        combo.setOnShowing(e -> Platform.runLater(() -> {
            scrollComboToSelected(combo);
            ListView<?> lv = findPopupListView(combo);
            if (lv != null) {
                lv.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
                lv.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
            }
        }));
    }

    private static <T> void selectIndex(ComboBox<T> combo, int index) {
        combo.getSelectionModel().select(index);
        T item = combo.getItems().get(index);
        combo.setValue(item);
        if (combo.isShowing()) {
            scrollComboToSelected(combo);
        }
    }

    private static <T> int findNextMatch(ObservableList<T> items, Function<T, String> textExtractor, String prefix, int startIndex) {
        String p = prefix.trim().toLowerCase();
        if (p.isEmpty()) return -1;

        // 1. Prvo traži tačno poklapanje početka reči
        for (int i = startIndex; i < items.size(); i++) {
            T item = items.get(i);
            if (item == null) continue;
            String text = textExtractor != null ? textExtractor.apply(item) : item.toString();
            if (text != null && text.trim().toLowerCase().startsWith(p)) {
                return i;
            }
        }

        // 2. Ako nema tačnog, probaj sa tolerancijom na naša slova (c <-> č, ć itd.)
        String pStripped = stripDiacritics(p);
        for (int i = startIndex; i < items.size(); i++) {
            T item = items.get(i);
            if (item == null) continue;
            String text = textExtractor != null ? textExtractor.apply(item) : item.toString();
            if (text != null && stripDiacritics(text.trim()).startsWith(pStripped)) {
                return i;
            }
        }

        return -1;
    }

    public static String stripDiacritics(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replace("č", "c")
                .replace("ć", "c")
                .replace("š", "s")
                .replace("ž", "z")
                .replace("đ", "dj");
    }

    public static <T> void scrollComboToSelected(ComboBox<T> combo) {
        if (combo == null) return;
        T sel = combo.getValue();
        if (sel == null) return;
        int idx = combo.getItems().indexOf(sel);
        if (idx < 0) return;
        ListView<?> lv = findPopupListView(combo);
        if (lv != null) {
            lv.scrollTo(idx);
        }
    }

    public static ListView<?> findPopupListView(ComboBox<?> combo) {
        if (combo == null) return null;
        try {
            if (combo.getSkin() instanceof ComboBoxListViewSkin<?> skin) {
                Node popupContent = skin.getPopupContent();
                if (popupContent instanceof ListView<?> lv) {
                    return lv;
                }
            }
        } catch (Throwable ignored) {}

        // Fallback preko Window list-view lookup
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w.getScene() != null && w.getScene().getRoot() != null) {
                Node node = w.getScene().getRoot().lookup(".list-view");
                if (node instanceof ListView<?> lv && lv.getItems().size() == combo.getItems().size()) {
                    return lv;
                }
            }
        }
        return null;
    }
}
