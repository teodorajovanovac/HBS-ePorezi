package rs.hbs.zarade.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Konfiguracija baze podataka.
 *
 * Ucitava putanje do Access baza iz app.local.properties fajla.
 * Prema CLAUDE.md, koriste se tri baze:
 * - aj_fn_cmn.mdb (common/shared baza)
 * - data_zarade.mdb (glavna poslovna baza)
 * - obracun-honorara.mdb (baza obracuna)
 */
public class DatabaseConfig {

    private static final String CONFIG_FILE = "app.local.properties";

    private static final String KEY_DB_COMMON = "db.common.path";
    private static final String KEY_DB_MAIN = "db.main.path";
    private static final String KEY_DB_CALC = "db.calc.path";

    private final String commonDbPath;
    private final String mainDbPath;
    private final String calcDbPath;

    private DatabaseConfig(String commonDbPath, String mainDbPath, String calcDbPath) {
        this.commonDbPath = commonDbPath;
        this.mainDbPath = mainDbPath;
        this.calcDbPath = calcDbPath;
    }

    /**
     * Ucitava konfiguraciju iz app.local.properties fajla.
     * Vraća null ako konfiguracija ne može da se učita.
     *
     * @return DatabaseConfig instanca ili null
     */
    public static DatabaseConfig loadFromProperties() {
        try {
            return load();
        } catch (DatabaseConfigException e) {
            System.err.println("Upozorenje: Nije moguće učitati konfiguraciju baze: " + e.getMessage());
            return null;
        }
    }

    /**
     * Ucitava konfiguraciju iz app.local.properties fajla.
     *
     * @return DatabaseConfig instanca
     * @throws DatabaseConfigException ako fajl ne postoji ili nedostaju obavezni parametri
     */
    public static DatabaseConfig load() throws DatabaseConfigException {
        Path configPath = findConfigFile();

        if (configPath == null) {
            throw new DatabaseConfigException(
                    "Konfiguracioni fajl '" + CONFIG_FILE + "' nije pronadjen.\n" +
                            "Trazene lokacije:\n" +
                            "  - Trenutni folder: " + Paths.get(".").toAbsolutePath().normalize() + "\n" +
                            "  - Parent folder: " + Paths.get("..").toAbsolutePath().normalize() + "\n" +
                            "  - Pored JAR fajla\n\n" +
                            "Kreirajte fajl sa putanjama do baza:\n" +
                            "  db.common.path=C:/putanja/do/aj_fn_cmn.mdb\n" +
                            "  db.main.path=C:/putanja/do/data_zarade.mdb\n" +
                            "  db.calc.path=C:/putanja/do/obracun-honorara.mdb"
            );
        }

        System.out.println("Koristi se konfiguracija: " + configPath.toAbsolutePath());

        Properties props = new Properties();
        try (InputStream is = new FileInputStream(configPath.toFile())) {
            props.load(is);
        } catch (IOException e) {
            throw new DatabaseConfigException("Greska pri citanju konfiguracionog fajla: " + e.getMessage(), e);
        }

        String mainPath = getRequiredProperty(props, KEY_DB_MAIN);
        validateDatabaseFile(mainPath, KEY_DB_MAIN);

        // common i calc su opcioni — ne moraju biti konfigurisani
        String commonPath = props.getProperty(KEY_DB_COMMON, "").trim();
        String calcPath = props.getProperty(KEY_DB_CALC, "").trim();
        if (!commonPath.isEmpty()) validateDatabaseFile(commonPath, KEY_DB_COMMON);
        if (!calcPath.isEmpty()) validateDatabaseFile(calcPath, KEY_DB_CALC);

        return new DatabaseConfig(commonPath, mainPath, calcPath);
    }

    /**
     * Traži konfiguracioni fajl na više lokacija:
     * 1. Trenutni radni direktorijum
     * 2. Parent direktorijum (za pokretanje iz bin/)
     * 3. Direktorijum gde se nalazi JAR fajl
     */
    private static Path findConfigFile() {
        // 1. Trenutni direktorijum
        Path current = Paths.get(CONFIG_FILE);
        if (Files.exists(current)) {
            return current;
        }

        // 2. Parent direktorijum (za pokretanje iz bin/)
        Path parent = Paths.get("..", CONFIG_FILE);
        if (Files.exists(parent)) {
            return parent;
        }

        // 3. Pored JAR fajla
        try {
            Path jarPath = Paths.get(DatabaseConfig.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            // Ako je JAR u lib/ folderu, idi dva nivoa gore
            Path jarDir = jarPath.getParent();
            if (jarDir != null) {
                // Probaj direktno pored JAR-a
                Path nearJar = jarDir.resolve(CONFIG_FILE);
                if (Files.exists(nearJar)) {
                    return nearJar;
                }
                // Probaj parent od lib/ (tj. root distribucije)
                Path libParent = jarDir.getParent();
                if (libParent != null) {
                    Path inDistRoot = libParent.resolve(CONFIG_FILE);
                    if (Files.exists(inDistRoot)) {
                        return inDistRoot;
                    }
                }
            }
        } catch (Exception e) {
            // Ignoriši greške pri pronalaženju JAR lokacije
        }

        return null;
    }

    private static String getRequiredProperty(Properties props, String key) throws DatabaseConfigException {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new DatabaseConfigException("Obavezan parametar '" + key + "' nije definisan u " + CONFIG_FILE);
        }
        return value.trim();
    }

    private static void validateDatabaseFile(String path, String keyName) throws DatabaseConfigException {
        Path dbPath = Paths.get(path);
        if (!Files.exists(dbPath)) {
            throw new DatabaseConfigException(
                    "Baza podataka (" + keyName + ") ne postoji na putanji: " + path
            );
        }
        if (!Files.isReadable(dbPath)) {
            throw new DatabaseConfigException(
                    "Baza podataka (" + keyName + ") nije citljiva: " + path
            );
        }
    }

    /**
     * @return Putanja do common/shared baze (aj_fn_cmn.mdb)
     */
    public String getCommonDbPath() {
        return commonDbPath;
    }

    /**
     * @return Putanja do glavne poslovne baze (data_zarade.mdb)
     */
    public String getMainDbPath() {
        return mainDbPath;
    }

    /**
     * @return Putanja do baze obracuna (obracun-honorara.mdb)
     */
    public String getCalcDbPath() {
        return calcDbPath;
    }

    /**
     * @return JDBC URL za common bazu, ili null ako nije konfigurisana
     */
    public String getCommonJdbcUrl() {
        if (commonDbPath == null || commonDbPath.isEmpty()) return null;
        return buildJdbcUrl(commonDbPath);
    }

    /**
     * @return JDBC URL za glavnu bazu
     */
    public String getMainJdbcUrl() {
        return buildJdbcUrl(mainDbPath);
    }

    /**
     * @return JDBC URL za bazu obracuna, ili null ako nije konfigurisana
     */
    public String getCalcJdbcUrl() {
        if (calcDbPath == null || calcDbPath.isEmpty()) return null;
        return buildJdbcUrl(calcDbPath);
    }

    private String buildJdbcUrl(String dbPath) {
        // UCanAccess JDBC URL format
        return "jdbc:ucanaccess://" + Paths.get(dbPath).toAbsolutePath();
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "commonDbPath='" + commonDbPath + '\'' +
                ", mainDbPath='" + mainDbPath + '\'' +
                ", calcDbPath='" + calcDbPath + '\'' +
                '}';
    }
}
