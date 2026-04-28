package com.lastbastion.app.health;

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class HealthHttpServerTest {

    @Test
    void healthzAlwaysOk_readyzMirrorsProbe() throws Exception {
        int port;
        try (ServerSocket s = new ServerSocket(0)) { port = s.getLocalPort(); }
        AtomicBoolean ready = new AtomicBoolean(false);
        HealthHttpServer server = new HealthHttpServer(port, ready::get);
        server.start();
        try {
            HttpClient http = HttpClient.newHttpClient();
            HttpResponse<String> healthy = http.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/healthz")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, healthy.statusCode());
            assertTrue(healthy.body().contains("ok"));

            HttpResponse<String> notReady = http.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/readyz")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(503, notReady.statusCode());

            ready.set(true);
            HttpResponse<String> readyResp = http.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/readyz")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, readyResp.statusCode());
        } finally {
            server.stop();
        }
    }
}
