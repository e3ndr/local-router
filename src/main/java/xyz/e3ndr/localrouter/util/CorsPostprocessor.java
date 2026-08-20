package xyz.e3ndr.localrouter.util;

import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.postprocessors.Postprocessor;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;

public class CorsPostprocessor implements Postprocessor.Http<Void> {

    @Override
    public void postprocess(HttpSession session, HttpResponse response, EndpointData<Void> data) {
        response
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Headers", "*")
            .header("Access-Control-Allow-Methods", "GET, POST, PATCH, PUT, DELETE, OPTIONS")
            .header("Access-Control-Allow-Private-Network", "true");
    }

}
