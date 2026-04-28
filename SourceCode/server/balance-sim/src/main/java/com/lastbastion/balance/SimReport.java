package com.lastbastion.balance;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregate output of all simulators. Serialised to JSON / pretty-printed Markdown.
 */
public final class SimReport {

    public final Map<String, Map<String, Object>> sims = new LinkedHashMap<>();
    /** Errors / hard-fail messages per sim. Empty = all green. */
    public final Map<String, String> assertionFailures = new LinkedHashMap<>();
    public long generatedAtMs = System.currentTimeMillis();

    public Map<String, Object> beginSim(String name) {
        Map<String, Object> bag = new LinkedHashMap<>();
        sims.put(name, bag);
        return bag;
    }

    public void fail(String simName, String message) {
        assertionFailures.put(simName, message);
    }

    public boolean allPassed() {
        return assertionFailures.isEmpty();
    }
}
