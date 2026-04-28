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
        connect: async (url, userId, opts = {}) => {
            if (state.net) state.net.close();
            state.serverUrl = url;
            serverConfig.textContent = `server = ${url}`;
            const net = new NetClient(url, log);
            net.setStatusCallback(setStatus);
            state.net = net;
            await net.connect();
            const payload = { userId };
            if (opts.secret) {
                // 启用 HMAC 鉴权时，浏览器侧用 SubtleCrypto 生成签名。
                const deviceId = opts.deviceId || ("web-" + Math.random().toString(36).slice(2, 10));
                const ts = Date.now();
                const sig = await hmacSha256(opts.secret, `${userId}|${deviceId}|${ts}`);
                payload.deviceId = deviceId;
                payload.ts = ts;
                payload.sig = sig;
            }
            const p = await net.call("user.login", payload);
            state.player = p;
            playerTag.textContent = `${p.externalId} · #${p.playerId}`;
            if (p.authStatus) log(`auth: ${p.authStatus}`, p.authStatus === "OK" || p.authStatus === "OPEN_MODE" ? "ok" : "warn");
            return p;
        },
    };
}

async function hmacSha256(secret, message) {
    const enc = new TextEncoder();
    const key = await crypto.subtle.importKey(
        "raw",
        enc.encode(secret),
        { name: "HMAC", hash: "SHA-256" },
        false,
        ["sign"],
    );
    const sig = await crypto.subtle.sign("HMAC", key, enc.encode(message));
    return [...new Uint8Array(sig)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

setScreen(renderLogin);
window._ctx = ctxApi(); // expose for console debugging
