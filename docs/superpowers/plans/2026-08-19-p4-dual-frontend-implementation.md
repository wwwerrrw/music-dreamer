# Music Dreamer P4 双前端实施计划

日期：2026-08-19

依据：`docs/superpowers/specs/2026-08-19-p4-dual-frontend-design.md`

## 1. 目标

在不回滚 P1-P3 未提交修改、不进入 P5、不提交或发布的前提下，串行交付：

- Identity 登录元数据和媒体专用 Cookie。
- Gateway 媒体 GET Cookie 认证边界。
- Catalog 管理员歌曲列表、歌手资料和封面/歌词关联。
- `frontend/shared` 框架无关共享层。
- Vue 2.7 `music-web` 和 `admin-web`。
- 两个前端的聚焦测试、生产构建和 P4 Playwright 主流程。

每项新增生产行为严格执行 RED -> GREEN -> REFACTOR。测试、构建和 E2E 都视为写操作，由 Codex 单写者串行执行。

## 2. 执行边界

- 当前主分支和 P1-P3 用户修改保持原样。
- 不使用 OpenCode 或并发写者，不创建 worktree。
- 不清理 MySQL、Redis、媒体、日志、`target`、`node_modules`、`dist` 或测试证据。
- 不删除用户或会话；用户 15-18 的 Redis 会话始终排除。
- 不把密码、bearer、激活 token 或媒体 Cookie 写入日志、截图或测试汇总。
- 不运行完整 Maven reactor；P4 只运行受影响模块聚焦测试。
- 任何会覆盖现有 `target`、`dist`、锁文件或测试输出的命令，在执行前报告精确范围并取得对应执行临界点确认。
- npm 依赖下载需要先做缓存/网络预检；需要沙箱外网络时单独请求授权。

## 3. 锁定工具链

- Java：`E:\.minecraft\runtime\java-runtime-delta\bin\java.exe`，21.0.3
- Maven：3.9.9，离线仓库 `E:/Zmkk/project/infra/runtime/maven-repository`
- Node：`E:\nodejs-24.18.0\node.exe`，24.18.0
- npm：`E:\nodejs-24.18.0\npm.cmd`
- Vue：2.7.16
- music-web：Vite 4.5.x
- admin-web：Vue CLI 5.x

## 4. Task 0：状态与忽略规则

文件：

- 修改 `.gitignore`
- 保留 `.superpowers/brainstorm/**`

步骤：

1. 记录 `main`、HEAD 和完整 `git status`。
2. 确认 `3306/6380/8080-8084/8848` 当前运行态，不把运行态当作测试通过。
3. 在 `.gitignore` 增加 `/.superpowers/`，只停止视觉草稿污染 Git 状态，不删除草稿。
4. 运行简单匹配检查和 `git diff --check`。

验收：只新增忽略规则；既有 8 个 tracked 修改和 58 个 untracked 文件不被回滚或覆盖。

## 5. Task 1：Identity 登录响应与媒体 Cookie

主要文件：

- `backend/identity-service/src/main/java/com/musicdreamer/identity/application/LoginSession.java`
- `backend/identity-service/src/main/java/com/musicdreamer/identity/application/IdentityApplicationService.java`
- `backend/identity-service/src/main/java/com/musicdreamer/identity/session/PersistentSessionManager.java`
- `backend/identity-service/src/main/java/com/musicdreamer/identity/web/IdentitySessionController.java`
- `backend/identity-service/src/main/java/com/musicdreamer/identity/web/IdentityLogoutController.java`
- 对应 identity 聚焦测试

RED：

1. 登录响应应包含服务端 `userId/role/status`。
2. 登录响应应设置 `HttpOnly`、`SameSite=Strict`、`Path=/api/media/objects` 的媒体 Cookie。
3. Cookie 到期不超过 Redis 会话到期。
4. 退出响应应以相同 Path 过期媒体 Cookie。
5. 测试不得输出 raw token。

GREEN：

1. `LoginSession` 携带非秘密身份元数据。
2. Session issuer 从已验证 `UserAccount` 构建响应。
3. Controller 统一创建和过期 Cookie。
4. bearer 响应和现有 Redis 摘要语义保持兼容。

聚焦命令范围：

```powershell
mvn -o -gs infra/maven/global-settings.xml `
  -s infra/maven/user-settings.xml `
  "-Dmaven.repo.local=E:/Zmkk/project/infra/runtime/maven-repository" `
  -f backend/pom.xml -pl identity-service -am `
  "-Dtest=IdentitySessionControllerTest,IdentityLogoutControllerTest,IdentityApplicationServiceTest,PersistentSessionManagerTest" `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

执行前报告会写入的 identity 及其 reactor 依赖模块 `target` 目录。

## 6. Task 2：Gateway 媒体 Cookie 认证

主要文件：

- `backend/gateway-service/src/main/java/com/musicdreamer/gateway/security/GatewaySecurityFilter.java`
- 必要的媒体 Cookie 解析辅助类
- `backend/gateway-service/src/test/java/com/musicdreamer/gateway/security/GatewaySecurityFilterTest.java`

RED：

1. 无 bearer 的媒体 GET 可使用有效媒体 Cookie。
2. 非媒体路径、非 GET 方法不得使用 Cookie。
3. 同时存在错误 bearer 和有效 Cookie 时返回 `401`，不得回退。
4. 客户端伪造的可信身份头继续被剥离。
5. Redis 不可用继续返回稳定 `503`。

GREEN：

1. bearer 解析保持第一优先级。
2. Cookie 分支严格匹配方法和 `/api/media/objects/**`。
3. 两种 token 最终复用现有 `GatewayAuthenticationService` 和摘要解析。

聚焦命令使用 `-pl gateway-service -am` 和 Gateway 安全测试集合；不运行完整 reactor。

## 7. Task 3：Catalog 管理员歌曲列表

主要文件：

- `backend/catalog-service/src/main/java/com/musicdreamer/catalog/CatalogOperations.java`
- `backend/catalog-service/src/main/java/com/musicdreamer/catalog/CatalogApplicationService.java`
- `backend/catalog-service/src/main/java/com/musicdreamer/catalog/CatalogController.java`
- 新增分页响应 record
- Catalog service/controller 聚焦测试

RED：

1. `ADMIN/ACTIVE` 可按单一状态读取第 0 页。
2. 普通用户和歌手得到 `403`。
3. `size > 100` 或负页码得到 `400`。
4. 结果稳定按 `created_at DESC, id DESC` 排序。
5. 返回正确 `totalItems/totalPages`。

GREEN：

1. 只查询 Catalog schema。
2. 使用参数化状态、limit 和 offset。
3. 不加入高级筛选或跨服务查询。

## 8. Task 4：Catalog 歌手资料和媒体关联

主要文件：

- Catalog controller、operations、application service
- `TrackSummary.java`
- 新增 `ArtistSummary.java`
- Catalog service/controller 聚焦测试

RED：

1. 歌手可 upsert 自己的 `stageName/biography`。
2. 非歌手不能写资料；活跃用户可读资料。
3. 草稿创建/更新可保存可选封面和歌词 ID。
4. Track 响应返回媒体关联和可用艺名。
5. 既有仅音频草稿保持兼容。

GREEN：

1. 使用现有 `artist_profiles` 和 tracks 可空列。
2. 不创建跨服务外键，不查询 Media schema。
3. 未配置资料时前端显示 `歌手 #<userId>`。

Task 3/4 合并运行 Catalog 聚焦测试，范围只包括 Catalog 及其 reactor 依赖模块 `target`。

## 9. Task 5：前端 workspace 与 shared

主要文件：

- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/shared/package.json`
- `frontend/shared/src/api/**`
- `frontend/shared/src/contracts/**`
- `frontend/shared/src/styles/**`
- `frontend/shared/tests/**`

依赖预检：

1. 精确验证 Node 和 npm 路径/版本。
2. 检查 npm cache 是否已有锁定依赖。
3. 报告 `frontend/package-lock.json` 与 `frontend/node_modules/**` 写入范围。
4. 需要联网时请求沙箱外安装授权；不修改全局 npm prefix。

RED：

1. Node 版本门拒绝错误 executable/version。
2. API client 只在非媒体 API 注入 bearer。
3. `ProblemDetail` 映射 `status/code/detail/traceId`。
4. 401、403、409、503 保持不同语义。

GREEN：

1. 建立 npm workspaces。
2. 共享层不依赖 Vue。
3. 使用 Node 内置测试运行 shared 测试。

## 10. Task 6：music-web 基线、认证与壳层

主要文件：

- `frontend/music-web/package.json`
- Vite 配置与 `/api -> 127.0.0.1:8080` proxy
- Vue 入口、Router、Vuex auth
- 登录、注册、激活页面
- 应用壳层、桌面侧栏、手机底栏
- 聚焦组件/store 测试

RED：

1. 未登录访问保护页回登录。
2. 登录按 USER/SINGER 进入正确默认页面。
3. 非歌手不显示工作台。
4. 401 清会话；403 不清会话。
5. 开发激活只选择当前注册邮箱消息，不持久化 token。

GREEN：

1. `sessionStorage` 保存 bearer 和非秘密元数据。
2. 登录页不记录密码。
3. 视觉按已批准方向落地，图标提供 tooltip 和焦点状态。

## 11. Task 7：发现、搜索、详情和播放器

主要文件：

- music-web catalog/player Vuex
- 发现、搜索、歌曲和歌手详情页面
- 播放器组件、LRC parser、默认封面资源
- 聚焦测试

RED：

1. 发现页明确显示最新/热门排序，不出现“个性化推荐”。
2. 搜索和歌曲详情使用真实 Catalog 响应。
3. `<audio>` 使用同源媒体 URL，不拼 bearer。
4. 播放、暂停、进度、音量和切歌状态正确。
5. LRC 按时间同步；无歌词时不显示歌词入口。
6. history 在暂停、拖动、切歌、离页和 15 秒间隔写入，不随每个 timeupdate 写入。

GREEN：

1. 只有一个 `HTMLAudioElement`。
2. 固定播放器尺寸不因加载或长标题跳动。
3. 默认封面使用下载到项目内、带来源与许可记录的合法可复用音乐摄影素材；真实封面优先，运行时不依赖远程图片。

## 12. Task 8：收藏、歌单、历史和消息

主要文件：

- music-web library Vuex
- 收藏、歌单、历史、消息页面和组件
- 聚焦测试

RED：

1. 重复收藏保持 UI 幂等。
2. 歌单创建、查看、增删歌曲映射现有 API。
3. 非拥有者 403 保留页面并显示错误。
4. 消息读取和标记已读状态正确。
5. 空状态、加载状态和错误状态不共用模糊占位。

GREEN：只实现当前歌曲收藏和自有歌单，不实现歌单关注、公开分享、评论或点赞。

## 13. Task 9：歌手工作台

主要文件：

- music-web 歌手资料页
- 上传与草稿编辑页面
- 上传、Catalog API adapters
- 聚焦测试

RED：

1. 非歌手路由拒绝。
2. 艺名/简介可保存并重新加载。
3. 音频必填，封面/歌词可选。
4. 创建草稿使用真实媒体 ID。
5. 编辑和提交状态反馈准确。
6. 上传失败保留已输入标题和资料。

GREEN：客户端只提供提示；文件合法性最终由 Media 服务决定。

## 14. Task 10：admin-web

主要文件：

- `frontend/admin-web/package.json`
- Vue CLI 配置与 5174 端口 proxy
- Vue 入口、Router、auth
- 用户、歌曲、消息页面
- Element UI 表格、分页、确认对话框
- Jest/Vue Test Utils 聚焦测试

RED：

1. 非管理员不能进入管理页面。
2. 用户列表、歌曲状态分页和消息表单有独立加载/空/错状态。
3. 锁定、发布操作必须先确认。
4. 取消确认不调用 API。
5. 服务端成功刷新行；403/409/503 显示不同结果。

GREEN：不实现歌手管理、统计、审计或高级筛选。

## 15. Task 11：视觉、响应式与可访问性检查

检查：

- music-web 桌面固定播放栏和手机迷你播放器不遮挡正文。
- 长歌曲名、邮箱、艺名和按钮文字不溢出。
- admin 表格在目标桌面宽度可扫描；手机不作为 admin 专项验收。
- 可见焦点、合理对比、tooltip、稳定控件尺寸。
- CSS 不使用 viewport 字号、负字距、装饰性渐变球或一色主题。
- 浏览器真实加载默认位图和上传媒体。
- 对照 `.superpowers/brainstorm/782-1787105445/content/visual-direction.html` 检查颜色、排版、导航、播放器、后台表格和移动端六项视觉一致性。
- 检查静态摄影素材的来源记录；本阶段明确跳过 Image Gen。

使用 Playwright 截图和 DOM 尺寸断言验证，不只检查源码 CSS。

## 16. Task 12：P4 checkpoint

执行顺序：

1. Identity/Gateway/Catalog 聚焦 Maven 测试。
2. shared、music-web、admin-web 测试。
3. music-web 生产构建。
4. admin-web 生产构建。
5. 重启受影响后端服务，验证 Nacos 健康实例和 Gateway 路由。
6. 启动两个前端开发服务器。
7. Playwright 桌面普通用户流程。
8. Playwright 桌面歌手流程。
9. Playwright 桌面管理员流程。
10. 手机视口重跑普通用户流程。
11. 汇总控制台错误、关键截图和脱敏结果。

Playwright 真实流程：

- 普通用户：注册 -> 开发激活 -> 登录 -> 搜索/播放 -> 收藏/歌单 -> 消息。
- 歌手：登录 -> 资料 -> 上传音频及可选资源 -> 草稿 -> 提交。
- 管理员：登录 -> 待审核列表 -> 发布/锁定 -> 用户管理 -> 发布消息。

P4 checkpoint 不调用退出接口，不删除会话、用户、歌曲、媒体或测试结果。任何后续清理必须重新进入删除确认门。

## 17. 完成报告边界

P4 完成声明必须来自主工作树独立重跑，并分别报告：

- 后端聚焦测试命令和结果。
- 两个前端测试结果。
- 两个生产构建结果。
- 三角色桌面和普通用户手机 E2E 结果。
- Nacos、Gateway、Redis、MySQL 和真实媒体 Range 边界。
- 仍后置的 JWT、评论/点赞、歌单关注/分享、个性化推荐、统计、审计、SMTP、CodeArts、公网部署和课程材料。

不得把源文件存在、测试声明或旧证据当作本轮运行成功。
