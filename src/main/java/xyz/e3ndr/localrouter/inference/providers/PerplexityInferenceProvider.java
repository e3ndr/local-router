package xyz.e3ndr.localrouter.inference.providers;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class PerplexityInferenceProvider extends _OAICompatibleInferenceProvider {

    public PerplexityInferenceProvider(String id, JsonObject config) {
        super(id, config);
    }

    @Override
    protected String baseUrl() {
        return "https://api.perplexity.ai";
    }

    @Override
    public String v1Prefix() {
        return "/router/v1";
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.PERPLEXITY;
    }

}
