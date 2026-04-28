package com.lastbastion.app.net;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 一个 Action 处理器（业务路由单元）。对应 ioGame 的 {@code ActionController} 方法。
 * 真实 ioGame 通过注解 + APT 生成这层 dispatch，此处手写以保持 JDK17 兼容。
 */
public interface ActionHandler {

    /** e.g. "survivor.pullGacha" */
    String name();

    /** 是否需要已登录态。默认 true；只有 user.login 与 user.heartbeat 是 false。 */
    default boolean requiresLogin() { return true; }

    /**
     * @return 响应负载（将被序列化为 JSON）；null 表示空响应。
     */
    Object handle(Session session, JsonNode payload) throws Exception;
}
