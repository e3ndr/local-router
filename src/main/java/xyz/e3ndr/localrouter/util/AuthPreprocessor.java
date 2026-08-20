package xyz.e3ndr.localrouter.util;

import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.HeaderValue;
import co.casterlabs.rhs.protocol.api.preprocessors.Preprocessor;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import xyz.e3ndr.localrouter.db.ApiKeys;

public class AuthPreprocessor implements Preprocessor.Http<Void> {

    @Override
    public void preprocess(HttpSession session, PreprocessorContext<HttpResponse, Void> context) {
        HeaderValue authHeader = session.headers().getSingle("Authorization");

        if (authHeader == null) {
            System.out.printf("[%s] was missing auth header.\n", session.remoteNetworkAddress());
            context.respondEarly(HttpResponse.newFixedLengthResponse(StandardHttpStatus.UNAUTHORIZED, "Missing Authorization header."));
            return;
        }

        String token = authHeader.raw();

        if (!authHeader.raw().startsWith("Bearer ")) {
            System.out.printf("[%s] had a malformed auth header.\n", session.remoteNetworkAddress());
            context.respondEarly(HttpResponse.newFixedLengthResponse(StandardHttpStatus.UNAUTHORIZED, "Missing Authorization header."));
            return;
        }

        token = token.substring("Bearer ".length());

        if (!ApiKeys.isValid(token)) {
            System.out.printf("[%s] had an invalid auth token.\n", session.remoteNetworkAddress());
            context.respondEarly(HttpResponse.newFixedLengthResponse(StandardHttpStatus.UNAUTHORIZED, "Invalid Authorization header."));
            return;
        }
    }

}
