package xyz.e3ndr.localrouter.inference.providers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
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
import xyz.e3ndr.localrouter.util.RsonBodyHandler;

abstract class _OAICompatibleInferenceProvider implements InferenceProvider {
    private final HttpClient client = HttpClient.newBuilder()
        .version(Version.HTTP_1_1)
        .build();

    private final String id;
    private final @Nullable String apiKey;

    public _OAICompatibleInferenceProvider(String id, JsonObject config) {
        this.id = id;
        this.apiKey = config.getString("apiKey");
    }

    protected abstract String baseUrl();

    protected abstract String v1Prefix();

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public String resourcePool() {
        return "cloud";
    }

    @Override
    public JsonObject serializeConfig() {
        return new JsonObject()
            .put("apiKey", this.apiKey);
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
            // But we want to return false for 5xx errors.
            return statusCode >= 200 && statusCode < 500;
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
            .uri(URI.create(this.baseUrl() + path));

        if (this.apiKey != null && !this.apiKey.isEmpty()) {
            request.header("Authorization", "Bearer " + this.apiKey);
        }

        request = modify.apply(request);

        return this.client.send(request.build(), responseBodyHandler);
    }

    @Override
    public List<Model> models() throws IOException, InterruptedException {
        return this.sendRequest(
            this.v1Prefix() + "/models",
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
    public HttpResponse<InputStream> v1ChatCompletions(JsonObject body) throws IOException, InterruptedException {
        return this.proxy(
            this.v1Prefix() + "/chat/completions",
            (r) -> r
                .POST(BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
        );
    }

    @Override
    public HttpResponse<InputStream> v1Completions(JsonObject body) throws IOException, InterruptedException {
        return this.proxy(
            this.v1Prefix() + "/completions",
            (r) -> r
                .POST(BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
        );
    }

    @Override
    public HttpResponse<InputStream> v1Embeddings(JsonObject body) throws IOException, InterruptedException {
        return this.proxy(
            this.v1Prefix() + "/embeddings",
            (r) -> r
                .POST(BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
        );
    }

    @Override
    public void close() {
        this.client.close();
    }

}
