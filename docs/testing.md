# 测试与质量门禁

阶段六把测试分成四层。单元测试和契约检查适合每次提交；真实 MySQL/Redis
集成测试、浏览器旅程、Lighthouse 与 k6 负责上线前验收。

## 本地快速检查

```powershell
npm ci
npm test
npm run typecheck
npm run test:openapi
npm run build
```

后端集成测试通过 Testcontainers 自动启动 `mysql:9.7` 和 `redis:8.8-alpine`，
本机只需要 Docker 与 JDK 25/Maven 3.9：

```powershell
Set-Location backend
mvn verify
```

## E2E

首次运行安装浏览器：

```powershell
npx playwright install chromium
```

使用独立 Compose 项目启动干净环境，避免覆盖开发数据：

```powershell
$env:COMPOSE_PROJECT_NAME = "personal-blog-e2e"
$env:MYSQL_PASSWORD = "e2e_mysql_password"
$env:MYSQL_ROOT_PASSWORD = "e2e_mysql_root_password"
$env:REDIS_PASSWORD = "e2e_redis_password"
$env:JWT_SECRET = "e2e-jwt-secret-at-least-thirty-two-characters-long"
$env:ADMIN_INITIAL_USERNAME = "e2e-admin"
$env:ADMIN_INITIAL_PASSWORD = "E2e-Only-Password-2026!"
$env:ADMIN_ALLOWED_ORIGINS = "http://localhost,http://admin.localhost"
$env:E2E_ADMIN_USERNAME = "e2e-admin"
$env:E2E_ADMIN_PASSWORD = "E2e-Only-Password-2026!"
docker compose up --build -d --wait
npm run test:e2e
docker compose down -v
```

Playwright 覆盖公开站桌面/移动端、深色模式、发现端点、管理端登录以及文章创建、
发布和前台可见性。失败时报告、截图、视频和 Trace 写入 `playwright-report/` 与
`test-results/`。

## 性能与 Lighthouse

应用启动后执行：

```powershell
npm run test:lighthouse
$env:BASE_URL = "http://host.docker.internal"
npm run test:performance
```

k6 默认使用 5 个并发虚拟用户持续 30 秒，门禁为：

- 缓存公开接口 P95 小于 200ms。
- 指定 `ARTICLE_SLUG` 时，文章详情 P95 小于 500ms。
- 5xx 比率小于 0.1%，检查通过率高于 99%。

Lighthouse 对首页、文章列表、归档和隐私页执行两轮桌面检测，Performance、
Accessibility 与 SEO 均不得低于 90。

## CI

PR 执行后端、前端、OpenAPI、容器构建、漏洞扫描、E2E 和 Lighthouse。
主分支及每周定时任务额外执行 k6。高危或严重依赖漏洞、契约错误、阈值失败都会
阻止流水线通过。
