package com.lastbastion.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 战斗日志，用于回放/调试/前端动画驱动。
 */
public final class CombatLog {

    public enum EventType {
        ROUND_START,
        UNIT_ACTION,
        DAMAGE,
        HEAL,
        STATUS_APPLIED,
        STATUS_TICK,
        STATUS_EXPIRED,
        BOSS_RAGE,
        UNIT_DEATH,
        BATTLE_END
    }

    public static final class Event {
        public final int round;
        public final EventType type;
        public final String actor;
        public final String target;
        public final double value;
        public final String detail;

        public Event(int round, EventType type, String actor, String target, double value, String detail) {
            this.round = round;
            this.type = type;
            this.actor = actor;
            this.target = target;
            this.value = value;
            this.detail = detail;
        }

        @Override
        public String toString() {
            return "R" + round + " " + type + " " + actor + "→" + target + " v=" + value + " " + detail;
        }
    }

    private final List<Event> events = new ArrayList<>();

    public void add(int round, EventType type, String actor, String target, double value, String detail) {
        events.add(new Event(round, type, actor, target, value, detail));
    }

    public List<Event> events() {
        return Collections.unmodifiableList(events);
    }
}
