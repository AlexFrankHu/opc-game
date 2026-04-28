package com.lastbastion.game.numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lastbastion.common.CurrencyType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** 货币上限（resources.json）。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class ResourceTuning {
    public Map<CurrencyType, Long> currencyCaps = new LinkedHashMap<>();

    public EnumMap<CurrencyType, Long> capsAsEnumMap() {
        EnumMap<CurrencyType, Long> out = new EnumMap<>(CurrencyType.class);
        out.putAll(currencyCaps);
        return out;
    }

    public long cap(CurrencyType type) {
        Long v = currencyCaps.get(type);
        return v == null ? Long.MAX_VALUE : v;
    }
}
