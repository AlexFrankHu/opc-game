package com.lastbastion.app.health;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * 极简 HTTP 服务，提供：
 *   GET /healthz        — 200 OK + JSON，仅探活，nginx upstream / docker healthcheck 用
 *   GET /readyz         — 200 当 readyProbe 返回 true，否则 503
 *
 * 故意不引入 Spring/Netty —— 只占一个端口（默认 10199）。
 */
public final class HealthHttpServer {

    private static final Logger log = LoggerFactory.getLogger(HealthHttpServer.class);

    private final int port;
    private final Supplier<Boolean> readyProbe;
    private HttpServer server;

    public HealthHttpServer(int port, Supplier<Boolean> readyProbe) {
        this.port = port;
        this.readyProbe = readyProbe;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/healthz", ex -> writeJson(ex, 200, "{\"status\":\"ok\"}"));
        server.createContext("/readyz", ex -> {
            boolean ready = readyProbe == null || Boolean.TRUE.equals(readyProbe.get());
            writeJson(ex, ready ? 200 : 503,
                    ready ? "{\"status\":\"ready\"}" : "{\"status\":\"unready\"}");
        });
        server.setExecutor(Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "health-http");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        log.info("Health HTTP server listening on http://0.0.0.0:{}/healthz", port);
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private static void writeJson(com.sun.net.httpserver.HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
