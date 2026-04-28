package com.lastbastion.app.iogame.msg;

import java.io.Serializable;

public final class ZoneClearReq implements Serializable {
    public long playerId;
    public int chapter;
    public int stage;
    public boolean allyWon;
}
