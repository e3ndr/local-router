package xyz.e3ndr.localrouter.routes.ui;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import co.casterlabs.rhs.HttpServer;
import co.casterlabs.rhs.HttpServerBuilder;
import co.casterlabs.rhs.protocol.api.ApiFramework;
import co.casterlabs.rhs.protocol.http.HttpProtocol;

public class UiServer {

    public static void start() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        ApiFramework framework = new ApiFramework();
        framework.register(new RouteUi());
        framework.register(new RouteApiKeys());
        framework.register(new RouteModelAliases());
        framework.register(new RouteProviders());

        HttpServer server = new HttpServerBuilder()
            .withPort(8081)
            .withBehindProxy(true)
            .withKeepAliveSeconds(60)
            .withMinSoTimeoutSeconds(120)
            .withServerHeader("LocalRouter/1")
            .with(new HttpProtocol(), framework.httpHandler)
            .build();

        server.start();
    }

}
