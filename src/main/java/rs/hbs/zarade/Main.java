package rs.hbs.zarade;

import rs.hbs.zarade.db.DatabaseConfig;
import rs.hbs.zarade.db.DatabaseConfigException;
import rs.hbs.zarade.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Glavna ulazna tacka aplikacije.
 *
 * Trenutno sluzi kao smoke test za proveru konekcije ka bazama.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("ePorezi - Smoke Test");
        System.out.println("==============================================");
        System.out.println();

        try {
            // 1. Ucitaj konfiguraciju
            System.out.println("[1] Ucitavanje konfiguracije...");
            DatabaseConfig config = DatabaseConfig.load();
            System.out.println("    OK: Konfiguracija ucitana");
            System.out.println("    - Main DB:   " + config.getMainDbPath());
            System.out.println();

            DatabaseConnection dbConnection = new DatabaseConnection(config);

            // 2. Test konekcije ka bazi
            testDatabaseConnection(dbConnection, "Main (data_zarade.mdb)", () -> dbConnection.getMainConnection());

            System.out.println();
            System.out.println("==============================================");
            System.out.println("SMOKE TEST USPESNO ZAVRSEN!");
            System.out.println("==============================================");

        } catch (DatabaseConfigException e) {
            System.err.println();
            System.err.println("GRESKA PRI UCITAVANJU KONFIGURACIJE:");
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println("Proverite da postoji fajl 'app.local.properties' u root-u projekta");
            System.err.println("sa sledecim sadrzajem:");
            System.err.println();
            System.err.println("  db.main.path=stari_access/db/data_zarade.mdb");
            System.exit(1);
        }
    }

    /**
     * Testira konekciju ka bazi i ispisuje listu tabela.
     */
    private static void testDatabaseConnection(DatabaseConnection dbConnection, String dbName, ConnectionSupplier connectionSupplier) {
        System.out.println("[*] Testiranje baze: " + dbName);

        try (Connection conn = connectionSupplier.get()) {
            // Ispisi info o konekciji
            String info = dbConnection.getConnectionInfo(conn);
            System.out.println("    Konekcija: " + info);

            // Islistaj tabele
            List<String> tables = dbConnection.listTables(conn);
            System.out.println("    Broj tabela: " + tables.size());

            if (!tables.isEmpty()) {
                System.out.println("    Tabele:");
                for (String table : tables) {
                    System.out.println("      - " + table);
                }
            }

            System.out.println("    STATUS: OK");
            System.out.println();

        } catch (SQLException e) {
            System.err.println("    GRESKA: " + e.getMessage());
            System.err.println("    STATUS: NEUSPESNO");
            System.err.println();
        }
    }

    /**
     * Funkcionalni interfejs za supplier koji moze da baci SQLException.
     */
    @FunctionalInterface
    private interface ConnectionSupplier {
        Connection get() throws SQLException;
    }
}
