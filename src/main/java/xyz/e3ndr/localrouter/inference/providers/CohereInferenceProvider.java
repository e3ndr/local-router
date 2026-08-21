package xyz.e3ndr.localrouter.inference.providers;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class CohereInferenceProvider extends _OAICompatibleInferenceProvider {

    public CohereInferenceProvider(String id, JsonObject config) {
        super(id, config);
    }

    @Override
    protected String baseUrl() {
        return "https://api.cohere.ai";
    }

    @Override
    public String v1Prefix() {
        return "/compatibility/v1";
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.COHERE;
    }

}
