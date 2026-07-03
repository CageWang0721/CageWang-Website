# 生产部署、备份与恢复

生产目标是单机 Docker Compose，同时保持应用容器可替换、数据不暴露公网、
数据库备份可验证、发布失败可回退。

## 首次部署

1. 准备 Linux 主机、Docker Engine 与 Compose，开放 80/443。
2. 将证书放入 `deploy/certs/fullchain.pem` 和 `deploy/certs/privkey.pem`。
3. 复制 `.env.production.example` 为 `.env.production`，替换全部示例域名和密钥。
4. 首次部署临时填写 `ADMIN_INITIAL_USERNAME` 与至少 12 位的
   `ADMIN_INITIAL_PASSWORD`。
5. 校验配置并发布：

   ```bash
   docker compose --env-file .env.production \
     -f docker-compose.yml -f docker-compose.prod.yml config --quiet
   bash deploy/scripts/deploy.sh 0.1.0
   ```

6. 首次登录成功后，从 `.env.production` 删除两个管理员初始化值，再重新创建
   backend 容器。

生产覆盖文件启用 TLS 1.2/1.3、HTTPS 跳转、安全 Cookie、ECS JSON 日志、
Prometheus 指标、只读后端根文件系统和容器日志轮换。MySQL、Redis 与 Backend
只连接内部网络；网关只公开健康和信息端点，不代理 Prometheus。

## 日常发布

每个版本使用不可变版本号：

```bash
bash deploy/scripts/deploy.sh 0.1.1
```

发布脚本按以下顺序执行：

1. 生成并验证加密 MySQL 备份。
2. 构建带版本号的 Backend、Web、Admin 镜像。
3. 重建服务并等待健康检查。
4. 检查公开站、管理端、API、RSS 和 Sitemap。
5. 失败时恢复上一版应用镜像。

Flyway 迁移不会自动回滚。若新版迁移已经执行，数据库问题必须追加迁移做前向修复。

## 备份

`.env.production` 中的 `BACKUP_ENCRYPTION_PASSWORD` 必须与数据库密码分开保管。
执行：

```bash
bash deploy/scripts/backup-mysql.sh
```

脚本使用事务一致性 `mysqldump`，gzip 后以 AES-256/PBKDF2 加密，立即解密校验，
再写入 SHA-256 清单。默认保留 7 个日周期、4 个周周期和约 12 个月备份。
设置 `BACKUP_OSS_URI` 后会通过 `ossutil` 上传到独立备份 Bucket。

建议的 cron：

```cron
15 2 * * * cd /srv/personal-blog && bash deploy/scripts/backup-mysql.sh >> /var/log/personal-blog-backup.log 2>&1
```

OSS 业务 Bucket 应开启版本控制或回收站；备份 Bucket 使用独立 RAM 权限与生命周期。

## 恢复与月度演练

先把备份下载到主机，然后恢复到隔离的临时数据库：

```bash
bash deploy/scripts/restore-drill.sh \
  /var/backups/personal-blog/daily/personal-blog-YYYYMMDDTHHMMSSZ.sql.gz.enc
```

演练会验证校验和、解密、表数量和 Flyway 成功记录，结束后删除临时数据库。
把日期、备份文件、耗时和结果记录到运维工单；每月至少成功一次。

正式恢复必须显式确认目标数据库：

```bash
RESTORE_CONFIRM=personal_blog bash deploy/scripts/restore-mysql.sh \
  /path/to/backup.sql.gz.enc personal_blog
```

随后按 MySQL、Flyway 状态、Redis 缓存、OSS 对账、Backend、前端/Nginx、
生产冒烟测试的顺序恢复。正式恢复前先停止写入，并保留故障现场备份。

## 上线检查单

- DNS 与证书覆盖公开域名、管理域名，证书续期已自动化。
- `.env.production` 权限为 600，仓库与镜像中没有密钥。
- 管理员初始化密码已删除，Refresh Cookie 带 Secure/HttpOnly/SameSite。
- MySQL/Redis 没有宿主机端口映射，Prometheus 仅内网采集。
- `npm audit`、Trivy、完整 CI、E2E、Lighthouse 和 k6 均通过。
- 加密备份已上传独立位置，恢复演练成功。
- 日志可按 `traceId` 关联，告警覆盖 5xx、P95、磁盘、Outbox 积压与备份失败。
