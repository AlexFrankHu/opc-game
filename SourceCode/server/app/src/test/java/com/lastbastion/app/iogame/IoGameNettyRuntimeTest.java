package com.lastbastion.app.iogame;

import com.lastbastion.app.GameBootstrap;
import com.lastbastion.app.net.SessionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.net.InetSocketAddress;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke test: 真正把 ioGame NettyRunOne（broker + external WS + logic）跑起来，
 * 验证 TCP 端口可连通，然后停掉。
 *
 * 端口使用 29110/29210，与 {@link IoGameNettyRuntime#DEFAULT_EXTERNAL_PORT}
 * (10110) 错开以免和本地正在跑的服务冲突。CI 里设置 {@code SKIP_IOGAME_BOOT=1}
 * 可以关掉此用例。
 */
@DisabledIfEnvironmentVariable(named = "SKIP_IOGAME_BOOT", matches = "1|true|yes")
class IoGameNettyRuntimeTest {

    @Test
    void bootAndBindPorts() throws Exception {
        GameBootstrap.Services services = new GameBootstrap().boot();
        IoGameNettyRuntime rt = new IoGameNettyRuntime(services, new SessionRegistry());

        int extPort = 29110;
        int brokerPort = 29210;
        rt.start(extPort, brokerPort);
        // 框架启动是异步的，等它完全起来再验证端口
        Thread.sleep(2500);

        assertNotNull(rt.externalServer(), "external server should be built");
        assertNotNull(rt.brokerServer(), "broker server should be built");

        // TCP 握手验证端口对外可达
        assertDoesNotThrow(() -> {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", brokerPort), 1500);
            }
        }, "broker port " + brokerPort + " should accept TCP connections");

        assertDoesNotThrow(() -> {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", extPort), 1500);
            }
        }, "external WS port " + extPort + " should accept TCP connections");

        rt.stop();
    }
}
