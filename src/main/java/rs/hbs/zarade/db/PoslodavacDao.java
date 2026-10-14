package rs.hbs.zarade.db;

import rs.hbs.zarade.domain.Poslodavac;

import java.sql.*;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO za rad sa tabelom Poslodavac.
 * Baza: data_zarade.mdb (Main DB)
 */
public class PoslodavacDao implements BaseDao<Poslodavac, Integer> {

    private static final String TABLE_NAME = "Poslodavac";

    // SQL upiti
    private static final String SELECT_ALL = "SELECT * FROM " + TABLE_NAME + " ORDER BY NazivPrezimeIme";
    private static final String SELECT_BY_ID = "SELECT * FROM " + TABLE_NAME + " WHERE IDPoslodavac = ?";
    private static final String SELECT_BY_PIB = "SELECT * FROM " + TABLE_NAME + " WHERE PoreskiIdentifikacioniBroj = ?";
    private static final String COUNT_ALL = "SELECT COUNT(*) FROM " + TABLE_NAME;

    private static final String UPDATE =
            "UPDATE " + TABLE_NAME +
            " SET TipIsplatioca=?, PoreskiIdentifikacioniBroj=?, MaticniBrojisplatioca=?," +
            " NazivPrezimeIme=?, BrojZaposlenih=?, SedistePrebivaliste=?," +
            " UlicaIBroj=?, Telefon=?, eMail=?, OdgovornoLice=?" +
            " WHERE IDPoslodavac=?";

    @Override
    public Optional<Poslodavac> findById(Connection connection, Integer id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPoslodavac(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Poslodavac> findAll(Connection connection) throws SQLException {
        List<Poslodavac> result = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL)) {
            while (rs.next()) {
                result.add(mapResultSetToPoslodavac(rs));
            }
        }
        return result;
    }

    @Override
    public Poslodavac save(Connection connection, Poslodavac entity) throws SQLException {
        throw new UnsupportedOperationException("INSERT Poslodavac nije podrzano - postoji tacno jedan isplatilac");
    }

    /**
     * Azurira podatke isplatioca.
     */
    @Override
    public void update(Connection connection, Poslodavac entity) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(UPDATE)) {
            setIntOrNull(stmt, 1, entity.getTipIsplatioca());
            stmt.setString(2, entity.getPoreskiIdentifikacioniBroj());
            stmt.setString(3, entity.getMaticniBrojIsplatioca());
            stmt.setString(4, entity.getNazivPrezimeIme());
            setIntOrNull(stmt, 5, entity.getBrojZaposlenih());
            stmt.setString(6, entity.getSedistePrebivaliste());
            stmt.setString(7, entity.getUlicaIBroj());
            stmt.setString(8, entity.getTelefon());
            stmt.setString(9, entity.getEmail());
            stmt.setString(10, entity.getOdgovornoLice());
            stmt.setInt(11, entity.getIdPoslodavac());
            stmt.executeUpdate();
        }
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
        throw new UnsupportedOperationException("Delete nije jos implementiran");
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
     * Pronalazi poslodavca po PIB-u.
     *
     * @param connection aktivna konekcija
     * @param pib poreski identifikacioni broj
     * @return Optional sa poslodavcem ili prazan Optional
     */
    public Optional<Poslodavac> findByPib(Connection connection, String pib) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_BY_PIB)) {
            stmt.setString(1, pib);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPoslodavac(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Mapira ResultSet red na Poslodavac objekat.
     */
    private Poslodavac mapResultSetToPoslodavac(ResultSet rs) throws SQLException {
        Poslodavac p = new Poslodavac();

        p.setIdPoslodavac(getIntOrNull(rs, "IDPoslodavac"));
        p.setTipIsplatioca(getIntOrNull(rs, "TipIsplatioca"));
        p.setPoreskiIdentifikacioniBroj(rs.getString("PoreskiIdentifikacioniBroj"));
        p.setMaticniBrojIsplatioca(rs.getString("MaticniBrojisplatioca"));
        p.setNazivPrezimeIme(rs.getString("NazivPrezimeIme"));
        p.setBrojZaposlenih(getIntOrNull(rs, "BrojZaposlenih"));
        p.setSedistePrebivaliste(rs.getString("SedistePrebivaliste"));
        p.setUlicaIBroj(rs.getString("UlicaIBroj"));
        p.setGrad(rs.getString("Grad"));
        p.setTelefon(rs.getString("Telefon"));
        p.setEmail(rs.getString("eMail"));
        p.setOdgovornoLice(rs.getString("OdgovornoLice"));

        return p;
    }

    private Integer getIntOrNull(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
