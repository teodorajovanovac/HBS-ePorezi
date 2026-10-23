package rs.hbs.zarade.domain;

import java.math.BigDecimal;

/**
 * Poreske stope i stope doprinosa.
 *
 * Mapira se na tabelu: Obracun_Stope (19 kolona)
 * Baza: data_zarade.mdb (Main DB)
 *
 * Ova tabela sadrzi sve stope potrebne za obracun:
 * - Stopa poreza
 * - Stope doprinosa (PIO, ZDR, NEZ) - i za zaposlenog i za poslodavca
 * - Procenat normiranih troskova
 * - Poresko oslobodjenje
 *
 * NAPOMENA: Stope se menjaju kroz vreme - polje PromenaStope oznacava verziju.
 */
public class ObracunStope {

    // === PRIMARNI KLJUC ===
    private Integer idObracun;                      // ID_OBRACUN [PK]

    // === IDENTIFIKACIJA STOPA ===
    private String tip;                             // TIP - vrsta prihoda (npr. "101", "405")
    private Integer promenaStope;                   // PromenaStope - verzija stopa

    // === OPISI ===
    private String tipNaslov;                       // TIP_NASLOV
    private String tipOpis;                         // TIP_OPIS (MEMO polje)

    // === STOPA POREZA ===
    private Double porezP;                          // POREZ_P (npr. 0.10 za 10%)

    // === STOPE DOPRINOSA - NA TERET POSLODAVCA ===
    private Double pioP;                            // PIO_P
    private Double zdrP;                            // ZDR_P
    private Double nezP;                            // NEZ_P

    // === STOPE DOPRINOSA - NA TERET ZAPOSLENOG ===
    private Double pioZ;                            // PIO_Z
    private Double zdrZ;                            // ZDR_Z
    private Double nezZ;                            // NEZ_Z

    // === NORMIRANI TROSKOVI ===
    private Double normTrosak;                      // NORM_TROSAK (npr. 0.20 za 20%)

    // === PORESKO OSLOBODJENJE ===
    private BigDecimal pOsl;                        // P_OSL

    // === OSNOVICE ===
    private String pOsn;                            // P_OSN - poreska osnovica opis
    private String osnDop;                          // OSN_DOP - osnovica doprinosa opis
    private String minOsnDop;                       // MIN_OSN_DOP - minimalna osnovica
    private String maxOsnDop;                       // MAX_OSN_DOP - maksimalna osnovica

    // === KOEFICIJENTI ===
    private Double koefMinRad;                      // KOEF_MIN_RAD

    // === KONSTRUKTORI ===

    public ObracunStope() {
    }

    // === POMOCNE METODE ===

    /**
     * @return Ukupna stopa PIO (poslodavac + zaposleni) kao BigDecimal
     */
    public BigDecimal getUkupnoPio() {
        double p = pioP != null ? pioP : 0.0;
        double z = pioZ != null ? pioZ : 0.0;
        return BigDecimal.valueOf(p + z);
    }

    /**
     * @return Ukupna stopa ZDR (poslodavac + zaposleni) kao BigDecimal
     */
    public BigDecimal getUkupnoZdr() {
        double p = zdrP != null ? zdrP : 0.0;
        double z = zdrZ != null ? zdrZ : 0.0;
        return BigDecimal.valueOf(p + z);
    }

    /**
     * @return Ukupna stopa NEZ (poslodavac + zaposleni) kao BigDecimal
     */
    public BigDecimal getUkupnoNez() {
        double p = nezP != null ? nezP : 0.0;
        double z = nezZ != null ? nezZ : 0.0;
        return BigDecimal.valueOf(p + z);
    }

    /**
     * @return Stopa poreza kao BigDecimal
     */
    public BigDecimal getPorezPBigDecimal() {
        return porezP != null ? BigDecimal.valueOf(porezP) : BigDecimal.ZERO;
    }

    /**
     * @return Normirani trosak kao BigDecimal
     */
    public BigDecimal getNormTrosakBigDecimal() {
        return normTrosak != null ? BigDecimal.valueOf(normTrosak) : BigDecimal.ZERO;
    }

    // === GETTERI I SETTERI ===

    public Integer getIdObracun() {
        return idObracun;
    }

    public void setIdObracun(Integer idObracun) {
        this.idObracun = idObracun;
    }

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }

    public Integer getPromenaStope() {
        return promenaStope;
    }

    public void setPromenaStope(Integer promenaStope) {
        this.promenaStope = promenaStope;
    }

    public String getTipNaslov() {
        return tipNaslov;
    }

    public void setTipNaslov(String tipNaslov) {
        this.tipNaslov = tipNaslov;
    }

    public String getTipOpis() {
        return tipOpis;
    }

    public void setTipOpis(String tipOpis) {
        this.tipOpis = tipOpis;
    }

    public Double getPorezP() {
        return porezP;
    }

    public void setPorezP(Double porezP) {
        this.porezP = porezP;
    }

    public Double getPioP() {
        return pioP;
    }

    public void setPioP(Double pioP) {
        this.pioP = pioP;
    }

    public Double getZdrP() {
        return zdrP;
    }

    public void setZdrP(Double zdrP) {
        this.zdrP = zdrP;
    }

    public Double getNezP() {
        return nezP;
    }

    public void setNezP(Double nezP) {
        this.nezP = nezP;
    }

    public Double getPioZ() {
        return pioZ;
    }

    public void setPioZ(Double pioZ) {
        this.pioZ = pioZ;
    }

    public Double getZdrZ() {
        return zdrZ;
    }

    public void setZdrZ(Double zdrZ) {
        this.zdrZ = zdrZ;
    }

    public Double getNezZ() {
        return nezZ;
    }

    public void setNezZ(Double nezZ) {
        this.nezZ = nezZ;
    }

    public Double getNormTrosak() {
        return normTrosak;
    }

    public void setNormTrosak(Double normTrosak) {
        this.normTrosak = normTrosak;
    }

    public BigDecimal getpOsl() {
        return pOsl;
    }

    public void setpOsl(BigDecimal pOsl) {
        this.pOsl = pOsl;
    }

    public String getpOsn() {
        return pOsn;
    }

    public void setpOsn(String pOsn) {
        this.pOsn = pOsn;
    }

    public String getOsnDop() {
        return osnDop;
    }

    public void setOsnDop(String osnDop) {
        this.osnDop = osnDop;
    }

    public String getMinOsnDop() {
        return minOsnDop;
    }

    public void setMinOsnDop(String minOsnDop) {
        this.minOsnDop = minOsnDop;
    }

    public String getMaxOsnDop() {
        return maxOsnDop;
    }

    public void setMaxOsnDop(String maxOsnDop) {
        this.maxOsnDop = maxOsnDop;
    }

    public Double getKoefMinRad() {
        return koefMinRad;
    }

    public void setKoefMinRad(Double koefMinRad) {
        this.koefMinRad = koefMinRad;
    }

    @Override
    public String toString() {
        return "ObracunStope{" +
                "tip='" + tip + '\'' +
                ", promenaStope=" + promenaStope +
                ", porezP=" + porezP +
                ", normTrosak=" + normTrosak +
                '}';
    }
}
