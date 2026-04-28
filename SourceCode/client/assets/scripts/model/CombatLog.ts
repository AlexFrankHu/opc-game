/**
 * 客户端回放服务端战斗结果用的日志结构（镜像 combat-engine.CombatLog）。
 */

export enum CombatEventType {
    ROUND_START = "ROUND_START",
    ACT_START = "ACT_START",
    DAMAGE = "DAMAGE",
    HEAL = "HEAL",
    STATUS_APPLIED = "STATUS_APPLIED",
    STATUS_EXPIRED = "STATUS_EXPIRED",
    SHIELD_ABSORBED = "SHIELD_ABSORBED",
    BOSS_RAGE = "BOSS_RAGE",
    UNIT_DOWN = "UNIT_DOWN",
    END = "END",
}

export interface CombatEvent {
    round: number;
    type: CombatEventType;
    actor?: string;
    target?: string;
    value?: number;
    detail?: string;
}

export enum CombatOutcome {
    ALLY_WIN = "ALLY_WIN",
    ENEMY_WIN = "ENEMY_WIN",
    DRAW = "DRAW",
}

export interface CombatResultView {
    outcome: CombatOutcome;
    totalRounds: number;
    events: CombatEvent[];
}
