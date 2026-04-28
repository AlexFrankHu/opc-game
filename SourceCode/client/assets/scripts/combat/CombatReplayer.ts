import { CombatEvent, CombatEventType, CombatResultView } from "../model/CombatLog";

/**
 * 简易战斗回放：按事件顺序依次触发回调；UI 层绑定触发即可。
 */
export class CombatReplayer {

    private queue: CombatEvent[] = [];
    private callbacks: Partial<Record<CombatEventType, (e: CombatEvent) => void>> = {};
    private finished: (() => void) | null = null;

    load(result: CombatResultView): this {
        this.queue = [...result.events];
        return this;
    }

    on(type: CombatEventType, cb: (e: CombatEvent) => void): this {
        this.callbacks[type] = cb;
        return this;
    }

    onFinished(cb: () => void): this {
        this.finished = cb;
        return this;
    }

    async play(delayMs = 120): Promise<void> {
        for (const ev of this.queue) {
            const cb = this.callbacks[ev.type];
            if (cb) cb(ev);
            await new Promise((r) => setTimeout(r, delayMs));
        }
        if (this.finished) this.finished();
    }
}
