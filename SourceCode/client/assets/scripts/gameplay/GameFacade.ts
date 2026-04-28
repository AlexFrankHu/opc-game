import { NetClient } from "../net/NetClient";
import { Analytics } from "../analytics/Analytics";
import {
    ArenaRecordView,
    BattlePassStateView,
    SurvivorInstance,
} from "../model/Types";
import { CombatResultView } from "../model/CombatLog";

/**
 * 游戏全局 Facade：集中封装对服务端的所有调用，供 UI 层绑定。
 */
export class GameFacade {
    private readonly net: NetClient;

    constructor(net: NetClient) {
        this.net = net;
    }

    async login(userId: string): Promise<void> {
        Analytics.setUserId(userId);
        await this.net.call("user.login", { userId });
        Analytics.track("login");
    }

    async gachaPull(pool: "FREE" | "PREMIUM", count: 1 | 10): Promise<unknown> {
        Analytics.track("gacha_attempt", { pool, count });
        return this.net.call("survivor.pullGacha", { pool, count });
    }

    async zoneClear(chapter: number, stage: number): Promise<CombatResultView> {
        const r = await this.net.call<CombatResultView>("zone.clear", { chapter, stage });
        Analytics.track("zone_clear", { chapter, stage, outcome: r.outcome });
        return r;
    }

    async zoneSettleIdle(): Promise<Record<string, number>> {
        const r = await this.net.call<Record<string, number>>("zone.settleIdle", {});
        Analytics.track("idle_settled");
        return r;
    }

    async arenaMatch(): Promise<unknown> {
        return this.net.call("arena.match", {});
    }

    async arenaChallenge(opponentId: number): Promise<ArenaRecordView> {
        const r = await this.net.call<ArenaRecordView>("arena.challenge", { opponentId });
        Analytics.track("arena_challenge", { result: r.won ? "win" : "lose" });
        return r;
    }

    async claimBattlePassLevel(level: number, premiumSide: boolean): Promise<unknown> {
        Analytics.track("bp_claim", { level, premium: premiumSide });
        return this.net.call("bp.claim", { level, premium: premiumSide });
    }

    async iapVerify(receipt: string, productId: string): Promise<unknown> {
        Analytics.track("iap_attempt", { productId });
        return this.net.call("store.iapVerify", { receipt, productId });
    }

    async completeOnboardingStep(step: string): Promise<void> {
        Analytics.track("tutorial_step", { step });
        await this.net.call("onboarding.completeStep", { step });
    }

    async levelUpSurvivor(id: number, books: { s: number; m: number; l: number }): Promise<SurvivorInstance> {
        return this.net.call<SurvivorInstance>("survivor.levelUp", { id, books });
    }

    async getBattlePassState(): Promise<BattlePassStateView> {
        return this.net.call<BattlePassStateView>("bp.claim", { _query: true });
    }
}
