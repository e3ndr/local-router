package xyz.e3ndr.localrouter.inference.providers;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class NovitaInferenceProvider extends _OAICompatibleInferenceProvider {

    public NovitaInferenceProvider(String id, JsonObject config) {
        super(id, config);
    }

    @Override
    protected String baseUrl() {
        return "https://api.novita.ai";
    }

    @Override
    public String v1Prefix() {
        return "/openai/v1";
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.NOVITA;
    }

}
