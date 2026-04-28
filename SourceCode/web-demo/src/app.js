import { NetClient } from "./net.js";
import { renderLogin } from "./screens/login.js";
import { renderLobby } from "./screens/lobby.js";
import { renderGacha } from "./screens/gacha.js";
import { renderZone } from "./screens/zone.js";
import { renderArena } from "./screens/arena.js";
import { renderBattlePass } from "./screens/battlepass.js";

const DEFAULT_WS = (() => {
    const u = new URL(window.location.href);
    const override = u.searchParams.get("ws");
    if (override) return override;
    if (u.hostname === "" || u.hostname === "localhost" || u.hostname === "127.0.0.1") {
        return "ws://localhost:10100/";
    }
    return `ws://${u.hostname}:10100/`;
})();

const state = {
    net: null,
    player: null,
    serverUrl: DEFAULT_WS,
};

const app = document.getElementById("app");
const statusDot = document.getElementById("conn-status");
const playerTag = document.getElementById("player-tag");
const serverConfig = document.getElementById("server-config");
const logEl = document.getElementById("log");

document.getElementById("log-clear").onclick = () => { logEl.textContent = ""; };

function log(msg, level) {
    const line = document.createElement("div");
    if (level) line.className = level;
    const ts = new Date().toLocaleTimeString();
    line.textContent = `[${ts}] ${msg}`;
    logEl.appendChild(line);
    logEl.scrollTop = logEl.scrollHeight;
}

function setStatus(s) {
    statusDot.classList.remove("dot-on", "dot-off", "dot-loading");
    if (s === "on") { statusDot.classList.add("dot-on"); statusDot.textContent = "ONLINE"; }
    else if (s === "loading") { statusDot.classList.add("dot-loading"); statusDot.textContent = "CONNECTING"; }
    else { statusDot.classList.add("dot-off"); statusDot.textContent = "OFFLINE"; }
}

setStatus("off");
serverConfig.textContent = `server = ${state.serverUrl}`;

function setScreen(renderFn, ...args) {
    app.innerHTML = "";
    renderFn(app, ctxApi(), ...args);
}

function ctxApi() {
    return {
        net: () => state.net,
        player: () => state.player,
        log,
        goto: setScreen,
        screens: { renderLogin, renderLobby, renderGacha, renderZone, renderArena, renderBattlePass },
        setPlayer: (p) => {
            state.player = p;
            playerTag.textContent = p ? `${p.externalId} · #${p.playerId}` : "";
        },
        connect: async (url, userId) => {
            if (state.net) state.net.close();
            state.serverUrl = url;
            serverConfig.textContent = `server = ${url}`;
            const net = new NetClient(url, log);
            net.setStatusCallback(setStatus);
            state.net = net;
            await net.connect();
            const p = await net.call("user.login", { userId });
            state.player = p;
            playerTag.textContent = `${p.externalId} · #${p.playerId}`;
            return p;
        },
    };
}

setScreen(renderLogin);
window._ctx = ctxApi(); // expose for console debugging
