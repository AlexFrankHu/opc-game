package com.lastbastion.common;

public class GameException extends RuntimeException {

    private final ErrorCode errorCode;

    public GameException(ErrorCode errorCode) {
        super(errorCode.name() + ": " + errorCode.description());
        this.errorCode = errorCode;
    }

    public GameException(ErrorCode errorCode, String message) {
        super(errorCode.name() + ": " + message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
