import { renderGacha } from "./gacha.js";
import { renderZone } from "./zone.js";
import { renderArena } from "./arena.js";
import { renderBattlePass } from "./battlepass.js";

export function renderLobby(host, ctx) {
    const p = ctx.player();
    host.innerHTML = `
        <div class="panel">
            <h2>主城 · ${p ? p.externalId : "?"}</h2>
            <div class="kv" id="currencies"></div>
            <div class="row" style="margin-top:12px;">
                <button id="btn-idle">结算挂机</button>
                <button class="ghost" id="btn-logout">切换账号</button>
            </div>
        </div>
        <div class="lobby-grid">
            <div class="tile" data-go="gacha">
                <h3>招募</h3>
                <p>10 抽必出 R+，80 抽保底 LEGENDARY；测试服免费券充足。</p>
            </div>
            <div class="tile" data-go="zone">
                <h3>推图</h3>
                <p>Zone 1~3 章共 40 关；线性解锁，挂机 12h（开 BP 24h）。</p>
            </div>
            <div class="tile" data-go="arena">
                <h3>Arena 竞技场</h3>
                <p>每日 5 次免费挑战；换位机制、段位赛分。</p>
            </div>
            <div class="tile" data-go="bp">
                <h3>Battle Pass</h3>
                <p>50 级；免费/付费双轨；测试服可一键 grant XP 看奖励链。</p>
            </div>
        </div>
    `;

    function fillCurrencies() {
        const cur = ctx.player()?.currencies ?? {};
        const dl = host.querySelector("#currencies");
        dl.innerHTML = "";
        for (const [k, v] of Object.entries(cur)) {
            const dk = document.createElement("div"); dk.className = "k"; dk.textContent = k;
            const dv = document.createElement("div"); dv.textContent = String(v);
            dl.appendChild(dk); dl.appendChild(dv);
        }
    }
    fillCurrencies();

    host.querySelectorAll("[data-go]").forEach((el) => {
        el.onclick = () => {
            const target = el.getAttribute("data-go");
            if (target === "gacha") ctx.goto(renderGacha);
            else if (target === "zone") ctx.goto(renderZone);
            else if (target === "arena") ctx.goto(renderArena);
            else if (target === "bp") ctx.goto(renderBattlePass);
        };
    });

    host.querySelector("#btn-idle").onclick = async () => {
        try {
            const r = await ctx.net().call("zone.settleIdle", {});
            ctx.log("idle reward: " + JSON.stringify(r), "ok");
            // refresh player from a no-op login? simpler: just append to currencies if present
            if (ctx.player() && r && typeof r === "object" && r.rewards) {
                const cur = ctx.player().currencies || {};
                for (const [k, v] of Object.entries(r.rewards)) {
                    cur[k] = (cur[k] ?? 0) + (v ?? 0);
                }
                ctx.setPlayer({ ...ctx.player(), currencies: cur });
            }
            fillCurrencies();
        } catch (e) {
            ctx.log("idle failed: " + e.message, "err");
        }
    };

    host.querySelector("#btn-logout").onclick = () => {
        ctx.net()?.close();
        ctx.setPlayer(null);
        ctx.goto(ctx.screens.renderLogin);
    };
}
