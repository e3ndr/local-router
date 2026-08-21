package xyz.e3ndr.localrouter.inference.providers;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class TogetherAIInferenceProvider extends _OAICompatibleInferenceProvider {

    public TogetherAIInferenceProvider(String id, JsonObject config) {
        super(id, config);
    }

    @Override
    protected String baseUrl() {
        return "https://api.together.ai";
    }

    @Override
    public String v1Prefix() {
        return "/v1";
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.TOGETHER_AI;
    }

}
