package xyz.e3ndr.localrouter.inference;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Function;

import co.casterlabs.rakurai.json.element.JsonObject;

public interface InferenceProvider extends Closeable {

    public String id();

    public String resourcePool();

    default boolean isCloud() {
        return this.resourcePool().equals("cloud");
    }

    public InferenceProviderType type();

    public JsonObject serializeConfig();

    public boolean healthCheck();

    public void sleep() throws IOException, InterruptedException;

    public void wakeUp() throws IOException, InterruptedException;

    public List<Model> models() throws IOException, InterruptedException;

    public HttpResponse<InputStream> proxy(String path, Function<Builder, Builder> modify) throws IOException, InterruptedException;

    default HttpResponse<InputStream> v1ChatCompletions(JsonObject body) throws IOException, InterruptedException {
        return this.proxy(
            "/v1/chat/completions",
            (r) -> r
                .POST(BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
        );
    }

    default HttpResponse<InputStream> v1Completions(JsonObject body) throws IOException, InterruptedException {
        return this.proxy(
            "/v1/completions",
            (r) -> r
                .POST(BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
        );
    }

    default HttpResponse<InputStream> v1Embeddings(JsonObject body) throws IOException, InterruptedException {
        return this.proxy(
            "/v1/embeddings",
            (r) -> r
                .POST(BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
        );
    }

}
