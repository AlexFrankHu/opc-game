package com.lastbastion.common;

/** Server-side error codes; shared with the client for i18n mapping. */
public enum ErrorCode {
    OK(0, "ok"),
    ILLEGAL_ARG(1001, "illegal argument"),
    NOT_FOUND(1002, "not found"),
    INSUFFICIENT_CURRENCY(1101, "insufficient currency"),
    INSUFFICIENT_ITEM(1102, "insufficient item"),
    GEAR_LOCKED(1201, "gear is locked"),
    GEAR_ALREADY_EQUIPPED(1202, "gear already equipped on another survivor"),
    BAG_FULL(1203, "bag is full"),
    ENHANCE_MAX(1204, "enhancement reached maximum level"),
    FUSION_MISMATCH(1301, "augment fusion requires 3 same-type same-star"),
    ZONE_LOCKED(1401, "zone locked (previous stage not cleared)"),
    ARENA_DAILY_LIMIT(1501, "arena daily challenge limit reached"),
    ARENA_BUY_LIMIT(1502, "arena daily buy limit reached"),
    GACHA_POOL_EMPTY(1601, "gacha pool empty"),
    PASS_NOT_ACTIVE(1701, "battle pass not active"),
    PASS_ALREADY_CLAIMED(1702, "battle pass level already claimed"),
    STARTER_PACK_USED(1703, "starter pack already purchased"),
    OFFER_EXPIRED(1704, "limited offer expired"),
    GUIDE_STEP_ORDER(1801, "onboarding step out of order"),
    INTERNAL(9000, "internal server error"),
    UNKNOWN_ACTION(9001, "unknown action"),
    NOT_LOGGED_IN(9002, "not logged in"),
    BAD_FRAME(9003, "malformed frame");

    private final int code;
    private final String description;

    ErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int code() {
        return code;
    }

    public String description() {
        return description;
    }
}
