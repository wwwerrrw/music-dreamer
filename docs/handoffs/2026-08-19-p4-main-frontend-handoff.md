# P4 主分支后端契约完成与前端启动交接 - 2026-08-19

## 1. 当前结论

- P4 已获用户批准，继续按已批准的双 Vue 2.7 前端设计实施。
- P4 Task 0-4 的最小后端契约已经完成；最新 Catalog 聚焦验收通过。
- 精确停点是 P4 Task 5：创建 `frontend/shared`、`frontend/music-web` 和 `frontend/admin-web`。交接时 `frontend/` 尚不存在。
- 原课程素材已由用户明确弃用；不得继续读取或使用 `项目素材/`。
- 用户明确跳过 Image Gen。后续只使用少量下载到项目内、带来源 URL、作者和许可记录的合法可复用音乐摄影素材。
- 本轮不提交、不合并、不推送、不发布，不进入 P5。

## 2. 必读文件

按顺序阅读：

1. `AGENTS.md`
2. `docs/handoffs/2026-08-19-p4-main-frontend-handoff.md`
3. `docs/superpowers/specs/2026-08-19-p4-dual-frontend-design.md`
4. `docs/superpowers/plans/2026-08-19-p4-dual-frontend-implementation.md`
5. `docs/handoffs/2026-08-19-p3-main-shutdown-handoff.md`
6. `docs/handoffs/2026-08-18-p2-main-verification-handoff.md`
7. `docs/handoffs/2026-08-18-p1-main-verification-handoff.md`

视觉基线：

`E:\Zmkk\project\.superpowers\brainstorm\782-1787105445\content\visual-direction.html`

`.superpowers/` 已加入 `.gitignore`，视觉草稿保留但不提交。设计已经批准，不重新发散设计，也不再请求 Image Gen。

## 3. Git 与工作树

- 工作区：`E:\Zmkk\project`
- 分支：`main`
- HEAD：`fc24b70d234490f53c4d161a884c8bf6f5983e30`
- 交接检查时间：`2026-08-19 15:49:13 +08:00`
- `git status --porcelain=v1`：33 项，其中 20 项 tracked modified、13 项 untracked。
- 工作树包含用户、P1、P2、P3、P4 的未提交修改。禁止回滚、清理、覆盖或用旧版本替换。
- `git diff --check` 在交接时通过。

完整范围必须在新会话用 `git status --short` 重新读取；不得只依赖本文枚举。当前主要 P4 修改包括：

- `.gitignore`
- Identity 登录元数据、媒体 Cookie、退出 Cookie 过期逻辑及测试
- Gateway 媒体 GET Cookie 认证边界及测试
- Catalog 管理列表、歌手资料、封面/歌词关联、草稿更新及测试
- P4 设计和实施计划

不要提交、合并、推送、发布或清理任何未提交文件。

## 4. P4 已完成后端行为

### Identity

- 登录响应增加 `token/expiresAt/userId/role/status`。
- 媒体 Cookie 名为 `music_dreamer_media_session`。
- Cookie 为 `HttpOnly`、`SameSite=Strict`、`Path=/api/media/objects`。
- 本地 `music-dreamer.identity.media-cookie-secure` 默认 `false`。
- 退出按相同 Path 过期 Cookie。
- RED 为 3 个预期失败；GREEN 聚焦结果为 11 tests，0 failures，0 errors。

### Gateway

- bearer 始终优先。
- 仅无 bearer 的 `GET /api/media/objects` 及子路径可使用媒体 Cookie。
- 非媒体路径和写方法拒绝 Cookie。
- 错误 bearer 不回退有效 Cookie。
- 聚焦结果为 9 tests，0 failures，0 errors。
- 做过受控错误-bearer变异，测试成功捕获 `200` 越权；随后已恢复正确实现并重跑为 GREEN。

### Catalog

- 新增 `GET /api/catalog/admin/tracks`，支持状态过滤、分页和 `created_at DESC, id DESC` 稳定排序。
- 前端 `SUBMITTED` 明确映射现有 `PENDING_REVIEW`。
- 新增 `PUT /api/catalog/artists/me` 和 `GET /api/catalog/artists/{userId}`。
- 新增 `ArtistSummary`、`TrackPage`。
- `TrackSummary` 增加 `coverMediaObjectId`、`lyricsMediaObjectId`、`artistStageName`。
- 草稿创建和扩展更新均持久化封面/歌词 ID；更新支持将歌词 ID 清为 `null`。
- `find/search/adminTracks` 只 LEFT JOIN Catalog 自有 `artist_profiles`，没有跨 schema 查询或跨服务外键。
- 最近的草稿更新 TDD：RED 返回旧封面 `201` 而非期望的 `211`；GREEN 后服务测试 10/10 通过。
- 最新 Catalog 聚焦验收：21 tests，0 failures，0 errors，0 skipped，reactor 5/5 SUCCESS。

最新 Catalog 验收命令：

```powershell
mvn -f backend/pom.xml -pl catalog-service -am `
  '-Dtest=CatalogApplicationServiceTest,CatalogControllerTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test
```

结果：

- `CatalogApplicationServiceTest`：10/10
- `CatalogControllerTest`：11/11
- 合计：21/21
- 完成时间：`2026-08-19 15:01:06 +08:00`

Identity/Gateway 的结果来自本会话较早执行和现有 Surefire 文件，交接时未再次运行；新会话不得把它们描述成新会话独立重跑。

## 5. 数据库事实

- 最近的 Catalog TDD 使用 H2 内存数据库，不会创建或修改 MySQL 业务表。
- 本会话只读确认 MySQL 已存在四个 schema：`music_dreamer_identity`、`music_dreamer_catalog`、`music_dreamer_interaction`、`music_dreamer_media`。
- 这些业务表不是本轮刚创建的；本轮没有运行 MySQL 建表或迁移命令。
- MySQL 密码只能从 `runtime-data\identity-db.env` 在进程内读取；禁止读取后回显、命令行传递、写日志或写 handoff。
- 用户 15-18 的 Redis 会话必须保留，不得调用退出接口或清理 Redis 键。

## 6. 交接瞬间运行态

交接时只做了 TCP 快速探测，没有重新执行健康接口或真实 E2E：

- `3306` MySQL：可连接；`MySQL800` 为 `Running / Automatic`。
- `6380` Redis：TCP 可连接。
- `8080` Gateway：TCP 可连接。
- `8081` Identity：TCP 可连接。
- `8082` Catalog：TCP 可连接。
- `8083` Media：TCP 可连接。
- `8084` Interaction：TCP 可连接。
- `8085`：不可连接。
- `8848` Nacos：TCP 可连接。

以上只代表 `2026-08-19 15:49` 的瞬时监听状态，不代表服务健康、代码版本或新会话现状。新会话先只读复查，不自动启动、停止或重启服务，不自动重跑 E2E。

## 7. 精确下一步：P4 Task 5

创建：

- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/shared/**`
- `frontend/music-web/**`
- `frontend/admin-web/**`

锁定工具链和框架：

- Node：`E:\nodejs-24.18.0\node.exe`，24.18.0
- npm：`E:\nodejs-24.18.0\npm.cmd`
- Vue：2.7.16
- music-web：Vite 4.5.x，开发端口 5173
- admin-web：Vue CLI 5.x，开发端口 5174
- shared：框架无关 JavaScript/CSS，不包含 Vue 组件或 store

开始前：

1. 只读验证 Node/npm 精确路径和版本。
2. 检查 npm cache 和网络需求，不修改全局 npm prefix。
3. 在任何 `npm install` 前报告写入/覆盖范围：
   - `E:\Zmkk\project\frontend\package-lock.json`
   - `E:\Zmkk\project\frontend\node_modules\**`
4. 对依赖安装取得新的独立执行临界点确认；不得沿用此前任何“允许/批准”。
5. 新行为继续严格 RED -> GREEN -> REFACTOR，并保存 RED/GREEN 命令与结果。

优先完成 Task 5 的 workspace、shared API client、契约、错误映射和视觉变量，再按计划串行进入 music-web 与 admin-web。用户要求节省上下文，因此保持单 Agent、短更新、聚焦命令，不重复已批准设计讨论。

## 8. 前端锁定边界

- 双应用独立测试和构建，只共享框架无关层。
- 认证继续使用 Redis 不透明会话，不改 JWT。
- 媒体元素使用同源 `/api/media/objects/{id}` 和媒体 Cookie，URL 不拼 bearer。
- bearer 与非秘密元数据只存 `sessionStorage`，不存 `localStorage`。
- 推荐只表述为 `LATEST` 和 `POPULAR` 排序，不声称个性化算法。
- 不实现 JWT、评论/点赞、歌单关注/公开分享、个性化推荐、统计、审计、真实 SMTP、CodeArts、公网发布、课程报告或答辩材料。
- 不使用旧 `项目素材/`；不调用 Image Gen。
- 视觉严格对照已批准方向稿，不做营销落地页，不扩大文案和功能。

## 9. 验证与写入确认门

- 测试、构建、Playwright、截图和报告都视为写操作。
- 运行前报告精确写入/覆盖目录并取得对应执行临界点确认。
- 完整 Maven verify 当前未授权。
- 如后续要求完整 Maven verify，先重新报告：12 个模块的 `target` 目录和 `E:\Zmkk\project\derby.log`，再取得新的独立确认；不得沿用此前确认。
- 不删除或清理 MySQL、Redis、媒体、日志、`target`、`node_modules`、`dist`、测试证据或运行目录。
- 不提交、合并、推送或发布。

## 10. P4 最终验收仍未完成

P4 不能仅凭后端聚焦测试宣布完成。后续仍需：

1. shared、music-web、admin-web 聚焦测试。
2. music-web 和 admin-web 独立生产构建。
3. 受影响后端服务与 Nacos/Gateway 路由复核。
4. Playwright 桌面普通用户、歌手、管理员三条真实流程。
5. 手机视口普通用户主流程。
6. 控制台错误、截图、空白、重叠、文字溢出、真实媒体 Range 和本地素材加载检查。
7. 所有结果必须来自主工作树新鲜执行，且不得清理用户 15-18 或其他业务数据。

## 11. 新会话首条提示词

```text
老大，接手 E:\Zmkk\project 的 Music Dreamer P4 主分支工作。

先完整阅读并遵守：
1. AGENTS.md
2. docs/handoffs/2026-08-19-p4-main-frontend-handoff.md
3. docs/superpowers/specs/2026-08-19-p4-dual-frontend-design.md
4. docs/superpowers/plans/2026-08-19-p4-dual-frontend-implementation.md
5. docs/handoffs/2026-08-19-p3-main-shutdown-handoff.md

当前分支应为 main，已知 HEAD=fc24b70d234490f53c4d161a884c8bf6f5983e30，工作树含用户/P1-P4 大量未提交修改，禁止回滚、清理、覆盖、提交、合并、推送或发布。先只读复核 git branch、HEAD、git status，以及 3306、6380、8080-8085、8848 和 MySQL800 状态；不要自动启动、停止或重启服务，不自动跑 E2E。

P4 Task 0-4 已完成；最新 Catalog 聚焦验收为 21 tests，0 failures/errors。精确停点是 Task 5，frontend/ 尚未创建。继续按已批准设计，以单 Agent 串行创建 frontend/shared、frontend/music-web、frontend/admin-web。不要重新讨论设计，不使用已弃用的 项目素材/，跳过 Image Gen。素材只允许下载到项目内并记录来源、作者和许可。

先只读验证 E:\nodejs-24.18.0\node.exe 和 E:\nodejs-24.18.0\npm.cmd 的精确路径/版本，检查 npm cache 和网络需求。任何 npm install 前，必须报告 frontend/package-lock.json 与 frontend/node_modules/** 写入范围，并请求新的独立执行临界点确认，不能沿用旧批准。所有新增行为执行 RED -> GREEN -> REFACTOR。

不得读取、回显或记录 MySQL 密码；密码只能从 runtime-data\identity-db.env 在进程内使用。用户 15-18 Redis 会话不得清理或退出。完整 Maven verify 未授权；若后续要跑，先重新报告 12 个模块 target 和根 derby.log 并取得新的独立确认。

先完成只读预检并简短报告，然后从 P4 Task 5 继续。
```
