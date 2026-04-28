import { renderLobby } from "./lobby.js";

export function renderBattlePass(host, ctx) {
    host.innerHTML = `
        <div class="panel">
            <div class="row" style="justify-content:space-between;">
                <h2>Battle Pass</h2>
                <button class="ghost" id="back">← 返回主城</button>
            </div>
            <div id="bp-info" class="kv"></div>
            <div class="bp-grid" id="bp-grid"></div>
            <div class="row" style="margin-top:12px;">
                <input id="claim-level" type="number" min="1" max="50" value="1" style="width:80px;" />
                <select id="claim-side"><option value="false">免费</option><option value="true">付费</option></select>
                <button id="btn-claim">领取该等级奖励</button>
                <button class="ghost" id="btn-buy">购买 BP</button>
            </div>
        </div>
    `;
    host.querySelector("#back").onclick = () => ctx.goto(renderLobby);

    function renderGrid() {
        const grid = host.querySelector("#bp-grid");
        grid.innerHTML = "";
        for (let i = 1; i <= 50; i++) {
            const cell = document.createElement("div");
            cell.className = "bp-cell";
            cell.textContent = i;
            grid.appendChild(cell);
        }
    }
    renderGrid();
    host.querySelector("#bp-info").innerHTML = `
        <div class="k">说明</div><div>测试服 BP 数据来自服务端 PlayerContext.battlePassState；点击「领取」会调 bp.claim。</div>
    `;

    host.querySelector("#btn-claim").onclick = async () => {
        const level = Number(host.querySelector("#claim-level").value);
        const premium = host.querySelector("#claim-side").value === "true";
        try {
            const r = await ctx.net().call("bp.claim", { level, premium });
            ctx.log(`bp.claim lv${level} ${premium ? "premium" : "free"}: ` + JSON.stringify(r), "ok");
        } catch (e) {
            ctx.log("bp.claim failed: " + e.message, "err");
        }
    };
    host.querySelector("#btn-buy").onclick = async () => {
        try {
            const r = await ctx.net().call("bp.buy", { tier: "STANDARD" });
            ctx.log("bp.buy: " + JSON.stringify(r), "ok");
        } catch (e) {
            ctx.log("bp.buy failed: " + e.message, "err");
        }
    };
}
