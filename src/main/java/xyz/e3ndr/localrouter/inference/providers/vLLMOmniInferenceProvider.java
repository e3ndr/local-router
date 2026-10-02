package xyz.e3ndr.localrouter.inference.providers;

import java.io.IOException;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;
import xyz.e3ndr.localrouter.util.RsonBodyHandler;

public class vLLMOmniInferenceProvider extends _OAICompatibleInferenceProvider {
    private final String baseUrl;
    private final String resourcePool;
    private final int concurrency;
    private final boolean concurrencyMatchesModel;

    public vLLMOmniInferenceProvider(String id, JsonObject config) {
        super(id, config);
        this.baseUrl = config.getString("url");
        this.resourcePool = config.getString("resourcePool");
        this.concurrency = config.containsKey("concurrency") ? config.getNumber("concurrency").intValue() : 1;
        this.concurrencyMatchesModel = config.containsKey("concurrencyMatchesModel") && config.getBoolean("concurrencyMatchesModel");
    }

    @Override
    protected String baseUrl() {
        return this.baseUrl;
    }

    @Override
    protected String v1Prefix() {
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
        return this.concurrencyMatchesModel;
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.VLLM_OMNI;
    }

    @Override
    public JsonObject serializeConfig() {
        return super.serializeConfig()
            .put("url", this.baseUrl)
            .put("resourcePool", this.resourcePool)
            .put("concurrency", this.concurrency)
            .put("concurrencyMatchesModel", this.concurrencyMatchesModel);
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

    private boolean isSleeping() throws IOException, InterruptedException {
        HttpResponse<JsonObject> response = this.sendRequest(
            "/is_sleeping",
            Function.identity(),
            RsonBodyHandler.of(JsonObject.class)
        );
        return response.body().getBoolean("is_sleeping");
    }

    @Override
    public void sleep() throws IOException, InterruptedException {
        // NB: Omni doesn't currently support waking up from level 2...
        // meaning that you'll brick your instance if you do it :D
        this.sendRequest(
            "/v1/omni/sleep",
            (r) -> r.POST(BodyPublishers.ofString("{\"stage_ids\": [0], \"level\": 1}")),
            BodyHandlers.discarding()
        );

        // It seems like vLLM only satisfies the request once the model is evicted,
        // but to be safe...
        TimeUnit.SECONDS.sleep(1);
    }

    @Override
    public void wakeUp() throws IOException, InterruptedException {
        if (!this.isSleeping()) {
            return;
        }

        this.sendRequest(
            "/v1/omni/wakeup",
            (r) -> r.POST(BodyPublishers.ofString("{\"stage_ids\": [0]}")),
            BodyHandlers.discarding()
        );
    }

}
