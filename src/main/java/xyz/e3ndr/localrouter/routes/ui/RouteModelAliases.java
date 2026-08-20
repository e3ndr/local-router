package xyz.e3ndr.localrouter.routes.ui;

import java.io.IOException;
import java.sql.SQLException;

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
import xyz.e3ndr.localrouter.db.Models;
import xyz.e3ndr.localrouter.db.Providers;
import xyz.e3ndr.localrouter.inference.InferenceProvider;
import xyz.e3ndr.localrouter.inference.Model;
import xyz.e3ndr.localrouter.util.CorsPostprocessor;

public class RouteModelAliases implements EndpointProvider {

    @HttpEndpoint(path = "/api/models", allowedMethods = {
            HttpMethod.GET
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onListModels(HttpSession session, EndpointData<Void> data) {
        JsonArray responseData = new JsonArray();

        for (InferenceProvider provider : Providers.providers()) {
            try {
                for (Model model : provider.models()) {
                    responseData.add(model.asPrefixed(provider.id()));
                }
            } catch (Throwable t) {
                t.printStackTrace();
//                return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "An error occurred whilst fetching models from: " + provider.id() + "\n\n" + t.getMessage());
            }
        }

        return HttpResponse.newFixedLengthResponse(
            StandardHttpStatus.OK,
            responseData.toString(true)
        ).mime("application/json; charset=utf-8");
    }

    @HttpEndpoint(path = "/api/models/aliases", allowedMethods = {
            HttpMethod.GET
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onList(HttpSession session, EndpointData<Void> data) {
        try {
            return HttpResponse.newFixedLengthResponse(
                StandardHttpStatus.OK,
                Rson.DEFAULT.toJson(Models.aliases()).toString(true)
            )
                .mime("application/json; charset=utf-8");
        } catch (SQLException e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "Error listing Model aliases: " + e.getMessage());
        }
    }

    @HttpEndpoint(path = "/api/models/aliases", allowedMethods = {
            HttpMethod.POST
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onCreate(HttpSession session, EndpointData<Void> data) {
        JsonObject body;
        try {
            body = Rson.DEFAULT.fromJson(session.body().string(), JsonObject.class);
        } catch (IOException e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.BAD_REQUEST, "Invalid JSON body: " + e.getMessage());
        }

        try {
            Models.add(body.getString("alias"), body.getString("actual"));
        } catch (Exception e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "Error creating Model alias: " + e.getMessage());
        }

        return HttpResponse.newFixedLengthResponse(
            StandardHttpStatus.CREATED,
            Rson.DEFAULT.toJson(body).toString(true)
        )
            .mime("application/json; charset=utf-8");
    }

    @HttpEndpoint(path = "/api/models/aliases/:alias", allowedMethods = {
            HttpMethod.DELETE
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onDelete(HttpSession session, EndpointData<Void> data) {
        String alias = data.uriParameters().get("alias");

        try {
            Models.remove(alias);
        } catch (Exception e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "Error deleting Model alias: " + e.getMessage());
        }

        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NO_CONTENT, "");
    }

}
