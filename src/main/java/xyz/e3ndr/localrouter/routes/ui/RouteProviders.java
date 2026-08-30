package xyz.e3ndr.localrouter.routes.ui;

import java.io.IOException;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rhs.HttpMethod;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointProvider;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import xyz.e3ndr.localrouter.InFlight;
import xyz.e3ndr.localrouter.db.Providers;
import xyz.e3ndr.localrouter.inference.InferenceProvider;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;
import xyz.e3ndr.localrouter.util.CorsPostprocessor;

public class RouteProviders implements EndpointProvider {

    @HttpEndpoint(path = "/api/providers", allowedMethods = {
            HttpMethod.GET
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onList(HttpSession session, EndpointData<Void> data) {
        JsonArray responseData = new JsonArray();

        for (InferenceProvider provider : Providers.providers()) {
            JsonObject entry = new JsonObject()
                .put("id", provider.id())
                .put("type", provider.type().name())
                .put("resourcePool", provider.resourcePool());

            // Expose the config fields the in-place editor needs to prefill. Not every
            // provider serializes every key, so only include them when present.
            JsonObject config = provider.serializeConfig();
            String url = config.getString("url");
            if (url != null) {
                entry.put("url", url);
            }
            String apiKey = config.getString("apiKey");
            if (apiKey != null) {
                entry.put("apiKey", apiKey);
            }

            responseData.add(entry);
        }

        return HttpResponse.newFixedLengthResponse(
            StandardHttpStatus.OK,
            responseData.toString(true)
        ).mime("application/json; charset=utf-8");
    }

    @HttpEndpoint(path = "/api/providers/health", allowedMethods = {
            HttpMethod.GET
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onGetHealth(HttpSession session, EndpointData<Void> data) {
        JsonObject responseData = new JsonObject();

        for (InferenceProvider provider : Providers.providers()) {
            responseData.put(provider.id(), provider.healthCheck());
        }

        return HttpResponse.newFixedLengthResponse(
            StandardHttpStatus.OK,
            responseData.toString(true)
        ).mime("application/json; charset=utf-8");
    }

    @HttpEndpoint(path = "/api/providers/inflight", allowedMethods = {
            HttpMethod.GET
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onGetInflight(HttpSession session, EndpointData<Void> data) {
        return HttpResponse.newFixedLengthResponse(
            StandardHttpStatus.OK,
            Rson.DEFAULT.toJson(InFlight.inFlight()).toString(true)
        ).mime("application/json; charset=utf-8");
    }

    @HttpEndpoint(path = "/api/providers/inflight/:id", allowedMethods = {
            HttpMethod.DELETE
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onCancelInflight(HttpSession session, EndpointData<Void> data) {
        String id = data.uriParameters().get("id");

        InFlight.cancel(id);

        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NO_CONTENT, "");
    }

    @HttpEndpoint(path = "/api/providers", allowedMethods = {
            HttpMethod.POST
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onCreate(HttpSession session, EndpointData<Void> data) {
        JsonObject body;
        try {
            body = Rson.DEFAULT.fromJson(session.body().string(), JsonObject.class);
        } catch (IOException e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.BAD_REQUEST, "Invalid JSON body: " + e.getMessage());
        }

        Providers.create(InferenceProviderType.valueOf(body.getString("type")), body.getString("id"), body);

        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NO_CONTENT, "");
    }

    @HttpEndpoint(path = "/api/providers/:id", allowedMethods = {
            HttpMethod.PATCH
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onUpdate(HttpSession session, EndpointData<Void> data) {
        String providerId = data.uriParameters().get("id");

        if (Providers.get(providerId) == null) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NOT_FOUND, "Provider not found: " + providerId);
        }

        JsonObject body;
        try {
            body = Rson.DEFAULT.fromJson(session.body().string(), JsonObject.class);
        } catch (IOException e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.BAD_REQUEST, "Invalid JSON body: " + e.getMessage());
        }

        InferenceProviderType type;
        try {
            type = InferenceProviderType.valueOf(body.getString("type"));
        } catch (Exception e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.BAD_REQUEST, "Invalid provider type: " + body.getString("type"));
        }

        // Editing a provider = remove the existing reference + insert the updated one.
        // Providers.create() re-sanitizes the config via provider.serializeConfig().
        Providers.remove(providerId);
        Providers.create(type, providerId, body);

        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NO_CONTENT, "");
    }

    @HttpEndpoint(path = "/api/providers/:id", allowedMethods = {
            HttpMethod.DELETE
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onDelete(HttpSession session, EndpointData<Void> data) {
        String providerId = data.uriParameters().get("id");

        Providers.remove(providerId);

        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NO_CONTENT, "");
    }

}
