package xyz.e3ndr.localrouter.inference.providers;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class DeepSeekInferenceProvider extends _OAICompatibleInferenceProvider {

    public DeepSeekInferenceProvider(String id, JsonObject config) {
        super(id, config);
    }

    @Override
    protected String baseUrl() {
        return "https://api.deepseek.com";
    }

    @Override
    public String v1Prefix() {
        return "";
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.DEEPSEEK;
    }

}
