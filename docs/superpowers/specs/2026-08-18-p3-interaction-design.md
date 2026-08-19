# Music Dreamer P3 互动域落地设计

## 目的与边界

P3 在既有主体设计和实施计划的边界内实现最小互动域：收藏、歌单、最近播放和简单消息。服务只拥有 `interaction` schema；`userId` 与 `trackId` 是跨服务标量引用，不建立跨 schema 查询或外键。

明确后置：歌单关注、角色定向消息、标签偏好、推荐降级编排、播放计数 outbox 和可靠审计事件。

## 实现方案

`interaction-service` 采用 `JdbcTemplate`、应用服务和显式 SQL，沿用 Catalog/Identity 的本地事务模式。保留已有 `database/interaction` V1 迁移，不为 `ALL` 广播增加无必要的字段或迁移。自增写入使用显式 generated-key 列和 `KeyHolder.getKey()`，兼容 MySQL 与 H2。

服务使用 8084 端口并注册 Nacos；Gateway 增加 `/api/interaction/**` 到 `lb://interaction-service` 的路由。所有接口从可信身份头解析 ACTIVE 会话；管理员发布消息要求 ADMIN，歌单写操作要求所有者。

## API 契约

- `PUT /api/interaction/favorites/{trackId}`：幂等添加当前用户收藏。
- `DELETE /api/interaction/favorites/{trackId}`：取消收藏。
- `GET /api/interaction/favorites`：读取当前用户收藏。
- `POST /api/interaction/playlists`：创建歌单。
- `GET /api/interaction/playlists`：读取当前用户歌单。
- `GET /api/interaction/playlists/{playlistId}`：读取自己的歌单及歌曲。
- `POST /api/interaction/playlists/{playlistId}/tracks`：所有者添加歌曲；关系重复时保持单行。
- `DELETE /api/interaction/playlists/{playlistId}/tracks/{trackId}`：所有者移除歌曲。
- `PUT /api/interaction/history/{trackId}`：更新或创建最近播放位置。
- `GET /api/interaction/history`：按最近播放时间读取当前用户历史。
- `POST /api/interaction/admin/messages`：ADMIN 发布 `ALL` 广播消息。
- `GET /api/interaction/messages`：用户读取收件箱；消息与当前用户回执左连接展示已读状态。
- `POST /api/interaction/messages/{messageId}/read`：幂等标记已读。

重复收藏、重复添加歌单歌曲和重复标记已读不返回冲突。权限不足返回 403，资源不存在返回 404，参数错误返回 400，响应继续使用统一 `ProblemDetail`。

## 测试与验收

按 RED -> GREEN -> REFACTOR 实施：

1. 同一用户两次收藏同一曲目只保留一行。
2. 歌单所有者可写，非所有者得到 403；重复添加只保留一行。
3. 同一用户同一曲目两次历史更新只保留一行，位置和时间取最新值。
4. ADMIN 发布广播后用户可读，标记已读持久化且重复标记幂等；非 ADMIN 不能发布。

先用 H2/Flyway 和真实 JDBC 做聚焦测试，再通过真实 MySQL interaction schema、Nacos、Gateway 和 Redis 会话完成 P3 checkpoint。重启 interaction-service 后复核四类数据仍可读取。最终由主分支独立运行完整 Maven verify 并从 Surefire XML 汇总结果。

不删除或重建任何 schema，不读取或记录 MySQL 密码，不进入 P4。
