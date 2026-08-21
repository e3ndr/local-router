package xyz.e3ndr.localrouter.inference.providers;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class QwenInferenceProvider extends _OAICompatibleInferenceProvider {

    public QwenInferenceProvider(String id, JsonObject config) {
        super(id, config);
    }

    @Override
    protected String baseUrl() {
        return "https://dashscope-intl.aliyuncs.com";
    }

    @Override
    public String v1Prefix() {
        return "/compatible-mode/v1";
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.QWEN;
    }

}
