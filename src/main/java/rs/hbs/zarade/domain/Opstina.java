package rs.hbs.zarade.domain;

/**
 * Opština (šifarnik).
 * Mapira se na tabelu: SFR_OPSTINA
 * Baza: data_zarade.mdb (Main DB)
 */
public class Opstina {

    private Integer id;           // ID
    private String sifra;         // Sifra (3 karaktera, npr. "101")
    private String naziv;         // Naziv opštine

    public Opstina() {
    }

    public Opstina(Integer id, String sifra, String naziv) {
        this.id = id;
        this.sifra = sifra;
        this.naziv = naziv;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSifra() {
        return sifra;
    }

    public void setSifra(String sifra) {
        this.sifra = sifra;
    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    /**
     * @return Prikaz u formatu "XXX - Naziv"
     */
    public String getDisplayName() {
        return (sifra != null ? sifra : "") + " - " + (naziv != null ? naziv : "");
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Opstina)) return false;
        Opstina other = (Opstina) o;
        return sifra != null && sifra.equals(other.sifra);
    }

    @Override
    public int hashCode() {
        return sifra != null ? sifra.hashCode() : 0;
    }
}
