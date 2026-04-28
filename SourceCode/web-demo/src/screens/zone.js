import { renderLobby } from "./lobby.js";

const CHAPTERS = 3;
const STAGES_PER_CHAPTER = 15;

export function renderZone(host, ctx) {
    host.innerHTML = `
        <div class="panel">
            <div class="row" style="justify-content:space-between;">
                <h2>推图 · Zone</h2>
                <button class="ghost" id="back">← 返回主城</button>
            </div>
            <div class="tabs" id="chapter-tabs"></div>
            <div id="stage-area"></div>
        </div>
        <div class="panel" id="combat-panel" style="display:none;">
            <h2>战斗回放</h2>
            <div class="kv" id="combat-summary"></div>
            <div class="combat-log" id="combat-log"></div>
        </div>
    `;
    host.querySelector("#back").onclick = () => ctx.goto(renderLobby);

    const tabs = host.querySelector("#chapter-tabs");
    let currentChapter = 1;
    for (let c = 1; c <= CHAPTERS; c++) {
        const b = document.createElement("button");
        b.textContent = "第 " + c + " 章";
        if (c === 1) b.classList.add("active");
        b.onclick = () => {
            currentChapter = c;
            tabs.querySelectorAll("button").forEach((x) => x.classList.remove("active"));
            b.classList.add("active");
            renderStages();
        };
        tabs.appendChild(b);
    }

    const stageArea = host.querySelector("#stage-area");
    function renderStages() {
        stageArea.innerHTML = "";
        const grid = document.createElement("div");
        grid.className = "zone-grid";
        for (let s = 1; s <= STAGES_PER_CHAPTER; s++) {
            const cell = document.createElement("div");
            cell.className = "zone-cell";
            cell.innerHTML = `<div class="stage-id">${currentChapter}-${s}</div>
                              <div class="stage-tag">${s % 5 === 0 ? "BOSS" : "Normal"}</div>`;
            cell.onclick = () => clearStage(currentChapter, s);
            grid.appendChild(cell);
        }
        stageArea.appendChild(grid);
    }
    renderStages();

    async function clearStage(chapter, stage) {
        ctx.log(`zone.clear ${chapter}-${stage}...`, "info");
        try {
            const r = await ctx.net().call("zone.clear", { chapter, stage });
            renderCombatPanel(host, r, chapter, stage);
        } catch (e) {
            ctx.log("zone.clear failed: " + e.message, "err");
        }
    }
}

function renderCombatPanel(host, r, chapter, stage) {
    const panel = host.querySelector("#combat-panel");
    panel.style.display = "block";
    const summary = host.querySelector("#combat-summary");
    summary.innerHTML = "";
    const data = r || {};
    const won = data.won ?? (data.outcome === "ALLY_WIN");
    const kvs = [
        ["关卡", `${chapter}-${stage}`],
        ["结果", won ? "胜利" : (data.outcome || data.won === false ? "失败" : "?")],
    ];
    if (data.totalRounds != null) kvs.push(["回合数", data.totalRounds]);
    if (data.rewards) kvs.push(["奖励", Object.entries(data.rewards).map(([k,v]) => `${k.replace("CURRENCY_","")}+${v}`).join(", ")]);
    for (const [k, v] of kvs) {
        const dk = document.createElement("div"); dk.className = "k"; dk.textContent = k;
        const dv = document.createElement("div"); dv.textContent = String(v);
        summary.appendChild(dk); summary.appendChild(dv);
    }

    const logEl = host.querySelector("#combat-log");
    logEl.innerHTML = "";
    const events = data.log?.events || data.events || [];
    if (events.length === 0) {
        logEl.textContent = JSON.stringify(data, null, 2);
        return;
    }
    let lastRound = 0;
    for (const e of events) {
        if (e.round && e.round !== lastRound) {
            const div = document.createElement("div");
            div.className = "turn";
            div.textContent = `── Round ${e.round} ──`;
            logEl.appendChild(div);
            lastRound = e.round;
        }
        const div = document.createElement("div");
        let cls = "hit";
        if (e.type === "DEAD" || (e.text && /dead|死/i.test(e.text))) cls = "dead";
        else if (e.type === "HEAL") cls = "heal";
        div.className = cls;
        div.textContent = ` ${e.actor || ""} -> ${e.target || ""}  ${e.type || ""}  ${e.text || ""}  dmg=${e.damage ?? ""}`.trim();
        logEl.appendChild(div);
    }
    logEl.scrollTop = logEl.scrollHeight;
}
