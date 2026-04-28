import { renderLobby } from "./lobby.js";

export function renderGacha(host, ctx) {
    host.innerHTML = `
        <div class="panel">
            <div class="row" style="justify-content:space-between;">
                <h2>招募 · Recruitment</h2>
                <button class="ghost" id="back">← 返回主城</button>
            </div>
            <div class="row">
                <button id="pull-1">单抽</button>
                <button id="pull-10">十连</button>
                <span class="muted" style="margin-left:12px;">招募券 / 保底进度由服务端记录，每次返回最新结果</span>
            </div>
            <div id="result-area" style="margin-top:16px;"></div>
        </div>
    `;
    host.querySelector("#back").onclick = () => ctx.goto(renderLobby);

    async function pull(count) {
        const area = host.querySelector("#result-area");
        area.innerHTML = `<div class="muted">抽卡中...</div>`;
        try {
            const r = await ctx.net().call("survivor.pullGacha", { pool: "PREMIUM", count });
            renderResult(area, r);
        } catch (e) {
            area.innerHTML = `<div style="color:var(--red);">抽卡失败: ${e.message}</div>`;
        }
    }

    host.querySelector("#pull-1").onclick = () => pull(1);
    host.querySelector("#pull-10").onclick = () => pull(10);
}

function renderResult(area, payload) {
    const cards = payload?.results || payload?.items || payload || [];
    area.innerHTML = "";
    const grid = document.createElement("div");
    grid.className = "gacha-grid";
    if (Array.isArray(cards)) {
        for (const c of cards) {
            const rarity = c.rarity || c.tier || "RARE";
            const name = c.name || c.survivorId || c.id || "?";
            const card = document.createElement("div");
            card.className = "gacha-card " + rarity;
            card.innerHTML = `<div class="rarity">${rarity[0]}</div><div class="name">${name}</div>`;
            grid.appendChild(card);
        }
    } else {
        const dump = document.createElement("pre");
        dump.style.fontSize = "11px";
        dump.style.background = "var(--panel-2)";
        dump.style.padding = "8px";
        dump.style.borderRadius = "6px";
        dump.textContent = JSON.stringify(payload, null, 2);
        area.appendChild(dump);
        return;
    }
    area.appendChild(grid);
    const summary = document.createElement("div");
    summary.className = "muted";
    summary.style.marginTop = "8px";
    summary.style.fontSize = "12px";
    const counts = cards.reduce((acc, c) => {
        const r = c.rarity || c.tier || "RARE";
        acc[r] = (acc[r] ?? 0) + 1;
        return acc;
    }, {});
    summary.textContent = "本次稀有度分布: " + Object.entries(counts).map(([k, v]) => `${k}×${v}`).join(", ");
    area.appendChild(summary);
}
