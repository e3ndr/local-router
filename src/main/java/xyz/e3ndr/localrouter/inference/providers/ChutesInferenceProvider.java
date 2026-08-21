package xyz.e3ndr.localrouter.inference.providers;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class ChutesInferenceProvider extends _OAICompatibleInferenceProvider {

    public ChutesInferenceProvider(String id, JsonObject config) {
        super(id, config);
    }

    @Override
    protected String baseUrl() {
        return "https://llm.chutes.ai";
    }

    @Override
    public String v1Prefix() {
        return "/v1";
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.CHUTES;
    }

}
