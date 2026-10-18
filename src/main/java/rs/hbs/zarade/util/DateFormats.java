package rs.hbs.zarade.util;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

/**
 * Formatiranje i unos datuma za ekran i PDF (srpski latinica: dd.MM.yyyy.).
 * Ne koristiti za XML export.
 *
 * Dozvoljen unos: "10.09.2026", "10-9" (tekuća godina), "1-2-25" (01.02.2025),
 * "+1" / "-2" (pomak u danima u odnosu na danas), "+0" ili "." (danas).
 */
public final class DateFormats {

    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    private DateFormats() {}

    public static String format(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY);
    }

    /** @return datum, ili null za prazan unos; baca DateTimeException za neispravan unos. */
    public static LocalDate parse(String text) {
        return parse(text, LocalDate.now());
    }

    /**
     * Isto kao {@link #parse(String)}, ali se pomak ("+1", "-2", ".") računa u odnosu na
     * zadati osnovni datum umesto na današnji.
     */
    public static LocalDate parse(String text, LocalDate base) {
        if (text == null) return null;
        String s = text.trim();
        if (s.isEmpty()) return null;
        if (base == null) base = LocalDate.now();

        if (s.equals(".")) return base;
        if (s.matches("[+-]\\s*\\d+")) {
            return base.plusDays(Long.parseLong(s.replace(" ", "").replace("+", "")));
        }

        String[] parts = s.split("[.\\-/\\s]+");
        try {
            int day, month, year;
            switch (parts.length) {
                case 2:
                    day = Integer.parseInt(parts[0]);
                    month = Integer.parseInt(parts[1]);
                    year = LocalDate.now().getYear();
                    break;
                case 3:
                    day = Integer.parseInt(parts[0]);
                    month = Integer.parseInt(parts[1]);
                    year = Integer.parseInt(parts[2]);
                    if (parts[2].length() <= 2) year += 2000;
                    break;
                default:
                    throw new DateTimeParseException("Neispravan datum: " + text, text, 0);
            }
            return LocalDate.of(year, month, day);
        } catch (NumberFormatException | DateTimeException e) {
            if (e instanceof DateTimeParseException dtpe) throw dtpe;
            throw new DateTimeParseException("Neispravan datum: " + text, text, 0, e);
        }
    }

    public static final StringConverter<LocalDate> CONVERTER = new StringConverter<>() {
        @Override
        public String toString(LocalDate date) {
            return format(date);
        }

        @Override
        public LocalDate fromString(String text) {
            return parse(text);
        }
    };

    /** Podešava DatePicker da koristi srpski format i fleksibilan unos. */
    public static void install(DatePicker picker) {
        picker.setConverter(CONVERTER);
        picker.setPromptText("dd.mm.gggg.");
        commitOnFocusLost(picker);
    }

    /**
     * DatePicker po defaultu potvrđuje ukucani tekst samo na Enter ili izbor iz kalendara.
     * Ovde se tekst parsira i formatira i kad polje izgubi fokus; neispravan unos se vraća
     * na prethodnu vrednost.
     */
    private static void commitOnFocusLost(DatePicker picker) {
        picker.getEditor().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                // Ceo tekst se selektuje pri ulasku u polje (kao kod ostalih polja)
                javafx.application.Platform.runLater(() -> picker.getEditor().selectAll());
                return;
            }
            var converter = picker.getConverter();
            try {
                LocalDate date = converter.fromString(picker.getEditor().getText());
                picker.setValue(date);
                picker.getEditor().setText(converter.toString(date));
            } catch (DateTimeException e) {
                picker.getEditor().setText(converter.toString(picker.getValue()));
            }
        });
    }

    /**
     * Kao {@link #install(DatePicker)}, ali se pomaci ("+1", "-2", ".") računaju u odnosu na
     * datum koji vrati {@code baseSupplier} (npr. vrednost drugog polja); ako vrati null,
     * koristi se današnji datum.
     */
    public static void install(DatePicker picker, Supplier<LocalDate> baseSupplier) {
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                return parse(text, baseSupplier.get());
            }
        });
        picker.setPromptText("dd.mm.gggg.");
        commitOnFocusLost(picker);
    }
}
