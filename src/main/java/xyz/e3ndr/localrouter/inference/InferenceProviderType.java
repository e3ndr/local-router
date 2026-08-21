package xyz.e3ndr.localrouter.inference;

import java.util.function.BiFunction;

import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.localrouter.inference.providers.BasetenInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.CerebrasInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.ChutesInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.CohereInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.DeepInfraInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.DeepSeekInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.FireworksAIInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.GenericInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.GoogleGeminiInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.GroqInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.HuggingFaceInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.MistralInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.MoonshotInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.NovitaInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.NvidiaNimInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.OllamaInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.OpenAIInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.OpenRouterInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.PerplexityInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.PoolsideInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.QwenInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.SambaNovaInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.TogetherAIInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.XAIInferenceProvider;
import xyz.e3ndr.localrouter.inference.providers.vLLMInferenceProvider;

@RequiredArgsConstructor
public enum InferenceProviderType {
    OLLAMA(OllamaInferenceProvider::new),
    VLLM(vLLMInferenceProvider::new),
    GENERIC(GenericInferenceProvider::new),

    OPENAI(OpenAIInferenceProvider::new),
    DEEPINFRA(DeepInfraInferenceProvider::new),
    GROQ(GroqInferenceProvider::new),
    TOGETHER_AI(TogetherAIInferenceProvider::new),
    FIREWORKS_AI(FireworksAIInferenceProvider::new),
    CEREBRAS(CerebrasInferenceProvider::new),
    DEEPSEEK(DeepSeekInferenceProvider::new),
    MISTRAL(MistralInferenceProvider::new),
    XAI(XAIInferenceProvider::new),
    OPENROUTER(OpenRouterInferenceProvider::new),
    PERPLEXITY(PerplexityInferenceProvider::new),
    SAMBANOVA(SambaNovaInferenceProvider::new),
    NVIDIA_NIM(NvidiaNimInferenceProvider::new),
    NOVITA(NovitaInferenceProvider::new),
    QWEN(QwenInferenceProvider::new),
    MOONSHOT(MoonshotInferenceProvider::new),
    GOOGLE_GEMINI(GoogleGeminiInferenceProvider::new),
    CHUTES(ChutesInferenceProvider::new),
    COHERE(CohereInferenceProvider::new),
    HUGGINGFACE(HuggingFaceInferenceProvider::new),
    POOLSIDE(PoolsideInferenceProvider::new),
    BASETEN(BasetenInferenceProvider::new),
    ;

    public final BiFunction<String, JsonObject, InferenceProvider> factory;
}
