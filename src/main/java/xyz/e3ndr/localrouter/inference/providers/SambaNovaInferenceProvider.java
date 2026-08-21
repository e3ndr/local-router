package xyz.e3ndr.localrouter.inference.providers;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class SambaNovaInferenceProvider extends _OAICompatibleInferenceProvider {

    public SambaNovaInferenceProvider(String id, JsonObject config) {
        super(id, config);
    }

    @Override
    protected String baseUrl() {
        return "https://api.sambanova.ai";
    }

    @Override
    public String v1Prefix() {
        return "/v1";
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.SAMBANOVA;
    }

}
