package rs.hbs.zarade.domain;

/**
 * Korisnik sistema — tabela Staff iz data_zarade.mdb.
 *
 * Level vrednosti:
 *   3 = Pregledanje (ne koristi se aktivno)
 *   4 = Ograničeni pristup (samo Primaoci prihoda)
 *   8 = Moderator (sve osim izmene Admin naloga)
 *   9 = Administrator (pun pristup)
 */
public class Staff {

    private Integer idUser;
    private String user;
    private String staffLogin; // plain text lozinka
    private Integer level;
    private String inc;

    public Staff() {}

    public Staff(Integer idUser, String user, String staffLogin, Integer level, String inc) {
        this.idUser = idUser;
        this.user = user;
        this.staffLogin = staffLogin;
        this.level = level;
        this.inc = inc;
    }

    public Integer getIdUser() { return idUser; }
    public void setIdUser(Integer idUser) { this.idUser = idUser; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getStaffLogin() { return staffLogin; }
    public void setStaffLogin(String staffLogin) { this.staffLogin = staffLogin; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getInc() { return inc; }
    public void setInc(String inc) { this.inc = inc; }

    public String getLevelNaziv() {
        if (level == null) return "Nepoznat";
        return switch (level) {
            case 3 -> "Pregledanje";
            case 4 -> "Ogranič. pristup";
            case 8 -> "Moderator";
            case 9 -> "Administrator";
            default -> "Nivo " + level;
        };
    }

    @Override
    public String toString() {
        return user != null ? user : "(bez imena)";
    }
}
