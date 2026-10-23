package rs.hbs.zarade.reporting;

import rs.hbs.zarade.domain.Obracun;
import rs.hbs.zarade.domain.ObracunDefinicija;
import rs.hbs.zarade.domain.Poslodavac;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generisanje XML-a poreske deklaracije (PPP-PO) za Poresku upravu Srbije.
 *
 * Format je identičan izlazu iz Accessa (primer/obracun_489-access.xml):
 * koren PodaciPoreskeDeklaracije sa sekcijama PodaciOPrijavi, PodaciOIsplatiocu
 * i DeklarisaniPrihodi (jedan PodaciOPrihodima po primaocu). Elementi su u
 * zasebnim redovima bez uvlačenja, prazni elementi nisu skraćeni.
 *
 * NAPOMENA: Ovaj XML se direktno koristi za prijavljivanje Poreskoj upravi.
 * Struktura mora biti 100% kompatibilna sa propisanim formatom.
 */
public class XmlExporter {

    private static final String TNS_NS = "http://pid.purs.gov.rs";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Generiše XML string za pregled.
     *
     * @param definicija zaglavlje obračuna
     * @param stavke     lista pojedinačnih obračuna
     * @param poslodavac podaci o isplatiocu
     * @return XML kao String
     */
    public static String generateXmlString(ObracunDefinicija definicija,
                                           List<Obracun> stavke,
                                           Poslodavac poslodavac) {
        return buildXml(definicija, stavke, poslodavac);
    }

    /**
     * Upisuje XML u fajl (UTF-8).
     *
     * @param outputFile odredišni fajl
     * @param definicija zaglavlje obračuna
     * @param stavke     lista pojedinačnih obračuna
     * @param poslodavac podaci o isplatiocu
     */
    public static void exportToFile(File outputFile,
                                    ObracunDefinicija definicija,
                                    List<Obracun> stavke,
                                    Poslodavac poslodavac) throws Exception {
        Files.write(outputFile.toPath(),
                buildXml(definicija, stavke, poslodavac).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Naziv XML fajla po šablonu {@code id_mesecYYYYMM_nazivobracuna.xml}.
     * U nazivu obračuna se znaci {@code : \ . ,}, slova š đ č ć ž, razmak i ostali znakovi koji
     * nisu dozvoljeni u nazivu fajla zamenjuju sa "-" (uzastopne crtice se spajaju u jednu).
     */
    public static String defaultFileName(ObracunDefinicija def) {
        String id = def.getIdObracun() != null ? String.valueOf(def.getIdObracun()) : "";
        String mesec = nvl(def.getMesec()).replace("-", "").trim();
        String naziv = nvl(def.getNazivObracuna())
                .replaceAll("[:\\\\.,šđčćžŠĐČĆŽ\\s/*?\"<>|]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return id + "_" + mesec + "_" + naziv + ".xml";
    }

    // -----------------------------------------------------------------------
    // Private implementation
    // -----------------------------------------------------------------------

    private static String buildXml(ObracunDefinicija def, List<Obracun> stavke, Poslodavac p) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<tns:PodaciPoreskeDeklaracije")
                .append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
                .append(" xmlns:tns=\"").append(TNS_NS).append("\"")
                .append(" xsi:schemaLocation=\"").append(TNS_NS).append("\">\n");

        // Podaci o prijavi
        sb.append("<tns:PodaciOPrijavi>\n");
        tag(sb, "KlijentskaOznakaDeklaracije", "");
        tag(sb, "VrstaPrijave", "1"); // 1 = originalna
        tag(sb, "ObracunskiPeriod", nvl(def.getMesec()));
        tag(sb, "DatumPlacanja", def.getDatumObracuna() != null ? def.getDatumObracuna().format(DATE_FMT) : "");
        tag(sb, "NajnizaOsnovica", "0");
        sb.append("</tns:PodaciOPrijavi>\n");

        // Podaci o isplatiocu
        sb.append("<tns:PodaciOIsplatiocu>\n");
        tag(sb, "TipIsplatioca", p != null && p.getTipIsplatioca() != null ? String.valueOf(p.getTipIsplatioca()) : "");
        tag(sb, "VrstaIdentifikatorIsplatioca", "0");
        tag(sb, "PoreskiIdentifikacioniBroj", p != null ? nvl(p.getPoreskiIdentifikacioniBroj()) : "");
        tag(sb, "BrojZaposlenih", p != null && p.getBrojZaposlenih() != null ? String.valueOf(p.getBrojZaposlenih()) : "0");
        tag(sb, "MaticniBrojisplatioca", p != null ? nvl(p.getMaticniBrojIsplatioca()) : "");
        tag(sb, "NazivPrezimeIme", p != null ? nvl(p.getNazivPrezimeIme()) : "");
        tag(sb, "SedistePrebivaliste", p != null ? nvl(p.getSedistePrebivaliste()) : "");
        tag(sb, "Telefon", p != null ? nvl(p.getTelefon()) : "");
        tag(sb, "UlicaIBroj", p != null ? nvl(p.getUlicaIBroj()) : "");
        tag(sb, "eMail", p != null ? nvl(p.getEmail()) : "");
        sb.append("</tns:PodaciOIsplatiocu>\n");

        // Deklarisani prihodi — jedan PodaciOPrihodima po stavci
        sb.append("<tns:DeklarisaniPrihodi>\n");
        int redniBroj = 0;
        for (Obracun o : stavke) {
            if (o.getNeto() == null || o.getNeto().compareTo(BigDecimal.ZERO) <= 0) {
                continue; // preskoči nulte stavke
            }
            sb.append("<tns:PodaciOPrihodima>\n");
            tag(sb, "RedniBroj", String.valueOf(++redniBroj));
            tag(sb, "VrstaIdentifikatoraPrimaoca", nvl(o.getVrstaIdentifikatoraPrimaoca()));
            tag(sb, "IdentifikatorPrimaoca", nvl(o.getIdentifikatorPrimaoca()));
            tag(sb, "Prezime", nvl(o.getPrezime()));
            tag(sb, "Ime", nvl(o.getIme()));
            tag(sb, "OznakaPrebivalista", nvl(o.getOznakaPrebivalista()));
            tag(sb, "SVP", svp(o));
            tag(sb, "BrojKalendarskihDana",
                    o.getBrojKalendarskihDana() != null ? String.valueOf(o.getBrojKalendarskihDana()) : "0");
            tag(sb, "Bruto", bd(o.getBruto()));
            tag(sb, "OsnovicaPorez", bd(o.getOsnovicaPorez()));
            tag(sb, "Porez", bd(o.getPorez()));
            tag(sb, "OsnovicaDoprinosi", bd(o.getOsnovicaDoprinosi()));
            tag(sb, "PIO", bd(o.getPio()));
            tag(sb, "ZDR", bd(o.getZdr()));
            tag(sb, "NEZ", bd(o.getNez()));
            tag(sb, "PIOBen", bd(o.getPioBen()));
            sb.append("</tns:PodaciOPrihodima>\n");
        }
        sb.append("</tns:DeklarisaniPrihodi>\n");
        sb.append("</tns:PodaciPoreskeDeklaracije>\n");
        return sb.toString();
    }

    /** Kompletna šifra SVP (9 cifara); ako nije upisana, slaže se iz delova SVP-1..SVP-5. */
    private static String svp(Obracun o) {
        if (o.getSvp() != null && !o.getSvp().trim().isEmpty()) {
            return o.getSvp().trim();
        }
        return nvl(Obracun.composeSvp(o.getSvp1(), o.getSvp2(), o.getSvp3(), o.getSvp4(), o.getSvp5()));
    }

    private static void tag(StringBuilder sb, String name, String value) {
        sb.append("<tns:").append(name).append('>').append(escape(value))
                .append("</tns:").append(name).append(">\n");
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String nvl(String value) {
        return value != null ? value : "";
    }

    private static String bd(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
    }
}
