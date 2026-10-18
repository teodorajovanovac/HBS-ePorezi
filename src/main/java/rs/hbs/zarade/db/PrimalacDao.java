package rs.hbs.zarade.db;

import rs.hbs.zarade.domain.Primalac;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO za rad sa tabelom Primaoci.
 * Baza: data_zarade.mdb (Main DB)
 */
public class PrimalacDao implements BaseDao<Primalac, Integer> {

    private static final String TABLE_NAME = "Primaoci";

    // SQL upiti
    private static final String SELECT_ALL = "SELECT * FROM " + TABLE_NAME + " ORDER BY Prezime, Ime";
    private static final String SELECT_BY_ID = "SELECT * FROM " + TABLE_NAME + " WHERE IDZaposleni = ?";
    private static final String SELECT_BY_MB = "SELECT * FROM " + TABLE_NAME + " WHERE MB = ?";
    private static final String SELECT_BY_NAME = "SELECT * FROM " + TABLE_NAME +
            " WHERE Prezime LIKE ? AND Ime LIKE ? ORDER BY Prezime, Ime";
    private static final String COUNT_ALL = "SELECT COUNT(*) FROM " + TABLE_NAME;

    private static final String INSERT = "INSERT INTO " + TABLE_NAME +
            " (VrstaIdentifikatoraPrimaoca, VrstaPrimaocaID, Prezime, Ime, AdresaP, PBGradP," +
            " OznakaPrebivalista, MB, Telefon, Email, Mobilni, Status, ProcenatZaposlenja," +
            " DatumAzuriranja, Napomena)" +
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE = "UPDATE " + TABLE_NAME +
            " SET VrstaIdentifikatoraPrimaoca=?, VrstaPrimaocaID=?, Prezime=?, Ime=?," +
            " AdresaP=?, PBGradP=?, OznakaPrebivalista=?, MB=?, Telefon=?, Email=?," +
            " Mobilni=?, Status=?, ProcenatZaposlenja=?, DatumAzuriranja=?, Napomena=?" +
            " WHERE IDZaposleni=?";

    @Override
    public Optional<Primalac> findById(Connection connection, Integer id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPrimalac(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Primalac> findAll(Connection connection) throws SQLException {
        List<Primalac> result = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL)) {
            while (rs.next()) {
                result.add(mapResultSetToPrimalac(rs));
            }
        }
        return result;
    }

    /**
     * Unosi novog primaoca u bazu. Vraca objekat sa popunjenim IDZaposleni.
     */
    @Override
    public Primalac save(Connection connection, Primalac entity) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bindParams(stmt, entity);
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entity.setIdZaposleni(generatedKeys.getInt(1));
                }
            }
        }
        return entity;
    }

    /**
     * Azurira postojeceg primaoca u bazi.
     */
    @Override
    public void update(Connection connection, Primalac entity) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(UPDATE)) {
            bindParams(stmt, entity);
            stmt.setInt(16, entity.getIdZaposleni());
            stmt.executeUpdate();
        }
    }

    /**
     * Pomocna metoda - vezuje parametre za INSERT i UPDATE.
     * Redosled: VrstaIdentifikatoraPrimaoca, VrstaPrimaocaID, Prezime, Ime,
     *           AdresaP, PBGradP, OznakaPrebivalista, MB, Telefon, Email,
     *           Mobilni, Status, ProcenatZaposlenja, DatumAzuriranja, Napomena
     */
    private void bindParams(PreparedStatement stmt, Primalac p) throws SQLException {
        setIntOrNull(stmt, 1, p.getVrstaIdentifikatoraPrimaoca());
        setIntOrNull(stmt, 2, p.getVrstaPrimaocaId());
        stmt.setString(3, p.getPrezime());
        stmt.setString(4, p.getIme());
        stmt.setString(5, p.getAdresaP());
        stmt.setString(6, p.getPbGradP());
        stmt.setString(7, p.getOznakaPrebivalista());
        stmt.setString(8, p.getMb());
        stmt.setString(9, p.getTelefon());
        stmt.setString(10, p.getEmail());
        stmt.setString(11, p.getMobilni());
        setIntOrNull(stmt, 12, p.getStatus());
        setIntOrNull(stmt, 13, p.getProcenatZaposlenja());
        stmt.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
        stmt.setString(15, p.getNapomena());
    }

    private void setIntOrNull(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value != null) {
            stmt.setInt(index, value);
        } else {
            stmt.setNull(index, Types.INTEGER);
        }
    }

    @Override
    public void delete(Connection connection, Integer id) throws SQLException {
        // Brisanje je dozvoljeno samo ako primalac nema nijedan obracun
        long brojObracuna = new ObracunDao().countByLnkZap(connection, id);
        if (brojObracuna > 0) {
            throw new SQLException("Primalac ima obračune (" + brojObracuna + ") i ne može biti obrisan.");
        }
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM " + TABLE_NAME + " WHERE IDZaposleni = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public long count(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(COUNT_ALL)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    /**
     * Pretrazuje primaoce po prezimenu i imenu.
     *
     * @param connection aktivna konekcija
     * @param prezime prezime (moze biti parcijalno, dodaje se % na kraj)
     * @param ime ime (moze biti parcijalno, dodaje se % na kraj)
     * @return lista primalaca koji odgovaraju kriterijumu
     */
    public List<Primalac> findByPrezimeAndIme(Connection connection, String prezime, String ime) throws SQLException {
        List<Primalac> result = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_BY_NAME)) {
            stmt.setString(1, (prezime != null ? prezime : "") + "%");
            stmt.setString(2, (ime != null ? ime : "") + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToPrimalac(rs));
                }
            }
        }
        return result;
    }

    /**
     * Pronalazi primaoca po maticnom broju (JMBG).
     *
     * @param connection aktivna konekcija
     * @param mb maticni broj
     * @return Optional sa primaocem ili prazan Optional
     */
    public Optional<Primalac> findByMaticniBroj(Connection connection, String mb) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_BY_MB)) {
            stmt.setString(1, mb);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPrimalac(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Mapira ResultSet red na Primalac objekat.
     */
    private Primalac mapResultSetToPrimalac(ResultSet rs) throws SQLException {
        Primalac p = new Primalac();

        // Primarni kljuc
        p.setIdZaposleni(getIntOrNull(rs, "IDZaposleni"));

        // Identifikacija
        p.setVrstaIdentifikatoraPrimaoca(getIntOrNull(rs, "VrstaIdentifikatoraPrimaoca"));
        p.setVrstaPrimaocaId(getIntOrNull(rs, "VrstaPrimaocaID"));
        p.setMb(rs.getString("MB"));
        p.setOznakaPrebivalista(rs.getString("OznakaPrebivalista"));

        // Licni podaci
        p.setPrezime(rs.getString("Prezime"));
        p.setIme(rs.getString("Ime"));
        p.setPrezimeCyr(rs.getString("PrezimeCyr"));
        p.setImeCyr(rs.getString("ImeCyr"));
        p.setPrezimeLat(rs.getString("PrezimeLAT"));
        p.setImeLat(rs.getString("ImeLAT"));

        Timestamp datumR = rs.getTimestamp("DatumR");
        p.setDatumR(datumR != null ? datumR.toLocalDateTime().toLocalDate() : null);
        p.setMestoR(rs.getString("MestoR"));

        // Adresa
        p.setAdresaP(rs.getString("AdresaP"));
        p.setPbGradP(rs.getString("PBGradP"));

        // Kontakt
        p.setTelefon(rs.getString("Telefon"));
        p.setEmail(rs.getString("Email"));
        p.setMobilni(rs.getString("Mobilni"));

        // Zaposlenje
        p.setDatumZaposlenja(rs.getString("DatumZaposlenja"));
        Timestamp datumPrestanka = rs.getTimestamp("DatumPrestanka");
        p.setDatumPrestanka(datumPrestanka != null ? datumPrestanka.toLocalDateTime().toLocalDate() : null);
        p.setStatus(getIntOrNull(rs, "Status"));
        p.setRadnaPozicija(rs.getString("RadnaPozicija"));
        p.setZap(rs.getObject("Zap") != null ? rs.getBoolean("Zap") : null);
        p.setPenz(rs.getObject("Penz") != null ? rs.getBoolean("Penz") : null);
        p.setProcenatZaposlenja(getIntOrNull(rs, "ProcenatZaposlenja"));
        p.setPrethodniStaz(rs.getString("PrethodniStaz"));
        p.setStrucnaSprema(getIntOrNull(rs, "StrucnaSprema"));
        p.setKoeficijent(getDoubleOrNull(rs, "Koeficijent"));

        // Sindikat
        BigDecimal sindikatNomIznos = rs.getBigDecimal("SindikatNomIznos");
        p.setSindikatNomIznos(sindikatNomIznos);
        p.setSindikatProcenat(getDoubleOrNull(rs, "SindikatProcenat"));

        // Meta podaci
        p.setNapomena(rs.getString("Napomena"));
        Timestamp datumAzuriranja = rs.getTimestamp("DatumAzuriranja");
        p.setDatumAzuriranja(datumAzuriranja != null ? datumAzuriranja.toLocalDateTime() : null);
        p.setLnkAzurirao(getIntOrNull(rs, "lnkAzurirao"));

        return p;
    }

    /**
     * Helper metoda za citanje Integer vrednosti koja moze biti NULL.
     */
    private Integer getIntOrNull(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    /**
     * Helper metoda za citanje Double vrednosti koja moze biti NULL.
     */
    private Double getDoubleOrNull(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }
}
