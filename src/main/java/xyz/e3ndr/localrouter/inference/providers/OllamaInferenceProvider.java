package xyz.e3ndr.localrouter.inference.providers;

import java.io.IOException;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;
import xyz.e3ndr.localrouter.util.RsonBodyHandler;

public class OllamaInferenceProvider extends _OAICompatibleInferenceProvider {
    private final String baseUrl;
    private final String resourcePool;

    public OllamaInferenceProvider(String id, JsonObject config) {
        super(id, config);
        this.baseUrl = config.getString("url");
        this.resourcePool = config.getString("resourcePool");
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
    public InferenceProviderType type() {
        return InferenceProviderType.OLLAMA;
    }

    @Override
    public JsonObject serializeConfig() {
        return super.serializeConfig()
            .put("url", this.baseUrl)
            .put("resourcePool", this.resourcePool);
    }

    @Override
    public boolean healthCheck() {
        try {
            int statusCode = this.sendRequest(
                "/",
                Function.identity(),
                BodyHandlers.discarding()
            ).statusCode();

            return statusCode >= 200 && statusCode < 300;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private List<String> listRunningModels() throws IOException, InterruptedException {
        return this.sendRequest(
            "/api/ps",
            Function.identity(),
            RsonBodyHandler.of(JsonObject.class)
        )
            .body()
            .getArray("models")
            .toList()
            .stream()
            .map((e) -> e.getAsObject().getString("name"))
            .toList();
    }

    @Override
    public void sleep() throws IOException, InterruptedException {
        List<String> running = this.listRunningModels();
        if (running.isEmpty()) {
            return; // Nothing to put to sleep, so we can just return early.
        }

        // Now, we send a request to /api/generate with a keep_alive of 0 to trigger an
        // immediate unload.
        for (String model : running) {
            this.sendRequest(
                "/api/generate",
                (r) -> r.POST(
                    BodyPublishers.ofString(
                        new JsonObject()
                            .put("model", model)
                            .put("keep_alive", 0)
                            .toString()
                    )
                ),
                BodyHandlers.discarding()
            );
        }

        // Ollama evicts the models asynchronously, so we need to wait a bit before we
        // can be sure that they are actually unloaded.
        TimeUnit.SECONDS.sleep(5);
    }

    @Override
    public void wakeUp() throws IOException, InterruptedException {
        // Automatic
    }

}
