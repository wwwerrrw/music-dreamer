# P4 Task 11-12 前端验收续接交接 - 2026-08-19

## 1. 当前结论与精确停点

- 工作区仍为 `E:\Zmkk\project`，分支为 `main`，HEAD 为 `fc24b70d234490f53c4d161a884c8bf6f5983e30`。
- 工作树继续包含用户与 P1-P4 的大量未提交修改。禁止回滚、清理、覆盖、提交、合并、推送或发布。
- P4 Task 5-10 的双前端源码已经存在于 `frontend/shared`、`frontend/music-web` 和 `frontend/admin-web`。
- 本会话修复了导致注册、登录及所有 POST/PUT 请求正文丢失的 shared API client 缺陷，并完成 RED -> GREEN。
- 修复后最新前端聚焦测试为 shared `14/14`、music-web `22/22`、admin-web `4/4`，合计 `40/40`。
- Task 11-12 尚未完成。没有执行 Playwright、截图、真实三角色流程或手机视口验收，也没有在本次修复后重新生成生产构建。
- 精确停点是：在新会话重新只读预检后，先单独请求“前端开发服务器启动授权”；该授权尚未取得，不能沿用本会话的请求。

## 2. 必读顺序

1. `AGENTS.md`
2. `docs/handoffs/2026-08-19-p4-task11-12-continuation-handoff.md`
3. `docs/handoffs/2026-08-19-p4-main-frontend-handoff.md`
4. `docs/superpowers/specs/2026-08-19-p4-dual-frontend-design.md`
5. `docs/superpowers/plans/2026-08-19-p4-dual-frontend-implementation.md`
6. `docs/handoffs/2026-08-19-p3-main-shutdown-handoff.md`

设计已经批准，不重新讨论视觉或功能范围，不使用已弃用的 `项目素材/`，不调用 Image Gen。

## 3. 本会话注册故障与 TDD 证据

### 根因

`frontend/shared/src/api/api-client.js` 原先把 POST/PUT 请求载荷放在 Axios 配置的 `body` 字段：

```js
request('POST', url, { ...options, body })
```

Axios 使用 `data` 作为请求正文，因此浏览器发到 Identity 的注册请求没有有效 JSON 正文并返回 `400`。路由、Vite proxy、Gateway 和 Identity 本身可达，故障位于共享前端客户端边界。

### RED

在 `frontend/shared/tests/shared.test.mjs` 增加：

```text
api client sends write payloads through the Axios data field
```

命令：

```powershell
& 'E:\nodejs-24.18.0\node.exe' --test 'frontend/shared/tests/shared.test.mjs'
```

结果：`6 tests`，`5 pass`，`1 fail`。唯一失败为新增用例，实际 `config.data` 为 `undefined`，符合预期 RED。

### GREEN

最小修复：

```js
post: (url, body, options = {}) => request('POST', url, { ...options, data: body })
put: (url, body, options = {}) => request('PUT', url, { ...options, data: body })
```

先重跑单文件：`6/6` 通过。随后执行：

```powershell
& 'E:\nodejs-24.18.0\npm.cmd' test
```

工作目录：`E:\Zmkk\project\frontend`

结果：

- shared：`14/14`
- music-web：`22/22`
- admin-web：`4/4`
- 合计：`40/40`，0 failures/errors/skipped

没有为了修复扩大重构。真实浏览器注册仍必须在获得 Playwright/E2E 授权后验证，不能仅凭单元测试宣称修复完成。

## 4. 交接时 Git 与运行态

检查时间：`2026-08-19 19:37:33 +08:00`

Git：

- branch：`main`
- HEAD：`fc24b70d234490f53c4d161a884c8bf6f5983e30`
- `frontend/` 整体仍为 untracked；新增 handoff 也为 untracked。
- 不得用 `git diff` 为空来推断 `frontend/` 没有变化；untracked 文件不会进入普通 `git diff`。

监听：

| 端口 | PID | 状态 |
|---|---:|---|
| 3306 | 7148 | LISTEN |
| 5173 music-web | 16204 | LISTEN |
| 5174 admin-web | 20896 | LISTEN |
| 6380 Redis | 4000 | LISTEN |
| 8080 Gateway | 17956 | LISTEN |
| 8081 Identity | 9844 | LISTEN |
| 8082 Catalog | 17120 | LISTEN |
| 8083 Media | 4064 | LISTEN |
| 8084 Interaction | 22808 | LISTEN |
| 8085 | - | 未监听 |
| 8848 Nacos | 356 | LISTEN |

`MySQL800`：`Running / Automatic`。

前端进程：

- music-web：`E:\nodejs-24.18.0\node.exe ...\vite.js --host 127.0.0.1 --port 5173 --strictPort`
- admin-web：`E:\nodejs-24.18.0\node.exe ...\vue-cli-service.js serve --host 127.0.0.1 --port 5174`

带浏览器等价的 `Accept: text/html` 只读探针：

- `http://127.0.0.1:5173/register` -> `200`，title=`Music Dreamer`
- `http://127.0.0.1:5174/login` -> `200`，title=`MD 管理`

无 `Accept: text/html` 的 PowerShell 请求曾得到直接路由 `404`；加入浏览器请求头后两者均为 `200`，因此不是 SPA history fallback 缺陷。

以上仅是交接瞬间状态。新会话必须重新检查，不能把 PID 或监听当作当前事实。

## 5. 前端启动独立确认门

用户明确要求把前端开发服务器启动授权留到新会话。

新会话先只读检查 `5173/5174`：

- 如果仍正常监听且浏览器等价 HTTP 探针为 `200`，不要为形式上的“启动”而停止或重启现有进程；报告已经运行，再请求用户确认是否将现有进程继续用于本次 Task 11-12。
- 如果任一端口未监听，先报告下列写入范围并单独请求启动授权：
  - `frontend/node_modules/.vite/**`
  - `frontend/node_modules/.cache/**`
  - `runtime-data/logs/*web-p4-*`
- 获得确认后只启动缺失的进程，不停止或重启仍在运行的进程。
- 启动不需要 UAC；Codex 能直接完成。后台进程使用隐藏窗口。

该确认不授权生产构建、依赖安装、Playwright/E2E、截图或真实业务数据写入。

## 6. 生产构建仍需新的覆盖/删除确认

现有构建时间早于本次 shared 修复：

- `frontend/music-web/dist`：`2026-08-19 18:08:09`
- `frontend/admin-web/dist`：`2026-08-19 18:10:13`

Vite `emptyOutDir: true`，Vue CLI 也会重建输出目录。重新构建会删除/覆盖现有构建产物，因此必须在实际执行前报告并对以下精确范围取得新的删除/覆盖二次确认：

- `E:\Zmkk\project\frontend\music-web\dist\**`
- `E:\Zmkk\project\frontend\admin-web\dist\**`

预期影响：只替换可重新生成的生产构建产物，不触碰源码、锁文件、`node_modules`、数据库、媒体、Redis 或用户数据。不要把“继续计划”或前端启动授权当作这次二次确认。

确认后执行：

```powershell
& 'E:\nodejs-24.18.0\npm.cmd' run build
```

工作目录：`E:\Zmkk\project\frontend`

## 7. Playwright/E2E 独立确认门

### 当前工具事实

- Browser 插件技能存在，但本会话缺少其要求的浏览器控制入口，不能走内置 Browser 路径。
- 不应擅自改用外部 Playwright 控制工具。
- `frontend/package.json` 和 `frontend/package-lock.json` 当前没有 Playwright 依赖，也没有现成 Playwright 配置或 E2E 测试。

### 依赖安装边界

如果采用仓库 Playwright，先只读检查系统 Edge/Chrome 可执行文件和 npm cache。任何 `npm install` 前必须报告并取得新的独立安装确认：

- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/node_modules/**`

优先使用已安装的系统 Edge/Chrome，避免下载浏览器。如果必须执行 Playwright browser install，还要先报告 `C:\Users\LENOVO\AppData\Local\ms-playwright\**` 写入并取得独立确认。不得沿用此前 lucide/Element UI 的增量安装批准。

### E2E 文件、证据与真实数据写入

在实际执行 Playwright/E2E 前，另行报告并取得用户明确确认。预期文件范围：

- `frontend/e2e/**`（若新增正式 E2E 配置和测试）
- `test-results/p4-*/**`
- `playwright-report/**` 或 `.playwright-mcp/**`（仅实际工具需要时；使用时间戳子目录，不覆盖旧证据）

真实流程会持久化或修改：

- 新普通用户、激活记录和新的 Redis 会话
- 收藏、歌单、播放历史和消息读取状态
- 歌手资料、上传媒体对象、`media-data/**` 文件、歌曲草稿及审核状态
- 管理员广播/角色消息，以及计划明确要求的歌曲发布或锁定状态

不得在验收后删除或清理上述数据；不得退出或清理用户 15-18 的 Redis 会话。测试账号密码、bearer、激活 token 和媒体 Cookie只能在进程或浏览器内使用，不写入源码、截图、报告或命令输出。

Playwright 目标流程：

1. 桌面普通用户：注册 -> 开发激活 -> 登录 -> 搜索/播放 -> 收藏/歌单 -> 消息。
2. 桌面歌手：登录 -> 资料 -> 上传音频及可选资源 -> 草稿 -> 提交。
3. 桌面管理员：登录 -> 待审核列表 -> 发布/锁定 -> 用户管理 -> 发布消息。
4. 手机视口：重跑普通用户主流程。
5. Task 11：DOM 尺寸、遮挡、溢出、焦点、tooltip、位图/媒体加载、控制台、视觉方向六项一致性及截图。

不要让用户为了排错反复注册同一邮箱。若需要确认其先前尝试状态，只询问邮箱，不询问密码；MySQL 密码仍只能从 `runtime-data\identity-db.env` 在进程内使用且不得读取后回显。

## 8. 后端与 Maven 边界

- 不自动启动、停止或重启 MySQL、Redis、Nacos、Gateway 或任何后端服务。
- 本次 shared 修复不要求后端重启；先只读验证 Nacos 健康实例、Gateway 路由和真实 Range 边界。
- Task 12 要求的新鲜 Identity/Gateway/Catalog 聚焦 Maven 测试会覆盖对应 reactor 模块的 `target`。若要执行，必须先重新计算并报告精确 `target` 目录，再取得本次执行确认。
- 完整 Maven verify 仍未授权。若需要运行，必须重新报告 12 个模块 `target` 目录和 `E:\Zmkk\project\derby.log`，再取得新的独立确认。
- 不读取、回显或记录 MySQL 密码；不清理 MySQL、Redis、媒体、日志、`target`、`node_modules`、`dist` 或测试证据。

## 9. 新会话提示词

```text
老大，继续接手 E:\Zmkk\project 的 Music Dreamer P4 Task 11-12 主分支验收。

先完整阅读并遵守：
1. AGENTS.md
2. docs/handoffs/2026-08-19-p4-task11-12-continuation-handoff.md
3. docs/handoffs/2026-08-19-p4-main-frontend-handoff.md
4. docs/superpowers/specs/2026-08-19-p4-dual-frontend-design.md
5. docs/superpowers/plans/2026-08-19-p4-dual-frontend-implementation.md
6. docs/handoffs/2026-08-19-p3-main-shutdown-handoff.md

当前应为 main，已知 HEAD=fc24b70d234490f53c4d161a884c8bf6f5983e30。工作树含用户/P1-P4 大量未提交修改，frontend/ 与新 handoff 均未跟踪。禁止回滚、清理、覆盖、提交、合并、推送或发布。

先只读复核 branch、HEAD、完整 git status、3306、5173、5174、6380、8080-8085、8848、MySQL800，以及两个前端带 Accept: text/html 的 URL/title。不要自动启动、停止或重启服务，不自动运行构建、Playwright 或 E2E。

本会话已修复 shared API client 把 Axios 写请求正文放在 body 而非 data 的缺陷。TDD 证据：RED 为新增 shared 用例唯一失败；GREEN 后 shared 14/14、music-web 22/22、admin-web 4/4，合计 40/40。真实注册尚未经过浏览器验证。Task 11-12 未完成，修复后的生产构建也未重跑。

用户要求把“前端开发服务器启动授权”留到本新会话。先检查 5173/5174；即使当前已运行，也先简短报告现状，然后单独请求本次启动授权。若进程仍正常，不要为了启动而停止/重启；若缺失，只在确认后启动缺失进程。启动可能写入 frontend/node_modules/.vite/**、frontend/node_modules/.cache/**、runtime-data/logs/*web-p4-*，不改锁文件、不安装依赖。

前端启动授权与 Playwright/E2E 授权必须分开。取得前端启动确认后，再处理生产构建覆盖确认和 Playwright/E2E：
- 构建会清空/覆盖 frontend/music-web/dist/** 与 frontend/admin-web/dist/**，执行临界点必须按删除规则单独二次确认。
- Browser 插件路径缺少所需控制入口，不能擅自回退。
- 仓库目前没有 Playwright 依赖。任何安装前报告 frontend/package.json、frontend/package-lock.json、frontend/node_modules/** 并取得新的独立确认；优先只读检查并使用系统 Edge/Chrome，避免浏览器下载。
- E2E 前另行报告 frontend/e2e/**、test-results/p4-*/**、可能的 playwright-report/**/.playwright-mcp/**，以及将持久化的新用户、会话、收藏、歌单、历史、消息、歌手资料、媒体、歌曲和审核状态，再取得明确确认。

完成 Task 11-12 必须覆盖桌面普通用户、歌手、管理员、手机普通用户、视觉/响应式/可访问性、控制台、截图、真实媒体 Range、本地素材，以及 Nacos/Gateway/Redis/MySQL 边界。不得退出或清理用户 15-18 的 Redis 会话。密码、bearer、激活 token 和 Cookie 不得输出或写入证据。MySQL 密码只能从 runtime-data\identity-db.env 在进程内使用。

不使用已弃用的 项目素材/，跳过 Image Gen。保持单 Agent 串行。先完成只读预检并只请求前端启动授权，不要把 Playwright/E2E 请求合并到同一个确认中。
```
