import { Action, ActionKey } from "./ActionCodes";
import { NetConfig } from "./NetConfig";

/**
 * 轻量 WebSocket 客户端，对齐 ioGame 的 cmd × subCmd × msgId 帧结构。
 * 以 JSON-over-WS 方式实现；上线前可替换为 Protobuf。
 */
export class NetClient {
    private ws: WebSocket | null = null;
    private msgId = 0;
    private pending = new Map<number, (data: unknown) => void>();
    private heartbeatTimer: number | null = null;

    connect(): Promise<void> {
        return new Promise((resolve, reject) => {
            const ws = new WebSocket(NetConfig.WS_URL);
            this.ws = ws;
            ws.onopen = () => {
                this.startHeartbeat();
                resolve();
            };
            ws.onerror = (err) => reject(err);
            ws.onmessage = (ev) => this.onMessage(ev);
            ws.onclose = () => this.stopHeartbeat();
        });
    }

    call<T = unknown>(action: ActionKey, payload: unknown): Promise<T> {
        if (!this.ws) return Promise.reject(new Error("not connected"));
        const [cmd, subCmd] = Action[action];
        const id = ++this.msgId;
        const frame = { id, cmd, subCmd, payload };
        const json = JSON.stringify(frame);
        return new Promise<T>((resolve, reject) => {
            const timer = setTimeout(() => {
                this.pending.delete(id);
                reject(new Error(`timeout: ${action}`));
            }, NetConfig.REQUEST_TIMEOUT_MS);
            this.pending.set(id, (data) => {
                clearTimeout(timer);
                resolve(data as T);
            });
            this.ws!.send(json);
        });
    }

    private onMessage(ev: MessageEvent) {
        try {
            const frame = JSON.parse(ev.data as string) as {
                id?: number;
                ok?: boolean;
                error?: string;
                data?: unknown;
            };
            if (frame.id != null) {
                const cb = this.pending.get(frame.id);
                if (cb) {
                    this.pending.delete(frame.id);
                    if (frame.ok === false) {
                        console.warn("server error:", frame.error);
                    }
                    cb(frame.data);
                }
            }
        } catch (e) {
            console.error("bad frame", e);
        }
    }

    private startHeartbeat() {
        this.heartbeatTimer = setInterval(() => {
            this.call("user.heartbeat", { t: Date.now() }).catch(() => {});
        }, NetConfig.HEARTBEAT_INTERVAL_MS) as unknown as number;
    }

    private stopHeartbeat() {
        if (this.heartbeatTimer != null) clearInterval(this.heartbeatTimer);
        this.heartbeatTimer = null;
    }
}
