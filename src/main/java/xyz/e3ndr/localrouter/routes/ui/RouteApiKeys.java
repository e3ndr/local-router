package xyz.e3ndr.localrouter.routes.ui;

import java.io.IOException;
import java.sql.SQLException;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rhs.HttpMethod;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointProvider;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import xyz.e3ndr.localrouter.db.ApiKeys;
import xyz.e3ndr.localrouter.db.ApiKeys.ApiKey;
import xyz.e3ndr.localrouter.util.CorsPostprocessor;

public class RouteApiKeys implements EndpointProvider {

    @HttpEndpoint(path = "/api/keys", allowedMethods = {
            HttpMethod.GET
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onList(HttpSession session, EndpointData<Void> data) {
        try {
            return HttpResponse.newFixedLengthResponse(
                StandardHttpStatus.OK,
                Rson.DEFAULT.toJson(ApiKeys.list()).toString(true)
            )
                .mime("application/json; charset=utf-8");
        } catch (SQLException e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "Error listing API keys: " + e.getMessage());
        }
    }

    @HttpEndpoint(path = "/api/keys", allowedMethods = {
            HttpMethod.POST
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onCreate(HttpSession session, EndpointData<Void> data) {
        JsonObject body;
        try {
            body = Rson.DEFAULT.fromJson(session.body().string(), JsonObject.class);
        } catch (IOException e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.BAD_REQUEST, "Invalid JSON body: " + e.getMessage());
        }

        ApiKey created;
        try {
            created = ApiKeys.create(body.getString("description"));
        } catch (SQLException e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "Error creating API key: " + e.getMessage());
        }

        return HttpResponse.newFixedLengthResponse(
            StandardHttpStatus.CREATED,
            Rson.DEFAULT.toJson(created).toString(true)
        )
            .mime("application/json; charset=utf-8");
    }

    @HttpEndpoint(path = "/api/keys/:key", allowedMethods = {
            HttpMethod.DELETE
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onDelete(HttpSession session, EndpointData<Void> data) {
        String keyId = data.uriParameters().get("key");

        try {
            ApiKeys.delete(keyId);
        } catch (SQLException e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "Error deleting API key: " + e.getMessage());
        }

        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NO_CONTENT, "");
    }

    @HttpEndpoint(path = "/api/keys/:key", allowedMethods = {
            HttpMethod.PATCH
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onUpdateDescription(HttpSession session, EndpointData<Void> data) {
        String keyId = data.uriParameters().get("key");

        JsonObject body;
        try {
            body = Rson.DEFAULT.fromJson(session.body().string(), JsonObject.class);
        } catch (IOException e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.BAD_REQUEST, "Invalid JSON body: " + e.getMessage());
        }

        try {
            ApiKeys.updateDescription(keyId, body.getString("description"));
        } catch (SQLException e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "Error updating API key description: " + e.getMessage());
        }

        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NO_CONTENT, "");
    }

}
