package rs.hbs.zarade.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formatiranje brojeva za prikaz na ekranu i u PDF-u (srpski latinica: 1.234,56).
 * Ne koristiti za XML export.
 */
public final class NumberFormats {

    public static final Locale SR_LATN = Locale.forLanguageTag("sr-Latn-RS");

    private NumberFormats() {}

    /** Iznos sa 2 decimale, "." za hiljade i "," za decimale. */
    public static String money(Number value) {
        DecimalFormat df = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(SR_LATN));
        return df.format(value instanceof BigDecimal ? value : value.doubleValue());
    }
}
