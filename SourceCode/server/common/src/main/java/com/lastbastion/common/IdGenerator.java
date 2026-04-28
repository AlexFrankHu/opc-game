package com.lastbastion.common;

import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory monotonic id generator. Good enough for MVP; replace with DB sequence / snowflake in prod.
 */
public final class IdGenerator {

    private static final AtomicLong SEQ = new AtomicLong(1);

    private IdGenerator() {
    }

    public static long next() {
        return SEQ.getAndIncrement();
    }
}
