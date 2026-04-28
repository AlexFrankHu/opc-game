package com.lastbastion.game.analytics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TASK-011 埋点事件结构（Firebase/AppsFlyer 通用格式）。
 */
public final class AnalyticsEvent {

    private final String name;
    private final long timestamp;
    private final Map<String, Object> properties;

    private AnalyticsEvent(String name, long timestamp, Map<String, Object> properties) {
        this.name = name;
        this.timestamp = timestamp;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    public String name() { return name; }
    public long timestamp() { return timestamp; }
    public Map<String, Object> properties() { return properties; }

    public static Builder of(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private final Map<String, Object> props = new LinkedHashMap<>();
        private long timestamp = System.currentTimeMillis();

        private Builder(String name) {
            this.name = name;
        }

        public Builder prop(String key, Object value) {
            props.put(key, value);
            return this;
        }

        public Builder at(long ts) {
            this.timestamp = ts;
            return this;
        }

        public AnalyticsEvent build() {
            return new AnalyticsEvent(name, timestamp, props);
        }
    }

    @Override
    public String toString() {
        return name + " " + properties;
    }
}
