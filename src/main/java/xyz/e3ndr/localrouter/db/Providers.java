package xyz.e3ndr.localrouter.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import xyz.e3ndr.localrouter.LR;
import xyz.e3ndr.localrouter.inference.InferenceProvider;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class Providers {
    private static Map<String, InferenceProvider> providers = new HashMap<>();

    static {
        try {
            LR.database.prepareStatement("CREATE TABLE IF NOT EXISTS providers (id TEXT PRIMARY KEY NOT NULL, config TEXT NOT NULL);").execute();
        } catch (SQLException e) {}
    }

    public static synchronized void init() {
        try (PreparedStatement ps = LR.database.prepareStatement("SELECT id, config FROM providers;")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                JsonObject config = Rson.DEFAULT.fromJson(rs.getString("config"), JsonObject.class);

                InferenceProviderType type = InferenceProviderType.valueOf(config.getString("type"));
                InferenceProvider provider = type.factory.apply(id, config);

                providers.put(id, provider);
            }
        } catch (SQLException | JsonParseException e) {
            e.printStackTrace();
        }
    }

    public static synchronized List<InferenceProvider> providers() {
        return new ArrayList<>(providers.values());
    }

    public static synchronized InferenceProvider get(String id) {
        return providers.get(id);
    }

    public static synchronized void create(InferenceProviderType type, String id, JsonObject config) {
        if (providers.containsKey(id)) {
            throw new IllegalArgumentException("Provider with ID already exists: " + id);
        }

        InferenceProvider provider = type.factory.apply(id, config);

        providers.put(id, provider);

        // Sanitize & normalize the config before saving it to the database.
        config = provider.serializeConfig();
        config.put("type", type.name());

        try (PreparedStatement ps = LR.database.prepareStatement("INSERT INTO providers (id, config) VALUES (?, ?);")) {
            ps.setString(1, id);
            ps.setString(2, config.toString());
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static synchronized void remove(String id) {
        InferenceProvider provider = providers.remove(id);
        if (provider == null) {
            throw new IllegalArgumentException("Provider with ID does not exist: " + id);
        }

        try (PreparedStatement ps = LR.database.prepareStatement("DELETE FROM providers WHERE id = ?;")) {
            ps.setString(1, id);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            provider.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
