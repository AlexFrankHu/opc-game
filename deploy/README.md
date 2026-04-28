# Last Bastion 测试服部署脚本

> 配套规格说明见 [`tasks/product/DEPLOYMENT-TESTSERVER.md`](../tasks/product/DEPLOYMENT-TESTSERVER.md)。
> 这里只放可以直接 `docker compose up -d` 拉起的脚本。

## 1. 你需要准备的

- 一台 4C / 8G / 50G SSD 的 Linux 机器（Ubuntu 22.04+ / Debian 12+ / Rocky 9+）
- Docker 24+ + Docker Compose v2（系统包安装即可）
- 一个解析到这台机的域名（可选，但开 HTTPS 必填）
- 一对 TLS 证书（可选；用 Let's Encrypt / acme.sh 自助签也行）

## 2. 一键部署（Docker Compose）

```bash
# 仓库根目录
cp deploy/.env.example deploy/.env
$EDITOR deploy/.env                                 # 至少把 LOGIN_SHARED_SECRET 改了
# 想开 HTTPS：把证书放到 deploy/nginx/certs/{fullchain.pem,privkey.pem}
docker compose -f deploy/docker-compose.yml --env-file deploy/.env build
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d
```

启动后服务对外暴露：

| URL | 说明 |
|---|---|
| `https://<host>/`             | web-demo 测试页（静态） |
| `wss://<host>/api`            | JSON 网关（10100 内网，nginx 终止 TLS） |
| `wss://<host>/api-bin`        | ioGame BarMessage 二进制网关（10110） |
| `https://<host>/healthz`      | 健康检查 |
| `https://<host>/readyz`       | 就绪检查 |

> 没有域名/证书时：注释掉 `nginx.conf` 里 `server { listen 443 ssl }` 整段，把 80 配成普通 http；或者直接通过 `EXPOSE_JSON_PORT=10100` 把 app 端口暴露出来（仅限内部测试）。

### 数据落地目录

| 卷 | 路径 | 说明 |
|---|---|---|
| `mysql-data` | `/var/lib/mysql` | MySQL 数据；切忌随手删 |
| `redis-data` | `/data`           | Redis RDB 快照 |
| `app-data`   | `/data` (容器内)   | 当 STORE_KIND=file 时的快照 |

### 升级流程

```bash
git pull
docker compose -f deploy/docker-compose.yml --env-file deploy/.env build app
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d app
```

策划改 `assets/numeric/*.json` 不需要重 build；只需要把新文件挂进容器或重启 app。
> Dockerfile 把 `assets/numeric` 烤进了镜像；想热更可以挂卷覆盖：
> `volumes: ["../assets/numeric:/assets/numeric:ro"]`

## 3. systemd 部署（不用 Docker）

适合已有 MySQL / Redis 的环境。流程：

```bash
mvn -B -f SourceCode/server/pom.xml clean package -DskipTests
sudo install -d -o lastbastion -g lastbastion /opt/lastbastion /var/lib/lastbastion/players
sudo install -m 0644 SourceCode/server/app/target/last-bastion-app-fat.jar \
    /opt/lastbastion/last-bastion-app.jar
sudo cp -r assets/numeric /opt/lastbastion/numeric
sudo cp deploy/systemd/lastbastion.env.example /etc/lastbastion.env  # 编辑后再启
sudo cp deploy/systemd/lastbastion.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now lastbastion
sudo journalctl -fu lastbastion
```

## 4. MySQL 自助初始化

`docker-compose` 启动 MySQL 容器会自动跑 [`mysql/init/01-schema.sql`](mysql/init/01-schema.sql)，
创建 `lastbastion` 库和 `player_snapshot` 表（含 `idx_updated_at`）。

如果你用现成 MySQL，自己执行：

```bash
mysql -u root -p < deploy/mysql/init/01-schema.sql
```

`JdbcPlayerStore` 启动时也会 `CREATE TABLE IF NOT EXISTS`，所以这个脚本只是为了让人工排查 schema 时方便。

## 5. 验证

```bash
# 健康检查
curl -fsS https://<host>/healthz
# 期望: {"status":"ok"}

# JSON 网关握手（需要 wscat / websocat）
websocat wss://<host>/api <<EOF
{"id":1,"action":"user.login","payload":{"userId":"smoke-1"}}
EOF
```

启用了 `LOGIN_SHARED_SECRET` 时上面会返回 `UNAUTHENTICATED`；要 OK 就得先在客户端用同一 secret
计算 `HMAC-SHA256(secret, userId|deviceId|ts)`，把 `deviceId/ts/sig` 一并塞进 payload。
web-demo 登录页的「鉴权 secret」输入框就是干这个用的。

## 6. 监控 & 日志

容器化部署：`docker compose logs -f app` / `docker compose logs -f mysql`。
systemd 部署：`journalctl -fu lastbastion`。

监控建议（脚本里没接，留给运维侧）：
- 健康检查：让外部 HTTP 探测 `https://<host>/healthz` 每 30s 一次。
- 应用日志：app 容器 stdout 可挂 vector / loki。
- MySQL：`mysqld_exporter` + Prometheus。
- Redis：`redis_exporter` + Prometheus。

## 7. 备份 & 灾备（最低限度）

```bash
# MySQL 每天 03:00 dump 一份
docker exec lastbastion-mysql-1 \
    mysqldump -uroot -p$MYSQL_ROOT_PASSWORD lastbastion \
    | gzip > /backup/lastbastion-$(date +%F).sql.gz

# Redis 直接拷 RDB
docker cp lastbastion-redis-1:/data/dump.rdb /backup/redis-$(date +%F).rdb
```

把 `/backup` 挂到对象存储（OSS/S3）即可。

## 8. 常见问题

- **app 起不来，日志显示 `JDBC URL invalid`**：检查 `MYSQL_URL` 里是否带 `useSSL=false&serverTimezone=UTC`。
- **WS 握手 502**：通常是 nginx 没开 `Connection: upgrade`，检查 `lastbastion.conf`。
- **HMAC 老报 STALE**：客户端 `ts` 单位是毫秒，不是秒；服务端 skew 默认 5 分钟。
- **改完数值不生效**：默认 `assets/numeric` 是烤进镜像的，必须 `docker compose build app` 重打包，或者挂卷。
