package rs.hbs.zarade.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Test;
import rs.hbs.zarade.domain.Opstina;

import static org.junit.jupiter.api.Assertions.*;

public class ComboBoxHelperTest {

    @Test
    void testStripDiacritics() {
        assertEquals("cacak", ComboBoxHelper.stripDiacritics("Čačak"));
        assertEquals("sabac", ComboBoxHelper.stripDiacritics("Šabac"));
        assertEquals("zrenjanin", ComboBoxHelper.stripDiacritics("Zrenjanin"));
        assertEquals("djakovo", ComboBoxHelper.stripDiacritics("Đakovo"));
        assertEquals("beograd", ComboBoxHelper.stripDiacritics("Beograd"));
    }

    @Test
    void testOpstinaSearch() {
        ObservableList<Opstina> opstine = FXCollections.observableArrayList(
                new Opstina(1, "001", "Aleksandrovac"),
                new Opstina(2, "002", "Aleksinac"),
                new Opstina(3, "003", "Babušnica"),
                new Opstina(4, "004", "Bač"),
                new Opstina(5, "005", "Beograd"),
                new Opstina(6, "006", "Čačak")
        );

        // Pretraga po nazivu
        // 1. Slovo B
        Opstina b1 = opstine.stream()
                .filter(o -> o.getNaziv().toLowerCase().startsWith("b"))
                .findFirst().orElse(null);
        assertNotNull(b1);
        assertEquals("Babušnica", b1.getNaziv());

        // 2. Slovo C sa tolerancijom dijakritika -> Čačak
        Opstina c1 = opstine.stream()
                .filter(o -> ComboBoxHelper.stripDiacritics(o.getNaziv()).startsWith("c"))
                .findFirst().orElse(null);
        assertNotNull(c1);
        assertEquals("Čačak", c1.getNaziv());

        // 3. Pretraga po šifri
        Opstina s1 = opstine.stream()
                .filter(o -> o.getSifra().startsWith("005"))
                .findFirst().orElse(null);
        assertNotNull(s1);
        assertEquals("Beograd", s1.getNaziv());
    }
}
