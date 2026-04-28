import { _decorator, Component, Label } from "cc";
import { NetClient } from "../net/NetClient";
import { GameFacade } from "../gameplay/GameFacade";
import { Analytics } from "../analytics/Analytics";

const { ccclass, property } = _decorator;

/**
 * 主场景根节点。实际项目中会拆分为 Lobby / Zone / Arena / Shop 面板。
 */
@ccclass("MainScene")
export class MainScene extends Component {

    @property(Label)
    statusLabel: Label | null = null;

    private facade!: GameFacade;

    async start() {
        const net = new NetClient();
        this.setStatus("connecting...");
        try {
            await net.connect();
            this.facade = new GameFacade(net);
            await this.facade.login("guest-" + Date.now());
            this.setStatus("connected");
            Analytics.track("client_boot");
        } catch (e) {
            this.setStatus("offline (dev mode)");
            console.warn("net connect failed:", e);
        }
    }

    private setStatus(msg: string) {
        if (this.statusLabel) this.statusLabel.string = msg;
    }
}
