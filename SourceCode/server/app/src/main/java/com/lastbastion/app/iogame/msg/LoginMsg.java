package com.lastbastion.app.iogame.msg;

import java.io.Serializable;

/**
 * ioGame 的 Action 方法必须使用 BarSkeleton 支持的序列化类型，默认 JSON/Protobuf 都行。
 * 这里用普通 POJO，ioGame 会通过 Jackson 自动处理。
 */
public final class LoginMsg implements Serializable {
    public String userId;
    public String clientVer;
    /** 设备号；启用 HMAC 鉴权时必填。 */
    public String deviceId;
    /** 客户端时间戳（毫秒），用于 replay 防护。 */
    public long ts;
    /** HMAC-SHA256(secret, userId|deviceId|ts) 的小写 hex。 */
    public String sig;
}
