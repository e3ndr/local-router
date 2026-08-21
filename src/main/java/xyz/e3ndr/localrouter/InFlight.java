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
    private static final Map<String, InFlightRequest> requestsInFlight = new LinkedHashMap<>();

    public static synchronized InFlightRequest register(String providerId, String modelId, Runnable cancel) {
        InFlightRequest request = new InFlightRequest(UUID.randomUUID().toString(), providerId, modelId, cancel);
        requestsInFlight.put(request.id, request);
        return request;
    }

    public static synchronized void cancel(String id) {
        InFlightRequest request = requestsInFlight.get(id);
        if (request != null) {
            request.cancel();
        }
    }

    public static synchronized List<InFlightRequest> inFlight() {
        return new ArrayList<>(requestsInFlight.values());
    }

    private static synchronized void unregister(String id) {
        requestsInFlight.remove(id);
    }

    @RequiredArgsConstructor
    @JsonClass(exposeAll = true)
    public static class InFlightRequest {
        public final String id;

        public final String providerId;
        public final String modelId;

        public volatile InFlightStatus status = InFlightStatus.WAITING;

        private final @JsonExclude Runnable cancel;

        public void cancel() {
            this.status = InFlightStatus.CANCELLED;
            this.cancel.run();
        }

        public void markCompleted() {
            unregister(this.id);
        }

    }

    public static enum InFlightStatus {
        WAITING,
        RUNNING,
        CANCELLED
    }

}
