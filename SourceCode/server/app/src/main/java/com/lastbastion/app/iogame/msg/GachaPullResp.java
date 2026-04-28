package com.lastbastion.app.iogame.msg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class GachaPullResp implements Serializable {
    public List<Entry> results = new ArrayList<>();

    public static final class Entry implements Serializable {
        public String configId;
        public String rarity;
        public boolean duplicate;
        public long shardsAdded;
    }
}
