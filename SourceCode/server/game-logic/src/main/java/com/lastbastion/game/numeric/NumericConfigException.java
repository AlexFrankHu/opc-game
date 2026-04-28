package com.lastbastion.game.numeric;

/** assets/numeric/ 加载失败。fail-fast：禁止吞掉，必须中止启动。 */
public final class NumericConfigException extends RuntimeException {
    public NumericConfigException(String msg) { super(msg); }
    public NumericConfigException(String msg, Throwable cause) { super(msg, cause); }
}
