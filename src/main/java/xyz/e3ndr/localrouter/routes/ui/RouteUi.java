package xyz.e3ndr.localrouter.routes.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import co.casterlabs.rhs.HttpMethod;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointProvider;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import co.casterlabs.rhs.util.MimeTypes;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.localrouter.util.CorsPostprocessor;

public class RouteUi implements EndpointProvider {

    @HttpEndpoint(path = ".*", allowedMethods = {
            HttpMethod.GET
    }, priority = -10)
    public HttpResponse onResource(HttpSession session, EndpointData<Void> data) {
        String path = session.uri().path
            .replace('\\', '/')
            .replace("%5c", "/")
            .replace("%5C", "/");

        if (path.isEmpty()) {
            path = "/index.html";
        } else {
            // Append `index.html` to the end when required.
            if (!path.contains(".")) {
                if (path.endsWith("/")) {
                    path += "index.html";
                } else {
                    path += ".html";
                }
            }
        }

        try (InputStream resource = RouteUi.class.getResourceAsStream("/webui" + path)) {
            if (resource == null) {
                return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NOT_FOUND);
            }

            byte[] bytes = resource.readAllBytes();

            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, bytes)
                .mime(MimeTypes.getMimeForFile(new File(path)));
        } catch (IOException e) {
            FastLogger.logStatic(LogLevel.SEVERE, "Error serving resource:\n%s", e);
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR);
        }
    }

    @HttpEndpoint(path = ".*", allowedMethods = {
            HttpMethod.OPTIONS
    }, postprocessor = CorsPostprocessor.class)
    public HttpResponse onCors(HttpSession session, EndpointData<Void> data) {
        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NO_CONTENT);
    }

}
