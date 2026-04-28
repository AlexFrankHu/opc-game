package com.lastbastion.app.iogame.msg;

import java.io.Serializable;

public final class GachaPullReq implements Serializable {
    public long playerId;
    public String pool;
    public int count;
}
