package xyz.e3ndr.localrouter.routes.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import xyz.e3ndr.localrouter.Pools;
import xyz.e3ndr.localrouter.db.Providers;
import xyz.e3ndr.localrouter.inference.InferenceProvider;
import xyz.e3ndr.localrouter.inference.InferenceProviderType;
import xyz.e3ndr.localrouter.util.CorsPostprocessor;

public class RouteProviders implements EndpointProvider {

    /**
     * Stands in for a stored API key in list responses. The management panel is
     * unauthenticated by design, so the actual key never leaves the provider.
     */
    public static final String API_KEY_SENTINEL = "SENTINELDONOTUSE";

    @HttpEndpoint(path = "/api/providers", allowedMethods = {
            HttpMethod.GET
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onList(HttpSession session, EndpointData<Void> data) {
        JsonArray responseData = new JsonArray();

        for (InferenceProvider provider : Providers.providers()) {
            JsonObject entry = new JsonObject()
                .put("id", provider.id())
                .put("type", provider.type().name())
                .put("resourcePool", provider.resourcePool())
                .put("concurrency", provider.concurrency())
                .put("concurrencyMatchesModel", provider.concurrencyMatchesModel());

            // Expose the config fields the in-place editor needs to prefill. Not every
            // provider serializes every key, so only include them when present.
            JsonObject config = provider.serializeConfig();
            String url = config.getString("url");
            if (url != null) {
                entry.put("url", url);
            }
            String apiKey = config.getString("apiKey");
            if (apiKey != null && !apiKey.isEmpty()) {
                entry.put("apiKey", API_KEY_SENTINEL); // Blank the real key with the sentinel.
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

    /** Manual force-unload: join each affected pool, then sleep its local providers under the pool's lock. */
    @HttpEndpoint(path = "/api/providers/sleep", allowedMethods = {
            HttpMethod.POST
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onSleep(HttpSession session, EndpointData<Void> data) {
        String pool = null;
        try {
            JsonObject body = Rson.DEFAULT.fromJson(session.body().string(), JsonObject.class);
            String requested = body.getString("pool");
            if (requested != null && !requested.isBlank()) {
                pool = requested;
            }
        } catch (IOException ignored) {
            // Blank or invalid body = all pools.
        }

        // The local providers grouped by pool - a pool is joined and locked as a unit,
        // since its providers share the coordinator.
        Map<String, List<InferenceProvider>> targets = new LinkedHashMap<>();
        for (InferenceProvider provider : Providers.providers()) {
            if (provider.isCloud()) {
                continue;
            }
            if (pool != null && !pool.equals(provider.resourcePool())) {
                continue;
            }

            targets.computeIfAbsent(provider.resourcePool(), (k) -> new ArrayList<>()).add(provider);
        }

        JsonArray slept = new JsonArray();
        JsonObject failed = new JsonObject();

        for (Map.Entry<String, List<InferenceProvider>> target : targets.entrySet()) {
            List<InferenceProvider> providers = target.getValue();

            try {
                this.joinPool(providers);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                for (InferenceProvider provider : providers) {
                    failed.put(provider.id(), "Interrupted whilst waiting for the pool to drain");
                }
                continue;
            }

            try {
                Pools.pool(target.getKey()).withLock(() -> {
                    for (InferenceProvider provider : providers) {
                        try {
                            provider.sleep();
                            slept.add(provider.id());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Throwable t) {
                            failed.put(provider.id(), t.getMessage() != null ? t.getMessage() : t.toString());
                        }
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                for (InferenceProvider provider : providers) {
                    failed.put(provider.id(), "Interrupted whilst waiting for the pool lock");
                }
            }
        }

        return HttpResponse.newFixedLengthResponse(
            StandardHttpStatus.OK,
            new JsonObject()
                .put("slept", slept)
                .put("failed", failed)
                .toString(true)
        ).mime("application/json; charset=utf-8");
    }

    /**
     * Wait until no in-flight or queued requests remain on the given pool's
     * providers (the pool has been joined), so sleeping them cannot interrupt a
     * live request.
     */
    private void joinPool(List<InferenceProvider> providers) throws InterruptedException {
        Set<String> ids = new HashSet<>();
        for (InferenceProvider provider : providers) {
            ids.add(provider.id());
        }

        while (true) {
            boolean busy = false;
            for (InFlight.InFlightRequest request : InFlight.inFlight()) {
                if (ids.contains(request.providerId)) {
                    busy = true;
                    break;
                }
            }

            if (!busy) {
                return;
            }

            Thread.sleep(250);
        }
    }

    @HttpEndpoint(path = "/api/providers/:id", allowedMethods = {
            HttpMethod.PATCH
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onUpdate(HttpSession session, EndpointData<Void> data) {
        String providerId = data.uriParameters().get("id");

        InferenceProvider existing = Providers.get(providerId);
        if (existing == null) {
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

        // The unchanged sentinel means "keep the current key". The real key never crosses
        // the wire, so snipe it from the original provider's serialized config before the
        // provider is deleted (sentinel with no existing key = no key, never the literal).
        if (API_KEY_SENTINEL.equals(body.getString("apiKey"))) {
            String existingKey = existing.serializeConfig().getString("apiKey");
            body.put("apiKey", existingKey != null ? existingKey : "");
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
