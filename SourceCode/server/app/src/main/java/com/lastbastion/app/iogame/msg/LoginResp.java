package com.lastbastion.app.iogame.msg;

import java.io.Serializable;

public final class LoginResp implements Serializable {
    public long playerId;
    public String externalId;
    public long registerTimestamp;
    /** 鉴权状态："OK" 或 "OPEN_MODE"（无 secret）/"BAD_SIGNATURE"/"STALE"/"MISSING_FIELDS"。 */
    public String authStatus;
}
