package xyz.e3ndr.localrouter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.annotating.JsonExclude;
import lombok.RequiredArgsConstructor;

public class InFlight {
    private static final Map<UUID, InFlightRequest> requestsInFlight = new LinkedHashMap<>();

    public static synchronized InFlightRequest register(String providerId, String modelId) {
        InFlightRequest request = new InFlightRequest(UUID.randomUUID(), providerId, modelId);
        requestsInFlight.put(request.id, request);
        return request;
    }

    public static synchronized List<InFlightRequest> inFlight() {
        return new ArrayList<>(requestsInFlight.values());
    }

    private static synchronized void unregister(UUID id) {
        requestsInFlight.remove(id);
    }

    @RequiredArgsConstructor
    @JsonClass(exposeAll = true)
    public static class InFlightRequest {
        public final @JsonExclude UUID id;

        public final String providerId;
        public final String modelId;

        public boolean isWaiting = true;

        public void markCompleted() {
            unregister(this.id);
        }

    }

}
