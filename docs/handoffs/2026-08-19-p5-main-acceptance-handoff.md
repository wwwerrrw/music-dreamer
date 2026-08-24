# Music Dreamer P5 主分支验收交接 - 2026-08-19

## 1. 验收结论

- P5 最小合规加速验收结果：`PASS`。
- 工作区：`E:\Zmkk\project`；分支：`main`；HEAD：`fc24b70d234490f53c4d161a884c8bf6f5983e30`。
- 工作树继续保留用户与 P1-P5 的大量未提交修改。没有回滚、清理、提交、合并、推送或发布。
- 七个应用进程由 `infra/config/application-runtime-manifest.psd1` 管理，最终均为 `Ready / Owned`：Gateway `8080`、Identity `8081`、Catalog `8082`、Media `8083`、Interaction `8084`、music-web `5173`、admin-web `5174`。
- MySQL `3306`、Redis `6380` 和 Nacos `8848` 在应用受控停止/启动期间始终排除在停止范围之外。
- 完整后端回归为 `115` tests、`0` failures、`0` errors、`1` skipped；13 个 reactor 项目全部成功。
- P4 前端 `41/41`、双生产构建和 Playwright attempt 9 经 306 文件哈希失效检查继续有效。
- 最小真实安全边界新跑通过：未认证 `401`、伪造可信身份头 `401`、普通用户访问管理员接口 `403`、媒体 Cookie Range `206`、非法媒体 `400` 且未产生可用对象。

## 2. 实现范围

P5 新增或修改：

- `infra/config/application-runtime-manifest.psd1`
- `infra/scripts/runtime-common.ps1`
- `infra/scripts/start.ps1`
- `infra/scripts/status.ps1`
- `infra/tests/application-runtime-contract.Tests.ps1`
- `infra/tests/fixtures/application-runtime-probe.mjs`
- `backend/pom.xml`
- `backend/{gateway,identity,catalog,media,interaction}-service/pom.xml`
- `test-results/p5-20260819-224207/**`
- 本 handoff

生命周期实现提供每进程工作目录、子进程内环境变量映射、唯一 stdout/stderr、TCP/HTTP readiness、PID 所有权验证和未知进程拒绝接管。数据库密码只从 `runtime-data/identity-db.env` 映射进子进程环境，不进入参数、状态、日志或证据。

## 3. Fresh P5 Evidence

证据根目录：`test-results/p5-20260819-224207/`

### 3.1 生命周期 RED -> GREEN

命令：

```powershell
pwsh -NoProfile -File infra/tests/application-runtime-contract.Tests.ps1
```

- 初始 RED：`lifecycle-red.txt`，因目标函数和 application manifest 尚不存在而失败。
- 首轮 GREEN：`lifecycle-green.txt`。
- 浏览器等价 readiness 回归 RED/GREEN：`lifecycle-browser-readiness-red.txt`、`lifecycle-browser-readiness-green.txt`。
- 最终 GREEN：`82/82` 断言通过，包括 `Accept: text/html`、工作目录、子进程环境、唯一日志、状态脱敏、PID 所有权和七进程 manifest 契约。
- readiness 缺少 `Accept: text/html` 是本次 P5 实现问题，已按 RED -> GREEN 修复，不属于前端故障。

### 3.2 Maven 与可执行 JAR

最终离线命令：

```powershell
mvn -o -gs infra/maven/global-settings.xml `
  -s infra/maven/user-settings.xml `
  "-Dmaven.repo.local=E:/Zmkk/project/infra/runtime/maven-repository" `
  -f backend/pom.xml verify
```

- 权威汇总：`maven-verify-summary-v4.json`。
- 结果：`BUILD SUCCESS`，13 个 reactor 项目成功，`115` tests、`0` failures、`0` errors、`1` skipped。
- `maven-verify-summary.json`、`v2`、`v3` 是解析器校正过程，保留但不作为最终统计。
- 隔离仓库最初缺少 `spring-boot-maven-plugin:3.2.4`；补齐插件及传递依赖后，最终验证使用 `-o` 离线成功。
- `packaging-contract.json` 与 `maven-verify-summary-v4.json` 确认五个服务 JAR 均包含 `BOOT-INF/` 和 Spring Boot loader。

### 3.3 受控生命周期与持久化

- 旧七进程预检：`runtime-pre-switch.json`。
- 只替换七个应用进程：`runtime-unmanaged-stop.json`；`3306/6380/8848` 保持监听。
- 第一次受控停止：`runtime-managed-stop-1.json`，`7/7 Stopped` 且均通过 PID 所有权验证。
- 第二次停止：`runtime-managed-stop-2.json`，`7/7 AlreadyStopped`。
- 最终启动：`runtime-managed-start-final.stdout.json`；最终状态：`runtime-status-final.json`，`7/7 Ready / Owned`，新 PID、唯一日志路径和 readiness 均存在。
- `persistence-before.json` 与 `persistence-after.json` 完全一致：用户 `47` 为 `ADMIN/ACTIVE`，歌曲 `10` 为 `PUBLISHED`，媒体 `11` 为 `READY` 且 8611 字节，歌单 `6`、收藏、历史和消息数据存在，四 schema Flyway 历史一致，Redis 为 `PONG`。
- 用户 `15-18` 的会话集合在重启前已为 0，重启前后不变；未调用退出或清理。

应用进程没有暴露 Actuator shutdown，因此已拥有 PID 的停止使用验证后的强制停止。这是明确记录的停止方式，不是未知进程接管；第二次停止的幂等结果和基础设施排除范围均通过。

### 3.4 最小真实边界

命令：

```powershell
& 'E:\nodejs-24.18.0\node.exe' `
  'test-results/p5-20260819-224207/p5-minimal-runtime-acceptance.mjs'
```

权威结果：`minimal-runtime-acceptance.json`，`PASS`。

- Nacos：Gateway、Identity、Catalog、Media、Interaction 各 `1` 个 healthy/enabled 实例，端口分别为 `8080-8084`。
- Redis：`PONG`。
- music-web `/register`：`200`，title=`Music Dreamer`。
- admin-web `/login`：`200`，title=`MD 管理`。
- 无会话管理员请求：`401`。
- 伪造 `X-Music-Dreamer-*` 可信身份头：`401`，不能提权。
- 无 Cookie 的 Range 请求：`401`。
- 真实普通用户访问管理员歌曲接口：`403`。
- 登录产生的媒体 Cookie 在进程内用于对象 `11` 的 `bytes=0-31`：`206`，存在 `Content-Range`，返回 32 字节。
- 伪装 MP3 上传：`400`；该验收用户的媒体对象数前后均为 `0`。
- 四 schema 的 Flyway 历史均为 1 条成功记录；关键主键和唯一索引从 `information_schema.statistics` 真实读取。

本项只创建 1 个随机验收账号。它先以 `USER` 完成 `403`，再提升为 `SINGER` 完成非法上传拒绝。最终用户 ID 为 `48`；激活记录和两个 Redis 会话按“不清理验收数据”规则保留。没有使用用户提供的账号密码，也没有调用 logout 或删除。

## 4. Hash-Validated P4 Reuse

基线：`baseline-reuse.json`、`source-hashes-before.csv`。

最终复算：`source-hashes-after.csv`、`source-hash-comparison.json`。

- 基线与最终文件数均为 `306`。
- 变化共 `9` 个：五个服务 POM、父 POM，以及 `runtime-common.ps1`、`start.ps1`、`status.ps1`。
- `frontendInvalidatingCount=0`。
- `apiBehaviorInvalidatingCount=0`。
- `unexpectedCount=0`。
- 结论：`reuseDecision=reuse`。

因此继续复用：

- `test-results/p4-task11-12-20260819-195300/frontend-tests-after-media-fix.txt`：shared `15/15`、music-web `22/22`、admin-web `4/4`，合计 `41/41`。
- 修复后 music-web/admin-web 双生产构建。admin-web 仅有 bundle 体积性能警告。
- `test-results/p4-task11-12-20260819-195300/e2e-summary-attempt9.json`：桌面三角色、手机普通用户、上传发布、收藏、歌单、消息、布局、截图及桌面/手机 Range，结果 `PASS`。
- `test-results/p4-task11-12-20260819-195300/media-range-green.txt`：Media Range 聚焦回归。

## 5. 完成标准对照

| 标准 | 结果 | 直接证据 |
|---|---|---|
| 受控生命周期测试及真实停止/启动 | PASS | `lifecycle-browser-readiness-green.txt`、`runtime-managed-stop-1.json`、`runtime-status-final.json` |
| 新鲜完整 Maven verify | PASS | `maven-verify-summary-v4.json` |
| MySQL、Redis、Nacos、Gateway、双前端最终边界 | PASS | `persistence-after.json`、`minimal-runtime-acceptance.json` |
| 自动化与最小真实安全拒绝 | PASS | Maven 115 tests、`minimal-runtime-acceptance.json` |
| 重启后用户、歌曲、互动和媒体 Range | PASS | `persistence-before.json`、`persistence-after.json`、`minimal-runtime-acceptance.json` |
| P4 前端证据仍有效 | PASS | `source-hash-comparison.json`、P4 `41/41` 和 attempt 9 |
| 证据分类与剩余范围明确 | PASS | 本 handoff |

## 6. 脱敏与保留边界

- `secret-scan.json` 对 P5 证据、当前七进程 14 个运行日志和 `application-processes.json` 使用共享只读句柄扫描，所有目标均成功读取，禁止秘密模式命中 `0`，结果 `PASS`。
- 密码、bearer、激活 token、Cookie 和会话标识没有写入证据或 handoff。
- MySQL 密码只在需要的进程内从 `runtime-data/identity-db.env` 读取并通过 `MYSQL_PWD` 子进程环境传递。
- 不得退出或清理现有 Redis 会话，也不得删除用户 `48` 或本次验收数据。

## 7. Known Non-Blocking Issues

- admin-web 生产构建存在 bundle 体积性能警告，不影响构建和功能验收；拆包归计划外。
- P4 浏览器控制台包含 Lucide Vue 2 弃用警告，以及未创建歌手资料前的预期 `GET /api/catalog/artists/44 -> 404`；无未解释的 request failure。
- 应用没有 Actuator shutdown，受控停止对已验证 PID 使用强制停止；未知进程仍拒绝接管。
- Maven 汇总 `v1-v3` 是解析器校正过程文件，最终统计只认 `v4`，不清理旧证据。

## 8. Out Of Scope

- 第二轮完整 P4 三角色注册、Playwright 或截图。
- 再次运行前端测试、双构建或重复聚焦 Maven 测试。
- MySQL、Redis、Nacos 重启或任何会话/数据/证据清理。
- bundle 拆分、依赖升级、UI 抛光、全设备矩阵和所有接口角色排列。
- JWT、个性化推荐、评论/点赞、歌单关注/分享、可靠 outbox/审计。
- CodeArts、真实 SMTP、公网发布、课程报告和答辩材料。
- Git commit、merge、push、发布或 worktree 清理。

## 9. 当前运行与后续入口

七个应用当前保持运行。后续使用：

```powershell
pwsh -NoProfile -File infra/scripts/status.ps1 `
  -ManifestPath infra/config/application-runtime-manifest.psd1 -Json
```

若以后停止或重启，必须重新报告当时的精确 PID、端口、影响和恢复路径，并重新取得进程影响确认。不得把本次确认用于未来停止、删除、清理、构建、Playwright、提交或发布。
