package rs.hbs.zarade.db;

import rs.hbs.zarade.domain.Staff;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO za tabelu Staff iz data_zarade.mdb (Main DB).
 *
 * Autentifikacija koristi direktno poređenje lozinki (plain text),
 * kao što je rađeno u originalnoj VBA aplikaciji.
 */
public class StaffDao {

    /**
     * Vraća sve korisnike sa nivoom > 0.
     */
    public List<Staff> findAll(Connection conn) throws SQLException {
        List<Staff> result = new ArrayList<>();
        String sql = "SELECT IDUser, [User], StaffLogin, Level, INC FROM Staff WHERE Level > 0 ORDER BY [User]";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    /**
     * Autentifikuje korisnika po imenu i lozinci (plain text poređenje).
     * Vraća Staff objekat ako je autentifikacija uspešna, null inače.
     * Podržava korisnike bez lozinke (StaffLogin je null ili prazan string).
     */
    public Staff authenticate(Connection conn, String username, String password) throws SQLException {
        String sql = "SELECT IDUser, [User], StaffLogin, Level, INC FROM Staff WHERE [User] = ? AND Level > 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Staff staff = map(rs);
                    String storedPass = staff.getStaffLogin();
                    // Plain text poređenje — kao u VBA: stafLG = Format([StaffLogin])
                    // Ako korisnik u bazi nema lozinku (null ili prazno), dozvoljava se prijava kada je i unos prazan
                    String stored = storedPass != null ? storedPass.trim() : "";
                    String entered = password != null ? password.trim() : "";
                    if (stored.equals(entered)) {
                        return staff;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Čuva novog korisnika u bazu.
     */
    public void save(Connection conn, Staff staff) throws SQLException {
        String sql = "INSERT INTO Staff ([User], StaffLogin, Level, INC) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, staff.getUser());
            ps.setString(2, staff.getStaffLogin());
            ps.setInt(3, staff.getLevel() != null ? staff.getLevel() : 4);
            ps.setString(4, staff.getInc());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    staff.setIdUser(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Ažurira postojećeg korisnika. Ako je lozinka prazna, ne menja je.
     */
    public void update(Connection conn, Staff staff, boolean changePassword) throws SQLException {
        String sql;
        if (changePassword && staff.getStaffLogin() != null && !staff.getStaffLogin().isEmpty()) {
            sql = "UPDATE Staff SET [User] = ?, StaffLogin = ?, Level = ?, INC = ? WHERE IDUser = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, staff.getUser());
                ps.setString(2, staff.getStaffLogin());
                ps.setInt(3, staff.getLevel() != null ? staff.getLevel() : 4);
                ps.setString(4, staff.getInc());
                ps.setInt(5, staff.getIdUser());
                ps.executeUpdate();
            }
        } else {
            sql = "UPDATE Staff SET [User] = ?, Level = ?, INC = ? WHERE IDUser = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, staff.getUser());
                ps.setInt(2, staff.getLevel() != null ? staff.getLevel() : 4);
                ps.setString(3, staff.getInc());
                ps.setInt(4, staff.getIdUser());
                ps.executeUpdate();
            }
        }
    }

    /**
     * Briše korisnika po ID-u.
     */
    public void delete(Connection conn, int idUser) throws SQLException {
        String sql = "DELETE FROM Staff WHERE IDUser = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ps.executeUpdate();
        }
    }

    private Staff map(ResultSet rs) throws SQLException {
        Staff s = new Staff();
        s.setIdUser(rs.getInt("IDUser"));
        s.setUser(rs.getString("User"));
        s.setStaffLogin(rs.getString("StaffLogin"));
        int level = rs.getInt("Level");
        s.setLevel(rs.wasNull() ? null : level);
        s.setInc(rs.getString("INC"));
        return s;
    }
}
