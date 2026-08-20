package xyz.e3ndr.localrouter.db;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import xyz.e3ndr.localrouter.LR;

public class ApiKeys {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    static {
        try {
            LR.database.prepareStatement("CREATE TABLE IF NOT EXISTS api_keys (key TEXT PRIMARY KEY NOT NULL, id TEXT NOT NULL, description TEXT NOT NULL);").execute();
        } catch (SQLException e) {}
    }

    public static boolean isValid(String key) {
        try (PreparedStatement ps = LR.database.prepareStatement("SELECT * FROM api_keys WHERE key = ?;")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static ApiKey create(String description) throws SQLException {
        String id = UUID.randomUUID().toString();
        String key = "sk_" + NanoIdUtils.randomNanoId(RANDOM, ALPHABET, 32 - 3);

        try (PreparedStatement ps = LR.database.prepareStatement("INSERT INTO api_keys (key, id, description) VALUES (?, ?, ?);")) {
            ps.setString(1, key);
            ps.setString(2, id);
            ps.setString(3, description);

            ps.execute();

            return new ApiKey(key, id, description);
        }
    }

    public static void updateDescription(String id, String description) throws SQLException {
        try (PreparedStatement ps = LR.database.prepareStatement("UPDATE api_keys SET description = ? WHERE id = ?;")) {
            ps.setString(1, description);
            ps.setString(2, id);
            ps.execute();
        }
    }

    public static void delete(String id) throws SQLException {
        try (PreparedStatement ps = LR.database.prepareStatement("DELETE FROM api_keys WHERE id = ?;")) {
            ps.setString(1, id);
            ps.execute();
        }
    }

    public static List<ApiKey> list() throws SQLException {
        try (ResultSet rs = LR.database.prepareStatement("SELECT * FROM api_keys;").executeQuery()) {
            List<ApiKey> keys = new LinkedList<>();
            while (rs.next()) {
                keys.add(
                    new ApiKey(
                        null,
                        rs.getString("id"),
                        rs.getString("description")
                    )
                );
            }
            return keys;
        }
    }

    @JsonClass(exposeAll = true)
    public static record ApiKey(String key, String id, String description) {
    }

}
