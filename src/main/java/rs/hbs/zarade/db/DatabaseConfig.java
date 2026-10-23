package rs.hbs.zarade.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * Konfiguracija baze podataka.
 *
 * Ucitava putanju do Access baze (data_zarade.mdb) iz app.local.properties fajla.
 */
public class DatabaseConfig {

    private static final String CONFIG_FILE = "app.local.properties";

    private static final String KEY_DB_MAIN = "db.main.path";

    private final String mainDbPath;

    private DatabaseConfig(String mainDbPath) {
        this.mainDbPath = mainDbPath;
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
                            "Kreirajte fajl sa putanjom do baze:\n" +
                            "  db.main.path=C:\\putanja\\do\\data_zarade.mdb (ili npr. db\\data_zarade.mdb)"
            );
        }

        System.out.println("Koristi se konfiguracija: " + configPath.toAbsolutePath());

        Properties props;
        try {
            props = loadProperties(configPath);
        } catch (IOException e) {
            throw new DatabaseConfigException("Greska pri citanju konfiguracionog fajla: " + e.getMessage(), e);
        }

        Path configDir = configPath.toAbsolutePath().getParent();

        String rawMain = getRequiredProperty(props, KEY_DB_MAIN);
        Path mainPath = resolveDatabasePath(rawMain, configDir);
        validateDatabaseFile(mainPath, KEY_DB_MAIN, rawMain);

        return new DatabaseConfig(mainPath.toString());
    }

    /**
     * Učitava properties fajl tako da podržava Windows backslash separator (\)
     * bez escape-ovanja, što omogućava direktno kopiranje putanja iz Windows Explorer-a.
     */
    public static Properties loadProperties(Path configPath) throws IOException {
        Properties props = new Properties();
        List<String> lines;
        try {
            lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            lines = Files.readAllLines(configPath, StandardCharsets.ISO_8859_1);
        }

        for (String line : lines) {
            if (line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }

            int eqIndex = trimmed.indexOf('=');
            int colonIndex = trimmed.indexOf(':');
            int sepIndex;
            if (eqIndex != -1 && colonIndex != -1) {
                sepIndex = Math.min(eqIndex, colonIndex);
            } else if (eqIndex != -1) {
                sepIndex = eqIndex;
            } else {
                sepIndex = colonIndex;
            }

            if (sepIndex != -1) {
                String key = trimmed.substring(0, sepIndex).trim();
                String value = trimmed.substring(sepIndex + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    if (value.length() >= 2) {
                        value = value.substring(1, value.length() - 1).trim();
                    }
                }
                props.setProperty(key, value);
            }
        }
        return props;
    }

    /**
     * Razrešava putanju do baze podataka.
     * Automatski prepoznaje da li je putanja apsolutna ili relativna:
     * - Apsolutna: npr. C:\db\baza.mdb, C:/db/baza.mdb, \\server\share\baza.mdb
     * - Relativna: npr. db\baza.mdb, ..\..\db\baza.mdb, stari_access/db/baza.mdb
     *
     * Podržava i Windows backslash (\) i standardni slash (/).
     * Relativne putanje se razrešavaju u odnosu na lokaciju app.local.properties
     * konfiguracionog fajla (ili radnog direktorijuma).
     *
     * @param rawPath putanja uneta u konfiguraciji
     * @param configDir direktorijum u kome se nalazi app.local.properties fajl
     * @return razrešena apsolutna i normalizovana Path putanja
     */
    public static Path resolveDatabasePath(String rawPath, Path configDir) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            return null;
        }

        String cleaned = rawPath.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
            (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            if (cleaned.length() >= 2) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            }
        }

        Path p = Paths.get(cleaned);

        // 1. Ako je putanja apsolutna (npr. C:\db\baza.mdb, C:/db/baza.mdb, \\server\share\baza.mdb)
        if (p.isAbsolute()) {
            return p.normalize().toAbsolutePath();
        }

        // 2. Ako je relativna, prvo probaj u odnosu na folder konfiguracionog fajla
        if (configDir != null) {
            Path fromConfigDir = configDir.resolve(p).normalize().toAbsolutePath();
            if (Files.exists(fromConfigDir)) {
                return fromConfigDir;
            }
        }

        // 3. Probaj u odnosu na trenutni radni direktorijum
        Path fromWorkingDir = Paths.get(".").toAbsolutePath().resolve(p).normalize().toAbsolutePath();
        if (Files.exists(fromWorkingDir)) {
            return fromWorkingDir;
        }

        // 4. Ako fajl ne postoji (za jasnu poruku o grešci), koristi konfiguracioni folder kao osnovu
        if (configDir != null) {
            return configDir.resolve(p).normalize().toAbsolutePath();
        }
        return fromWorkingDir;
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

    private static void validateDatabaseFile(Path dbPath, String keyName, String rawConfigValue) throws DatabaseConfigException {
        if (dbPath == null) {
            throw new DatabaseConfigException("Putanja za '" + keyName + "' nije zadata.");
        }
        if (!Files.exists(dbPath)) {
            throw new DatabaseConfigException(
                    "Baza podataka (" + keyName + ") ne postoji na putanji: " + dbPath + "\n" +
                    "  (Konfigurisana vrednost u " + CONFIG_FILE + ": " + rawConfigValue + ")"
            );
        }
        if (!Files.isReadable(dbPath)) {
            throw new DatabaseConfigException(
                    "Baza podataka (" + keyName + ") nije citljiva: " + dbPath + "\n" +
                    "  (Konfigurisana vrednost u " + CONFIG_FILE + ": " + rawConfigValue + ")"
            );
        }
    }

    /**
     * @return Putanja do glavne poslovne baze (data_zarade.mdb)
     */
    public String getMainDbPath() {
        return mainDbPath;
    }

    /**
     * @return JDBC URL za glavnu bazu
     */
    public String getMainJdbcUrl() {
        return buildJdbcUrl(mainDbPath);
    }

    private String buildJdbcUrl(String dbPath) {
        // UCanAccess JDBC URL format
        return "jdbc:ucanaccess://" + Paths.get(dbPath).toAbsolutePath();
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "mainDbPath='" + mainDbPath + '\'' +
                '}';
    }
}
