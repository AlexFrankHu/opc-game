import { renderLobby } from "./lobby.js";

const DEFAULT_USER = "demo-" + Math.random().toString(36).slice(2, 8);

export function renderLogin(host, ctx) {
    host.innerHTML = `
        <div class="panel" style="max-width:480px;margin:60px auto;">
            <h2>登录测试服</h2>
            <div class="kv" style="margin-bottom:16px;">
                <div class="k">服务器</div><div><input id="ws" style="width:100%;" /></div>
                <div class="k">玩家 ID</div><div><input id="uid" style="width:100%;" /></div>
                <div class="k">鉴权 secret</div><div><input id="secret" type="password" placeholder="（可空，仅在服务端开启 LOGIN_SHARED_SECRET 时填写）" style="width:100%;" /></div>
            </div>
            <div class="row">
                <button id="btn-login">登录</button>
                <button class="ghost" id="btn-random">随机 ID</button>
                <span class="muted" id="login-msg"></span>
            </div>
        </div>
        <div class="panel" style="max-width:480px;margin:0 auto;">
            <h2>说明</h2>
            <p class="muted" style="font-size:12px;line-height:1.7;">
                这是 Last Bastion 的功能验证 demo（无美术）。<br>
                · 默认连本机 <code>ws://localhost:10100/</code>（服务端 JSON 网关）<br>
                · 玩家 ID 任意字符串即可，首次登录会建立角色档<br>
                · 数据落地到服务端 <code>./data/players/&lt;extId&gt;.ser</code>，重启不丢<br>
                · 可玩流程：抽卡 → 推图 → 结算挂机 → Arena → BP 领奖
            </p>
        </div>
    `;
    const wsInput = host.querySelector("#ws");
    const uidInput = host.querySelector("#uid");
    const secretInput = host.querySelector("#secret");
    const msg = host.querySelector("#login-msg");
    wsInput.value = ctx.net()?.url || (window._ctx && window._ctx.net()?.url) || "ws://localhost:10100/";
    if (!wsInput.value) {
        const u = new URL(window.location.href);
        wsInput.value = u.hostname && u.hostname !== "localhost"
            ? `ws://${u.hostname}:10100/` : "ws://localhost:10100/";
    }
    uidInput.value = DEFAULT_USER;

    host.querySelector("#btn-random").onclick = () => {
        uidInput.value = "demo-" + Math.random().toString(36).slice(2, 8);
    };

    const doLogin = async () => {
        msg.textContent = "连接中...";
        try {
            const secret = secretInput.value.trim();
            await ctx.connect(
                wsInput.value.trim(),
                uidInput.value.trim(),
                secret ? { secret } : {},
            );
            ctx.goto(renderLobby);
        } catch (e) {
            msg.textContent = "失败: " + (e.message || e);
        }
    };
    host.querySelector("#btn-login").onclick = doLogin;
    uidInput.onkeydown = (e) => { if (e.key === "Enter") doLogin(); };
}
