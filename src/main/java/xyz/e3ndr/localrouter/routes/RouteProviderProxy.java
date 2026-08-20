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
import xyz.e3ndr.localrouter.InFlight.InFlightRequest;
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
            path.startsWith("/v1/completions") || path.startsWith("/v1/chat/completions") || path.startsWith("/v1/embeddings"); // OpenAI

        boolean requiresLock = !provider.isCloud() && isInferenceEndpoint;

        InFlightRequest[] $flight = {
                null // pointer hax
        };
        if (isInferenceEndpoint) {
            try {
                JsonObject body = Rson.DEFAULT.fromJson(session.body().string(), JsonObject.class);

                String model = body.getString("model");
                $flight[0] = InFlight.register(providerId, model);
            } catch (IOException ignored) {}
        }

        Runnable cleanup = () -> {
            if ($flight[0] != null) {
                $flight[0].markCompleted();
            }

            if (requiresLock) {
                LR.unlockLocalModels(provider.resourcePool());
            }
        };

        if (requiresLock) {
            LR.lockLocalModels(provider.resourcePool(), providerId);
            try {
                provider.wakeUp();
            } catch (Throwable t) {
                cleanup.run();
                t.printStackTrace();
                return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "An error occurred whilst waking up: " + provider.id() + "\n\n" + t.getMessage());
            }
        }

        if ($flight[0] != null) {
            $flight[0].isWaiting = false;
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
        } catch (Throwable t) {
            cleanup.run();
            t.printStackTrace();
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "An error occurred whilst proxying request:\n\n" + t.getMessage());
        }

        return new HttpResponse(
            new ResponseContent() {
                @Override
                public void close() throws IOException {
                    result.body().close();
                    cleanup.run();
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
    }

}
