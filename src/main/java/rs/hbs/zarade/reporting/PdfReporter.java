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
import java.util.Map;
import java.util.TreeMap;

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

        private static final DateTimeFormatter DATE_FMT = rs.hbs.zarade.util.DateFormats.DISPLAY;
        private static final DeviceRgb HEADER_BG = new DeviceRgb(21, 101, 192); // #1565c0
        private static final DeviceRgb ALT_ROW_BG = new DeviceRgb(227, 242, 253); // #e3f2fd
        private static final DeviceRgb TOTAL_BG = new DeviceRgb(200, 230, 201); // #c8e6c9
        private static final DeviceRgb PPP_GRAY = new DeviceRgb(224, 224, 224); // #E0E0E0 — PPP-PO grayscale

        /**
         * Generiše zbirni PDF izveštaj.
         *
         * @param outputFile izlazni fajl
         * @param definicija zaglavlje obračuna
         * @param stavke     lista stavki obračuna
         * @param poslodavac podaci o isplatiocu (može biti null)
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
                        if (font != null)
                                doc.setFont(font);

                        addHeader(doc, "ZBIRNI IZVEŠTAJ OBRAČUNA", definicija, poslodavac);
                        addZbirnaTabela(doc, stavke);
                        addPotpis(doc);
                }
        }

        /**
         * Generiše pojedinačni PDF izveštaj za jednog primaoca.
         *
         * @param outputFile izlazni fajl
         * @param stavka     stavka obračuna
         * @param definicija zaglavlje obračuna
         * @param poslodavac podaci o isplatiocu
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
                        if (font != null)
                                doc.setFont(font);

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
         * @param outputFile izlazni fajl
         * @param primalac   primalac čiji se karton generiše
         */
        public static void generatePrimalacKarton(File outputFile, Primalac primalac) throws Exception {
                try (PdfWriter writer = new PdfWriter(outputFile);
                                PdfDocument pdf = new PdfDocument(writer);
                                Document doc = new Document(pdf, PageSize.A4)) {

                        doc.setMargins(36, 36, 36, 36);
                        PdfFont font = loadPdfFont();
                        if (font != null)
                                doc.setFont(font);

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

                        Table t1 = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
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

                        Table t2 = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                                        .setWidth(UnitValue.createPercentValue(100))
                                        .setMarginBottom(12);
                        Integer proc = primalac.getProcenatZaposlenja();
                        addInfoCell(t2, "% zaposlenja:", proc != null ? proc + "%" : "");
                        addInfoCell(t2, "Status:",
                                        primalac.getStatus() != null && primalac.getStatus() == 0 ? "Neaktivan"
                                                        : "Aktivan");
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
                Table infoTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1, 1 }))
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
                                .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY,
                                                0.5f)));
        }

        private static void addZbirnaTabela(Document doc, List<Obracun> stavke) {
                doc.add(new Paragraph("Pregled stavki obračuna")
                                .setFontSize(12).setBold().setMarginBottom(6));

                float[] cols = { 3, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f };
                Table table = new Table(UnitValue.createPercentArray(cols))
                                .setWidth(UnitValue.createPercentValue(100));

                // Zaglavlje tabele
                String[] headers = { "Primalac", "NETO", "BRUTO", "Porez", "PIO", "ZDR", "NEZ" };
                for (String h : headers) {
                        table.addHeaderCell(new Cell()
                                        .add(new Paragraph(h).setFontSize(9).setBold()
                                                        .setFontColor(ColorConstants.WHITE))
                                        .setBackgroundColor(HEADER_BG)
                                        .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.WHITE,
                                                        0.5f)));
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

                Table infoTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
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

                float[] cols = { 2, 2 };
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
                                .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY,
                                                0.5f)));
                table.addCell(new Cell()
                                .add(new Paragraph(value).setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                                .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY,
                                                0.5f)));
        }

        private static void addDataCell(Table table, String text, DeviceRgb bg,
                        TextAlignment align, float fontSize) {
                Cell cell = new Cell()
                                .add(new Paragraph(text).setFontSize(fontSize).setTextAlignment(align))
                                .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY,
                                                0.5f));
                if (bg != null)
                        cell.setBackgroundColor(bg);
                table.addCell(cell);
        }

        private static void addPotpis(Document doc) {
                doc.add(new Paragraph("\n\n"));
                Table potpis = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
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
         * @param outputFile   izlazni fajl
         * @param primalac     primalac prihoda
         * @param opstinaNaziv naziv opštine primaoca (lookup po OznakaPrebivalista)
         * @param poslodavac   isplatilac prihoda (može biti null)
         * @param stavke       lista obračuna za datu godinu
         * @param godina       kalendarska godina
         * @param grupaPoVpz   mapa SVP-3 (VPZ) -> GrupaSVR iz SFR_VRSTAPRIHODA
         */
        public static void generatePppPoPotvrda(File outputFile,
                        Primalac primalac,
                        String opstinaNaziv,
                        Poslodavac poslodavac,
                        List<Obracun> stavke,
                        int godina,
                        Map<String, String> grupaPoVpz) throws Exception {
                try (PdfWriter writer = new PdfWriter(outputFile);
                                PdfDocument pdf = new PdfDocument(writer);
                                Document doc = new Document(pdf, PageSize.A4.rotate())) {

                        doc.setMargins(36, 36, 36, 36);
                        PdfFont font = loadPdfFont();
                        if (font != null)
                                doc.setFont(font);

                        com.itextpdf.layout.borders.SolidBorder borderBlack = new com.itextpdf.layout.borders.SolidBorder(
                                        ColorConstants.BLACK, 0.5f);

                        // === ZAGLAVLJE — gray box with black border ===
                        Table headerBox = new Table(UnitValue.createPercentArray(new float[] { 2.5f, 1f }))
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

                        Table ispl = new Table(UnitValue.createPercentArray(new float[] { 1.3f, 2f }))
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

                        Table priml = new Table(UnitValue.createPercentArray(new float[] { 1.3f, 2f }))
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

                        addPppPoTabela(doc, stavke, grupaPoVpz);

                        // === SEKCIJA 4: FOOTER — odgovorno lice ===
                        addPppPoFooter(doc, poslodavac);
                }
        }

        private static void addPppRow(Table table, String label, String value) {
                com.itextpdf.layout.borders.SolidBorder b = new com.itextpdf.layout.borders.SolidBorder(
                                ColorConstants.BLACK, 0.5f);
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

        /** Zbir iznosa jedne SVP šifre u okviru grupe. */
        private static class SvpSuma {
                BigDecimal bruto = BigDecimal.ZERO, norm = BigDecimal.ZERO, osnovica = BigDecimal.ZERO;
                BigDecimal porez = BigDecimal.ZERO, doprPr = BigDecimal.ZERO, doprIs = BigDecimal.ZERO;
        }

        private static void addPppPoTabela(Document doc, List<Obracun> stavke,
                        Map<String, String> grupaPoVpz) {
                com.itextpdf.layout.borders.SolidBorder bBlack = new com.itextpdf.layout.borders.SolidBorder(
                                ColorConstants.BLACK, 0.5f);
                com.itextpdf.layout.borders.SolidBorder bThick = new com.itextpdf.layout.borders.SolidBorder(
                                ColorConstants.BLACK, 1.5f);

                float[] cols = { 2f, 1f, 1f, 1f, 1f, 1f, 1f };
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
                for (String h : new String[] { "3.1", "3.2", "3.3", "3.4", "3.5", "3.6", "3.7" }) {
                        table.addHeaderCell(new Cell()
                                        .add(new Paragraph(h).setFontSize(8).setFontColor(ColorConstants.BLACK))
                                        .setBackgroundColor(ColorConstants.WHITE)
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setBorder(bBlack));
                }

                BigDecimal sumaBruto = BigDecimal.ZERO, sumaNorm = BigDecimal.ZERO;
                BigDecimal sumaOsnovica = BigDecimal.ZERO, sumaPorez = BigDecimal.ZERO;
                BigDecimal sumaDoprPrimaoca = BigDecimal.ZERO, sumaDoprIsplatioca = BigDecimal.ZERO;

                // Grupisanje: GrupaSVR -> SVP -> zbir
                Map<String, Map<String, SvpSuma>> grupe = new TreeMap<>();
                for (Obracun o : stavke) {
                        String svp3 = o.getSvp3() != null ? o.getSvp3().trim() : "";
                        String grupa = grupaPoVpz != null && grupaPoVpz.get(svp3) != null ? grupaPoVpz.get(svp3) : "";
                        String svp = o.getSvp() != null && !o.getSvp().trim().isEmpty() ? o.getSvp().trim() : svp3;

                        SvpSuma s = grupe.computeIfAbsent(grupa, k -> new TreeMap<>())
                                        .computeIfAbsent(svp, k -> new SvpSuma());
                        BigDecimal doprPr = o.getUkupnoDoprinosiZaposleni();
                        BigDecimal doprIs = o.getUkupnoDoprinosiPoslodavac();
                        s.bruto = add(s.bruto, o.getBruto());
                        s.norm = add(s.norm, o.getNormTroskovi());
                        s.osnovica = add(s.osnovica, o.getOsnovicaPorez());
                        s.porez = add(s.porez, o.getPorez());
                        s.doprPr = s.doprPr.add(doprPr);
                        s.doprIs = s.doprIs.add(doprIs);

                        sumaBruto = add(sumaBruto, o.getBruto());
                        sumaNorm = add(sumaNorm, o.getNormTroskovi());
                        sumaOsnovica = add(sumaOsnovica, o.getOsnovicaPorez());
                        sumaPorez = add(sumaPorez, o.getPorez());
                        sumaDoprPrimaoca = sumaDoprPrimaoca.add(doprPr);
                        sumaDoprIsplatioca = sumaDoprIsplatioca.add(doprIs);
                }

                for (Map.Entry<String, Map<String, SvpSuma>> g : grupe.entrySet()) {
                        // Mergovan red sa nazivom grupe
                        table.addCell(new Cell(1, 7)
                                        .add(new Paragraph(g.getKey()).setFontSize(9).setBold()
                                                        .setFontColor(ColorConstants.BLACK))
                                        .setBackgroundColor(ColorConstants.WHITE)
                                        .setBorder(bBlack));
                        for (Map.Entry<String, SvpSuma> e : g.getValue().entrySet()) {
                                SvpSuma s = e.getValue();
                                table.addCell(new Cell()
                                                .add(new Paragraph(e.getKey()).setFontSize(9)
                                                                .setFontColor(ColorConstants.BLACK)
                                                                .setCharacterSpacing(2f)
                                                                .setTextAlignment(TextAlignment.RIGHT))
                                                .setBackgroundColor(ColorConstants.WHITE)
                                                .setBorder(bBlack));
                                addPppDataCell(table, fmt(s.bruto), TextAlignment.RIGHT, bBlack);
                                addPppDataCell(table, fmt(s.norm), TextAlignment.RIGHT, bBlack);
                                addPppDataCell(table, fmt(s.osnovica), TextAlignment.RIGHT, bBlack);
                                addPppDataCell(table, fmt(s.porez), TextAlignment.RIGHT, bBlack);
                                addPppDataCell(table, fmt(s.doprPr), TextAlignment.RIGHT, bBlack);
                                addPppDataCell(table, fmt(s.doprIs), TextAlignment.RIGHT, bBlack);
                        }
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
                                ? poslodavac.getOdgovornoLice()
                                : "";

                com.itextpdf.layout.borders.SolidBorder b = new com.itextpdf.layout.borders.SolidBorder(
                                ColorConstants.BLACK, 0.5f);

                Table footer = new Table(UnitValue.createPercentArray(new float[] { 1f, 2f }))
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

        /**
         * Generiše potvrde obračuna — jedna strana po stavci obračuna (primaocu).
         *
         * @param outputFile izlazni fajl
         * @param stavke     stavke obračuna (po jedna strana svaka)
         * @param nazivPoVpz mapa SVP-3 (VPZ) -> naziv vrste prihoda
         * @param primaoci   mapa IDZaposleni -> primalac (za adresu)
         * @param poslodavac isplatilac prihoda (može biti null)
         */
        public static void generatePotvrdeObracuna(File outputFile,
                        List<Obracun> stavke,
                        Map<String, String> nazivPoVpz,
                        Map<Integer, Primalac> primaoci,
                        Poslodavac poslodavac) throws Exception {
                generatePotvrdeObracuna(outputFile, stavke, nazivPoVpz, primaoci, poslodavac, POTVRDA_001);
        }

        /** Varijante potvrde (vrednost podešavanja PotvrdeReport). */
        public static final String POTVRDA_001 = "Potvrda_001";
        /** Dvojezična (srpski/engleski) varijanta, bez potpisa primaoca. */
        public static final String POTVRDA_002 = "Potvrda_002";

        /**
         * Isto kao {@link #generatePotvrdeObracuna(File, List, Map, Map, Poslodavac)},
         * uz izbor
         * varijante potvrde; nepoznata ili prazna vrednost znači {@link #POTVRDA_001}.
         *
         * @param varijanta Potvrda_001 ili Potvrda_002
         */
        public static void generatePotvrdeObracuna(File outputFile,
                        List<Obracun> stavke,
                        Map<String, String> nazivPoVpz,
                        Map<Integer, Primalac> primaoci,
                        Poslodavac poslodavac,
                        String varijanta) throws Exception {
                generatePotvrdeObracuna(outputFile, stavke, nazivPoVpz, primaoci, poslodavac, varijanta, null);
        }

        /**
         * @param definicije mapa ID_Obracun -> definicija obračuna (datum plaćanja,
         *                   mesec,
         *                   naziv za naslov Potvrda_002); može biti null
         */
        public static void generatePotvrdeObracuna(File outputFile,
                        List<Obracun> stavke,
                        Map<String, String> nazivPoVpz,
                        Map<Integer, Primalac> primaoci,
                        Poslodavac poslodavac,
                        String varijanta,
                        Map<Integer, ObracunDefinicija> definicije) throws Exception {
                boolean v2 = varijanta != null && POTVRDA_002.equalsIgnoreCase(varijanta.trim());
                try (PdfWriter writer = new PdfWriter(outputFile);
                                PdfDocument pdf = new PdfDocument(writer);
                                Document doc = new Document(pdf, PageSize.A4)) {

                        doc.setMargins(36, 50, 36, 50);
                        PdfFont font = loadPdfFont();
                        if (font != null)
                                doc.setFont(font);

                        boolean prva = true;
                        for (Obracun o : stavke) {
                                if (!prva)
                                        doc.add(new com.itextpdf.layout.element.AreaBreak(
                                                        com.itextpdf.layout.properties.AreaBreakType.NEXT_PAGE));
                                prva = false;
                                ObracunDefinicija def = definicije != null ? definicije.get(o.getLnkObracun()) : null;
                                addPotvrdaStrana(doc, o, nazivPoVpz, primaoci.get(o.getLnkZap()), poslodavac, v2, def);
                        }
                }
        }

        /**
         * Natpis potvrde: u dvojezičnoj varijanti "srpski /english", inače samo srpski.
         */
        private static String bi(boolean v2, String sr, String en) {
                return v2 ? sr + " /" + en : sr;
        }

        private static void addPotvrdaStrana(Document doc, Obracun o, Map<String, String> nazivPoVpz,
                        Primalac primalac, Poslodavac poslodavac, boolean v2, ObracunDefinicija def) {
                com.itextpdf.layout.borders.Border none = com.itextpdf.layout.borders.Border.NO_BORDER;
                com.itextpdf.layout.borders.Border box = new com.itextpdf.layout.borders.SolidBorder(
                                ColorConstants.BLACK, 1f);
                com.itextpdf.layout.borders.Border hair = new com.itextpdf.layout.borders.SolidBorder(
                                ColorConstants.BLACK, 0.5f);
                String poslodavacNaziv = poslodavac != null ? nvl(poslodavac.getNazivPrezimeIme()) : "";
                String nazivPrihoda = nvl(nazivPoVpz.get(o.getSvp3()));
                if (nazivPrihoda.isEmpty())
                        nazivPrihoda = nvl(o.getSvp3());

                // Zaglavlje: naziv isplatioca desno, linija ispod
                doc.add(new Paragraph(poslodavacNaziv).setFontSize(9)
                                .setTextAlignment(TextAlignment.RIGHT)
                                .setBorderBottom(hair).setMarginBottom(v2 ? 14 : 24));

                if (v2) {
                        // Prvi naslov: srpski i engleski, u dva reda, manjim fontom
                        doc.add(new Paragraph("Potvrda o obračunatom porezu i doprinosima na druge prihode")
                                        .setFontSize(12).setBold()
                                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
                        doc.add(new Paragraph("Certificate of Tax and Contributions Withholding on Other Income")
                                        .setFontSize(12).setBold()
                                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(14));

                }

                // Naslov (Potvrda_001), odnosno drugi naslov (Potvrda_002): naziv prihoda
                doc.add(new Paragraph(nazivPrihoda).setFontSize(12).setBold()
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(36));

                // Primalac prihoda / Poslodavac
                String adresa = primalac != null ? nvl(primalac.getAdresaP()) : "";
                String grad = primalac != null ? nvl(primalac.getPbGradP()) : "";
                String jmbg = nvl(o.getIdentifikatorPrimaoca());
                if (jmbg.isEmpty() && primalac != null)
                        jmbg = nvl(primalac.getMb());

                Cell levo = new Cell().setBorder(none)
                                .add(boldIf(v2, new Paragraph("Primalac prihoda /Recipient of the fee").setFontSize(9)
                                                .setMarginBottom(8)))
                                .add(new Paragraph((nvl(o.getPrezime()) + " " + nvl(o.getIme())).trim())
                                                .setFontSize(10).setMarginLeft(14).setMarginBottom(0))
                                .add(new Paragraph(adresa).setFontSize(10).setMarginLeft(14).setMarginBottom(0))
                                .add(new Paragraph(grad).setFontSize(10).setMarginLeft(14).setMarginBottom(0))
                                .add(new Paragraph("JMBG:   " + jmbg).setFontSize(10).setMarginLeft(14)
                                                .setMarginBottom(0));

                // Blok "Poslodavac" je pomeren udesno za 2 cm (1 cm = 28,35 pt)
                Cell desno = new Cell().setBorder(none).setPaddingLeft(2 * 28.35f)
                                .add(boldIf(v2, new Paragraph("Poslodavac /Employer").setFontSize(9)
                                                .setMarginBottom(8)))
                                .add(new Paragraph(poslodavacNaziv).setFontSize(10).setMarginLeft(14)
                                                .setMarginBottom(4))
                                .add(new Paragraph(poslodavac != null ? nvl(poslodavac.getUlicaIBroj()) : "")
                                                .setFontSize(10).setMarginLeft(14).setMarginBottom(0))
                                .add(new Paragraph(poslodavac != null ? nvl(poslodavac.getGrad()) : "")
                                                .setFontSize(10).setMarginLeft(14));
                Table adrese = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(36);
                adrese.addCell(levo);
                adrese.addCell(desno);
                doc.add(adrese);

                // Vrsta obračuna: Neto / Bruto
                float[] w = { 5, 2, 2 };
                Table vrsta = new Table(UnitValue.createPercentArray(w)).setWidth(UnitValue.createPercentValue(100))
                                .setBorder(box);
                vrsta.addCell(potvrdaCell(bi(v2, "Vrsta obračuna", "Type of calculation"), TextAlignment.LEFT, true,
                                box, 0));
                vrsta.addCell(potvrdaCell(bi(v2, "Neto", "Net amount"), TextAlignment.RIGHT, false, box, 0));
                vrsta.addCell(potvrdaCell(bi(v2, "Bruto", "Gross amount"), TextAlignment.RIGHT, false, box, 0));
                doc.add(vrsta);

                Table stavka = new Table(UnitValue.createPercentArray(w)).setWidth(UnitValue.createPercentValue(100));
                stavka.addCell(potvrdaCell(nazivPrihoda, TextAlignment.LEFT, false, none, 0.5f));
                stavka.addCell(potvrdaCell(fmtDin(o.getNeto()), TextAlignment.RIGHT, false, none, 0.5f));
                stavka.addCell(potvrdaCell(fmtDin(o.getBruto()), TextAlignment.RIGHT, false, none, 0.5f));
                stavka.addCell(potvrdaCell(bi(v2, "Ukupno", "Total"), TextAlignment.LEFT, true, none, 0));
                stavka.addCell(potvrdaCell(fmtDin(o.getNeto()), TextAlignment.RIGHT, true, none, 0));
                stavka.addCell(potvrdaCell(fmtDin(o.getBruto()), TextAlignment.RIGHT, true, none, 0));
                doc.add(stavka.setMarginBottom(36));

                // Doprinosi na teret zaposlenog — samo ako postoje
                BigDecimal zPio = o.getzPio() != null ? o.getzPio() : BigDecimal.ZERO;
                BigDecimal zZdr = o.getzZdr() != null ? o.getzZdr() : BigDecimal.ZERO;
                boolean imaDoprinose = zPio.signum() != 0 || zZdr.signum() != 0;
                BigDecimal porez = o.getPorez() != null ? o.getPorez() : BigDecimal.ZERO;

                float[] w4 = { 4, 2, 2, 2 };
                if (imaDoprinose) {
                        Table dh = new Table(UnitValue.createPercentArray(w4))
                                        .setWidth(UnitValue.createPercentValue(100))
                                        .setBorder(box);
                        dh.addCell(potvrdaCell(bi(v2, "Doprinosi ", "Contributions"),
                                        TextAlignment.LEFT, true, box, 0));
                        dh.addCell(potvrdaCell(bi(v2, "Osnovica", "Base"), TextAlignment.RIGHT, false, box, 0));
                        dh.addCell(potvrdaCell(v2 ? "%" : "Procenat", TextAlignment.RIGHT, false, box, 0));
                        dh.addCell(potvrdaCell(bi(v2, "Iznos", "Amount"), TextAlignment.RIGHT, false, box, 0));
                        doc.add(dh);

                        Table d = new Table(UnitValue.createPercentArray(w4))
                                        .setWidth(UnitValue.createPercentValue(100));
                        addPotvrdaRed(d, bi(v2, "PIO", "Pension insurance"),
                                        o.getOsnovicaDoprinosi(), o.getzPio(), false);
                        addPotvrdaRed(d, bi(v2, "Zdravstveno osiguranje",
                                        "Health insurance"), o.getOsnovicaDoprinosi(), o.getzZdr(), false);
                        // Linija iznad ukupnog reda (isto kao ispred "Ukupno" u tabeli Vrsta obračuna)
                        d.addCell(potvrdaCell(bi(v2, "Ukupno doprinosi ",
                                        "Total contributions"), TextAlignment.LEFT, true, none,
                                        0).setBorderTop(hair));
                        d.addCell(potvrdaCell("", TextAlignment.RIGHT, false, none, 0).setBorderTop(hair));
                        d.addCell(potvrdaCell("", TextAlignment.RIGHT, false, none, 0).setBorderTop(hair));
                        d.addCell(potvrdaCell(fmtDin(zPio.add(zZdr)), TextAlignment.RIGHT, true, none, 0)
                                        .setBorderTop(hair));
                        doc.add(d.setMarginBottom(24));
                }

                // Porez
                Table ph = new Table(UnitValue.createPercentArray(w4)).setWidth(UnitValue.createPercentValue(100))
                                .setBorder(box);
                ph.addCell(potvrdaCell(bi(v2, "Porez", "Tax"), TextAlignment.LEFT, true, box, 0));
                ph.addCell(potvrdaCell(bi(v2, "Osnovica", "Base"), TextAlignment.RIGHT, false, box, 0));
                ph.addCell(potvrdaCell(v2 ? "%" : "Procenat", TextAlignment.RIGHT, false, box, 0));
                ph.addCell(potvrdaCell(bi(v2, "Iznos", "Amount"), TextAlignment.RIGHT, false, box, 0));
                doc.add(ph);

                Table p = new Table(UnitValue.createPercentArray(w4)).setWidth(UnitValue.createPercentValue(100));
                p.addCell(potvrdaCell(bi(v2, "Neoprezivi iznos", "Non-taxable amount"), TextAlignment.LEFT, false,
                                none, 0));
                p.addCell(potvrdaCell("", TextAlignment.RIGHT, false, none, 0));
                p.addCell(potvrdaCell("", TextAlignment.RIGHT, false, none, 0));
                p.addCell(potvrdaCell(fmtDin(o.getPoreskoOslobodjenje()), TextAlignment.RIGHT, false, none, 0));
                addPotvrdaRed(p, bi(v2, "Porez na prihode", "Income tax"), o.getOsnovicaPorez(), o.getPorez(), true);
                doc.add(p.setMarginBottom(18));

                // Ukupni troškovi
                Table uk = new Table(UnitValue.createPercentArray(new float[] { 7, 2 }))
                                .setWidth(UnitValue.createPercentValue(100)).setBorder(box).setMarginBottom(24);
                uk.addCell(potvrdaCell(
                                v2 ? "Ukupno troškovi obračuna doprinosa i poreza /Total costs of calculation of contributions and taxes"
                                                : "Ukupno troškovi obračuna doprinosa i poreza",
                                TextAlignment.LEFT, true, box, 0));
                uk.addCell(potvrdaCell(fmtDin(zPio.add(zZdr).add(porez)), TextAlignment.RIGHT, true, box, 0));
                doc.add(uk);

                // Potpisi
                Table potpis;
                potpis = new Table(UnitValue.createPercentArray(new float[] { 1 }))
                                .setWidth(UnitValue.createPercentValue(40));
                potpis.addCell(new Cell().setBorder(none).setTextAlignment(TextAlignment.LEFT)
                                .add(new Paragraph("OBRAČUNAO").setFontSize(10).setMarginBottom(12))
                                .add(new Paragraph("________________________").setFontSize(10)));
                doc.add(potvrdaSpacer());
                doc.add(potpis);
        }

        private static Paragraph boldIf(boolean bold, Paragraph p) {
                return bold ? p.setBold() : p;
        }

        private static Paragraph potvrdaSpacer() {
                return new Paragraph(" ").setFontSize(4);
        }

        /**
         * Red sa osnovicom, procentom (iz iznosa/osnovice) i iznosom; iznos podebljan
         * ako je {@code bold}.
         */
        private static void addPotvrdaRed(Table t, String naziv, BigDecimal osnovica, BigDecimal iznos, boolean bold) {
                com.itextpdf.layout.borders.Border none = com.itextpdf.layout.borders.Border.NO_BORDER;
                String procenat = "";
                if (osnovica != null && iznos != null && osnovica.signum() != 0) {
                        procenat = rs.hbs.zarade.util.NumberFormats.money(
                                        iznos.multiply(BigDecimal.valueOf(100)).divide(osnovica, 2,
                                                        java.math.RoundingMode.HALF_UP))
                                        + "%";
                }
                t.addCell(potvrdaCell(naziv, TextAlignment.LEFT, bold, none, 0));
                t.addCell(potvrdaCell(osnovica != null ? fmtDin(osnovica) : "", TextAlignment.RIGHT, false, none, 0));
                t.addCell(potvrdaCell(procenat, TextAlignment.RIGHT, false, none, 0));
                t.addCell(potvrdaCell(fmtDin(iznos), TextAlignment.RIGHT, bold, none, 0));
        }

        /**
         * Ćelija potvrde bez sopstvenog okvira; {@code donjaLinija} > 0 crta donju
         * liniju te debljine.
         * Okvir kutije (zaglavlja) crta se na nivou tabele, pa se {@code border} ovde
         * ne koristi.
         */
        private static Cell potvrdaCell(String text, TextAlignment align, boolean bold,
                        com.itextpdf.layout.borders.Border border, float donjaLinija) {
                Paragraph par = new Paragraph(text).setFontSize(9).setTextAlignment(align);
                if (bold)
                        par.setBold();
                Cell c = new Cell().add(par).setPadding(5).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
                if (donjaLinija > 0) {
                        c.setBorderBottom(
                                        new com.itextpdf.layout.borders.SolidBorder(ColorConstants.BLACK, donjaLinija));
                }
                return c;
        }

        private static String fmtDin(BigDecimal v) {
                return fmt(v) + " Din.";
        }

        private static String buildAdresaPrimaoca(Primalac primalac, String opstinaNaziv) {
                StringBuilder sb = new StringBuilder();
                if (primalac.getAdresaP() != null && !primalac.getAdresaP().isEmpty())
                        sb.append(primalac.getAdresaP());
                if (primalac.getPbGradP() != null && !primalac.getPbGradP().isEmpty()) {
                        if (sb.length() > 0)
                                sb.append(", ");
                        sb.append(primalac.getPbGradP());
                }
                if (opstinaNaziv != null && !opstinaNaziv.isEmpty()) {
                        if (sb.length() > 0)
                                sb.append(", ");
                        sb.append(opstinaNaziv);
                }
                return sb.toString();
        }

        /**
         * Učitava Arial font koji podržava srpska slova (ć, č, š, đ, ž).
         * Arial je uvek prisutan na Windows sistemima.
         * Ako nije dostupan, vraća null i tekst ostaje sa defaultnim fontom (bez
         * srpskih slova).
         */
        private static PdfFont loadPdfFont() {
                try {
                        return PdfFontFactory.createFont(
                                        "C:\\Windows\\Fonts\\arial.ttf",
                                        "Identity-H",
                                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                } catch (Exception e) {
                        System.err.println(
                                        "Upozorenje: Arial font nije dostupan, srpski znakovi možda neće biti ispravni. "
                                                        + e.getMessage());
                        return null;
                }
        }

        private static String nvl(String s) {
                return s != null ? s : "";
        }

        private static String fmt(BigDecimal v) {
                return v != null ? rs.hbs.zarade.util.NumberFormats.money(v) : "0,00";
        }

        private static BigDecimal add(BigDecimal a, BigDecimal b) {
                return b != null ? a.add(b) : a;
        }
}
