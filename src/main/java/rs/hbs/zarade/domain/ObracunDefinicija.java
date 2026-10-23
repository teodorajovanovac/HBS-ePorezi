package rs.hbs.zarade.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Definicija obracuna - grupise vise pojedinacnih obracuna.
 *
 * Mapira se na tabelu: ObracunDef (18 kolona)
 * Baza: data_zarade.mdb (Main DB)
 *
 * Jedan ObracunDef moze imati vise Obracun zapisa (1:N relacija)
 */
public class ObracunDefinicija {

    // === PRIMARNI KLJUC ===
    private Integer idObracun;                      // ID_Obracun [PK]

    // === OSNOVNI PODACI ===
    private String nazivObracuna;                   // NAZIV_OBRACUNA
    private Integer ovp;                            // OVP - Oznaka Vrste Prihoda
    private String mesec;                           // Mesec - period obracuna (format "YYYY-MM")
    private Integer rbObracuna;                     // RBObracuna - redni broj obracuna

    // === DATUMI ===
    private LocalDate datumObracuna;                // DatumObracuna
    private LocalDate datumSlanja;                  // DatumSlanja

    // === OZNAKE ===
    private String oznakaZaKonacnu;                 // OznakaZaKonacnu - "K" za konacnu, "A" za akontaciju

    // === VEZE (FOREIGN KEYS) ===
    private Integer lnkPoslodavac;                  // lnkPoslodavac -> Poslodavac.IDPoslodavac
    private Integer lnkObracunPrijava;              // lnkObracunPrijava -> Obracun_Prijava.IDObracunPrijava
    private Integer lnkPromenaStope;                // lnkPromenaStope -> Obracun_Stope.PromenaStope

    // === FOND SATI (za zarade) ===
    private Integer brojDana;                       // BrojDana - broj kalendarskih dana
    private Integer fondSati;                       // FondSati - mesecni fond sati

    // === FINANSIJSKI PODACI ===
    private BigDecimal nn;                          // NN
    private BigDecimal nv;                          // NV

    // === OSTALO ===
    private String napomena;                        // Napomena
    private Integer puId;                           // PU_ID
    private Integer mId;                            // mID

    // === KONSTRUKTORI ===

    public ObracunDefinicija() {
    }

    // === POMOCNE METODE ===

    /**
     * @return true ako je konacna prijava
     */
    public boolean isKonacna() {
        return "K".equalsIgnoreCase(oznakaZaKonacnu);
    }

    /**
     * @return true ako je akontacija
     */
    public boolean isAkontacija() {
        return "A".equalsIgnoreCase(oznakaZaKonacnu);
    }

    // === GETTERI I SETTERI ===

    public Integer getIdObracun() {
        return idObracun;
    }

    public void setIdObracun(Integer idObracun) {
        this.idObracun = idObracun;
    }

    public String getNazivObracuna() {
        return nazivObracuna;
    }

    public void setNazivObracuna(String nazivObracuna) {
        this.nazivObracuna = nazivObracuna;
    }

    public Integer getOvp() {
        return ovp;
    }

    public void setOvp(Integer ovp) {
        this.ovp = ovp;
    }

    public String getMesec() {
        return mesec;
    }

    public void setMesec(String mesec) {
        this.mesec = mesec;
    }

    public Integer getRbObracuna() {
        return rbObracuna;
    }

    public void setRbObracuna(Integer rbObracuna) {
        this.rbObracuna = rbObracuna;
    }

    public LocalDate getDatumObracuna() {
        return datumObracuna;
    }

    public void setDatumObracuna(LocalDate datumObracuna) {
        this.datumObracuna = datumObracuna;
    }

    public LocalDate getDatumSlanja() {
        return datumSlanja;
    }

    public void setDatumSlanja(LocalDate datumSlanja) {
        this.datumSlanja = datumSlanja;
    }

    public String getOznakaZaKonacnu() {
        return oznakaZaKonacnu;
    }

    public void setOznakaZaKonacnu(String oznakaZaKonacnu) {
        this.oznakaZaKonacnu = oznakaZaKonacnu;
    }

    public Integer getLnkPoslodavac() {
        return lnkPoslodavac;
    }

    public void setLnkPoslodavac(Integer lnkPoslodavac) {
        this.lnkPoslodavac = lnkPoslodavac;
    }

    public Integer getLnkObracunPrijava() {
        return lnkObracunPrijava;
    }

    public void setLnkObracunPrijava(Integer lnkObracunPrijava) {
        this.lnkObracunPrijava = lnkObracunPrijava;
    }

    public Integer getLnkPromenaStope() {
        return lnkPromenaStope;
    }

    public void setLnkPromenaStope(Integer lnkPromenaStope) {
        this.lnkPromenaStope = lnkPromenaStope;
    }

    public Integer getBrojDana() {
        return brojDana;
    }

    public void setBrojDana(Integer brojDana) {
        this.brojDana = brojDana;
    }

    public Integer getFondSati() {
        return fondSati;
    }

    public void setFondSati(Integer fondSati) {
        this.fondSati = fondSati;
    }

    public BigDecimal getNn() {
        return nn;
    }

    public void setNn(BigDecimal nn) {
        this.nn = nn;
    }

    public BigDecimal getNv() {
        return nv;
    }

    public void setNv(BigDecimal nv) {
        this.nv = nv;
    }

    public String getNapomena() {
        return napomena;
    }

    public void setNapomena(String napomena) {
        this.napomena = napomena;
    }

    public Integer getPuId() {
        return puId;
    }

    public void setPuId(Integer puId) {
        this.puId = puId;
    }

    public Integer getmId() {
        return mId;
    }

    public void setmId(Integer mId) {
        this.mId = mId;
    }

    @Override
    public String toString() {
        return "ObracunDefinicija{" +
                "idObracun=" + idObracun +
                ", nazivObracuna='" + nazivObracuna + '\'' +
                ", mesec='" + mesec + '\'' +
                ", datumObracuna=" + datumObracuna +
                '}';
    }
}
