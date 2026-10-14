package rs.hbs.zarade.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pojedinacni obracun za jednog primaoca prihoda.
 *
 * Mapira se na tabelu: Obracun (37 kolona)
 * Baza: data_zarade.mdb (Main DB) ili obracun-honorara.mdb (Calc DB)
 *
 * Ovo je CENTRALNA tabela sistema - sadrzi sve izracunate vrednosti:
 * - NETO (ulazni podatak)
 * - BRUTO (izracunat)
 * - Porezi i doprinosi (izracunati)
 *
 * VAZNO: Svi novcani iznosi su BigDecimal zbog tacnosti!
 */
public class Obracun {

    // === PRIMARNI KLJUC ===
    private Integer idPobracun;                     // IDpobracun [PK]

    // === VEZE (FOREIGN KEYS) ===
    private Integer lnkObracun;                     // lnkObracun -> ObracunDef.ID_Obracun
    private Integer lnkZap;                         // lnkZap -> Primaoci.IDZaposleni

    // === PODACI O PRIMAOCU (denormalizovano) ===
    private String prezime;                         // Prezime
    private String ime;                             // Ime
    private String vrstaIdentifikatoraPrimaoca;     // VrstaIdentifikatoraPrimaoca, VARCHAR(1)
    private String identifikatorPrimaoca;           // IdentifikatorPrimaoca (JMBG), VARCHAR(13)
    private String oznakaPrebivalista;              // OznakaPrebivalista, VARCHAR(3)

    // === SVP - SIFRA VRSTE PRIHODA ===
    private String svp;                             // SVP - kompletna sifra, VARCHAR(9)
    private String svp1;                            // SVP-1, VARCHAR(1)
    private String svp2;                            // SVP-2 - grupa primaoca (npr. "01", "09"), VARCHAR(2)
    private String svp3;                            // SVP-3 - vrsta prihoda (npr. "101", "405"), VARCHAR(3)
    private String svp4;                            // SVP-4, VARCHAR(2)
    private String svp5;                            // SVP-5, VARCHAR(1)

    // === RADNO VREME ===
    private Integer brojKalendarskihDana;           // BrojKalendarskihDana
    private Integer mesecniFondSati;                // MesecniFondSati
    private Double brojEfektivnihSati;              // BrojEfektivnihSati
    private BigDecimal procenat;                    // PROCENAT (0.0 - 1.0)

    // === FINANSIJSKI PODACI - ULAZ ===
    private BigDecimal neto;                        // NETO - ulazna vrednost

    // === FINANSIJSKI PODACI - IZRACUNATO ===
    private BigDecimal bruto;                       // Bruto
    private BigDecimal normTroskovi;                // NORM_TROSKOVI - normirani troskovi

    // === OSNOVICE ===
    private BigDecimal osnovicaPorez;               // OsnovicaPorez
    private BigDecimal osnovicaDoprinosi;           // OsnovicaDoprinosi

    // === POREZ ===
    private BigDecimal porez;                       // Porez
    private BigDecimal poreskoOslobodjenje;         // PoreskoOslobodjenje

    // === DOPRINOSI - UKUPNI ===
    private BigDecimal pio;                         // PIO - ukupno
    private BigDecimal zdr;                         // ZDR - zdravstveno ukupno
    private BigDecimal nez;                         // NEZ - nezaposlenost ukupno
    private BigDecimal pioBen;                      // PIOBen - PIO beneficirani staz

    // === DOPRINOSI - NA TERET POSLODAVCA (P_*) ===
    private BigDecimal pPio;                        // P_PIO
    private BigDecimal pZdr;                        // P_ZDR
    private BigDecimal pNez;                        // P_NEZ

    // === DOPRINOSI - NA TERET ZAPOSLENOG (Z_*) ===
    private BigDecimal zPio;                        // Z_PIO
    private BigDecimal zZdr;                        // Z_ZDR
    private BigDecimal zNez;                        // Z_NEZ

    // === OSTALO ===
    private BigDecimal mfp;                         // MFP - dodatni podaci
    private LocalDateTime obracunato;               // OBRACUNATO - datum obracuna

    // === KONSTRUKTORI ===

    public Obracun() {
    }

    // === POMOCNE METODE ===

    /**
     * @return Ukupni doprinosi na teret zaposlenog
     */
    public BigDecimal getUkupnoDoprinosiZaposleni() {
        BigDecimal z1 = zPio != null ? zPio : BigDecimal.ZERO;
        BigDecimal z2 = zZdr != null ? zZdr : BigDecimal.ZERO;
        BigDecimal z3 = zNez != null ? zNez : BigDecimal.ZERO;
        return z1.add(z2).add(z3);
    }

    /**
     * @return Ukupni doprinosi na teret poslodavca
     */
    public BigDecimal getUkupnoDoprinosiPoslodavac() {
        BigDecimal p1 = pPio != null ? pPio : BigDecimal.ZERO;
        BigDecimal p2 = pZdr != null ? pZdr : BigDecimal.ZERO;
        BigDecimal p3 = pNez != null ? pNez : BigDecimal.ZERO;
        return p1.add(p2).add(p3);
    }

    /**
     * @return Ukupan trosak poslodavca (bruto + doprinosi poslodavca)
     */
    public BigDecimal getUkupanTrosakPoslodavca() {
        BigDecimal b = bruto != null ? bruto : BigDecimal.ZERO;
        return b.add(getUkupnoDoprinosiPoslodavac());
    }

    /**
     * Postavlja SVP kao spoj SVP-1 .. SVP-5 (SVP = SVP1 & SVP2 & SVP3 & SVP4 & SVP5).
     * Poziva se pre snimanja u bazu.
     */
    public void rebuildSvp() {
        StringBuilder sb = new StringBuilder();
        for (String part : new String[]{svp1, svp2, svp3, svp4, svp5}) {
            if (part != null) sb.append(part.trim());
        }
        this.svp = sb.length() > 0 ? sb.toString() : null;
    }

    // === GETTERI I SETTERI ===

    public Integer getIdPobracun() {
        return idPobracun;
    }

    public void setIdPobracun(Integer idPobracun) {
        this.idPobracun = idPobracun;
    }

    public Integer getLnkObracun() {
        return lnkObracun;
    }

    public void setLnkObracun(Integer lnkObracun) {
        this.lnkObracun = lnkObracun;
    }

    public Integer getLnkZap() {
        return lnkZap;
    }

    public void setLnkZap(Integer lnkZap) {
        this.lnkZap = lnkZap;
    }

    public String getPrezime() {
        return prezime;
    }

    public void setPrezime(String prezime) {
        this.prezime = prezime;
    }

    public String getIme() {
        return ime;
    }

    public void setIme(String ime) {
        this.ime = ime;
    }

    public String getVrstaIdentifikatoraPrimaoca() {
        return vrstaIdentifikatoraPrimaoca;
    }

    public void setVrstaIdentifikatoraPrimaoca(String vrstaIdentifikatoraPrimaoca) {
        this.vrstaIdentifikatoraPrimaoca = vrstaIdentifikatoraPrimaoca;
    }

    public String getIdentifikatorPrimaoca() {
        return identifikatorPrimaoca;
    }

    public void setIdentifikatorPrimaoca(String identifikatorPrimaoca) {
        this.identifikatorPrimaoca = identifikatorPrimaoca;
    }

    public String getOznakaPrebivalista() {
        return oznakaPrebivalista;
    }

    public void setOznakaPrebivalista(String oznakaPrebivalista) {
        this.oznakaPrebivalista = oznakaPrebivalista;
    }

    public String getSvp() {
        return svp;
    }

    public void setSvp(String svp) {
        this.svp = svp;
    }

    public String getSvp1() {
        return svp1;
    }

    public void setSvp1(String svp1) {
        this.svp1 = svp1;
    }

    public String getSvp2() {
        return svp2;
    }

    public void setSvp2(String svp2) {
        this.svp2 = svp2;
    }

    public String getSvp3() {
        return svp3;
    }

    public void setSvp3(String svp3) {
        this.svp3 = svp3;
    }

    public String getSvp4() {
        return svp4;
    }

    public void setSvp4(String svp4) {
        this.svp4 = svp4;
    }

    public String getSvp5() {
        return svp5;
    }

    public void setSvp5(String svp5) {
        this.svp5 = svp5;
    }

    public Integer getBrojKalendarskihDana() {
        return brojKalendarskihDana;
    }

    public void setBrojKalendarskihDana(Integer brojKalendarskihDana) {
        this.brojKalendarskihDana = brojKalendarskihDana;
    }

    public Integer getMesecniFondSati() {
        return mesecniFondSati;
    }

    public void setMesecniFondSati(Integer mesecniFondSati) {
        this.mesecniFondSati = mesecniFondSati;
    }

    public Double getBrojEfektivnihSati() {
        return brojEfektivnihSati;
    }

    public void setBrojEfektivnihSati(Double brojEfektivnihSati) {
        this.brojEfektivnihSati = brojEfektivnihSati;
    }

    public BigDecimal getProcenat() {
        return procenat;
    }

    public void setProcenat(BigDecimal procenat) {
        this.procenat = procenat;
    }

    public BigDecimal getNeto() {
        return neto;
    }

    public void setNeto(BigDecimal neto) {
        this.neto = neto;
    }

    public BigDecimal getBruto() {
        return bruto;
    }

    public void setBruto(BigDecimal bruto) {
        this.bruto = bruto;
    }

    public BigDecimal getNormTroskovi() {
        return normTroskovi;
    }

    public void setNormTroskovi(BigDecimal normTroskovi) {
        this.normTroskovi = normTroskovi;
    }

    public BigDecimal getOsnovicaPorez() {
        return osnovicaPorez;
    }

    public void setOsnovicaPorez(BigDecimal osnovicaPorez) {
        this.osnovicaPorez = osnovicaPorez;
    }

    public BigDecimal getOsnovicaDoprinosi() {
        return osnovicaDoprinosi;
    }

    public void setOsnovicaDoprinosi(BigDecimal osnovicaDoprinosi) {
        this.osnovicaDoprinosi = osnovicaDoprinosi;
    }

    public BigDecimal getPorez() {
        return porez;
    }

    public void setPorez(BigDecimal porez) {
        this.porez = porez;
    }

    public BigDecimal getPoreskoOslobodjenje() {
        return poreskoOslobodjenje;
    }

    public void setPoreskoOslobodjenje(BigDecimal poreskoOslobodjenje) {
        this.poreskoOslobodjenje = poreskoOslobodjenje;
    }

    public BigDecimal getPio() {
        return pio;
    }

    public void setPio(BigDecimal pio) {
        this.pio = pio;
    }

    public BigDecimal getZdr() {
        return zdr;
    }

    public void setZdr(BigDecimal zdr) {
        this.zdr = zdr;
    }

    public BigDecimal getNez() {
        return nez;
    }

    public void setNez(BigDecimal nez) {
        this.nez = nez;
    }

    public BigDecimal getPioBen() {
        return pioBen;
    }

    public void setPioBen(BigDecimal pioBen) {
        this.pioBen = pioBen;
    }

    public BigDecimal getpPio() {
        return pPio;
    }

    public void setpPio(BigDecimal pPio) {
        this.pPio = pPio;
    }

    public BigDecimal getpZdr() {
        return pZdr;
    }

    public void setpZdr(BigDecimal pZdr) {
        this.pZdr = pZdr;
    }

    public BigDecimal getpNez() {
        return pNez;
    }

    public void setpNez(BigDecimal pNez) {
        this.pNez = pNez;
    }

    public BigDecimal getzPio() {
        return zPio;
    }

    public void setzPio(BigDecimal zPio) {
        this.zPio = zPio;
    }

    public BigDecimal getzZdr() {
        return zZdr;
    }

    public void setzZdr(BigDecimal zZdr) {
        this.zZdr = zZdr;
    }

    public BigDecimal getzNez() {
        return zNez;
    }

    public void setzNez(BigDecimal zNez) {
        this.zNez = zNez;
    }

    public BigDecimal getMfp() {
        return mfp;
    }

    public void setMfp(BigDecimal mfp) {
        this.mfp = mfp;
    }

    public LocalDateTime getObracunato() {
        return obracunato;
    }

    public void setObracunato(LocalDateTime obracunato) {
        this.obracunato = obracunato;
    }

    @Override
    public String toString() {
        return "Obracun{" +
                "idPobracun=" + idPobracun +
                ", lnkZap=" + lnkZap +
                ", svp3='" + svp3 + '\'' +
                ", neto=" + neto +
                ", bruto=" + bruto +
                '}';
    }
}
