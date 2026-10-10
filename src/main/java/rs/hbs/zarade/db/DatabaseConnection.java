package rs.hbs.zarade.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Upravlja JDBC konekcijama ka Access bazama.
 *
 * Koristi UCanAccess JDBC driver za pristup .mdb/.accdb fajlovima.
 * Prema CLAUDE.md, postoje tri baze koje se koriste:
 * - common (aj_fn_cmn.mdb)
 * - main (data_zarade.mdb)
 * - calc (obracun-honorara.mdb)
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;
    private final DatabaseConfig config;

    public DatabaseConnection(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Vraća singleton instancu DatabaseConnection.
     * Koristi podrazumevanu konfiguraciju iz app.local.properties.
     * Vraća null ako konfiguracija nije dostupna.
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            DatabaseConfig config = DatabaseConfig.loadFromProperties();
            if (config == null) {
                return null;
            }
            instance = new DatabaseConnection(config);
        }
        return instance;
    }

    /**
     * Resetuje singleton instancu (korisno za testiranje).
     */
    public static synchronized void resetInstance() {
        instance = null;
    }

    /**
     * Kreira konekciju ka common bazi (aj_fn_cmn.mdb).
     *
     * @return JDBC Connection
     * @throws SQLException ako konekcija ne moze da se uspostavi ili baza nije konfigurisana
     */
    public Connection getCommonConnection() throws SQLException {
        String url = config.getCommonJdbcUrl();
        if (url == null) throw new SQLException("Common baza nije konfigurisana (db.common.path nije postavljen u app.local.properties).");
        return createConnection(url);
    }

    /**
     * Kreira konekciju ka glavnoj bazi (data_zarade.mdb).
     *
     * @return JDBC Connection
     * @throws SQLException ako konekcija ne moze da se uspostavi
     */
    public Connection getMainConnection() throws SQLException {
        return createConnection(config.getMainJdbcUrl());
    }

    /**
     * Kreira konekciju ka bazi obracuna (obracun-honorara.mdb).
     *
     * @return JDBC Connection
     * @throws SQLException ako konekcija ne moze da se uspostavi ili baza nije konfigurisana
     */
    public Connection getCalcConnection() throws SQLException {
        String url = config.getCalcJdbcUrl();
        if (url == null) throw new SQLException("Calc baza nije konfigurisana (db.calc.path nije postavljen u app.local.properties).");
        return createConnection(url);
    }

    private Connection createConnection(String jdbcUrl) throws SQLException {
        try {
            // UCanAccess driver se automatski registruje
            return DriverManager.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new SQLException("Nije moguce uspostaviti konekciju ka bazi: " + jdbcUrl + " - " + e.getMessage(), e);
        }
    }

    /**
     * Vraca listu svih tabela iz date baze.
     *
     * @param connection aktivna konekcija ka bazi
     * @return Lista imena tabela
     * @throws SQLException ako dodje do greske pri citanju metapodataka
     */
    public List<String> listTables(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();

        try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                // Preskoci sistemske tabele koje pocinju sa MSys ili USys
                if (!tableName.startsWith("MSys") && !tableName.startsWith("USys")) {
                    tables.add(tableName);
                }
            }
        }

        return tables;
    }

    /**
     * Testira konekciju ka bazi i vraca osnovne informacije.
     *
     * @param connection aktivna konekcija
     * @return String sa informacijama o bazi
     * @throws SQLException ako dodje do greske
     */
    public String getConnectionInfo(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        return String.format("Database: %s %s, Driver: %s %s",
                metaData.getDatabaseProductName(),
                metaData.getDatabaseProductVersion(),
                metaData.getDriverName(),
                metaData.getDriverVersion()
        );
    }

    /**
     * @return DatabaseConfig koji se koristi
     */
    public DatabaseConfig getConfig() {
        return config;
    }
}
