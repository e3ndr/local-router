package xyz.e3ndr.localrouter.inference;

import java.time.Instant;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.element.JsonObject;

public record Model(String id, Instant createdAt, String ownedBy, @Nullable String shutdownDate) {

    public JsonObject asPrefixed(String providerId) {
        JsonObject json = new JsonObject()
            .put("id", providerId + "." + this.id)
            .put("created", this.createdAt.getEpochSecond())
            .put("owned_by", this.ownedBy);

        if (this.shutdownDate != null) {
            json.put("shutdown_date", this.shutdownDate);
        }

        return json;
    }

}
