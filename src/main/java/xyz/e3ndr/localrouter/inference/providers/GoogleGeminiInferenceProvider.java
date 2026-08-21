package xyz.e3ndr.localrouter.inference.providers;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class GoogleGeminiInferenceProvider extends _OAICompatibleInferenceProvider {

    public GoogleGeminiInferenceProvider(String id, JsonObject config) {
        super(id, config);
    }

    @Override
    protected String baseUrl() {
        return "https://generativelanguage.googleapis.com";
    }

    @Override
    public String v1Prefix() {
        return "/v1beta/openai";
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.GOOGLE_GEMINI;
    }

}
