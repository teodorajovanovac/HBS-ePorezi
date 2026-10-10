package rs.hbs.zarade.reporting;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import rs.hbs.zarade.domain.Obracun;
import rs.hbs.zarade.domain.ObracunDefinicija;
import rs.hbs.zarade.domain.Poslodavac;
import rs.hbs.zarade.domain.Primalac;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generisanje PDF izveštaja za obračun honorara.
 *
 * Tipovi izveštaja:
 * 1. Zbirni — svi primaoci sa zbrojenim iznosima
 * 2. Pojedinačni — jedan primalac, sve detalje
 *
 * Koristi iText 7.
 */
public class PdfReporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
    private static final DeviceRgb HEADER_BG = new DeviceRgb(21, 101, 192);   // #1565c0
    private static final DeviceRgb ALT_ROW_BG = new DeviceRgb(227, 242, 253); // #e3f2fd
    private static final DeviceRgb TOTAL_BG = new DeviceRgb(200, 230, 201);   // #c8e6c9
    private static final DeviceRgb PPP_GRAY = new DeviceRgb(224, 224, 224);   // #E0E0E0 — PPP-PO grayscale

    /**
     * Generiše zbirni PDF izveštaj.
     *
     * @param outputFile  izlazni fajl
     * @param definicija  zaglavlje obračuna
     * @param stavke      lista stavki obračuna
     * @param poslodavac  podaci o isplatiocu (može biti null)
     */
    public static void generateZbirni(File outputFile,
                                      ObracunDefinicija definicija,
                                      List<Obracun> stavke,
                                      Poslodavac poslodavac) throws Exception {
        try (PdfWriter writer = new PdfWriter(outputFile);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 36, 36, 36);
            PdfFont font = loadPdfFont();
            if (font != null) doc.setFont(font);

            addHeader(doc, "ZBIRNI IZVEŠTAJ OBRAČUNA", definicija, poslodavac);
            addZbirnaTabela(doc, stavke);
            addPotpis(doc);
        }
    }

    /**
     * Generiše pojedinačni PDF izveštaj za jednog primaoca.
     *
     * @param outputFile  izlazni fajl
     * @param stavka      stavka obračuna
     * @param definicija  zaglavlje obračuna
     * @param poslodavac  podaci o isplatiocu
     */
    public static void generatePojedinacan(File outputFile,
                                           Obracun stavka,
                                           ObracunDefinicija definicija,
                                           Poslodavac poslodavac) throws Exception {
        try (PdfWriter writer = new PdfWriter(outputFile);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 36, 36, 36);
            PdfFont font = loadPdfFont();
            if (font != null) doc.setFont(font);

            String naslov = "OBRAČUN ISPLATE — " +
                    (stavka.getPrezime() != null ? stavka.getPrezime() : "") + " " +
                    (stavka.getIme() != null ? stavka.getIme() : "");
            addHeader(doc, naslov, definicija, poslodavac);
            addPojedinacanDetalj(doc, stavka);
            addPotpis(doc);
        }
    }

    /**
     * Generiše PDF karton primaoca prihoda (lični podaci i podaci o zaposlenju).
     *
     * @param outputFile  izlazni fajl
     * @param primalac    primalac čiji se karton generiše
     */
    public static void generatePrimalacKarton(File outputFile, Primalac primalac) throws Exception {
        try (PdfWriter writer = new PdfWriter(outputFile);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 36, 36, 36);
            PdfFont font = loadPdfFont();
            if (font != null) doc.setFont(font);

            // Naslov
            doc.add(new Paragraph("KARTON PRIMAOCA PRIHODA")
                    .setFontSize(16).setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(new DeviceRgb(21, 101, 192))
                    .setMarginBottom(4));

            String punoIme = nvl(primalac.getPrezime()) + " " + nvl(primalac.getIme());
            doc.add(new Paragraph(punoIme.trim())
                    .setFontSize(13).setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(12));

            // Lični podaci
            doc.add(new Paragraph("Lični podaci")
                    .setFontSize(12).setBold().setMarginBottom(4));

            Table t1 = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(12);
            addInfoCell(t1, "Prezime:", nvl(primalac.getPrezime()));
            addInfoCell(t1, "Ime:", nvl(primalac.getIme()));
            addInfoCell(t1, "MB/JMBG:", nvl(primalac.getMb()));
            addInfoCell(t1, "Adresa:", nvl(primalac.getAdresaP()));
            addInfoCell(t1, "PB/Grad:", nvl(primalac.getPbGradP()));
            addInfoCell(t1, "Opština:", nvl(primalac.getOznakaPrebivalista()));
            addInfoCell(t1, "Mobilni:", nvl(primalac.getMobilni()));
            addInfoCell(t1, "Email:", nvl(primalac.getEmail()));
            doc.add(t1);

            // Podaci o zaposlenju
            doc.add(new Paragraph("Podaci o zaposlenju")
                    .setFontSize(12).setBold().setMarginBottom(4));

            Table t2 = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(12);
            Integer proc = primalac.getProcenatZaposlenja();
            addInfoCell(t2, "% zaposlenja:", proc != null ? proc + "%" : "");
            addInfoCell(t2, "Status:", primalac.getStatus() != null && primalac.getStatus() == 0 ? "Neaktivan" : "Aktivan");
            addInfoCell(t2, "Napomena:", nvl(primalac.getNapomena()));
            doc.add(t2);

            addPotpis(doc);
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static void addHeader(Document doc, String naslov,
                                  ObracunDefinicija def, Poslodavac poslodavac) throws Exception {
        // Naslov
        Paragraph title = new Paragraph(naslov)
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(21, 101, 192))
                .setMarginBottom(4);
        doc.add(title);

        // Poslodavac
        if (poslodavac != null && poslodavac.getNazivPrezimeIme() != null) {
            doc.add(new Paragraph("Isplatilac: " + poslodavac.getNazivPrezimeIme())
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2));
            if (poslodavac.getPoreskiIdentifikacioniBroj() != null) {
                doc.add(new Paragraph("PIB: " + poslodavac.getPoreskiIdentifikacioniBroj())
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(8));
            }
        }

        // Detalji obračuna
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(12);

        addInfoCell(infoTable, "Naziv obračuna:", nvl(def.getNazivObracuna()));
        addInfoCell(infoTable, "Periode (mesec):", nvl(def.getMesec()));
        addInfoCell(infoTable, "Datum obračuna:",
                def.getDatumObracuna() != null ? def.getDatumObracuna().format(DATE_FMT) : "-");

        doc.add(infoTable);
    }

    private static void addInfoCell(Table table, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFontSize(9).setBold())
                .add(new Paragraph(value).setFontSize(10))
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
    }

    private static void addZbirnaTabela(Document doc, List<Obracun> stavke) {
        doc.add(new Paragraph("Pregled stavki obračuna")
                .setFontSize(12).setBold().setMarginBottom(6));

        float[] cols = {3, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f};
        Table table = new Table(UnitValue.createPercentArray(cols))
                .setWidth(UnitValue.createPercentValue(100));

        // Zaglavlje tabele
        String[] headers = {"Primalac", "NETO", "BRUTO", "Porez", "PIO", "ZDR", "NEZ"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFontSize(9).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(HEADER_BG)
                    .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.WHITE, 0.5f)));
        }

        // Podaci
        BigDecimal sumaNeto = BigDecimal.ZERO;
        BigDecimal sumaBruto = BigDecimal.ZERO;
        BigDecimal sumaPorez = BigDecimal.ZERO;
        BigDecimal sumaPio = BigDecimal.ZERO;
        BigDecimal sumaZdr = BigDecimal.ZERO;
        BigDecimal sumaNez = BigDecimal.ZERO;

        int idx = 0;
        for (Obracun o : stavke) {
            boolean alt = (idx++ % 2 == 1);
            DeviceRgb rowBg = alt ? ALT_ROW_BG : null;

            String ime = nvl(o.getPrezime()) + " " + nvl(o.getIme());
            addDataCell(table, ime, rowBg, TextAlignment.LEFT, 9);
            addDataCell(table, fmt(o.getNeto()), rowBg, TextAlignment.RIGHT, 9);
            addDataCell(table, fmt(o.getBruto()), rowBg, TextAlignment.RIGHT, 9);
            addDataCell(table, fmt(o.getPorez()), rowBg, TextAlignment.RIGHT, 9);
            addDataCell(table, fmt(o.getPio()), rowBg, TextAlignment.RIGHT, 9);
            addDataCell(table, fmt(o.getZdr()), rowBg, TextAlignment.RIGHT, 9);
            addDataCell(table, fmt(o.getNez()), rowBg, TextAlignment.RIGHT, 9);

            sumaNeto = add(sumaNeto, o.getNeto());
            sumaBruto = add(sumaBruto, o.getBruto());
            sumaPorez = add(sumaPorez, o.getPorez());
            sumaPio = add(sumaPio, o.getPio());
            sumaZdr = add(sumaZdr, o.getZdr());
            sumaNez = add(sumaNez, o.getNez());
        }

        // Red sa ukupnim iznosima
        addDataCell(table, "UKUPNO (" + stavke.size() + " stavki):", TOTAL_BG, TextAlignment.LEFT, 9);
        addDataCell(table, fmt(sumaNeto), TOTAL_BG, TextAlignment.RIGHT, 9);
        addDataCell(table, fmt(sumaBruto), TOTAL_BG, TextAlignment.RIGHT, 9);
        addDataCell(table, fmt(sumaPorez), TOTAL_BG, TextAlignment.RIGHT, 9);
        addDataCell(table, fmt(sumaPio), TOTAL_BG, TextAlignment.RIGHT, 9);
        addDataCell(table, fmt(sumaZdr), TOTAL_BG, TextAlignment.RIGHT, 9);
        addDataCell(table, fmt(sumaNez), TOTAL_BG, TextAlignment.RIGHT, 9);

        doc.add(table);
    }

    private static void addPojedinacanDetalj(Document doc, Obracun o) {
        doc.add(new Paragraph("Podaci o primaocu")
                .setFontSize(12).setBold().setMarginBottom(4));

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(60))
                .setMarginBottom(12);

        addInfoCell(infoTable, "Prezime:", nvl(o.getPrezime()));
        addInfoCell(infoTable, "Ime:", nvl(o.getIme()));
        addInfoCell(infoTable, "Identifikator:", nvl(o.getIdentifikatorPrimaoca()));
        addInfoCell(infoTable, "Vrsta identifikatora:", nvl(o.getVrstaIdentifikatoraPrimaoca()));
        addInfoCell(infoTable, "Opština:", nvl(o.getOznakaPrebivalista()));
        addInfoCell(infoTable, "Vrsta prihoda (SVP-3):", nvl(o.getSvp3()));
        doc.add(infoTable);

        doc.add(new Paragraph("Finansijski podaci")
                .setFontSize(12).setBold().setMarginBottom(4));

        float[] cols = {2, 2};
        Table finTable = new Table(UnitValue.createPercentArray(cols))
                .setWidth(UnitValue.createPercentValue(60))
                .setMarginBottom(12);

        addFinRow(finTable, "NETO iznos:", fmt(o.getNeto()), false);
        addFinRow(finTable, "BRUTO iznos:", fmt(o.getBruto()), false);
        addFinRow(finTable, "Porez:", fmt(o.getPorez()), false);
        addFinRow(finTable, "PIO:", fmt(o.getPio()), false);
        addFinRow(finTable, "ZDR:", fmt(o.getZdr()), false);
        addFinRow(finTable, "NEZ:", fmt(o.getNez()), false);
        doc.add(finTable);
    }

    private static void addFinRow(Table table, String label, String value, boolean bold) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFontSize(10).setBold())
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
        table.addCell(new Cell()
                .add(new Paragraph(value).setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
    }

    private static void addDataCell(Table table, String text, DeviceRgb bg,
                                    TextAlignment align, float fontSize) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFontSize(fontSize).setTextAlignment(align))
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f));
        if (bg != null) cell.setBackgroundColor(bg);
        table.addCell(cell);
    }

    private static void addPotpis(Document doc) {
        doc.add(new Paragraph("\n\n"));
        Table potpis = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(30);

        potpis.addCell(new Cell()
                .add(new Paragraph("Odgovorno lice:").setFontSize(10))
                .add(new Paragraph("_____________________________").setFontSize(10))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

        potpis.addCell(new Cell()
                .add(new Paragraph("Pečat:").setFontSize(10))
                .add(new Paragraph("_____________________________").setFontSize(10))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT));

        doc.add(potpis);
    }

    /**
     * Generiše PPP-PO Potvrdu o plaćenim porezima i doprinosima.
     *
     * @param outputFile    izlazni fajl
     * @param primalac      primalac prihoda
     * @param opstinaNaziv  naziv opštine primaoca (lookup po OznakaPrebivalista)
     * @param poslodavac    isplatilac prihoda (može biti null)
     * @param stavke        lista obračuna za datu godinu
     * @param godina        kalendarska godina
     */
    public static void generatePppPoPotvrda(File outputFile,
                                             Primalac primalac,
                                             String opstinaNaziv,
                                             Poslodavac poslodavac,
                                             List<Obracun> stavke,
                                             int godina) throws Exception {
        try (PdfWriter writer = new PdfWriter(outputFile);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4.rotate())) {

            doc.setMargins(36, 36, 36, 36);
            PdfFont font = loadPdfFont();
            if (font != null) doc.setFont(font);

            com.itextpdf.layout.borders.SolidBorder borderBlack =
                    new com.itextpdf.layout.borders.SolidBorder(ColorConstants.BLACK, 0.5f);

            // === ZAGLAVLJE — gray box with black border ===
            Table headerBox = new Table(UnitValue.createPercentArray(new float[]{2.5f, 1f}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(8);

            headerBox.addCell(new Cell()
                    .add(new Paragraph("POTVRDA O PLAĆENIM POREZIMA I DOPRINOSIMA PO ODBITKU ZA "
                            + godina + ". GODINU")
                            .setFontSize(10).setBold().setFontColor(ColorConstants.BLACK))
                    .setBackgroundColor(PPP_GRAY)
                    .setBorder(borderBlack)
                    .setBorderRight(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setPadding(6));

            headerBox.addCell(new Cell()
                    .add(new Paragraph("Obrazac PPP-PO")
                            .setFontSize(9).setFontColor(ColorConstants.BLACK)
                            .setTextAlignment(TextAlignment.RIGHT))
                    .setBackgroundColor(PPP_GRAY)
                    .setBorder(borderBlack)
                    .setBorderLeft(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setPadding(6));

            doc.add(headerBox);

            // === SEKCIJA 1: ISPLATILAC ===
            doc.add(new Paragraph("1. Podaci o isplatiocu prihoda")
                    .setFontSize(9).setFontColor(ColorConstants.BLACK).setMarginBottom(2));

            Table ispl = new Table(UnitValue.createPercentArray(new float[]{1.3f, 2f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);

            addPppRow(ispl, "1.1 Naziv/ime i prezime isplatioca",
                    poslodavac != null ? nvl(poslodavac.getNazivPrezimeIme()) : "");
            addPppRow(ispl, "1.2 Sedište/Prebivalište isplatioca",
                    poslodavac != null ? nvl(poslodavac.getPunaAdresa()) : "");
            addPppRow(ispl, "1.3 PIB/JMBG isplatioca",
                    poslodavac != null ? nvl(poslodavac.getPoreskiIdentifikacioniBroj()) : "");
            addPppRow(ispl, "1.4 Broj", "");
            addPppRow(ispl, "1.5 Datum izdavanja", LocalDate.now().format(DATE_FMT));
            doc.add(ispl);

            // === SEKCIJA 2: PRIMALAC ===
            doc.add(new Paragraph("2. Podaci o primaocu prihoda")
                    .setFontSize(9).setFontColor(ColorConstants.BLACK).setMarginBottom(2));

            Table priml = new Table(UnitValue.createPercentArray(new float[]{1.3f, 2f}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);

            String punoIme = (nvl(primalac.getPrezime()) + " " + nvl(primalac.getIme())).trim();
            addPppRow(priml, "2.1 Ime i prezime primaoca", punoIme);
            addPppRow(priml, "2.2 Adresa", buildAdresaPrimaoca(primalac, opstinaNaziv));
            addPppRow(priml, "2.3 Podatak o identifikaciji primaoca", nvl(primalac.getMb()));
            doc.add(priml);

            // === SEKCIJA 3: TABELA ===
            doc.add(new Paragraph(
                    "3. Podaci o oporezivim prihodima iz kojih je isplatilac obustavio i platio " +
                    "poreze i doprinose po odbitku")
                    .setFontSize(9).setFontColor(ColorConstants.BLACK).setMarginBottom(2));

            addPppPoTabela(doc, stavke);

            // === SEKCIJA 4: FOOTER — odgovorno lice ===
            addPppPoFooter(doc, poslodavac);
        }
    }

    private static void addPppRow(Table table, String label, String value) {
        com.itextpdf.layout.borders.SolidBorder b =
                new com.itextpdf.layout.borders.SolidBorder(ColorConstants.BLACK, 0.5f);
        table.addCell(new Cell()
                .add(new Paragraph(label).setFontSize(9).setFontColor(ColorConstants.BLACK))
                .setBackgroundColor(PPP_GRAY)
                .setBorder(b));
        table.addCell(new Cell()
                .add(new Paragraph(value != null ? value : "").setFontSize(9)
                        .setFontColor(ColorConstants.BLACK))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(b));
    }

    private static void addPppPoTabela(Document doc, List<Obracun> stavke) {
        com.itextpdf.layout.borders.SolidBorder bBlack =
                new com.itextpdf.layout.borders.SolidBorder(ColorConstants.BLACK, 0.5f);
        com.itextpdf.layout.borders.SolidBorder bThick =
                new com.itextpdf.layout.borders.SolidBorder(ColorConstants.BLACK, 1.5f);

        float[] cols = {0.9f, 1.4f, 1.7f, 1.4f, 1.0f, 1.5f, 1.5f};
        Table table = new Table(UnitValue.createPercentArray(cols))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(12);

        // Header row 1 — full column names, gray background
        String[] row1 = {
            "Šifra vrste prihoda isplaćena fizičkom licu",
            "Bruto prihod",
            "Normirani troškovi, poreske olakšice i druga umanjenja",
            "Poreska osnovica",
            "Porez",
            "Doprinosi za obavezno socijalno osiguranje na teret primaoca prihoda",
            "Doprinosi za obavezno socijalno osiguranje na teret isplatioca prihoda"
        };
        for (String h : row1) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFontSize(7).setFontColor(ColorConstants.BLACK))
                    .setBackgroundColor(PPP_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(bBlack));
        }

        // Header row 2 — column numbers, white background
        for (String h : new String[]{"3.1", "3.2", "3.3", "3.4", "3.5", "3.6", "3.7"}) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFontSize(8).setFontColor(ColorConstants.BLACK))
                    .setBackgroundColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(bBlack));
        }

        BigDecimal sumaBruto = BigDecimal.ZERO, sumaNorm = BigDecimal.ZERO;
        BigDecimal sumaOsnovica = BigDecimal.ZERO, sumaPorez = BigDecimal.ZERO;
        BigDecimal sumaDoprPrimaoca = BigDecimal.ZERO, sumaDoprIsplatioca = BigDecimal.ZERO;

        for (Obracun o : stavke) {
            BigDecimal doprPr = o.getUkupnoDoprinosiZaposleni();
            BigDecimal doprIs = o.getUkupnoDoprinosiPoslodavac();

            addPppDataCell(table, nvl(o.getSvp3()), TextAlignment.CENTER, bBlack);
            addPppDataCell(table, fmt(o.getBruto()), TextAlignment.RIGHT, bBlack);
            addPppDataCell(table, fmt(o.getNormTroskovi()), TextAlignment.RIGHT, bBlack);
            addPppDataCell(table, fmt(o.getOsnovicaPorez()), TextAlignment.RIGHT, bBlack);
            addPppDataCell(table, fmt(o.getPorez()), TextAlignment.RIGHT, bBlack);
            addPppDataCell(table, fmt(doprPr), TextAlignment.RIGHT, bBlack);
            addPppDataCell(table, fmt(doprIs), TextAlignment.RIGHT, bBlack);

            sumaBruto = add(sumaBruto, o.getBruto());
            sumaNorm = add(sumaNorm, o.getNormTroskovi());
            sumaOsnovica = add(sumaOsnovica, o.getOsnovicaPorez());
            sumaPorez = add(sumaPorez, o.getPorez());
            sumaDoprPrimaoca = sumaDoprPrimaoca.add(doprPr);
            sumaDoprIsplatioca = sumaDoprIsplatioca.add(doprIs);
        }

        // UKUPNO red — white background, thicker top border
        addPppUkupnoCell(table, "UKUPNO", TextAlignment.CENTER, bBlack, bThick);
        addPppUkupnoCell(table, fmt(sumaBruto), TextAlignment.RIGHT, bBlack, bThick);
        addPppUkupnoCell(table, fmt(sumaNorm), TextAlignment.RIGHT, bBlack, bThick);
        addPppUkupnoCell(table, fmt(sumaOsnovica), TextAlignment.RIGHT, bBlack, bThick);
        addPppUkupnoCell(table, fmt(sumaPorez), TextAlignment.RIGHT, bBlack, bThick);
        addPppUkupnoCell(table, fmt(sumaDoprPrimaoca), TextAlignment.RIGHT, bBlack, bThick);
        addPppUkupnoCell(table, fmt(sumaDoprIsplatioca), TextAlignment.RIGHT, bBlack, bThick);

        doc.add(table);
    }

    private static void addPppDataCell(Table table, String text, TextAlignment align,
                                        com.itextpdf.layout.borders.Border border) {
        table.addCell(new Cell()
                .add(new Paragraph(text).setFontSize(9).setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(align))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(border));
    }

    private static void addPppUkupnoCell(Table table, String text, TextAlignment align,
                                          com.itextpdf.layout.borders.Border border,
                                          com.itextpdf.layout.borders.Border topBorder) {
        table.addCell(new Cell()
                .add(new Paragraph(text).setFontSize(9).setBold()
                        .setFontColor(ColorConstants.BLACK).setTextAlignment(align))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(border)
                .setBorderTop(topBorder));
    }

    private static void addPppPoFooter(Document doc, Poslodavac poslodavac) {
        String odgovornoLice = poslodavac != null && poslodavac.getOdgovornoLice() != null
                ? poslodavac.getOdgovornoLice() : "";

        com.itextpdf.layout.borders.SolidBorder b =
                new com.itextpdf.layout.borders.SolidBorder(ColorConstants.BLACK, 0.5f);

        Table footer = new Table(UnitValue.createPercentArray(new float[]{1f, 2f}))
                .setWidth(UnitValue.createPercentValue(60))
                .setMarginTop(16);

        footer.addCell(new Cell()
                .add(new Paragraph("4.1 ODGOVORNO LICE").setFontSize(9)
                        .setFontColor(ColorConstants.BLACK))
                .setBackgroundColor(PPP_GRAY)
                .setBorder(b));

        footer.addCell(new Cell()
                .add(new Paragraph(odgovornoLice).setFontSize(9)
                        .setFontColor(ColorConstants.BLACK))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(b));

        doc.add(footer);
    }

    private static String buildAdresaPrimaoca(Primalac primalac, String opstinaNaziv) {
        StringBuilder sb = new StringBuilder();
        if (primalac.getAdresaP() != null && !primalac.getAdresaP().isEmpty())
            sb.append(primalac.getAdresaP());
        if (primalac.getPbGradP() != null && !primalac.getPbGradP().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(primalac.getPbGradP());
        }
        if (opstinaNaziv != null && !opstinaNaziv.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(opstinaNaziv);
        }
        return sb.toString();
    }

    /**
     * Učitava Arial font koji podržava srpska slova (ć, č, š, đ, ž).
     * Arial je uvek prisutan na Windows sistemima.
     * Ako nije dostupan, vraća null i tekst ostaje sa defaultnim fontom (bez srpskih slova).
     */
    private static PdfFont loadPdfFont() {
        try {
            return PdfFontFactory.createFont(
                "C:\\Windows\\Fonts\\arial.ttf",
                "Identity-H",
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (Exception e) {
            System.err.println("Upozorenje: Arial font nije dostupan, srpski znakovi možda neće biti ispravni. " + e.getMessage());
            return null;
        }
    }

    private static String nvl(String s) { return s != null ? s : ""; }
    private static String fmt(BigDecimal v) {
        return v != null ? String.format("%,.2f", v) : "0,00";
    }
    private static BigDecimal add(BigDecimal a, BigDecimal b) {
        return b != null ? a.add(b) : a;
    }
}
