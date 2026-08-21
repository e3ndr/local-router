package xyz.e3ndr.localrouter.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;

import co.casterlabs.commons.io.streams.StreamUtil;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rhs.HttpStatus;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.HeaderValue;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointProvider;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpResponse.ResponseContent;
import co.casterlabs.rhs.protocol.http.HttpSession;
import xyz.e3ndr.localrouter.InFlight;
import xyz.e3ndr.localrouter.InFlight.InFlightStatus;
import xyz.e3ndr.localrouter.LR;
import xyz.e3ndr.localrouter.db.Providers;
import xyz.e3ndr.localrouter.inference.InferenceProvider;
import xyz.e3ndr.localrouter.util.AuthPreprocessor;
import xyz.e3ndr.localrouter.util.CorsPostprocessor;

public class RouteProviderProxy implements EndpointProvider {

    @HttpEndpoint(path = "/proxy/:providerId/.*", preprocessor = AuthPreprocessor.class, postprocessor = CorsPostprocessor.class)
    public HttpResponse onOllamaProxy(HttpSession session, EndpointData<Void> data) {
        String providerId = data.uriParameters().get("providerId");
        InferenceProvider provider = Providers.get(providerId);

        if (provider == null) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NOT_FOUND, "Provider not found: " + providerId);
        }

        String path = session.uri().rawPath.substring("/proxy/".length() + providerId.length());

        boolean isInferenceEndpoint = path.startsWith("/api/generate") || path.startsWith("/api/chat") || path.startsWith("/api/embed") || // Ollama
            path.contains("/completions") || path.contains("/chat/completions") || path.contains("/embeddings"); // OpenAI

        // --------

        final _RequestCleanup cleanup = new _RequestCleanup(Thread.currentThread());

        try {
            if (isInferenceEndpoint) {
                try {
                    JsonObject body = Rson.DEFAULT.fromJson(session.body().string(), JsonObject.class);

                    String model = body.getString("model");
                    cleanup.inFlight = InFlight.register(providerId, model, cleanup::interrupt);
                } catch (IOException ignored) {}
            }

            if (!provider.isCloud() && isInferenceEndpoint) {
                cleanup.modelLockRelease = LR.lockLocalModels(provider.resourcePool(), providerId);
                try {
                    provider.wakeUp();
                } catch (IOException e) {
                    cleanup.close();
                    e.printStackTrace();
                    return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "An error occurred whilst waking up: " + provider.id() + "\n\n" + e.getMessage());
                }
            }

            if (cleanup.inFlight != null) {
                cleanup.inFlight.status = InFlightStatus.RUNNING;
            }

            java.net.http.HttpResponse<InputStream> result;
            try {
                BodyPublisher body = session.body().present() ? //
                    BodyPublishers.ofByteArray(session.body().bytes()) : BodyPublishers.noBody();

                result = provider.proxy(path, (r) -> {
                    r.method(session.rawMethod(), body);

                    HeaderValue contentType = session.headers().getSingle("Content-Type");
                    if (contentType != null) {
                        r.header("Content-Type", contentType.raw());
                    }

                    return r;
                });
            } catch (IOException e) {
                cleanup.close();
                e.printStackTrace();
                return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "An error occurred whilst proxying request:\n\n" + e.getMessage());
            }

            cleanup.streamToClose = result.body();
            if (cleanup.isInterrupted.get()) {
                throw new InterruptedException();
            }

            return new HttpResponse(
                new ResponseContent() {
                    @Override
                    public void close() throws IOException {
                        cleanup.close();
                    }

                    @Override
                    public void write(int recommendedBufferSize, OutputStream out) throws IOException {
                        StreamUtil.streamTransfer(result.body(), out, recommendedBufferSize);
                    }

                    @Override
                    public long length() {
                        return -1;
                    }
                },
                HttpStatus.adapt(result.statusCode(), null)
            )
                .mime(result.headers().firstValue("Content-Type").orElse("application/octet-stream"));
        } catch (InterruptedException e) {
            cleanup.close();
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "Request was cancelled.");
        }
    }

}
