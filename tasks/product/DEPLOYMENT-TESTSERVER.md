# 测试服部署指南 — Last Bastion

> 目的：给 QA / 运维一份可以直接去「下单云机器」的机器规格与安装步骤清单，
> 覆盖 **单机 100 并发（alpha）**、**双机 500 并发（closed beta）**、**集群 2000 并发（open beta）** 三档。
> 代码基线：`SourceCode/server/` 当前 main 分支（ioGame 21.26 + Java 21）。
>
> **配套脚本**：[`deploy/`](../../deploy/) 已经提供 `Dockerfile` + `docker-compose.yml` + Nginx + MySQL init + systemd unit；
> 看完本规格说明后想直接拉起，跳到 [`deploy/README.md`](../../deploy/README.md) 即可一键部署。

## 0. TL;DR

- **当 100 人内测**：**1 台** 4C8G Linux，单 JVM 跑 `app` 模块 即可；数据落 `./data/players/` 本地磁盘。
- **500 人内测**：**1 台 8C16G 游戏服 + 1 台 4C8G 中间件服**（Redis + MySQL）。
- **2000 人公测**：**2 台游戏服 + 独立 MySQL 主备 + Redis 哨兵 + Nginx TLS 前置**。

详见下文。

---

## 1. 代码需求回顾

运行时由 `SourceCode/server/app/Main.java` 启动，会监听 3 个端口：

| 端口 | 对应运行时 | 协议 | 默认值 | 是否暴露公网 |
|---|---|---|---|---|
| 10110 | `IoGameNettyRuntime`（ioGame External） | WebSocket / BarMessage 二进制 | `-Diogame.externalPort` | **是**（生产客户端） |
| 10210 | `IoGameNettyRuntime`（ioGame Broker） | Bolt 内部 | `-Diogame.brokerPort` | 否（同机或 VPC 内） |
| 10100 | `IoGameRuntime`（JSON dev 网关） | WebSocket + JSON | `-DdevPort` | 仅开发 / 内测 |

另外使用：
- **本地磁盘** `./data/players/` —— `FilePlayerStore` 写入玩家快照（每次 action 落盘一次）。
  - 如切换到 Redis/MySQL，实现 `PlayerStore` 接口并在 `Main.buildPlayerStore()` 增加分支即可。

## 2. 基础 OS / 依赖版本

| 组件 | 版本 |
|---|---|
| OS | Ubuntu 22.04 LTS x86_64（推荐），或 RHEL 9 / Amazon Linux 2023 |
| JDK | **OpenJDK 21**（Temurin / Zulu / Graal 21 皆可，需 class 版本 65） |
| Maven | 3.9+（仅构建机需要） |
| Docker | 24+（二进制部署也可） |
| 监控 | Node Exporter + Prometheus 抓取，或厂商云监控 |
| 防火墙 | 放行 10110（+ 可选 443 TLS 转发） |

> 运行只需要 JRE 21；构建推荐在 CI 里完成，产物为 `app-<ver>.jar` + lib 目录。

## 3. 三档机器配置

### 3.1 Alpha — 单机 ≤ 100 在线

| 角色 | 规格 | 备注 |
|---|---|---|
| all-in-one | 4 vCPU / 8 GB / 100 GB SSD / 10 Mbps | 阿里云 `ecs.g7.xlarge` / 腾讯云 S5 4C8G / AWS `t3.large` |

- `-Xms2G -Xmx4G`，启用 G1GC。
- `store.kind=file`，`store.root=/var/lib/lastbastion/players`。
- **无需 Redis / MySQL**，快照直接写磁盘。
- 需要每 15 分钟备份 `/var/lib/lastbastion/` 到对象存储（OSS / S3）。

### 3.2 Closed Beta — 500 在线

| 角色 | 规格 | 备注 |
|---|---|---|
| 游戏服 × 1 | 8 vCPU / 16 GB / 200 GB SSD / 50 Mbps | 单 JVM，`-Xms4G -Xmx10G` |
| 中间件服 × 1 | 4 vCPU / 8 GB / 100 GB SSD | Redis 7 + MySQL 8 同机（仅测试服） |

- `store.kind=redis`（实现后），Redis 作为**热数据**；MySQL 异步持久化。
- Nginx/Caddy 做 TLS 终结 + `/ws` 路径代理到 10110（WSS）。
- 日志走 stdout → `journalctl` → Loki / ELK。

### 3.3 Open Beta — 2000 在线 & 更高

| 角色 | 规格 | 数量 |
|---|---|---|
| 游戏服（External + Broker 同进程）| 8C16G | 2~3 台（粘性连接：client 登录后固定机器） |
| Logic Server（如果拆进程） | 8C16G | 1~2 台 |
| MySQL 主/备 | 8C16G + 500GB NVMe | 1+1，主备异步复制 |
| Redis 哨兵 | 4C8G | 3 节点 |
| Nginx / WSS 前置 | 4C8G | 2 台（LB） |
| 监控 Prometheus + Grafana | 4C8G | 1 台 |

- JVM 参数：`-Xms8G -Xmx12G -XX:+UseG1GC -XX:MaxGCPauseMillis=100`。
- ioGame 支持多逻辑服拆分；`run-one-netty` 可进一步拆成 `run-many-netty`，需要的话跟服务端开发同步。

## 4. 目录约定（Linux 生产）

```
/opt/lastbastion/
├── app/
│   ├── app-<ver>.jar
│   ├── lib/              # 所有依赖
│   └── run.sh            # systemd 启动脚本
├── config/
│   └── application.yml   # 预留，目前通过 -D 传参
└── logs/
    └── server.log

/var/lib/lastbastion/
└── players/              # FilePlayerStore 数据
```

## 5. systemd 示例

```ini
# /etc/systemd/system/lastbastion.service
[Unit]
Description=Last Bastion Game Server
After=network.target

[Service]
Type=simple
User=lastbastion
WorkingDirectory=/opt/lastbastion/app
Environment=JAVA_HOME=/usr/lib/jvm/temurin-21
ExecStart=/usr/lib/jvm/temurin-21/bin/java \
    -Xms4G -Xmx8G -XX:+UseG1GC -XX:+AlwaysPreTouch \
    -Dstore.kind=file \
    -Dstore.root=/var/lib/lastbastion/players \
    -Diogame.externalPort=10110 \
    -Diogame.brokerPort=10210 \
    -DdevPort=10100 \
    -cp "app-*.jar:lib/*" \
    com.lastbastion.app.Main
Restart=on-failure
RestartSec=5
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

## 6. Docker Compose 示例（单机）

```yaml
# ~/repos/opc-game/deploy/docker-compose.yml（仅参考）
services:
  lastbastion:
    image: eclipse-temurin:21-jre
    container_name: lastbastion
    working_dir: /app
    volumes:
      - ./app:/app          # 放 app-*.jar + lib/
      - ./data:/var/lib/lastbastion/players
    ports:
      - "10110:10110"       # 生产 WS
      - "10100:10100"       # 仅测试时开
    command: >-
      java -Xms2G -Xmx4G
      -Dstore.kind=file
      -Dstore.root=/var/lib/lastbastion/players
      -cp "app-*.jar:lib/*"
      com.lastbastion.app.Main
    restart: unless-stopped
```

## 7. Nginx TLS 前置

```nginx
# /etc/nginx/sites-available/lastbastion
upstream lb_ws {
    server 127.0.0.1:10110;
    keepalive 64;
}

server {
    listen 443 ssl http2;
    server_name testserver.lastbastion.example.com;
    ssl_certificate /etc/letsencrypt/live/testserver.../fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/testserver.../privkey.pem;

    location /ws {
        proxy_pass http://lb_ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 3600s;
    }
}
```

Cocos 客户端连接 `wss://testserver.lastbastion.example.com/ws` 即可。

## 8. 监控 & 告警

| 指标 | 阈值 | 工具 |
|---|---|---|
| JVM Heap 使用率 | > 85% 5m | Micrometer + Prometheus |
| GC Pause P99 | > 300ms | JFR / Datadog |
| WS 在线数 | 突然掉 > 50% | 自定义上报 `/metrics` |
| CPU 负载 | > 80% 5m | Node Exporter |
| 磁盘 `/var/lib/lastbastion` | > 85% | Node Exporter |
| 登录失败率 | > 5% 1m | 日志关键字告警 |

Prometheus scrape endpoint 预留（生产接入时开 `management.endpoints.web.exposure.include=prometheus`，当前 MVP 未接 Spring Actuator，下一阶段加）。

## 9. 备份 & 回滚

- **玩家数据**：`/var/lib/lastbastion/players` 每 30 分钟 `rsync` 到对象存储，保留 7 天。
- **配置表**：所有 JSON 配置 (`survivors.json` / `zones.json`) 随代码走 Git，回滚直接 `git checkout`.
- **灰度**：客户端侧保留 `cdnVersion` 字段；服务端支持 `-DminClientVersion=1.0.3` 拒绝低版本登录（需新增 handler，已在 TODO）。

## 10. 安全

- 对外仅开 443（WSS）+ 22（运维白名单）。
- 10210 Broker 绝对不暴露公网；如果多机部署，用 VPC 私网或云防火墙限制。
- 关闭 JVM JMX 远程端口；需要诊断时走 SSH 隧道。
- 配置文件 `config/secrets.env`（后续新增）权限 `600`，内含：
  - MySQL 账号密码（生产）
  - Redis ACL
  - 第三方 IAP 校验服务账号（Apple/Google）
  - Firebase / AppsFlyer API key
- 所有日志禁止打印玩家设备号 / IDFA / 第三方 token 明文。

## 11. 发布流程（测试服）

1. 本地 / CI `mvn -f SourceCode/server/pom.xml clean install`。
2. 拷贝 `app/target/app-*.jar` 与 `app/target/lib/` 到服务器 `/opt/lastbastion/app/`.
3. `systemctl restart lastbastion`.
4. `curl -s ws://127.0.0.1:10100/` 自测（或用 `wscat` 发登录帧）。
5. 跑 `SourceCode/client/` 里的 E2E 脚本（待补）。
6. 回滚：保留前一版本 jar；`systemctl stop` → 换 symlink → `start`。

## 12. 常见问题

| 症状 | 可能原因 | 排查 |
|---|---|---|
| 启动报 `port error!` | Broker/External 端口被占用 | `ss -ltnp \| grep 10110` |
| `ClassNotFoundException javax.annotation` | JDK 低于 21 | `java --version` 应 ≥ 21 |
| WSS 502 Bad Gateway | Nginx 未设置 Upgrade 头 | 见 §7 的 `proxy_set_header Upgrade` |
| 玩家进度回档 | `store.root` 路径权限错误或被 tmpfs 挂载 | 检查目录可写 + 持久化盘 |
| 登录时 `NOT_LOGGED_IN` | 客户端先发业务再发 `user.login` | 客户端排查 |

---

**维护人**：运维 + 服务端主程。
