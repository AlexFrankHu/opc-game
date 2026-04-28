import { renderLobby } from "./lobby.js";

export function renderArena(host, ctx) {
    host.innerHTML = `
        <div class="panel">
            <div class="row" style="justify-content:space-between;">
                <h2>Arena · 竞技场</h2>
                <button class="ghost" id="back">← 返回主城</button>
            </div>
            <div class="row">
                <button id="btn-match">匹配对手</button>
                <button id="btn-board">查看排行榜</button>
                <button id="btn-buy" class="ghost">购买挑战券</button>
            </div>
            <div id="match-area" style="margin-top:16px;"></div>
            <div id="board-area" style="margin-top:16px;"></div>
        </div>
    `;
    host.querySelector("#back").onclick = () => ctx.goto(renderLobby);

    let lastMatch = null;

    host.querySelector("#btn-match").onclick = async () => {
        const area = host.querySelector("#match-area");
        area.innerHTML = `<div class="muted">匹配中...</div>`;
        try {
            const r = await ctx.net().call("arena.match", {});
            lastMatch = r;
            renderMatchArea(area, r, async (oppId) => {
                area.innerHTML = `<div class="muted">挑战 ${oppId}...</div>`;
                try {
                    const result = await ctx.net().call("arena.challenge", { opponentId: oppId });
                    renderChallengeResult(area, result, lastMatch);
                } catch (e) {
                    area.innerHTML = `<div style="color:var(--red);">挑战失败: ${e.message}</div>`;
                }
            });
        } catch (e) {
            area.innerHTML = `<div style="color:var(--red);">匹配失败: ${e.message}</div>`;
        }
    };

    host.querySelector("#btn-board").onclick = async () => {
        const area = host.querySelector("#board-area");
        area.innerHTML = `<div class="muted">读取排行榜...</div>`;
        try {
            const r = await ctx.net().call("arena.leaderboard", { top: 20 });
            renderLeaderboard(area, r);
        } catch (e) {
            area.innerHTML = `<div style="color:var(--red);">${e.message}</div>`;
        }
    };

    host.querySelector("#btn-buy").onclick = async () => {
        try {
            const r = await ctx.net().call("arena.buyChallenge", {});
            ctx.log("buyChallenge: " + JSON.stringify(r), "ok");
        } catch (e) {
            ctx.log("buy failed: " + e.message, "err");
        }
    };
}

function renderMatchArea(area, payload, onPick) {
    const opps = payload?.candidates || payload?.opponents || payload || [];
    if (!Array.isArray(opps) || opps.length === 0) {
        area.innerHTML = "<pre>" + JSON.stringify(payload, null, 2) + "</pre>";
        return;
    }
    area.innerHTML = "";
    const tbl = document.createElement("table");
    tbl.innerHTML = `<thead><tr><th>名称</th><th>战力</th><th>积分</th><th>段位</th><th></th></tr></thead><tbody></tbody>`;
    const tbody = tbl.querySelector("tbody");
    for (const o of opps) {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${o.name ?? o.opponentId ?? "?"}</td>
            <td>${o.power ?? "?"}</td>
            <td>${o.score ?? "?"}</td>
            <td>${o.rank ?? "?"}</td>
            <td><button data-pick>挑战</button></td>
        `;
        tr.querySelector("button").onclick = () => onPick(o.opponentId ?? o.id);
        tbody.appendChild(tr);
    }
    area.appendChild(tbl);
}

function renderChallengeResult(area, r, lastMatch) {
    area.innerHTML = "";
    const data = r || {};
    const div = document.createElement("div");
    div.className = "panel";
    const won = data.won ?? data.win;
    div.innerHTML = `
        <div style="font-size:18px;font-weight:700;color:${won ? "var(--green)" : "var(--red)"}">${won ? "胜利!" : "失败"}</div>
        <div class="muted" style="font-size:12px;">${data.swap ? "换位生效" : "未触发换位"} · 分数变化: ${data.scoreDelta ?? "?"}</div>
    `;
    area.appendChild(div);
}

function renderLeaderboard(area, r) {
    const list = r?.entries || r || [];
    area.innerHTML = "";
    const tbl = document.createElement("table");
    tbl.innerHTML = `<thead><tr><th>#</th><th>名称</th><th>积分</th><th>段位</th></tr></thead><tbody></tbody>`;
    const tbody = tbl.querySelector("tbody");
    if (Array.isArray(list)) {
        list.forEach((e, idx) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `<td>${idx + 1}</td><td>${e.name ?? e.playerId ?? "?"}</td><td>${e.score ?? "?"}</td><td>${e.rank ?? "?"}</td>`;
            tbody.appendChild(tr);
        });
    } else {
        const pre = document.createElement("pre");
        pre.style.fontSize = "11px";
        pre.textContent = JSON.stringify(r, null, 2);
        area.appendChild(pre);
        return;
    }
    area.appendChild(tbl);
}
