package xyz.e3ndr.localrouter.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import xyz.e3ndr.localrouter.LR;
import xyz.e3ndr.localrouter.inference.InferenceProvider;

public class Models {

    static {
        try {
            LR.database.prepareStatement("CREATE TABLE IF NOT EXISTS model_aliases (alias TEXT PRIMARY KEY NOT NULL, actual TEXT NOT NULL);").execute();
        } catch (SQLException e) {}
    }

    public static List<ModelAlias> aliases() throws SQLException {
        List<ModelAlias> aliases = new ArrayList<>();

        try (PreparedStatement ps = LR.database.prepareStatement("SELECT * FROM model_aliases;")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    aliases.add(new ModelAlias(rs.getString("alias"), rs.getString("actual")));
                }
            }
        }

        return aliases;
    }

    public static void add(String alias, String actual) {
        try (PreparedStatement ps = LR.database.prepareStatement("INSERT OR REPLACE INTO model_aliases (alias, actual) VALUES (?, ?);")) {
            ps.setString(1, alias);
            ps.setString(2, actual);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void remove(String alias) {
        try (PreparedStatement ps = LR.database.prepareStatement("DELETE FROM model_aliases WHERE alias = ?;")) {
            ps.setString(1, alias);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static InferenceModelPair resolve(String model) {
        // First, check if the model is an alias.
        try (PreparedStatement ps = LR.database.prepareStatement("SELECT actual FROM model_aliases WHERE alias = ?;")) {
            ps.setString(1, model);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    model = rs.getString("actual");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        // Then, parse the model into provider and model ID.
        String[] requestedModel = model.split("\\.", 2);
        if (requestedModel.length != 2) {
            return null;
        }

        String providerId = requestedModel[0];
        String modelId = requestedModel[1];

        InferenceProvider provider = Providers.get(providerId);
        if (provider == null) {
            return null;
        }

        return new InferenceModelPair(provider, modelId);
    }

    public static record InferenceModelPair(InferenceProvider provider, String modelId) {
    }

    @JsonClass(exposeAll = true)
    public static record ModelAlias(String alias, String actual) {
    }

}
