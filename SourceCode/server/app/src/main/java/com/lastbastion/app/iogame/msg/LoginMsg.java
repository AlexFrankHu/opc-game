package com.lastbastion.app.iogame.msg;

import java.io.Serializable;

/**
 * ioGame 的 Action 方法必须使用 BarSkeleton 支持的序列化类型，默认 JSON/Protobuf 都行。
 * 这里用普通 POJO，ioGame 会通过 Jackson 自动处理。
 */
public final class LoginMsg implements Serializable {
    public String userId;
    public String clientVer;
}
