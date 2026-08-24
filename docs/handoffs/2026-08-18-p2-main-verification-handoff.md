# P2 主分支真实环境验收交接 - 2026-08-18

## 当前边界

- 工作区：`E:\Zmkk\project`
- 分支：`main`
- 当前 HEAD：`fc24b70d234490f53c4d161a884c8bf6f5983e30`
- P1 主分支身份/Gateway 验收已完成；本交接只覆盖用户批准的 P2 最小纵向链路。
- P2 范围：catalog/media 的音频上传、草稿编辑/提交、管理员发布/锁定、普通用户搜索和 Range 播放。
- 不进入 P3/P4；不做前端、互动、outbox、推荐、高级筛选、课程材料、SMTP、公网发布。

## Git 状态

未提交、未合并、未发布。当前已知工作树修改：

- `backend/catalog-service/pom.xml`
- `backend/media-service/pom.xml`
- `backend/gateway-service/src/main/resources/application.yml`
- `backend/platform-contracts/src/main/java/com/musicdreamer/contracts/error/ApiErrorCode.java`
- `backend/catalog-service/src/`
- `backend/media-service/src/`
- `docs/superpowers/plans/2026-08-11-music-dreamer-implementation.md`
- `docs/superpowers/specs/2026-08-11-music-dreamer-design.md`
- `docs/handoffs/2026-08-18-p1-main-verification-handoff.md`
- 本文件

不要回滚或清理上述用户范围内修改。

## 已完成的代码验证

- Flyway 增加 MySQL 类型识别与应用装配后，catalog 聚焦测试 `6/6` 通过，media 聚焦测试 `2/2` 通过。
- 已修复 MySQL 自增键兼容问题：使用 `PreparedStatement(..., new String[]{"id"})` 和 `keyHolder.getKey()`，避免依赖 MySQL/H2 不同的 generated-key 列标签。
- MySQL 风格 generated-key 测试已覆盖。
- Flyway 修复前的完整 reactor 记录：13/13 模块成功，86 tests，0 failures，0 errors，1 skipped。当前代码变更后的完整 reactor 尚未重跑。

## 真实运行环境

验收期间实时监听已确认：

- MySQL `3306`
- Redis `6380`
- Gateway `8080`
- Identity `8081`
- Catalog `8082`
- Media `8083`
- Nacos `8848`

MySQL 应用密码只从 `runtime-data\identity-db.env` 读取，未回显、未写入命令行或日志。

## P2 E2E 证据

最终通过的证据目录：

`E:\Zmkk\project\test-results\p2-main-20260818-205543-635`

关键结果：

1. 新建并激活三角色用户 `12/13/14`，登录后真实 Redis 会话建立。
2. 歌手上传 MP3 返回 `201`；文件 `8611` 字节，数据库元数据和物理文件 SHA-256 均为 `35b56315f8f3dba31ecf0c05f32c9edf3d0f16312c50a89541b6a60f4b2b922c`。
3. 媒体对象 `3`，曲目 `2`；草稿创建、编辑、提交、发布、锁定均通过。
4. 发布后搜索唯一返回，播放门返回 `200`，首次 Range 返回 `206 bytes 0-31/8611`。
5. 锁定后播放门返回 `409`，搜索结果隐藏。
6. Media 重启后，严格确认 JVM 启动、Nacos 健康实例和 Gateway 新实例推送，再次 Range 返回 `206` 且字节一致。
7. 退出临界点已向用户报告精确的三组 session hash/index 范围，并取得单独确认；随后三个退出接口均返回 `204`，对应 hash/index 均消失，旧 bearer 均返回 `401`。
8. MySQL 只读复核：用户 `12=SINGER/ACTIVE`、`13=ADMIN/ACTIVE`、`14=USER/ACTIVE`；曲目 `2=LOCKED`；媒体 `3=AUDIO/8611`，storage key 与真实文件一致。

最终脱敏汇总文件：

- `e2e-phase1.json`
- `e2e-phase2.json`
- `e2e-final.json`

## 失败尝试与根因

第一轮 E2E 在上传修复后进入 Media 重启阶段，但因为测试脚本输出目录硬编码为上一轮目录，且重启 flag 只依据 `8083` 监听就绪，Gateway 在 Nacos 实例推送前收到请求并返回 `503`。日志时间线已确认该竞态。

第二轮只在复制的测试脚本中将 `$runDir` 改为 `$PSScriptRoot`，并把重启放行门提升为 Spring 启动、Nacos 健康实例、Gateway 收到 `new ips(1)` 三项同时满足，最终 E2E 全部通过。没有修改产品源码来掩盖该测试问题。

## 数据与清理边界

- 本轮退出只处理用户 `12/13/14` 的当前会话及对应索引成员；用户 `6/7/8/9/10/11` 的 Redis 会话仍保留。
- 不删除 MySQL 用户、激活记录、曲目、媒体对象、媒体文件、Redis 其他键、日志或测试结果。
- 上一轮失败留下的孤立媒体文件仍保留：`E:\Zmkk\music-dreamer-media\audio\e079f318-cb87-428d-84c9-b8d835e9584f.mp3`。
- `项目素材/` 只读，禁止修改。

## 唯一待办

当前尚未在本次代码变更后重跑完整主分支 Maven verify。执行命令：

```powershell
mvn -o -gs infra/maven/global-settings.xml `
  -s infra/maven/user-settings.xml `
  "-Dmaven.repo.local=E:/Zmkk/project/infra/runtime/maven-repository" `
  -f backend/pom.xml verify
```

该命令会覆盖或生成 12 个模块的 `target` 目录以及 `E:\Zmkk\project\derby.log`。执行前必须再次报告精确覆盖范围并取得独立执行临界点确认；不能用此前的确认替代。

Maven 完整 verify 完成后，再独立汇总 Surefire XML，报告 tests/failures/errors/skipped；不要提交、合并、发布或进入 P3/P4。
