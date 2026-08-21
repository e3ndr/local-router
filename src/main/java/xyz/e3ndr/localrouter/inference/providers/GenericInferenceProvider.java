package xyz.e3ndr.localrouter.inference.providers;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class GenericInferenceProvider extends _OAICompatibleInferenceProvider {
    private final String baseUrl;

    public GenericInferenceProvider(String id, JsonObject config) {
        super(id, config);
        this.baseUrl = config.getString("url");
    }

    @Override
    protected String baseUrl() {
        return this.baseUrl;
    }

    @Override
    public String v1Prefix() {
        return ""; // We assume the baseUrl is already prefixed.
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.GENERIC;
    }

    @Override
    public JsonObject serializeConfig() {
        return super.serializeConfig()
            .put("url", this.baseUrl);
    }

}
