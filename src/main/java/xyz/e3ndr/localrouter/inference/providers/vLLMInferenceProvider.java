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

public class vLLMInferenceProvider extends OpenAIInferenceProvider {
    private final String resourcePool;

    public vLLMInferenceProvider(String id, JsonObject config) {
        super(id, config);
        this.resourcePool = config.getString("resourcePool");
    }

    @Override
    public String resourcePool() {
        return this.resourcePool;
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.VLLM;
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
        this.sendRequest(
            "/sleep?level=2",
            (r) -> r.POST(BodyPublishers.noBody()),
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

        // vLLM's wake up process is very sensitive to interruptions, so we need to make
        // sure we don't get interrupted during this process.
        boolean wasInterrupted = false;

        try {
            this.sendRequest(
                "/wake_up?tags=weights",
                (r) -> r.POST(BodyPublishers.noBody()),
                BodyHandlers.discarding()
            );
        } catch (InterruptedException ignored) {
            wasInterrupted = true;
            Thread.interrupted(); // Clear the interrupted status
        }
        try {
            this.sendRequest(
                "/collective_rpc",
                (r) -> r
                    .POST(BodyPublishers.ofString(JsonObject.singleton("method", "reload_weights").toString()))
                    .header("Content-Type", "application/json"),
                BodyHandlers.discarding()
            );
        } catch (InterruptedException ignored) {
            wasInterrupted = true;
            Thread.interrupted(); // Clear the interrupted status
        }
        try {
            this.sendRequest(
                "/reset_prefix_cache",
                (r) -> r.POST(BodyPublishers.noBody()),
                BodyHandlers.discarding()
            );
        } catch (InterruptedException ignored) {
            wasInterrupted = true;
            Thread.interrupted(); // Clear the interrupted status
        }
        try {
            this.sendRequest(
                "/wake_up?tags=kv_cache",
                (r) -> r.POST(BodyPublishers.noBody()),
                BodyHandlers.discarding()
            );
        } catch (InterruptedException ignored) {
            wasInterrupted = true;
            Thread.interrupted(); // Clear the interrupted status
        }

        if (wasInterrupted) {
            throw new InterruptedException();
        }
    }

}
