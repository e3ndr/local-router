package xyz.e3ndr.localrouter.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import co.casterlabs.commons.io.streams.StreamUtil;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rhs.HttpMethod;
import co.casterlabs.rhs.HttpStatus;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointProvider;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpResponse.ResponseContent;
import co.casterlabs.rhs.protocol.http.HttpSession;
import xyz.e3ndr.localrouter.InFlight;
import xyz.e3ndr.localrouter.InFlight.InFlightRequest;
import xyz.e3ndr.localrouter.LR;
import xyz.e3ndr.localrouter.db.Models;
import xyz.e3ndr.localrouter.db.Models.InferenceModelPair;
import xyz.e3ndr.localrouter.db.Models.ModelAlias;
import xyz.e3ndr.localrouter.db.Providers;
import xyz.e3ndr.localrouter.inference.InferenceProvider;
import xyz.e3ndr.localrouter.inference.Model;
import xyz.e3ndr.localrouter.util.AuthPreprocessor;
import xyz.e3ndr.localrouter.util.CorsPostprocessor;

public class RouteV1 implements EndpointProvider {

    @HttpEndpoint(path = "/inference/v1/models", allowedMethods = {
            HttpMethod.GET
    }, preprocessor = AuthPreprocessor.class, postprocessor = CorsPostprocessor.class)
    public HttpResponse onGetModels(HttpSession session, EndpointData<Void> data) {
        JsonArray responseData = new JsonArray();

        Map<String, Model> modelMap = new HashMap<>();

        for (InferenceProvider provider : Providers.providers()) {
            try {
                for (Model model : provider.models()) {
                    responseData.add(model.asPrefixed(provider.id()));
                    modelMap.put(provider.id() + "." + model.id(), model);
                }
            } catch (Throwable t) {
                t.printStackTrace();
//                return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "An error occurred whilst fetching models from: " + provider.id() + "\n\n" + t.getMessage());
            }
        }

        try {
            for (ModelAlias alias : Models.aliases()) {
                Model model = modelMap.get(alias.actual());
                if (model != null) {
                    responseData.add(
                        model
                            .asPrefixed("")
                            .put("id", alias.alias())
                    );
                }
            }
        } catch (SQLException e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "Error listing Model aliases: " + e.getMessage());
        }

        return HttpResponse.newFixedLengthResponse(
            StandardHttpStatus.OK, new JsonObject()
                .put("object", "list")
                .put("data", responseData)
                .toString(true)
        ).mime("application/json; charset=utf-8");
    }

    @HttpEndpoint(path = "/inference/v1/chat/completions", allowedMethods = {
            HttpMethod.POST
    }, preprocessor = AuthPreprocessor.class, postprocessor = CorsPostprocessor.class)
    public HttpResponse onChatCompletions(HttpSession session, EndpointData<Void> data) {
        return doRequest(session, RequestType.CHAT_COMPLETIONS);
    }

    @HttpEndpoint(path = "/inference/v1/completions", allowedMethods = {
            HttpMethod.POST
    }, preprocessor = AuthPreprocessor.class, postprocessor = CorsPostprocessor.class)
    public HttpResponse onCompletions(HttpSession session, EndpointData<Void> data) {
        return doRequest(session, RequestType.COMPLETIONS);
    }

    @HttpEndpoint(path = "/inference/v1/embeddings", allowedMethods = {
            HttpMethod.POST
    }, preprocessor = AuthPreprocessor.class, postprocessor = CorsPostprocessor.class)
    public HttpResponse onEmbeddings(HttpSession session, EndpointData<Void> data) {
        return doRequest(session, RequestType.EMBEDDINGS);
    }

    private static enum RequestType {
        CHAT_COMPLETIONS,
        COMPLETIONS,
        EMBEDDINGS
    }

    private static HttpResponse doRequest(HttpSession session, RequestType type) {
        JsonObject body;
        try {
            body = Rson.DEFAULT.fromJson(session.body().string(), JsonObject.class);
        } catch (IOException e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.BAD_REQUEST, "Invalid JSON body: " + e.getMessage());
        }

        String rawModel = body.getString("model");

        InferenceModelPair imp = Models.resolve(rawModel);
        if (imp == null) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.BAD_REQUEST, "Unknown provider or model: " + rawModel);
        }

        body.put("model", imp.modelId()); // Correct the model ID to be just the model name, without the provider prefix.

        InFlightRequest flight = InFlight.register(imp.provider().id(), imp.modelId());
        boolean requiresLock = !imp.provider().isCloud();

        Runnable cleanup = () -> {
            flight.markCompleted();

            if (requiresLock) {
                LR.unlockLocalModels(imp.provider().resourcePool());
            }
        };

        if (requiresLock) {
            LR.lockLocalModels(imp.provider().resourcePool(), imp.provider().id());
        }
        flight.isWaiting = false;

        java.net.http.HttpResponse<InputStream> result;
        try {
            imp.provider().wakeUp();

            result = switch (type) {
                case CHAT_COMPLETIONS -> imp.provider().v1ChatCompletions(body);
                case COMPLETIONS -> imp.provider().v1Completions(body);
                case EMBEDDINGS -> imp.provider().v1Embeddings(body);
            };
        } catch (IOException | InterruptedException e) {
            cleanup.run();
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "An error occurred whilst fetching completions from: " + imp.provider().id() + "\n\n" + e.getMessage());
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
