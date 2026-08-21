package xyz.e3ndr.localrouter.inference;

import java.util.function.BiFunction;

import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.localrouter.inference.providers.OllamaInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.OpenAIInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.vLLMInferenceProvider;

@RequiredArgsConstructor
public enum InferenceProviderType {
    OLLAMA(OllamaInferenceProvider::new),
    VLLM(vLLMInferenceProvider::new),
    OPENAI(OpenAIInferenceProvider::new),
    ;

    public final BiFunction<String, JsonObject, InferenceProvider> factory;
}