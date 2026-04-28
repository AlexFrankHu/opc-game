package com.lastbastion.game.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TASK-011 Analytics.
 *
 * 适配多后端（Firebase / AppsFlyer / 自建数仓）；
 * 默认 InMemorySink 仅用于测试与本地开发。
 */
public final class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    public interface Sink {
        void accept(AnalyticsEvent event);
    }

    public static final class InMemorySink implements Sink {
        private final List<AnalyticsEvent> events = new ArrayList<>();

        @Override
        public synchronized void accept(AnalyticsEvent event) {
            events.add(event);
        }

        public synchronized List<AnalyticsEvent> events() {
            return Collections.unmodifiableList(new ArrayList<>(events));
        }
    }

    public static final class LoggingSink implements Sink {
        @Override
        public void accept(AnalyticsEvent event) {
            log.info("[Analytics] {}", event);
        }
    }

    private final List<Sink> sinks = new CopyOnWriteArrayList<>();

    public void addSink(Sink sink) {
        sinks.add(sink);
    }

    public void emit(AnalyticsEvent event) {
        for (Sink s : sinks) {
            try {
                s.accept(event);
            } catch (Exception ex) {
                log.warn("analytics sink failed", ex);
            }
        }
    }
}
