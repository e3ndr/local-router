package xyz.e3ndr.localrouter.inference.providers;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class OpenRouterInferenceProvider extends _OAICompatibleInferenceProvider {

    public OpenRouterInferenceProvider(String id, JsonObject config) {
        super(id, config);
    }

    @Override
    protected String baseUrl() {
        return "https://openrouter.ai";
    }

    @Override
    public String v1Prefix() {
        return "/api/v1";
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.OPENROUTER;
    }

}
