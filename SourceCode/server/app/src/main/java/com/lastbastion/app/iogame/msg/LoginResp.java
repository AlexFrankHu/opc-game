package com.lastbastion.app.iogame.msg;

import java.io.Serializable;

public final class LoginResp implements Serializable {
    public long playerId;
    public String externalId;
    public long registerTimestamp;
}
