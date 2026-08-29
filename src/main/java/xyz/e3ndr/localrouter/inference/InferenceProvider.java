package xyz.e3ndr.localrouter.inference;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
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

    /**
     * The maximum number of in-flight requests this provider may satisfy at
     * the same time. Defaults to 1 (fully serialized).
     */
    default int concurrency() {
        return 1;
    }

    /**
     * When true, in-flight requests on this provider must all target the same
     * model; a request for a different model waits in the queue until the
     * others complete. Defaults to false.
     */
    default boolean concurrencyMatchesModel() {
        return false;
    }

    public InferenceProviderType type();

    public JsonObject serializeConfig();

    public boolean healthCheck();

    public void sleep() throws IOException, InterruptedException;

    public void wakeUp() throws IOException, InterruptedException;

    public List<Model> models() throws IOException, InterruptedException;

    public HttpResponse<InputStream> proxy(String path, Function<Builder, Builder> modify) throws IOException, InterruptedException;

    public HttpResponse<InputStream> v1ChatCompletions(JsonObject body) throws IOException, InterruptedException;

    public HttpResponse<InputStream> v1Completions(JsonObject body) throws IOException, InterruptedException;

    public HttpResponse<InputStream> v1Embeddings(JsonObject body) throws IOException, InterruptedException;

}
