package xyz.e3ndr.localrouter.routes;

import co.casterlabs.rhs.HttpMethod;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointProvider;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import xyz.e3ndr.localrouter.util.CorsPostprocessor;

public class RouteHome implements EndpointProvider {

    @HttpEndpoint(path = "/", allowedMethods = {
            HttpMethod.GET
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onIndexRequest(HttpSession session, EndpointData<Void> data) {
        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, "LR is running");
    }

    @HttpEndpoint(path = "/inference", allowedMethods = {
            HttpMethod.GET
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onHealthcheckRequest(HttpSession session, EndpointData<Void> data) {
        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, "LR is running");
    }

}
