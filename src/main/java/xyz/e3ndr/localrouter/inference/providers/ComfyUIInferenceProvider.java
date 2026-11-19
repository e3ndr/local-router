package xyz.e3ndr.localrouter.inference.providers;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;
import xyz.e3ndr.localrouter.inference.Model;
import xyz.e3ndr.localrouter.util.RsonBodyHandler;

public class ComfyUIInferenceProvider extends _OAICompatibleInferenceProvider {
    private final String baseUrl;
    private final String resourcePool;

    public ComfyUIInferenceProvider(String id, JsonObject config) {
        super(id, config);
        this.baseUrl = config.getString("url");
        this.resourcePool = config.getString("resourcePool");
    }

    @Override
    protected String baseUrl() {
        return this.baseUrl;
    }

    // ComfyUI's API has no OpenAI-compatible prefix - only referenced by base
    // methods this class overrides.
    @Override
    public String v1Prefix() {
        return "";
    }

    @Override
    public String resourcePool() {
        return this.resourcePool;
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.COMFYUI;
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
                "/system_stats",
                Function.identity(),
                BodyHandlers.discarding()
            ).statusCode();

            return statusCode >= 200 && statusCode < 300;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ComfyUI has no model catalog - models are selected per workflow prompt, so
    // there is nothing for model aliases to resolve against.
    @Override
    public List<Model> models() throws IOException, InterruptedException {
        return List.of();
    }

    @Override
    public void sleep() throws IOException, InterruptedException {
        // ComfyUI's /free is a queue flag, not a synchronous call: the worker
        // consumes it on its next idle tick, so a running generation finishes
        // first. We wait for the queue to drain, raise the flag, then settle for
        // a fixed period before we can be sure the models are actually unloaded
        // (the Ollama heuristic, lengthened to 10s for the larger models).
        if (!this.queueDrained()) {
            while (true) {
                TimeUnit.MILLISECONDS.sleep(500);
                if (this.queueDrained()) {
                    break;
                }
            }
        }

        this.sendRequest(
            "/free",
            (r) -> r
                .POST(BodyPublishers.ofString(
                    new JsonObject()
                        .put("unload_models", true)
                        .put("free_memory", true)
                        .toString()
                ))
                .header("Content-Type", "application/json"),
            BodyHandlers.discarding()
        );

        TimeUnit.SECONDS.sleep(10);
    }

    /**
     * True when ComfyUI's prompt queue has no running or pending work.
     */
    private boolean queueDrained() {
        try {
            JsonObject queue = this.sendRequest(
                "/queue",
                Function.identity(),
                RsonBodyHandler.of(JsonObject.class)
            ).body();

            JsonArray running = queue.getArray("queue_running");
            JsonArray pending = queue.getArray("queue_pending");
            return (running == null || running.isEmpty()) && (pending == null || pending.isEmpty());
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void wakeUp() throws IOException, InterruptedException {
        // Automatic - ComfyUI loads models on the next prompt.
    }

    // Unreachable - we don't list any models.
    @Override
    public HttpResponse<InputStream> v1ChatCompletions(JsonObject body) {
        throw new UnsupportedOperationException(
            "ComfyUI providers do not support v1 chat completions; use the /proxy/<id> passthrough (POST /prompt, GET /history, ...) instead"
        );
    }

    @Override
    public HttpResponse<InputStream> v1Completions(JsonObject body) {
        throw new UnsupportedOperationException(
            "ComfyUI providers do not support v1 completions; use the /proxy/<id> passthrough (POST /prompt, GET /history, ...) instead"
        );
    }

    @Override
    public HttpResponse<InputStream> v1Embeddings(JsonObject body) {
        throw new UnsupportedOperationException(
            "ComfyUI providers do not support v1 embeddings; use the /proxy/<id> passthrough (POST /prompt, GET /history, ...) instead"
        );
    }

}
