# Music Dreamer 工作区补充规则

## 适用范围

本文件只补充 `E:\Zmkk\project` 的项目级约束；用户提供的全局协作、删除确认、权限和验证规则继续生效。

## 需求与边界

- 主体设计以 `docs/superpowers/specs/2026-08-11-music-dreamer-design.md` 为准。
- 实施顺序和阶段验收以 `docs/superpowers/plans/2026-08-11-music-dreamer-implementation.md` 为准。
- 当前只实现项目主体；CodeArts、课程报告、答辩材料、真实 SMTP 和公网发布均后置。
- `项目素材/` 是只读原始输入。不得原地编辑、重命名、覆盖或解压；需要使用时复制到明确的派生目录。

## 服务与数据所有权

- `identity-service` 拥有用户、角色、账号状态、激活和会话语义。
- `catalog-service` 拥有歌手资料、歌曲元数据、搜索、排行和推荐候选。
- `interaction-service` 拥有收藏、歌单、历史、消息和审计事件。
- `media-service` 拥有文件校验、元数据、存储和 Range 下载。
- `gateway-service` 只负责外部入口、会话解析、限流、可信身份头和路由。
- 服务不得跨 schema 查询或建立跨服务外键；共享模块不得承载业务逻辑。

## Agent 文件所有权

- Codex 是主分支集成者，独占根配置、数据库迁移、共享契约、Gateway 和最终验收。
- OpenCode 只有在收到阶段任务后，才能在独立 Git worktree 修改明确分配的目录。
- 同一阶段同一文件只有一个写者。构建、测试、截图和报告视为写操作，只能在写者自己的 worktree 运行。
- OpenCode 不得直接修改根 POM、数据库迁移或共享契约；需要变更时通过 handoff 请求 Codex 集成。
- 并行阶段主 Agent 不与子 Agent 并发修改其所有目录；集成前先停止并行写入。

## 安全与验证

- 不提交密码、令牌、激活链接、真实 `.env`、数据库数据目录或媒体运行数据。
- 原始 SQL 只作业务参考；禁止恢复无盐 MD5 默认密码或缺失约束的表结构。
- 新行为严格遵循 RED -> GREEN -> REFACTOR，并保存 RED/GREEN 命令和结果。
- Agent handoff 必须包含提交哈希、文件范围、测试命令、结果、已知风险和共享变更请求。
- 完成声明必须来自主分支独立重跑；源文件存在、测试声明或 Agent 自报都不是运行成功证据。
