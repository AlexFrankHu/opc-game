/**
 * 通信配置。生产环境应由环境变量/构建脚本注入。
 */
export const NetConfig = {
    WS_URL: "ws://127.0.0.1:10100/ws",
    RECONNECT_MAX: 5,
    HEARTBEAT_INTERVAL_MS: 15_000,
    REQUEST_TIMEOUT_MS: 10_000,
};
