// 与 server/app/.../ActionRegistry.java 对齐的 cmd × subCmd 映射。
export const Action = {
    "user.login":              [1, 1],
    "user.heartbeat":          [1, 2],
    "survivor.levelUp":        [2, 1],
    "survivor.starUp":         [2, 2],
    "survivor.skillUp":        [2, 3],
    "survivor.pullGacha":      [2, 4],
    "gear.equip":              [3, 1],
    "gear.unequip":            [3, 2],
    "gear.enhance":            [3, 3],
    "gear.decompose":          [3, 4],
    "gear.lock":               [3, 5],
    "augment.fuse":            [4, 1],
    "augment.insert":          [4, 2],
    "augment.remove":          [4, 3],
    "zone.clear":              [5, 1],
    "zone.sweep":              [5, 2],
    "zone.settleIdle":         [5, 3],
    "arena.match":             [6, 1],
    "arena.challenge":         [6, 2],
    "arena.leaderboard":       [6, 3],
    "arena.buyChallenge":      [6, 4],
    "bp.claim":                [7, 1],
    "bp.buy":                  [7, 2],
    "store.iapVerify":         [8, 1],
    "store.starterPackBuy":    [8, 2],
    "store.limitedOfferBuy":   [8, 3],
    "onboarding.completeStep": [9, 1],
    "onboarding.skip":         [9, 2],
    "onboarding.claimQuest":   [9, 3],
    "analytics.track":         [10, 1],
};

const REQUEST_TIMEOUT_MS = 10000;

export class NetClient {
    constructor(url, log) {
        this.url = url;
        this.log = log;
        this.ws = null;
        this.msgId = 0;
        this.pending = new Map();
        this.heartbeatTimer = null;
        this.onStatusChange = () => {};
    }

    setStatusCallback(cb) { this.onStatusChange = cb; }

    connect() {
        this.onStatusChange("loading");
        return new Promise((resolve, reject) => {
            try {
                const ws = new WebSocket(this.url);
                this.ws = ws;
                ws.onopen = () => {
                    this.onStatusChange("on");
                    this.startHeartbeat();
                    this.log("connected " + this.url, "ok");
                    resolve();
                };
                ws.onerror = () => {
                    this.onStatusChange("off");
                    this.log("websocket error", "err");
                    reject(new Error("ws error"));
                };
                ws.onmessage = (ev) => this.onMessage(ev);
                ws.onclose = () => {
                    this.onStatusChange("off");
                    this.stopHeartbeat();
                    this.log("connection closed", "info");
                };
            } catch (e) {
                this.onStatusChange("off");
                reject(e);
            }
        });
    }

    call(action, payload) {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            return Promise.reject(new Error("not connected"));
        }
        const code = Action[action];
        if (!code) return Promise.reject(new Error("unknown action: " + action));
        const id = ++this.msgId;
        const frame = { id, cmd: code[0], subCmd: code[1], action, payload: payload ?? {} };
        const json = JSON.stringify(frame);
        return new Promise((resolve, reject) => {
            const timer = setTimeout(() => {
                this.pending.delete(id);
                reject(new Error("timeout: " + action));
            }, REQUEST_TIMEOUT_MS);
            this.pending.set(id, { resolve, reject, timer, action });
            this.ws.send(json);
        });
    }

    onMessage(ev) {
        let frame;
        try {
            frame = JSON.parse(ev.data);
        } catch (e) {
            this.log("bad frame: " + ev.data, "err");
            return;
        }
        if (frame.id == null) return;
        const p = this.pending.get(frame.id);
        if (!p) return;
        this.pending.delete(frame.id);
        clearTimeout(p.timer);
        if (frame.ok === false) {
            this.log(`${p.action} ✗ ${frame.error || "unknown"}`, "err");
            p.reject(new Error(frame.error || "request failed"));
        } else {
            if (p.action !== "user.heartbeat") {
                this.log(`${p.action} ✓`, "ok");
            }
            p.resolve(frame.data);
        }
    }

    startHeartbeat() {
        this.heartbeatTimer = setInterval(() => {
            if (this.ws && this.ws.readyState === WebSocket.OPEN) {
                this.call("user.heartbeat", { t: Date.now() }).catch(() => {});
            }
        }, 30000);
    }

    stopHeartbeat() {
        if (this.heartbeatTimer) clearInterval(this.heartbeatTimer);
        this.heartbeatTimer = null;
    }

    close() {
        if (this.ws) this.ws.close();
    }
}
