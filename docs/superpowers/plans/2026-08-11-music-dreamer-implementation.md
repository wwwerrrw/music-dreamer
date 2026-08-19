# Music Dreamer 项目主体实施计划

## 目标与执行规则

本计划实现已批准的“薄而完整”主体设计，不包含 CodeArts、课程报告、答辩材料、真实 SMTP 或公网发布。2026-08-18 起，主要用户可见能力以最小可靠实现完成；复杂算法、可靠事件架构和穷举式加固不再阻塞主体交付。

- 严格执行 RED -> GREEN -> REFACTOR；生产行为必须先有能因缺失实现而失败的测试。
- 根配置、数据库迁移、共享契约和主分支只由 Codex 写入。
- OpenCode 只在独立 Git worktree 写其获分配目录；阶段内不得跨目录修改。
- 阶段完成以主分支独立重跑为准，不接受 Agent 自报或仅有源文件作为完成证据。
- `项目素材/` 保留为只读输入；`.playwright-mcp/` 与调研截图保留在磁盘但不进入源码提交。

### 加速执行规则

- 保留五服务、四 schema、Gateway、Nacos、Redis、真实媒体存储和两个 Vue 应用，不通过合并服务或内存假数据换取速度。
- 新生产行为仍严格执行 RED -> GREEN -> REFACTOR；阶段内优先运行受影响模块的聚焦测试，在 P2/P3/P4 checkpoint 与 P5 运行全量回归。
- 只实现完成三角色核心流程所需的接口、页面和状态；不为已明确后置的表、页面或算法补占位实现。
- 后置项包括标签偏好推荐、歌单关注、播放计数事件/outbox、推荐降级编排、可靠审计事件、高级筛选、完整个人设置、全设备像素级适配和穷举故障矩阵。
- 不删除或回滚 P0 已建立的表、迁移与兼容字段；未使用结构不作为主体完成证据。

## P0：仓库与工具基线

### P0.1 仓库卫生

写入范围（Codex）：

- `.gitignore`
- `.gitattributes`
- `AGENTS.md`
- `README.md`

RED：

- `git check-ignore .playwright-mcp/example.png` 当前失败。
- `git check-attr text docs/superpowers/specs/2026-08-11-music-dreamer-design.md` 当前没有项目规则。

GREEN：

- 忽略工作树、构建输出、IDE 文件、真实环境文件、运行时数据、媒体存储和 Playwright 临时证据。
- 对源码、配置和文档固定 UTF-8/LF；二进制素材标记为 binary。
- workspace `AGENTS.md` 只补充项目目录所有权、素材只读和验收命令，不复制全局规则。

验收：`git status --short` 只显示预期项目文件；素材与证据未删除。

### P0.2 后端父工程与模块边界

写入范围（Codex）：

- `backend/pom.xml`
- `backend/platform-contracts/`
- `backend/platform-security/`
- 五个服务目录的模块 POM

RED：

- 先执行 `mvn -f backend/pom.xml test`，确认因父工程/模块缺失失败。
- 在共享模块中先写边界行为测试，再添加稳定错误码、身份上下文和 `ProblemDetail` 工厂。

GREEN：

- 锁定 Java 21、Boot 3.2.4、Cloud 2023.0.1、Alibaba 2023.0.1.0 和 MyBatis-Plus 3.5.7。
- Maven reactor 可编译全部模块；共享模块不含业务逻辑。

验收：`mvn -f backend/pom.xml test`、依赖树检查、无 SNAPSHOT/动态版本。

### P0.3 基础设施检查与受控脚本

写入范围（Codex）：`infra/`。

- 记录 MySQL 8.0.39、项目级 Redis 8.10.0、Nacos 2.3.x 和现有 Node 24.18.0 的固定路径与校验方式。
- Redis 与 Nacos 下载动作单独请求网络授权；不替换系统 Java、Node 或 Redis。
- PowerShell 脚本提供 preflight、start、status、stop；只管理本项目启动的 PID 和数据目录。
- stop/clean 不删除持久数据；任何清理动作另走删除确认门。

验收：缺失运行时给出明确错误；已有运行时返回精确版本和路径。

### P0.4 四 schema 数据库基线

写入范围（Codex）：

- `backend/pom.xml`
- `database/identity/`
- `database/catalog/`
- `database/interaction/`
- `database/media/`
- P0.4 结构与迁移行为测试

RED：

- 结构测试必须因四个 `database/` Maven 模块及其 Flyway 迁移资源缺失而失败。
- 迁移行为测试必须在隔离的 MySQL 兼容测试实例中执行每个模块的 V1 脚本，并验证表、主键、唯一约束、外键边界和版本字段；缺失脚本时失败。

GREEN：

- 四个数据库目录分别作为独立、带版本的 Maven JAR 模块加入现有 reactor，并只发布自身 schema 的 `db/migration` 资源。
- Identity 包含 `users`、`email_activation_tokens`；Catalog 包含 `artist_profiles`、`tracks`、`tags`、`track_tags`、`outbox_events`；Interaction 包含 `track_favorites`、`playlists`、`playlist_tracks`、`playlist_follows`、`listening_history`、`messages`、`message_receipts`、`audit_events`；Media 包含 `media_objects`。
- 主表使用明确主键、时间戳和乐观锁版本；关系表使用复合唯一约束。跨服务 ID 仅为标量引用，不建立跨 schema 外键，不允许跨 schema 查询。
- 各应用服务在后续阶段只消费自己拥有的迁移 artifact；`项目素材/musicDatabase.sql` 只作只读业务参考，不复制其单 schema 结构、跨域外键、缺失约束或 MD5 管理员数据。

验收：

1. 聚焦结构与迁移行为测试通过。
2. `mvn -f backend/pom.xml test` 完整 reactor 通过，且四个迁移 artifact 均被构建。
3. 使用 MySQL `login-path=music-dreamer` 完成只读权限预检后，真实迁移只创建或修改 `music_dreamer_identity`、`music_dreamer_catalog`、`music_dreamer_interaction`、`music_dreamer_media`；通过 MySQL 元数据独立检查表与约束。
4. 未配置安全认证或权限不满足时，真实 MySQL 验收明确报告为未运行，不通过密码参数、日志或源码绕过边界。
5. 不删除、清空或重建任何已有 schema；需要重建时另走删除执行临界点确认门。

P0.4 通过后创建 P0 checkpoint；再按后续阶段需要建立独立 OpenCode worktree 并进入双写者阶段。

## P1：身份、会话与 Gateway

写入范围：

- Codex：`identity-service`、`gateway-service`、`platform-security`。
- OpenCode：只读复核 P1 契约，不写 P1 文件。

TDD 顺序：

1. 未激活用户登录被拒绝。
2. 激活令牌只能使用一次且过期后失败。
3. 正确密码创建 Redis 会话，错误密码不创建。
4. 锁定用户无法登录，已有会话失效。
5. Gateway 拒绝无会话请求并剥离伪造身份头。
6. 管理员可查询/锁定用户，普通用户得到 403。
7. 所有失败响应符合统一 `ProblemDetail` 契约。

验收：注册 -> 开发收件箱 -> 激活 -> 登录 -> 受保护接口 -> 退出完整通过；MySQL 与 Redis 使用真实测试实例。

## P2：音乐目录与媒体纵向链路

并行文件所有权：

- OpenCode worktree：`backend/catalog-service/**`、`backend/media-service/**`。
- Codex 主工作树：共享契约、数据库迁移、Gateway 路由和集成测试。
- 并行期间 Codex 不修改 OpenCode 所有目录；契约/DDL 变更由 OpenCode写入 handoff 请求，不直接修改共享文件。

OpenCode TDD 顺序：

1. 非歌手不能创建歌曲草稿。
2. 合法图片/MP3/WAV/LRC 通过，伪装扩展名、超限和路径穿越失败。
3. Range 请求返回正确 206、`Content-Range` 和字节片段。
4. 歌手只能修改自己的歌曲，管理员可审核上下架。
5. 上线歌曲可搜索，锁定歌曲不可播放。
6. 热门或最新列表使用固定样本得到确定顺序，并明确返回排序策略。

Codex 集成验收：歌手经 Gateway 上传合法音频并创建草稿，管理员审核上线，普通用户搜索并完成 Range 播放；真实文件落盘且服务重启后仍可播放；catalog 不直接访问 media schema。标签偏好、播放计数事件和 outbox 不在本阶段范围。

## P3：互动域

写入范围（Codex）：`interaction-service`、对应迁移与契约。

TDD 顺序：

1. 重复收藏不产生重复记录。
2. 歌单所有者可增删歌曲，其他用户得到 403。
3. 收听历史更新最近位置而非无限复制同一条记录。
4. 简单消息可发布、读取并标记已读。

验收：收藏、歌单、消息和最近播放位置均真实持久化，服务重启后保持。歌单关注、标签偏好、推荐降级编排和可靠审计事件后置。

## P4：两个 Vue 前端

进入 P4 前加载 frontend-app-builder 指令，并重新建立文件所有权：

- OpenCode worktree：`frontend/music-web/**`。
- Codex 主工作树：`frontend/admin-web/**`、共享 API 契约与 E2E。

music-web TDD/验收：

- 登录/注册/激活、基础推荐、搜索、歌曲/歌手信息、收藏、歌单、简单消息和歌手上传主流程。
- `HTMLAudioElement` 支持播放、暂停、进度、音量、单曲队列、Range 拖动和可用时的 LRC 同步。
- 桌面播放栏与普通用户手机响应式主流程无重叠/溢出；独立手机全屏播放器和复杂队列后置。

admin-web TDD/验收：

- 用户管理、歌曲审核/管理和简单消息发布页面具备权限、必要分页、空状态和错误状态。
- 锁定/审核等危险业务操作有确认对话框和服务端结果反馈。
- 独立歌手管理、概览统计、审计日志和高级组合筛选后置。

集成验收：Playwright 在桌面跑以下三条主流程，并在手机视口重跑普通用户流程；检查控制台错误和关键截图。

1. 普通用户：注册激活 -> 搜索播放 -> 收藏/歌单 -> 读取消息。
2. 歌手：登录 -> 上传媒体 -> 创建并提交歌曲草稿。
3. 管理员：登录 -> 审核歌曲 -> 管理验收用户 -> 发布简单消息。

## P5：集成加固与主体验收

双写者停止修改后，由 Codex 在主分支执行：

1. Maven 全量测试与构建。
2. 两个前端测试与生产构建。
3. MySQL 四 schema 迁移与关键表元数据检查；不删除或重建已有 schema，任何重建仍单独经过删除确认门。
4. 五服务 Nacos 注册、Gateway 路由与 Redis 会话测试。
5. 合法上传及伪装文件、超限文件、路径穿越三个拒绝用例；三角色关键越权用例。
6. 重启后验证用户、歌曲、收藏、歌单、最近播放位置与媒体 Range 播放。
7. Playwright 桌面三角色与手机普通用户主流程、控制台和关键截图检查。

主体完成报告只陈述实际通过的边界。标签偏好、歌单关注、播放计数 outbox、推荐降级编排、可靠审计、高级前端功能、CodeArts、SMTP、公网部署和课程文档保持明确后置，不得包装成已完成。

## Agent handoff 模板

每次 OpenCode handoff 必须返回：

- 分支与提交哈希
- 所有新增/修改文件
- RED 命令、预期失败原因与实际失败摘要
- GREEN 命令与通过摘要
- 共享契约或数据库变更请求
- 剩余风险、未运行检查和复现步骤

Codex 合并前逐项核对提交范围，合并后独立重跑本阶段关键命令。
