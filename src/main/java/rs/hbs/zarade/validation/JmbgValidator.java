package rs.hbs.zarade.validation;

/**
 * Validator za JMBG (Jedinstveni Maticni Broj Gradjana).
 *
 * JMBG ima 13 cifara u formatu: DDMMGGGRRBBBK
 * - DD: dan rodjenja (01-31)
 * - MM: mesec rodjenja (01-12)
 * - GGG: poslednje 3 cifre godine rodjenja
 * - RR: region registracije
 * - BBB: jedinstveni broj (za pol: 000-499 musko, 500-999 zensko)
 * - K: kontrolna cifra
 *
 * Validacija proverava samo da li JMBG ima tačno 13 cifara.
 * Kontrolni broj se NE proverava.
 */
public final class JmbgValidator {

    private static final int JMBG_LENGTH = 13;

    private JmbgValidator() {
        // Utility class
    }

    /**
     * Proverava validnost JMBG-a (format + modulo 11 kontrolni broj).
     *
     * @param jmbg JMBG za proveru (13 cifara)
     * @return true ako je JMBG validan, false inače
     */
    public static boolean isValid(String jmbg) {
        if (jmbg == null || jmbg.length() != JMBG_LENGTH) return false;
        if (!isNumeric(jmbg)) return false;
        return checkControlDigit(jmbg);
    }

    /**
     * Validira JMBG i vraća rezultat sa opisom greške.
     * Proverava format (13 cifara) i modulo 11 kontrolni broj.
     *
     * @param jmbg JMBG za proveru
     * @return ValidationResult sa statusom i porukom
     */
    public static ValidationResult validate(String jmbg) {
        if (jmbg == null || jmbg.isEmpty()) {
            return new ValidationResult(false, "JMBG je obavezan");
        }
        if (jmbg.length() != JMBG_LENGTH) {
            return new ValidationResult(false, "JMBG mora imati tačno 13 cifara");
        }
        if (!isNumeric(jmbg)) {
            return new ValidationResult(false, "JMBG sme sadržati samo cifre");
        }
        if (!checkControlDigit(jmbg)) {
            return new ValidationResult(false, "JMBG nije ispravan (kontrolni broj)");
        }
        return new ValidationResult(true, "JMBG je validan");
    }

    /**
     * Proverava kontrolni broj JMBG-a algoritmom modulo 11.
     * Formula: K = 11 - ((7*(d1+d7) + 6*(d2+d8) + 5*(d3+d9) + 4*(d4+d10) + 3*(d5+d11) + 2*(d6+d12)) % 11)
     * Ako je K == 10 ili K == 11, onda K = 0
     */
    private static boolean checkControlDigit(String jmbg) {
        int[] d = new int[13];
        for (int i = 0; i < 13; i++) {
            d[i] = Character.getNumericValue(jmbg.charAt(i));
        }
        int sum = 7 * (d[0] + d[6])
                + 6 * (d[1] + d[7])
                + 5 * (d[2] + d[8])
                + 4 * (d[3] + d[9])
                + 3 * (d[4] + d[10])
                + 2 * (d[5] + d[11]);
        int k = 11 - (sum % 11);
        if (k == 10 || k == 11) k = 0;
        return k == d[12];
    }

    /**
     * Izvlači informacije iz validnog JMBG-a.
     *
     * @param jmbg validan JMBG
     * @return JmbgInfo sa podacima ili null ako JMBG nije validan
     */
    public static JmbgInfo extractInfo(String jmbg) {
        if (!isValid(jmbg)) {
            return null;
        }

        int dan = Integer.parseInt(jmbg.substring(0, 2));
        int mesec = Integer.parseInt(jmbg.substring(2, 4));
        int godinaKratka = Integer.parseInt(jmbg.substring(4, 7));
        int region = Integer.parseInt(jmbg.substring(7, 9));
        int jedinstveni = Integer.parseInt(jmbg.substring(9, 12));

        // Godina: ako je < 100, dodaje se 1900, inače 2000 za manje od trenutne
        int godina;
        if (godinaKratka > 900) {
            godina = 1000 + godinaKratka;
        } else {
            godina = 2000 + godinaKratka;
            // Ako je godina u budućnosti, verovatno je iz prošlog veka
            if (godina > java.time.LocalDate.now().getYear()) {
                godina = 1000 + godinaKratka;
            }
        }

        boolean musko = jedinstveni < 500;

        return new JmbgInfo(dan, mesec, godina, region, musko);
    }

    private static boolean isNumeric(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Informacije ekstraktovane iz JMBG-a.
     */
    public static class JmbgInfo {
        private final int dan;
        private final int mesec;
        private final int godina;
        private final int region;
        private final boolean musko;

        public JmbgInfo(int dan, int mesec, int godina, int region, boolean musko) {
            this.dan = dan;
            this.mesec = mesec;
            this.godina = godina;
            this.region = region;
            this.musko = musko;
        }

        public int getDan() { return dan; }
        public int getMesec() { return mesec; }
        public int getGodina() { return godina; }
        public int getRegion() { return region; }
        public boolean isMusko() { return musko; }

        @Override
        public String toString() {
            return String.format("Datum: %02d.%02d.%d, Region: %02d, Pol: %s",
                    dan, mesec, godina, region, musko ? "M" : "Ž");
        }
    }
}
