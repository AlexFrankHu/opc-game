package com.lastbastion.app.iogame.msg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ioGame Action 的请求/响应 DTO 集合。
 *
 * 由于 ioGame 用反射 + JSON/Protobuf 序列化返回值，DTO 里只放 {@code public} 字段即可。
 * 保持与 JSON Dev Gateway 的 payload key 尽量一致，避免两套协议互相不兼容。
 */
public final class Messages {

    private Messages() {}

    /** 心跳。 */
    public static final class HeartbeatResp {
        public long t;
    }

    /** 玩家基础请求，只携带 playerId（ioGame 生产环境应走 session.userId，这里保持简单）。 */
    public static class PlayerIdReq {
        public long playerId;
    }

    // --------- survivor ---------

    public static final class SurvivorLevelUpReq extends PlayerIdReq {
        public long instanceId;
        public int small;
        public int medium;
        public int large;
    }

    public static final class SurvivorLevelUpResp {
        public long instanceId;
        public int level;
        public int star;
    }

    // --------- zone ---------

    public static final class ZoneSettleIdleReq extends PlayerIdReq {
    }

    public static final class ZoneSettleIdleResp {
        public long elapsedMs;
        public long effectiveMs;
        public Map<String, Long> currency = new LinkedHashMap<>();
    }

    // --------- arena ---------

    public static final class ArenaMatchReq extends PlayerIdReq {
    }

    public static final class ArenaRoster {
        public long playerId;
        public String nickname;
        public int power;
        public int rank;
        public int score;
    }

    public static final class ArenaMatchResp {
        public List<ArenaRoster> candidates = new ArrayList<>();
    }

    public static final class ArenaChallengeReq extends PlayerIdReq {
        public long opponentId;
        public boolean won;
    }

    public static final class ArenaChallengeResp {
        public long opponentId;
        public boolean allyWon;
        public int selfRankBefore;
        public int selfRankAfter;
        public int scoreDelta;
    }

    public static final class ArenaLeaderboardReq {
        public int n = 10;
    }

    public static final class ArenaLeaderboardResp {
        public List<ArenaRoster> top = new ArrayList<>();
    }

    public static final class ArenaBuyChallengeReq extends PlayerIdReq {
    }

    public static final class ArenaBuyChallengeResp {
        public int remainingBuys;
    }

    // --------- battle pass ---------

    public static final class BpClaimReq extends PlayerIdReq {
        public int level;
        public boolean premium;
    }

    public static final class BpClaimResp {
        public int level;
        public long xp;
        public boolean premiumActive;
        public List<Integer> freeClaimed = new ArrayList<>();
        public List<Integer> premiumClaimed = new ArrayList<>();
        public List<Map<String, Object>> rewards = new ArrayList<>();
    }

    public static final class BpBuyReq extends PlayerIdReq {
        public String orderId;
    }

    public static final class BpBuyResp {
        public boolean premiumActive;
    }

    // --------- onboarding ---------

    public static final class OnboardingCompleteReq extends PlayerIdReq {
        public String step;
    }

    public static final class OnboardingResp {
        public String current;
        public boolean skipped;
    }

    public static final class OnboardingSkipReq extends PlayerIdReq {
    }

    /** 将 {@code Map<EnumKey, Long>} 转换成字符串 key，避免 ioGame 序列化器不识别枚举。 */
    public static Map<String, Long> stringKeys(Map<?, Long> m) {
        Map<String, Long> out = new HashMap<>(m.size());
        for (Map.Entry<?, Long> e : m.entrySet()) {
            out.put(String.valueOf(e.getKey()), e.getValue());
        }
        return out;
    }
}
