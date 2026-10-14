package rs.hbs.zarade.util;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
        if (text == null) return null;
        String s = text.trim();
        if (s.isEmpty()) return null;

        if (s.matches("[+-]\\s*\\d+")) {
            return LocalDate.now().plusDays(Long.parseLong(s.replace(" ", "").replace("+", "")));
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
                    throw new DateTimeException("Neispravan datum: " + text);
            }
            return LocalDate.of(year, month, day);
        } catch (NumberFormatException e) {
            throw new DateTimeException("Neispravan datum: " + text, e);
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
    }
}
