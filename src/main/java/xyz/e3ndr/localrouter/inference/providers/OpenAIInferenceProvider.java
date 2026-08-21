package xyz.e3ndr.localrouter.inference.providers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.element.JsonObject;
import xyz.e3ndr.localrouter.inference.InferenceProvider;
import xyz.e3ndr.localrouter.inference.Model;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;
import xyz.e3ndr.localrouter.util.RsonBodyHandler;

public class OpenAIInferenceProvider implements InferenceProvider {
    private final HttpClient client = HttpClient.newBuilder()
        .version(Version.HTTP_1_1)
        .build();

    private final String id;
    private final String url;
    private final @Nullable String apiKey;

    public OpenAIInferenceProvider(String id, JsonObject config) {
        this.id = id;
        this.url = config.getString("url");
        this.apiKey = config.getString("apiKey");
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public String resourcePool() {
        return "cloud";
    }

    @Override
    public InferenceProviderType type() {
        return InferenceProviderType.OPENAI;
    }

    @Override
    public JsonObject serializeConfig() {
        return new JsonObject()
            .put("url", this.url)
            .put("apiKey", this.apiKey)
            .put("resourcePool", this.resourcePool());
    }

    @Override
    public boolean healthCheck() {
        try {
            int statusCode = this.sendRequest(
                "/",
                Function.identity(),
                BodyHandlers.discarding()
            ).statusCode();

            // This is really an "is this available" check...
            // Some AI providers return 405 for the root endpoint, so we treat that as a
            // success. Really, we want to know if this request throws an exception, but
            // we'll also look for 5xx errors :^)

            return statusCode >= 200 && statusCode < 300 || statusCode == 405;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void sleep() throws IOException, InterruptedException {}

    @Override
    public void wakeUp() throws IOException, InterruptedException {}

    protected <T> HttpResponse<T> sendRequest(String path, Function<HttpRequest.Builder, HttpRequest.Builder> modify, BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder()
            .uri(URI.create(this.url + path));

        if (this.apiKey != null && !this.apiKey.isEmpty()) {
            request.header("Authorization", "Bearer " + this.apiKey);
        }

        request = modify.apply(request);

        return this.client.send(request.build(), responseBodyHandler);
    }

    @Override
    public List<Model> models() throws IOException, InterruptedException {
        return this.sendRequest(
            "/v1/models",
            Function.identity(),
            RsonBodyHandler.of(JsonObject.class)
        )
            .body()
            .getArray("data")
            .toList()
            .stream()
            .map((e) -> e.getAsObject())
            .map(
                (o) -> new Model(
                    o.getString("id"),
                    Instant.ofEpochSecond(o.getNumber("created").longValue()),
                    o.getString("owned_by"),
                    o.getString("shutdown_date")
                )
            )
            .toList();
    }

    @Override
    public HttpResponse<InputStream> proxy(String path, Function<Builder, Builder> modify) throws IOException, InterruptedException {
        return this.sendRequest(path, modify, BodyHandlers.ofInputStream());
    }

    @Override
    public void close() {
        this.client.close();
    }

}
