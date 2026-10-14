package rs.hbs.zarade.db;

import rs.hbs.zarade.domain.Obracun;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
// Timestamp already included via java.sql.*

/**
 * DAO za rad sa tabelom Obracun.
 * Baza: data_zarade.mdb (Main DB)
 *
 * Ovo je GLAVNA tabela za pojedinacne obracune.
 * Svaki red predstavlja obracun za jednog primaoca u okviru jedne definicije obracuna.
 */
public class ObracunDao implements BaseDao<Obracun, Integer> {

    private static final String TABLE_NAME = "Obracun";

    // SQL upiti
    private static final String SELECT_ALL = "SELECT * FROM " + TABLE_NAME + " ORDER BY lnkObracun DESC, IDpobracun";
    private static final String SELECT_BY_ID = "SELECT * FROM " + TABLE_NAME + " WHERE IDpobracun = ?";
    private static final String SELECT_BY_LNK_OBRACUN = "SELECT * FROM " + TABLE_NAME +
            " WHERE lnkObracun = ? ORDER BY Prezime, Ime";
    private static final String SELECT_BY_LNK_ZAP = "SELECT * FROM " + TABLE_NAME +
            " WHERE lnkZap = ? ORDER BY lnkObracun DESC";
    private static final String SELECT_BY_SVP3 = "SELECT * FROM " + TABLE_NAME +
            " WHERE [SVP-3] = ? ORDER BY lnkObracun DESC";
    private static final String COUNT_ALL = "SELECT COUNT(*) FROM " + TABLE_NAME;
    private static final String COUNT_BY_LNK_OBRACUN = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE lnkObracun = ?";

    @Override
    public Optional<Obracun> findById(Connection connection, Integer id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToObracun(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Obracun> findAll(Connection connection) throws SQLException {
        List<Obracun> result = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL)) {
            while (rs.next()) {
                result.add(mapResultSetToObracun(rs));
            }
        }
        return result;
    }

    private static final String INSERT_SQL =
        "INSERT INTO " + TABLE_NAME +
        " (lnkObracun, lnkZap, Prezime, Ime, VrstaIdentifikatoraPrimaoca," +
        "  IdentifikatorPrimaoca, OznakaPrebivalista, SVP, [SVP-1], [SVP-2], [SVP-3], [SVP-4], [SVP-5]," +
        "  Neto, BrojKalendarskihDana, MesecniFondSati, BrojEfektivnihSati, PROCENAT, Obracunato)" +
        " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE_FINANCIAL_SQL =
        "UPDATE " + TABLE_NAME +
        " SET Bruto=?, NORM_TROSKOVI=?, OsnovicaPorez=?, OsnovicaDoprinosi=?," +
        "     Porez=?, PoreskoOslobodjenje=?, PIO=?, ZDR=?, NEZ=?, PIOBen=?," +
        "     P_PIO=?, P_ZDR=?, P_NEZ=?, Z_PIO=?, Z_ZDR=?, Z_NEZ=?," +
        "     MFP=?, NETO=?, Obracunato=?," +
        "     lnkZap=?, Prezime=?, Ime=?, IdentifikatorPrimaoca=?, OznakaPrebivalista=?," +
        "     [SVP-2]=?, [SVP-3]=?, SVP=?, [SVP-1]=?, [SVP-4]=?, [SVP-5]=?" +
        " WHERE IDpobracun=?";

    private static final String DELETE_SQL = "DELETE FROM " + TABLE_NAME + " WHERE IDpobracun=?";

    @Override
    public Obracun save(Connection connection, Obracun entity) throws SQLException {
        entity.rebuildSvp();
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_SQL,
                Statement.RETURN_GENERATED_KEYS)) {
            setIntOrNull(stmt, 1, entity.getLnkObracun());
            setIntOrNull(stmt, 2, entity.getLnkZap());
            stmt.setString(3, entity.getPrezime());
            stmt.setString(4, entity.getIme());
            stmt.setString(5, entity.getVrstaIdentifikatoraPrimaoca());
            stmt.setString(6, entity.getIdentifikatorPrimaoca());
            stmt.setString(7, entity.getOznakaPrebivalista());
            stmt.setString(8, entity.getSvp());
            stmt.setString(9, entity.getSvp1());
            stmt.setString(10, entity.getSvp2());
            stmt.setString(11, entity.getSvp3());
            stmt.setString(12, entity.getSvp4());
            stmt.setString(13, entity.getSvp5());
            stmt.setBigDecimal(14, entity.getNeto());
            setIntOrNull(stmt, 15, entity.getBrojKalendarskihDana());
            setIntOrNull(stmt, 16, entity.getMesecniFondSati());
            if (entity.getBrojEfektivnihSati() != null) {
                stmt.setDouble(17, entity.getBrojEfektivnihSati());
            } else {
                stmt.setNull(17, java.sql.Types.DOUBLE);
            }
            stmt.setBigDecimal(18, entity.getProcenat());
            stmt.setTimestamp(19, entity.getObracunato() != null ?
                    Timestamp.valueOf(entity.getObracunato()) : null);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entity.setIdPobracun(generatedKeys.getInt(1));
                }
            }
        }
        return entity;
    }

    @Override
    public void update(Connection connection, Obracun entity) throws SQLException {
        entity.rebuildSvp();
        try (PreparedStatement stmt = connection.prepareStatement(UPDATE_FINANCIAL_SQL)) {
            stmt.setBigDecimal(1, entity.getBruto());
            stmt.setBigDecimal(2, entity.getNormTroskovi());
            stmt.setBigDecimal(3, entity.getOsnovicaPorez());
            stmt.setBigDecimal(4, entity.getOsnovicaDoprinosi());
            stmt.setBigDecimal(5, entity.getPorez());
            stmt.setBigDecimal(6, entity.getPoreskoOslobodjenje());
            stmt.setBigDecimal(7, entity.getPio());
            stmt.setBigDecimal(8, entity.getZdr());
            stmt.setBigDecimal(9, entity.getNez());
            stmt.setBigDecimal(10, entity.getPioBen());
            stmt.setBigDecimal(11, entity.getpPio());
            stmt.setBigDecimal(12, entity.getpZdr());
            stmt.setBigDecimal(13, entity.getpNez());
            stmt.setBigDecimal(14, entity.getzPio());
            stmt.setBigDecimal(15, entity.getzZdr());
            stmt.setBigDecimal(16, entity.getzNez());
            stmt.setBigDecimal(17, entity.getMfp());
            stmt.setBigDecimal(18, entity.getNeto());
            stmt.setTimestamp(19, entity.getObracunato() != null ?
                    Timestamp.valueOf(entity.getObracunato()) : null);
            setIntOrNull(stmt, 20, entity.getLnkZap());
            stmt.setString(21, entity.getPrezime());
            stmt.setString(22, entity.getIme());
            stmt.setString(23, entity.getIdentifikatorPrimaoca());
            stmt.setString(24, entity.getOznakaPrebivalista());
            stmt.setString(25, entity.getSvp2());
            stmt.setString(26, entity.getSvp3());
            stmt.setString(27, entity.getSvp());
            stmt.setString(28, entity.getSvp1());
            stmt.setString(29, entity.getSvp4());
            stmt.setString(30, entity.getSvp5());
            stmt.setInt(31, entity.getIdPobracun());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(Connection connection, Integer id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(DELETE_SQL)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private void setIntOrNull(PreparedStatement stmt, int paramIndex, Integer value) throws SQLException {
        if (value != null) {
            stmt.setInt(paramIndex, value);
        } else {
            stmt.setNull(paramIndex, java.sql.Types.INTEGER);
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
     * Pronalazi sve obracune za datu definiciju obracuna.
     *
     * @param connection aktivna konekcija
     * @param lnkObracun ID definicije obracuna (ObracunDef.ID_Obracun)
     * @return lista obracuna
     */
    public List<Obracun> findByLnkObracun(Connection connection, Integer lnkObracun) throws SQLException {
        List<Obracun> result = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_BY_LNK_OBRACUN)) {
            stmt.setInt(1, lnkObracun);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToObracun(rs));
                }
            }
        }
        return result;
    }

    /**
     * Broji obracune za datu definiciju obracuna.
     *
     * @param connection aktivna konekcija
     * @param lnkObracun ID definicije obracuna
     * @return broj obracuna
     */
    public long countByLnkObracun(Connection connection, Integer lnkObracun) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(COUNT_BY_LNK_OBRACUN)) {
            stmt.setInt(1, lnkObracun);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    /**
     * Učitava mapu gde je ključ lnkObracun (ID_Obracun definicije),
     * a vrednost skup svih SVP-3 kodova (vrste isplate/prihoda) koji se nalaze u tom obračunu.
     *
     * @param connection aktivna konekcija
     * @return mapa lnkObracun -> set SVP-3 kodova
     */
    public Map<Integer, Set<String>> findAllObracunDefSvp3Map(Connection connection) throws SQLException {
        Map<Integer, Set<String>> map = new HashMap<>();
        String sql = "SELECT lnkObracun, [SVP-3] FROM Obracun WHERE lnkObracun IS NOT NULL AND [SVP-3] IS NOT NULL";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int lnk = rs.getInt(1);
                String svp3 = rs.getString(2);
                if (svp3 != null && !svp3.trim().isEmpty()) {
                    map.computeIfAbsent(lnk, k -> new HashSet<>()).add(svp3.trim());
                }
            }
        }
        return map;
    }

    /**
     * Pronalazi sve obracune za datog primaoca.
     *
     * @param connection aktivna konekcija
     * @param lnkZap ID primaoca (Primaoci.IDZaposleni)
     * @return lista obracuna
     */
    public List<Obracun> findByLnkZap(Connection connection, Integer lnkZap) throws SQLException {
        List<Obracun> result = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_BY_LNK_ZAP)) {
            stmt.setInt(1, lnkZap);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToObracun(rs));
                }
            }
        }
        return result;
    }

    /**
     * Pronalazi obracune po vrsti prihoda (SVP-3).
     *
     * @param connection aktivna konekcija
     * @param svp3 sifra vrste prihoda (npr. "101", "405")
     * @return lista obracuna
     */
    public List<Obracun> findBySvp3(Connection connection, String svp3) throws SQLException {
        List<Obracun> result = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_BY_SVP3)) {
            stmt.setString(1, svp3);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToObracun(rs));
                }
            }
        }
        return result;
    }

    /**
     * Mapira ResultSet red na Obracun objekat.
     */
    private Obracun mapResultSetToObracun(ResultSet rs) throws SQLException {
        Obracun o = new Obracun();

        // Primarni kljuc
        o.setIdPobracun(getIntOrNull(rs, "IDpobracun"));

        // Veze
        o.setLnkObracun(getIntOrNull(rs, "lnkObracun"));
        o.setLnkZap(getIntOrNull(rs, "lnkZap"));

        // Podaci o primaocu (denormalizovano)
        o.setPrezime(rs.getString("Prezime"));
        o.setIme(rs.getString("Ime"));
        o.setVrstaIdentifikatoraPrimaoca(rs.getString("VrstaIdentifikatoraPrimaoca"));
        o.setIdentifikatorPrimaoca(rs.getString("IdentifikatorPrimaoca"));
        o.setOznakaPrebivalista(rs.getString("OznakaPrebivalista"));

        // SVP - Sifra Vrste Prihoda
        o.setSvp(rs.getString("SVP"));
        o.setSvp1(rs.getString("SVP-1"));
        o.setSvp2(rs.getString("SVP-2"));
        o.setSvp3(rs.getString("SVP-3"));
        o.setSvp4(rs.getString("SVP-4"));
        o.setSvp5(rs.getString("SVP-5"));

        // Radno vreme
        o.setBrojKalendarskihDana(getIntOrNull(rs, "BrojKalendarskihDana"));
        o.setMesecniFondSati(getIntOrNull(rs, "MesecniFondSati"));
        o.setBrojEfektivnihSati(getDoubleOrNull(rs, "BrojEfektivnihSati"));
        o.setProcenat(rs.getBigDecimal("PROCENAT"));

        // Finansijski podaci - ulaz
        o.setNeto(rs.getBigDecimal("NETO"));

        // Finansijski podaci - izracunato
        o.setBruto(rs.getBigDecimal("Bruto"));
        o.setNormTroskovi(rs.getBigDecimal("NORM_TROSKOVI"));

        // Osnovice
        o.setOsnovicaPorez(rs.getBigDecimal("OsnovicaPorez"));
        o.setOsnovicaDoprinosi(rs.getBigDecimal("OsnovicaDoprinosi"));

        // Porez
        o.setPorez(rs.getBigDecimal("Porez"));
        o.setPoreskoOslobodjenje(rs.getBigDecimal("PoreskoOslobodjenje"));

        // Doprinosi - ukupni
        o.setPio(rs.getBigDecimal("PIO"));
        o.setZdr(rs.getBigDecimal("ZDR"));
        o.setNez(rs.getBigDecimal("NEZ"));
        o.setPioBen(rs.getBigDecimal("PIOBen"));

        // Doprinosi - na teret poslodavca
        o.setpPio(rs.getBigDecimal("P_PIO"));
        o.setpZdr(rs.getBigDecimal("P_ZDR"));
        o.setpNez(rs.getBigDecimal("P_NEZ"));

        // Doprinosi - na teret zaposlenog
        o.setzPio(rs.getBigDecimal("Z_PIO"));
        o.setzZdr(rs.getBigDecimal("Z_ZDR"));
        o.setzNez(rs.getBigDecimal("Z_NEZ"));

        // Ostalo
        o.setMfp(rs.getBigDecimal("MFP"));
        Timestamp obracunato = rs.getTimestamp("OBRACUNATO");
        o.setObracunato(obracunato != null ? obracunato.toLocalDateTime() : null);

        return o;
    }

    private Integer getIntOrNull(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private Double getDoubleOrNull(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }
}
