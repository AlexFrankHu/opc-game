package com.lastbastion.app.iogame.msg;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class ZoneClearResp implements Serializable {
    public boolean won;
    public int chapter;
    public int stage;
    public Map<String, Long> rewards = new HashMap<>();
}
