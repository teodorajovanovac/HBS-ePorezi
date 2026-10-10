package rs.hbs.zarade;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Launcher klasa za pokretanje JavaFX aplikacije.
 *
 * Ova klasa NE extenduje Application, što omogućava pokretanje
 * bez eksplicitnog module-path podešavanja za JavaFX.
 *
 * Podržava sakrivanje konzole na Windows-u ako je u app.local.properties
 * postavljeno: console.visible=false (podrazumevano).
 */
public class Launcher {

    public static void main(String[] args) throws Exception {
        if (shouldHideConsole() && relaunchWithJavaw(args)) {
            return; // Re-launched sa javaw — ovaj proces završava
        }
        HbsZaradeApp.main(args);
    }

    /** Čita console.visible iz app.local.properties (default: false = sakrij). */
    private static boolean shouldHideConsole() {
        Properties props = new Properties();
        String[] locations = {"app.local.properties", "../app.local.properties"};
        for (String loc : locations) {
            var p = Paths.get(loc);
            if (Files.exists(p)) {
                try {
                    props = rs.hbs.zarade.db.DatabaseConfig.loadProperties(p);
                } catch (Exception ignored) {}
                break;
            }
        }
        return "false".equalsIgnoreCase(props.getProperty("console.visible", "false").trim());
    }

    /**
     * Na Windows-u: ako se aplikacija pokrenula sa java.exe,
     * pokreće novu instancu sa javaw.exe (bez konzole) i završava.
     * @return true ako je re-launch pokrenuo, false ako nije (nije Windows, već javaw, itd.)
     */
    private static boolean relaunchWithJavaw(String[] args) {
        try {
            // Samo Windows ima javaw
            if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return false;

            String cmd = ProcessHandle.current().info().command().orElse(null);
            if (cmd == null) return false;

            // Ako se već pokreće kao javaw.exe — ne treba ništa
            if (cmd.toLowerCase().endsWith("javaw.exe")) return false;

            // Zameni java.exe → javaw.exe
            String javaw = cmd.replaceAll("(?i)java\\.exe$", "javaw.exe");
            if (javaw.equals(cmd)) return false;

            List<String> command = new ArrayList<>();
            command.add(javaw);
            command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(Launcher.class.getName());
            command.addAll(Arrays.asList(args));

            new ProcessBuilder(command).start();
            System.exit(0);
            return true;
        } catch (Exception e) {
            return false; // Ne uspeva → nastavi normalno
        }
    }
}
