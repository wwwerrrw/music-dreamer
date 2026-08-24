# P1 主分支验收交接 - 2026-08-18

## 目标与范围

- 原任务：验收 OpenCode 会话 `ses_006273ac8ffelp1FXUzn6Fz11yP1` 的 P1 剩余工作。
- OpenCode 数据库中的实际会话 ID 为 `ses_006273ac8ffelp1FXUzn6Fz11y`；用户给出的末尾 `P1` 不是会话 ID 的一部分。
- P1 需求来源：`docs/superpowers/plans/2026-08-11-music-dreamer-implementation.md` 第 102-119 行。
- 本文件记录的 P1 主分支验收已完成；后续实现按用户批准的加速范围进入 P2，不发布，不修改课程材料、CodeArts、SMTP 或公网环境。

## Git 与文件状态

- 主工作树：`E:\Zmkk\project`
- 主分支：`main`
- 主分支 HEAD：`fc24b70d234490f53c4d161a884c8bf6f5983e30`
- OpenCode worktree：`E:\Zmkk\opencode-p1-worktree`
- OpenCode 分支：`opencode/p1-handoff`
- OpenCode HEAD：`56e0d4418022aa28090bf0ca12a09e325094edcd`
- OpenCode 相对 `fc24b70` 只新增 `docs/handoffs/2026-08-13-p1-opencode-handoff.md`，没有源码修改。
- 创建本文件前，主工作树与 OpenCode worktree 均为 clean。
- 不需要为了取得源码而合并 OpenCode 分支；如需保留其 handoff 文档，须另行决定集成方式。

## OpenCode 留存证据的复核结果

OpenCode 对 P1 七项行为做了只读复核，未发现需要修改源码的缺陷。其留存证据支持以下结果：

1. 完整离线 Maven reactor 输出为 `BUILD SUCCESS`，13 个模块成功。
2. 单独启用 `musicdreamer.redis.integration=true` 后，真实 Redis 集成测试 1/1 通过。
3. 真实 Gateway E2E 完成注册、开发收件箱、激活、登录、无会话 401、普通用户访问管理接口 403、Redis TTL/索引检查、退出和旧 bearer 失效检查。
4. 退出前先报告精确 Redis 会话键和用户索引成员，并在用户二次确认后才调用退出接口；确认门符合全局删除规则。
5. 未在留存结果中发现 MySQL 密码、bearer token、raw activation token 或业务用户密码。

证据表述问题：OpenCode handoff 把“29 份报告、68 tests、0 skipped”归到单次完整 reactor 不够严谨。真实 Redis 测试是在 reactor 之后单独运行，后一次测试覆盖了部分 Surefire 报告。因此应分别引用 reactor 原始 `BUILD SUCCESS` 和真实 Redis 1/1 结果，不能把最终报告目录统计全部归因于单次 reactor。

## Codex 已完成的主分支独立验证

用户在得知会覆盖 12 个模块的 `target/` 和根目录 `derby.log` 后，于执行临界点明确回复“现在覆盖并运行主分支验证”。随后在主工作树运行：

```powershell
mvn -o -gs infra/maven/global-settings.xml `
  -s infra/maven/user-settings.xml `
  "-Dmaven.repo.local=E:/Zmkk/project/infra/runtime/maven-repository" `
  -f backend/pom.xml verify
```

2026-08-18 17:10 的实际结果：

- 退出码：0
- Reactor：13/13 模块 `SUCCESS`
- Maven：`BUILD SUCCESS`
- 总用时：56.101 秒
- Surefire：29 份报告，68 tests，0 failures，0 errors，1 skipped
- skipped：`RedisSessionRepositoryIntegrationTest`；本次未启用真实 Redis 属性
- 验证后 `git status --short --branch` 仅显示 `## main`
- `git diff --check` 无输出

该结果只证明主分支离线构建、普通测试和内存数据库迁移行为通过，不证明主分支已重新完成真实 Redis/MySQL/Gateway E2E。

## 当前运行状态（验收前实时预检）

主分支验证收尾检查时，下列端口监听数均为 0：

- MySQL `3306`
- Redis `6380`
- Gateway `8080`
- Identity `8081`
- Nacos `8848`

因此验收开始前确认没有相关服务在运行；OpenCode 运行状态文件未作为当前证据。

## Codex 主分支真实环境 E2E

在完成上述离线 reactor 后，Codex 在主工作树重新启动真实 Redis、Nacos、Identity 和 Gateway，并通过 Gateway 完成完整身份链路。MySQL 应用密码只从 `runtime-data\identity-db.env` 读取，未回显、未写入参数或日志。

- 注册：`POST /api/identity/registrations` 返回 `201`，生成全新验收用户。
- 开发收件箱：`GET /api/identity/development-inbox` 返回 `200`，按唯一邮箱精确选取消息。
- 激活：`POST /api/identity/activations` 返回 `204`。
- 登录：`POST /api/identity/sessions` 返回 `200`，真实 Redis 创建会话。
- 无 bearer 访问管理接口：`401 / IDENTITY_SESSION_INVALID`。
- 普通用户访问管理接口：`403 / IDENTITY_FORBIDDEN`。
- 本次会话 hash 与用户会话索引 TTL 均约 `3600` 秒；字段只含用户 ID、角色和状态，未保存原始 bearer。
- 在退出临界点报告了本次会话 hash、用户索引成员、删除范围、排除项和影响；用户随后单独确认执行退出。
- 退出：`DELETE /api/identity/sessions/current` 返回 `204`；会话 hash、索引成员和空索引键均消失。
- 旧 bearer 再访问受保护接口：`401 / IDENTITY_SESSION_INVALID`。

MySQL 复核确认本次用户为 `USER / ACTIVE`，激活记录的 `used_at` 已设置，token digest 长度为 `64`，密码字段未以明文保存。Identity/Gateway stderr 均为 `0` 字节，日志 `ERROR` 行为 `0` 行；敏感模式扫描的 10 份验收证据文件匹配数为 `0`。

本次验收结束后，Identity、Gateway、Redis、Nacos 均已停止；MySQL 系统服务保持运行。实时端口 `6380/8080/8081/8848` 均未监听，未删除 MySQL、Redis 生命周期键或其他运行数据。

## 当前验收结论

- OpenCode 交付范围：完成；它只承担只读复核、测试和 handoff，没有待集成源码。
- 主分支 Maven reactor：已由 Codex 独立重跑并通过。
- P1 最终验收：完成。结论来自本会话在主分支的实时 Maven、MySQL、Redis、Identity、Gateway 和退出撤销证据。
- 当前准确状态：`P1 主分支最终验收完成；P2 最小音乐纵向链路实施中`。

## 后续实施边界

1. P2 只保留 catalog/media 最小纵向链路：歌手上传合法音频并创建/提交草稿，管理员发布或锁定，普通用户关键词搜索并完成 Range 播放。
2. P2 不实现标签偏好、播放计数 outbox、可靠审计、高级筛选、P3 互动和前端页面。
3. 服务通过各自 schema 和 HTTP 边界工作；catalog 不直接访问 media schema。
4. P2 仍需在主分支独立重跑关键测试、Gateway E2E、真实文件落盘和服务重启后的媒体读取。

## 安全与操作边界

- `项目素材/` 只读，不编辑、重命名、覆盖或解压。
- 不输出或保存密码、token、激活链接和真实 `.env` 内容。
- 不清理 Redis、MySQL、运行目录或构建目录。任何删除、清空、卸载或覆盖性移除都必须重新经过精确范围报告和执行临界点二次确认。
- 不合并、提交或发布，除非用户另行明确授权。
- 完成声明必须来自新会话在主分支获得的实时命令与 E2E 证据。
