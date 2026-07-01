# Personal Blog

依据《个人博客系统设计文档》实现的前后端分离个人博客。阶段一至阶段三已完成，下一步进入阶段四：互动与后台。

## 已完成

- Spring Boot 4.1 / Java 25 / MyBatis / Flyway 后端骨架
- Nuxt 4 公开站和 Vue 3 + Vite 管理端
- MySQL 9.7、Redis 8.8、Nginx 的 Docker Compose 编排
- V1 数据库基线、OpenAPI 3.1 骨架和 CI 流水线
- Actuator、容器健康检查和统一 `/api/` 反向代理
- 管理员安全引导创建、BCrypt 密码校验
- Access JWT、Refresh Cookie Rotation、Redis 会话撤销
- 管理端登录、会话恢复与退出
- 文章草稿增删改查、发布/撤回与乐观锁
- Markdown 安全渲染、字数和阅读时长派生
- 管理端文章列表、筛选和编辑器
- 分类与标签完整管理
- Markdown 实时预览与草稿自动保存
- OSS 图片安全校验、上传、引用保护和媒体库
- 公开文章、分类、标签、搜索、归档 API
- Nuxt SSR 博客首页、文章详情和内容发现页面
- 文章 canonical、Open Graph、JSON-LD 与独立 SEO 元数据
- RSS、Sitemap、robots.txt 和自定义 404
- 深色模式、响应式布局和减少动画偏好适配

下一阶段将实现评论、留言、点赞、审核后台与操作日志。

功能范围与视觉方向参考 MIT 许可的 FeiTwnd-Website；未直接复用其代码或资源。

## 一键启动

1. 复制环境变量：

   ```powershell
   Copy-Item .env.example .env
   ```

2. 修改 `.env` 中所有 `change-` / `replace-` 开头的密码与密钥。首次启动还需设置：

   - `ADMIN_INITIAL_USERNAME`
   - `ADMIN_INITIAL_PASSWORD`（至少 12 位）
   - `JWT_SECRET`（至少 32 字节）

   初始化成功后，从部署环境移除 `ADMIN_INITIAL_PASSWORD`。
3. 构建并启动：

   ```powershell
   docker compose up --build -d
   ```

4. 访问：

   - 公开站：<http://localhost/>
   - 管理端：<http://admin.localhost/>
   - API 状态：<http://localhost/api/v1/status>
   - 健康检查：<http://localhost/actuator/health>

停止环境：

```powershell
docker compose down
```

如需连同本地数据库卷一起清除，明确确认数据不再需要后再执行 `docker compose down -v`。

更多说明见 [本地运行手册](docs/runbook.md) 和 [数据库说明](docs/database.md)。
