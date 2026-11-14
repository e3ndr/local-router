package xyz.e3ndr.localrouter.inference.providers;

import java.io.IOException;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.function.Function;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;

public class KokoroFastAPIInferenceProvider extends _OAICompatibleInferenceProvider {
    private final String baseUrl;
    private final String resourcePool;
    private final int concurrency;

    public KokoroFastAPIInferenceProvider(String id, JsonObject config) {
        super(id, config);
        this.baseUrl = config.getString("url");
        this.resourcePool = config.getString("resourcePool");
        this.concurrency = config.containsKey("concurrency") ? config.getNumber("concurrency").intValue() : 1;
    }

    @Override
    protected String baseUrl() {
        return this.baseUrl;
    }

    @Override
    public String v1Prefix() {
        return "/v1";
    }

    @Override
    public String resourcePool() {
        return this.resourcePool;
    }

    @Override
    public int concurrency() {
        return this.concurrency;
    }

    @Override
    public boolean concurrencyMatchesModel() {
        return false;
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.KOKORO_FASTAPI;
    }

    @Override
    public JsonObject serializeConfig() {
        return super.serializeConfig()
            .put("url", this.baseUrl)
            .put("resourcePool", this.resourcePool)
            .put("concurrency", this.concurrency);
    }

    @Override
    public boolean healthCheck() {
        try {
            int statusCode = this.sendRequest(
                "/health",
                Function.identity(),
                BodyHandlers.discarding()
            ).statusCode();

            return statusCode >= 200 && statusCode < 300;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void sleep() throws IOException, InterruptedException {
        this.sendRequest(
            "/dev/unload",
            (r) -> r.POST(BodyPublishers.noBody()),
            BodyHandlers.discarding()
        );
    }

    @Override
    public void wakeUp() throws IOException, InterruptedException {
        // Automatic
    }

}
