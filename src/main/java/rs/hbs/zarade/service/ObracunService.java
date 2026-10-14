package rs.hbs.zarade.service;

import rs.hbs.zarade.domain.Obracun;
import rs.hbs.zarade.domain.ObracunStope;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Servis za obračun poreza i doprinosa.
 *
 * KRITIČNO: Tačnost obračuna je apsolutni prioritet.
 * Sve formule su preuzete direktno iz legacy SQL upita.
 *
 * Tipovi obračuna (SVP-3):
 * - 101, 206, 204, 202 (SVP-2=01): Zarada - sa poreskim oslobođenjem
 * - 101 (SVP-2=09): Penzioneri - samo PIO doprinosi
 * - 405: Autorski honorar - samo porez, bez doprinosa
 * - 305, 601, 605: Ugovori o delu - normirani troškovi + PIO + ZDR
 *
 * Pravilo zaokruživanja: na 2 decimale nakon obračuna.
 */
public class ObracunService {

    private static final int SCALE = 10; // Preciznost za međukalkulacije
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Izračunava kompletan obračun na osnovu NETO iznosa i stopa.
     *
     * @param neto NETO iznos za isplatu
     * @param stope poreske stope i stope doprinosa
     * @param procenat procenat zaposlenja (1.0 = 100%)
     * @param svp2 šifra SVP-2 (npr. "01", "09")
     * @param svp3 šifra SVP-3 (npr. "101", "405", "305")
     * @return popunjen Obracun objekat sa svim izračunatim vrednostima
     */
    public Obracun izracunaj(BigDecimal neto, ObracunStope stope, BigDecimal procenat,
                             String svp2, String svp3) {
        Obracun o = new Obracun();
        o.setNeto(neto);
        o.setProcenat(procenat);
        o.setSvp2(svp2);
        o.setSvp3(svp3);

        // Odredi tip obračuna i primeni odgovarajuću formulu
        TipObracuna tip = odrediTipObracuna(svp2, svp3);

        switch (tip) {
            case ZARADA_STANDARD:
                izracunajZaraduStandard(o, stope);
                break;
            case ZARADA_PENZIONER:
                izracunajZaraduPenzioner(o, stope);
                break;
            case AUTORSKI_HONORAR:
                izracunajAutorskiHonorar(o, stope);
                break;
            case UGOVOR_O_DELU:
                izracunajUgovorODelu(o, stope);
                break;
            default:
                throw new IllegalArgumentException("Nepoznat tip obračuna za SVP-2=" + svp2 + ", SVP-3=" + svp3);
        }

        // Zaokruži sve vrednosti na cele dinare
        zaokruzi(o);

        o.setObracunato(LocalDateTime.now());
        return o;
    }

    /**
     * Tip 101, 206, 204, 202 sa SVP-2=01
     * Formula iz OBRACUN_UPD_101:
     * Bruto = (NETO - P_OSL * POREZ_P * PROCENAT) / (1 - (POREZ_P + PIO_Z + ZDR_Z + NEZ_Z))
     */
    private void izracunajZaraduStandard(Obracun o, ObracunStope stope) {
        BigDecimal neto = o.getNeto();
        BigDecimal procenat = o.getProcenat() != null ? o.getProcenat() : BigDecimal.ONE;

        // Stope
        BigDecimal porezP = bd(stope.getPorezP());
        BigDecimal pioZ = bd(stope.getPioZ());
        BigDecimal zdrZ = bd(stope.getZdrZ());
        BigDecimal nezZ = bd(stope.getNezZ());
        BigDecimal pioP = bd(stope.getPioP());
        BigDecimal zdrP = bd(stope.getZdrP());
        BigDecimal nezP = bd(stope.getNezP());
        BigDecimal pOsl = stope.getpOsl() != null ? stope.getpOsl() : BigDecimal.ZERO;

        // Koeficijent za bruto
        // koef = 1 - (POREZ_P + PIO_Z + ZDR_Z + NEZ_Z)
        BigDecimal koef = BigDecimal.ONE
                .subtract(porezP)
                .subtract(pioZ)
                .subtract(zdrZ)
                .subtract(nezZ);

        // Bruto = (NETO - P_OSL * POREZ_P * PROCENAT) / koef
        BigDecimal pOslKomponenta = pOsl.multiply(porezP).multiply(procenat);
        BigDecimal bruto = neto.subtract(pOslKomponenta)
                .divide(koef, SCALE, ROUNDING);

        // Osnovica za porez = Bruto - P_OSL * PROCENAT
        BigDecimal osnovicaPorez = bruto.subtract(pOsl.multiply(procenat));

        // Osnovica za doprinose = Bruto
        BigDecimal osnovicaDoprinosi = bruto;

        // Porez = OsnovicaPorez * POREZ_P
        BigDecimal porez = osnovicaPorez.multiply(porezP);

        // Doprinosi - ukupni
        BigDecimal pio = osnovicaDoprinosi.multiply(pioZ.add(pioP));
        BigDecimal zdr = osnovicaDoprinosi.multiply(zdrZ.add(zdrP));
        BigDecimal nez = osnovicaDoprinosi.multiply(nezZ.add(nezP));

        // Doprinosi - na teret poslodavca
        BigDecimal pPio = osnovicaDoprinosi.multiply(pioP);
        BigDecimal pZdr = osnovicaDoprinosi.multiply(zdrP);
        BigDecimal pNez = osnovicaDoprinosi.multiply(nezP);

        // Doprinosi - na teret zaposlenog
        BigDecimal zPio = osnovicaDoprinosi.multiply(pioZ);
        BigDecimal zZdr = osnovicaDoprinosi.multiply(zdrZ);
        BigDecimal zNez = osnovicaDoprinosi.multiply(nezZ);

        // Popuni obračun
        o.setBruto(bruto);
        o.setOsnovicaPorez(osnovicaPorez);
        o.setOsnovicaDoprinosi(osnovicaDoprinosi);
        o.setPorez(porez);
        o.setPoreskoOslobodjenje(pOsl);
        o.setPio(pio);
        o.setZdr(zdr);
        o.setNez(nez);
        o.setpPio(pPio);
        o.setpZdr(pZdr);
        o.setpNez(pNez);
        o.setzPio(zPio);
        o.setzZdr(zZdr);
        o.setzNez(zNez);
    }

    /**
     * Tip 101 sa SVP-2=09 (penzioneri)
     * Formula iz OBRACUN_UPD_101_09:
     * Bruto = (NETO - P_OSL * POREZ_P * PROCENAT) / (1 - (POREZ_P + PIO_Z))
     * Nema ZDR i NEZ doprinosa
     */
    private void izracunajZaraduPenzioner(Obracun o, ObracunStope stope) {
        BigDecimal neto = o.getNeto();
        BigDecimal procenat = o.getProcenat() != null ? o.getProcenat() : BigDecimal.ONE;

        BigDecimal porezP = bd(stope.getPorezP());
        BigDecimal pioZ = bd(stope.getPioZ());
        BigDecimal pioP = bd(stope.getPioP());
        BigDecimal pOsl = stope.getpOsl() != null ? stope.getpOsl() : BigDecimal.ZERO;

        // koef = 1 - (POREZ_P + PIO_Z)
        BigDecimal koef = BigDecimal.ONE.subtract(porezP).subtract(pioZ);

        // Bruto = (NETO - P_OSL * POREZ_P * PROCENAT) / koef
        BigDecimal pOslKomponenta = pOsl.multiply(porezP).multiply(procenat);
        BigDecimal bruto = neto.subtract(pOslKomponenta)
                .divide(koef, SCALE, ROUNDING);

        BigDecimal osnovicaPorez = bruto.subtract(pOsl.multiply(procenat));
        BigDecimal osnovicaDoprinosi = bruto;
        BigDecimal porez = osnovicaPorez.multiply(porezP);

        // Samo PIO doprinosi
        BigDecimal pio = osnovicaDoprinosi.multiply(pioZ.add(pioP));
        BigDecimal pPio = osnovicaDoprinosi.multiply(pioP);
        BigDecimal zPio = osnovicaDoprinosi.multiply(pioZ);

        o.setBruto(bruto);
        o.setOsnovicaPorez(osnovicaPorez);
        o.setOsnovicaDoprinosi(osnovicaDoprinosi);
        o.setPorez(porez);
        o.setPoreskoOslobodjenje(pOsl);
        o.setPio(pio);
        o.setZdr(BigDecimal.ZERO);
        o.setNez(BigDecimal.ZERO);
        o.setpPio(pPio);
        o.setpZdr(BigDecimal.ZERO);
        o.setpNez(BigDecimal.ZERO);
        o.setzPio(zPio);
        o.setzZdr(BigDecimal.ZERO);
        o.setzNez(BigDecimal.ZERO);
    }

    /**
     * Tip 405 - Autorski honorar
     * Formula iz OBRACUN_UPD_405:
     * Bruto = NETO / (1 - (1 - NORM_TROSAK) * POREZ_P)
     * Nema doprinosa
     */
    private void izracunajAutorskiHonorar(Obracun o, ObracunStope stope) {
        BigDecimal neto = o.getNeto();

        BigDecimal porezP = bd(stope.getPorezP());
        BigDecimal normTrosak = bd(stope.getNormTrosak());

        // koef = 1 - (1 - NORM_TROSAK) * POREZ_P
        BigDecimal koef = BigDecimal.ONE.subtract(
                BigDecimal.ONE.subtract(normTrosak).multiply(porezP)
        );

        // Bruto = NETO / koef
        BigDecimal bruto = neto.divide(koef, SCALE, ROUNDING);

        // Normirani troškovi = Bruto * NORM_TROSAK
        BigDecimal normTroskovi = bruto.multiply(normTrosak);

        // Osnovica za porez = Bruto - Normirani troškovi
        BigDecimal osnovicaPorez = bruto.subtract(normTroskovi);

        // Porez = OsnovicaPorez * POREZ_P
        BigDecimal porez = osnovicaPorez.multiply(porezP);

        o.setBruto(bruto);
        o.setNormTroskovi(normTroskovi);
        o.setOsnovicaPorez(osnovicaPorez);
        o.setOsnovicaDoprinosi(BigDecimal.ZERO);
        o.setPorez(porez);
        o.setPio(BigDecimal.ZERO);
        o.setZdr(BigDecimal.ZERO);
        o.setNez(BigDecimal.ZERO);
        o.setpPio(BigDecimal.ZERO);
        o.setpZdr(BigDecimal.ZERO);
        o.setpNez(BigDecimal.ZERO);
        o.setzPio(BigDecimal.ZERO);
        o.setzZdr(BigDecimal.ZERO);
        o.setzNez(BigDecimal.ZERO);
    }

    /**
     * Tipovi 305, 601, 605 i drugi sa normiranim troškovima i doprinosima
     * Formula iz OBRACUN_UPD_4X_6X:
     * Bruto = NETO / (1 - (1 - NORM_TROSAK) * POREZ_P - (1 - NORM_TROSAK) * (PIO_P + PIO_Z + ZDR_P + ZDR_Z))
     *
     * NAPOMENA: Za ugovor o delu, SVI doprinosi (i na teret primaoca i na teret isplatioca)
     * se obračunavaju na osnovicu i oduzimaju od bruto iznosa da bi se dobio neto.
     * Ovo je specifično za ovaj tip ugovora u Srbiji.
     */
    private void izracunajUgovorODelu(Obracun o, ObracunStope stope) {
        BigDecimal neto = o.getNeto();

        BigDecimal porezP = bd(stope.getPorezP());
        BigDecimal normTrosak = bd(stope.getNormTrosak());
        BigDecimal pioZ = bd(stope.getPioZ());
        BigDecimal zdrZ = bd(stope.getZdrZ());
        BigDecimal pioP = bd(stope.getPioP());
        BigDecimal zdrP = bd(stope.getZdrP());

        // (1 - NORM_TROSAK)
        BigDecimal jedanMinusNorm = BigDecimal.ONE.subtract(normTrosak);

        // Ukupni doprinosi za koeficijent (PIO + ZDR, svi - i P i Z)
        // Za ugovor o delu, svi doprinosi ulaze u koeficijent
        BigDecimal ukupnoDoprinosi = pioP.add(pioZ).add(zdrP).add(zdrZ);

        // koef = 1 - (1 - NORM_TROSAK) * POREZ_P - (1 - NORM_TROSAK) * ukupnoDoprinosi
        BigDecimal koef = BigDecimal.ONE
                .subtract(jedanMinusNorm.multiply(porezP))
                .subtract(jedanMinusNorm.multiply(ukupnoDoprinosi));

        // Bruto = NETO / koef
        BigDecimal bruto = neto.divide(koef, SCALE, ROUNDING);

        // Normirani troškovi = Bruto * NORM_TROSAK
        BigDecimal normTroskovi = bruto.multiply(normTrosak);

        // Osnovica za porez i doprinose = Bruto - Normirani troškovi
        BigDecimal osnovicaPorez = bruto.subtract(normTroskovi);
        BigDecimal osnovicaDoprinosi = osnovicaPorez;

        // Porez
        BigDecimal porez = osnovicaPorez.multiply(porezP);

        // Doprinosi - ukupni (nema NEZ za ovaj tip)
        BigDecimal pio = osnovicaDoprinosi.multiply(pioZ.add(pioP));
        BigDecimal zdr = osnovicaDoprinosi.multiply(zdrZ.add(zdrP));

        // Doprinosi - razdvojeno
        BigDecimal pPio = osnovicaDoprinosi.multiply(pioP);
        BigDecimal pZdr = osnovicaDoprinosi.multiply(zdrP);
        BigDecimal zPio = osnovicaDoprinosi.multiply(pioZ);
        BigDecimal zZdr = osnovicaDoprinosi.multiply(zdrZ);

        o.setBruto(bruto);
        o.setNormTroskovi(normTroskovi);
        o.setOsnovicaPorez(osnovicaPorez);
        o.setOsnovicaDoprinosi(osnovicaDoprinosi);
        o.setPorez(porez);
        o.setPio(pio);
        o.setZdr(zdr);
        o.setNez(BigDecimal.ZERO);
        o.setpPio(pPio);
        o.setpZdr(pZdr);
        o.setpNez(BigDecimal.ZERO);
        o.setzPio(zPio);
        o.setzZdr(zZdr);
        o.setzNez(BigDecimal.ZERO);
    }

    /**
     * Zaokružuje sve novčane vrednosti na 2 decimale.
     * Iz OBRACUN_ZAOKRUZIVANJE upita.
     */
    private void zaokruzi(Obracun o) {
        o.setBruto(round(o.getBruto()));
        o.setNormTroskovi(round(o.getNormTroskovi()));
        o.setOsnovicaPorez(round(o.getOsnovicaPorez()));
        o.setOsnovicaDoprinosi(round(o.getOsnovicaDoprinosi()));
        o.setPorez(round(o.getPorez()));
        o.setPio(round(o.getPio()));
        o.setZdr(round(o.getZdr()));
        o.setNez(round(o.getNez()));
        o.setpPio(round(o.getpPio()));
        o.setpZdr(round(o.getpZdr()));
        o.setpNez(round(o.getpNez()));
        o.setzPio(round(o.getzPio()));
        o.setzZdr(round(o.getzZdr()));
        o.setzNez(round(o.getzNez()));
    }

    /**
     * Određuje tip obračuna na osnovu SVP šifara.
     */
    private TipObracuna odrediTipObracuna(String svp2, String svp3) {
        if (svp3 == null) {
            throw new IllegalArgumentException("SVP-3 ne može biti null");
        }

        // Tip 405 - Autorski honorar (samo porez)
        if ("405".equals(svp3)) {
            return TipObracuna.AUTORSKI_HONORAR;
        }

        // Tip 101 sa SVP-2=09 - Penzioneri
        if ("101".equals(svp3) && "09".equals(svp2)) {
            return TipObracuna.ZARADA_PENZIONER;
        }

        // Tipovi 101, 206, 204, 202 sa SVP-2=01 - Standardna zarada
        if (("101".equals(svp3) || "206".equals(svp3) || "204".equals(svp3) || "202".equals(svp3))
                && "01".equals(svp2)) {
            return TipObracuna.ZARADA_STANDARD;
        }

        // Tipovi 305, 601, 605 i drugi - Ugovor o delu
        if ("305".equals(svp3) || "601".equals(svp3) || "605".equals(svp3)) {
            return TipObracuna.UGOVOR_O_DELU;
        }

        // Default: Ugovor o delu (koristi formulu sa normiranim troškovima)
        return TipObracuna.UGOVOR_O_DELU;
    }

    /**
     * Konvertuje Double u BigDecimal, sa null-safe obradom.
     */
    private BigDecimal bd(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }

    /**
     * Zaokružuje na 2 decimale, null-safe.
     */
    private BigDecimal round(BigDecimal value) {
        return value != null ? value.setScale(2, ROUNDING) : null;
    }

    /**
     * Enum za tipove obračuna.
     */
    public enum TipObracuna {
        ZARADA_STANDARD,    // 101, 206, 204, 202 sa SVP-2=01
        ZARADA_PENZIONER,   // 101 sa SVP-2=09
        AUTORSKI_HONORAR,   // 405
        UGOVOR_O_DELU       // 305, 601, 605 i drugi
    }
}
