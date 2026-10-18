package rs.hbs.zarade.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO za tabelu 'settings' u data_zarade.mdb.
 * Tabela: settings (settingName VARCHAR, settingVal VARCHAR)
 */
public class AppSettingsDao {

    public static final String KEY_XML_EXPORT_PATH   = "ObracunXmlExportPath";
    public static final String KEY_PRIMALAC_PDF_PATH = "PrimaocPdfPath";
    public static final String KEY_OBRACUN_PDF_PATH  = "ObracunPdfPath";
    /** Varijanta potvrde obračuna: Potvrda_001 (podrazumevano) ili Potvrda_002. */
    public static final String KEY_POTVRDE_REPORT    = "PotvrdeReport";

    /**
     * Čita vrednost jednog podešavanja.
     * Vraća null ako ključ ne postoji ili tabela nije dostupna.
     */
    public String getSetting(Connection conn, String key) {
        try {
            String sql = "SELECT settingVal FROM settings WHERE settingName = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("settingVal");
                }
            }
        } catch (SQLException ignored) {}
        return null;
    }

    /**
     * Snima jedno podešavanje (upsert: UPDATE pa INSERT ako nema reda).
     */
    public void saveSetting(Connection conn, String key, String value) throws SQLException {
        String updateSql = "UPDATE settings SET settingVal = ? WHERE settingName = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, value);
            ps.setString(2, key);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                String insertSql = "INSERT INTO settings (settingName, settingVal) VALUES (?, ?)";
                try (PreparedStatement ps2 = conn.prepareStatement(insertSql)) {
                    ps2.setString(1, key);
                    ps2.setString(2, value);
                    ps2.executeUpdate();
                }
            }
        }
    }

    /**
     * Pomoćna metoda: čita putanju iz DB i vraća File ako folder postoji.
     * Korisno za FileChooser.setInitialDirectory().
     */
    public static java.io.File getExportDir(String key) {
        try {
            DatabaseConnection dbConn = DatabaseConnection.getInstance();
            if (dbConn == null) return null;
            try (Connection conn = dbConn.getMainConnection()) {
                String path = new AppSettingsDao().getSetting(conn, key);
                if (path != null && !path.isEmpty()) {
                    java.io.File dir = new java.io.File(path);
                    if (dir.exists() && dir.isDirectory()) return dir;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
