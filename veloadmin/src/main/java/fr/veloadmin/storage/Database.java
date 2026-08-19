package fr.veloadmin.storage;

import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Database {

    private final Path dataDirectory;
    private final Logger logger;
    private Connection connection;

    public Database(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void init() {
        try {
            Files.createDirectories(dataDirectory);
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataDirectory.resolve("veloadmin.db"));

            try (Statement st = connection.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS reports (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        reporter TEXT NOT NULL,
                        reported TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        verified INTEGER NOT NULL DEFAULT 0
                    );
                """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS bans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        banned_by TEXT NOT NULL,
                        start_time INTEGER NOT NULL,
                        end_time INTEGER NOT NULL,
                        server TEXT NOT NULL,
                        active INTEGER NOT NULL DEFAULT 1
                    );
                """);
            }
            logger.info("Base de données SQLite initialisée.");
        } catch (Exception e) {
            logger.error("Impossible d'initialiser la base de données", e);
        }
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            logger.error("Erreur en fermant la base de données", e);
        }
    }

    // ---------- Reports ----------

    public void addReport(String reporter, String reported, String reason) {
        String sql = "INSERT INTO reports (reporter, reported, reason, timestamp, verified) VALUES (?, ?, ?, ?, 0)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, reporter);
            ps.setString(2, reported);
            ps.setString(3, reason);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Erreur lors de l'ajout d'un report", e);
        }
    }

    public List<ReportEntry> getReports(boolean onlyUnverified) {
        List<ReportEntry> list = new ArrayList<>();
        String sql = "SELECT id, reporter, reported, reason, timestamp, verified FROM reports"
                + (onlyUnverified ? " WHERE verified = 0" : "")
                + " ORDER BY timestamp DESC LIMIT 50";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new ReportEntry(
                        rs.getInt("id"),
                        rs.getString("reporter"),
                        rs.getString("reported"),
                        rs.getString("reason"),
                        rs.getLong("timestamp"),
                        rs.getInt("verified") == 1
                ));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la lecture des reports", e);
        }
        return list;
    }

    /** @return true if the report existed and its state was flipped */
    public boolean toggleReportVerified(int id) {
        String select = "SELECT verified FROM reports WHERE id = ?";
        String update = "UPDATE reports SET verified = ? WHERE id = ?";
        try (PreparedStatement sel = connection.prepareStatement(select)) {
            sel.setInt(1, id);
            try (ResultSet rs = sel.executeQuery()) {
                if (!rs.next()) return false;
                int current = rs.getInt("verified");
                int newVal = current == 1 ? 0 : 1;
                try (PreparedStatement upd = connection.prepareStatement(update)) {
                    upd.setInt(1, newVal);
                    upd.setInt(2, id);
                    upd.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Erreur lors du toggle d'un report", e);
            return false;
        }
    }

    // ---------- Bans ----------

    public void addBan(UUID uuid, String name, String reason, String bannedBy, long start, long end, String server) {
        String sql = "INSERT INTO bans (uuid, name, reason, banned_by, start_time, end_time, server, active) VALUES (?, ?, ?, ?, ?, ?, ?, 1)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, reason);
            ps.setString(4, bannedBy);
            ps.setLong(5, start);
            ps.setLong(6, end);
            ps.setString(7, server);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Erreur lors de l'ajout d'un ban", e);
        }
    }

    /**
     * Returns the most restrictive active ban for this player that applies to
     * the given server ("ALL" bans always apply). Returns null if none.
     */
    public BanEntry getActiveBan(String nameOrUuid, String serverName) {
        String sql = "SELECT id, uuid, name, reason, banned_by, start_time, end_time, server FROM bans " +
                "WHERE active = 1 AND (name = ? OR uuid = ?) AND end_time > ? AND (server = 'ALL' OR server = ?) " +
                "ORDER BY end_time DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nameOrUuid);
            ps.setString(2, nameOrUuid);
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, serverName == null ? "ALL" : serverName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new BanEntry(
                        rs.getInt("id"),
                        rs.getString("uuid"),
                        rs.getString("name"),
                        rs.getString("reason"),
                        rs.getString("banned_by"),
                        rs.getLong("start_time"),
                        rs.getLong("end_time"),
                        rs.getString("server")
                );
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la lecture des bans", e);
            return null;
        }
    }

    public record ReportEntry(int id, String reporter, String reported, String reason, long timestamp, boolean verified) {}

    public record BanEntry(int id, String uuid, String name, String reason, String bannedBy, long start, long end, String server) {}
}
