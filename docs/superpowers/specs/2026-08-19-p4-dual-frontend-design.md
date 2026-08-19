# Music Dreamer P4 双前端专项设计

日期：2026-08-19

状态：已批准

## 1. 目标与依据

P4 在现有 P1-P3 真实 Gateway、Redis、MySQL、Nacos、媒体 Range 和三角色权限链路上，交付两个可独立测试、构建和运行的 Vue 2.7 应用：

- `music-web`：普通用户音乐站和歌手工作台。
- `admin-web`：用户管理、歌曲审核/管理和简单消息发布。

本设计补充但不替代：

- `docs/superpowers/specs/2026-08-11-music-dreamer-design.md`
- `docs/superpowers/plans/2026-08-11-music-dreamer-implementation.md`

发生冲突时，以本次用户明确批准的 P4 收敛决策为准；服务所有权、安全规则、删除确认门和 P5 验收边界继续沿用主体文档。

## 2. 已确认决策

- 原课程素材已经弃用，不再读取或依赖 `项目素材/`。
- 采用已确认的中性工作界面：中性底色、黄色播放强调、绿色可用状态、紧凑后台表格。
- 两个应用保持独立构建，只共享框架无关的 API、契约和视觉变量。
- P4 改为 Codex 单写者串行实施，不使用原计划的 OpenCode 前端分工。
- 为真实前端流程补齐严格最小的 Identity、Gateway 和 Catalog 契约。
- 认证继续使用 Redis 不透明会话，不改成 JWT。
- 推荐只使用 Catalog 的 `LATEST` 和 `POPULAR` 确定性排序，不描述为个性化推荐算法。
- 评论/点赞、歌单关注/公开分享、硬删除歌曲、个性化推荐、统计、审计和高级筛选不进入 P4。

## 3. 工程结构

```text
frontend/
  package.json
  package-lock.json
  shared/
    package.json
    src/
      api/
      contracts/
      styles/
  music-web/
    package.json
    src/
    tests/
  admin-web/
    package.json
    src/
    tests/
```

根 `frontend/package.json` 使用 npm workspaces 管理三个包，并验证实际 Node 可执行文件为 `E:\nodejs-24.18.0\node.exe`、版本为 `24.18.0`。

锁定基线：

- Vue `2.7.16`
- Vue Router `3.6.5`
- Vuex `3.6.2`
- Element UI `2.15.14`
- Axios `1.19.0`
- music-web：Vite `4.5.x` 与 Vue 2 插件
- admin-web：Vue CLI `5.x`

`frontend/shared` 只提供纯 JavaScript 和 CSS：

- Axios 工厂、bearer 注入和统一 `ProblemDetail` 转换。
- 角色、账号状态、歌曲状态和媒体类型常量。
- API 路径及稳定的请求/响应映射。
- 色彩、间距、焦点、排版和固定控件尺寸变量。

共享层不得包含 Vue 组件、Vuex store、路由或应用业务状态。两个应用必须能够分别测试和生产构建。

## 4. 最小后端契约补齐

### 4.1 登录元数据

`POST /api/identity/sessions` 的成功响应扩展为：

```json
{
  "token": "opaque-session-token",
  "expiresAt": "2026-08-19T03:00:00Z",
  "userId": 15,
  "role": "USER",
  "status": "ACTIVE"
}
```

这些字段来自已验证的登录账户，不由前端推断。路由守卫只用于体验控制；所有权限仍由 Gateway 和后端服务验证。

### 4.2 原生媒体会话 Cookie

登录成功时，Identity 同时设置一个媒体专用 Cookie：

- `HttpOnly`
- `SameSite=Strict`
- `Path=/api/media/objects`
- 生命周期不超过 Redis 会话到期时间
- 本地 HTTP 默认不启用 `Secure`；公网部署前必须通过配置启用 `Secure`

Gateway 认证顺序：

1. 如果请求带 `Authorization`，只验证该 bearer；bearer 无效时直接返回 `401`，不回退 Cookie。
2. 仅当请求为 `GET /api/media/objects/**` 且没有 bearer 时，允许从媒体 Cookie 解析同一不透明会话。
3. 其他路径或写方法不得使用 Cookie 认证。

退出仍以 bearer 撤销 Redis 会话，并在响应中让媒体 Cookie 立即过期。token 不进入 URL、日志、截图或长期存储。

### 4.3 Catalog 管理员歌曲列表

新增：

```text
GET /api/catalog/admin/tracks?status=SUBMITTED&page=0&size=20
```

规则：

- 仅 `ADMIN/ACTIVE` 可访问。
- `status` 必填并接受现有歌曲状态枚举。
- `page` 从 `0` 开始；`size` 默认 `20`，最大 `100`。
- 响应包含 `items`、`page`、`size`、`totalItems` 和 `totalPages`。
- 不加入高级组合筛选、统计或跨 schema 数据。

### 4.4 歌手资料

新增：

```text
PUT /api/catalog/artists/me
GET /api/catalog/artists/{userId}
```

歌手可维护 `stageName` 和可选 `biography`；写接口仅允许 `SINGER/ACTIVE` 修改自己的 Catalog 资料。读接口允许已登录的活跃账户访问。Catalog 只查询自己的 `artist_profiles` 表。

### 4.5 封面与歌词关联

歌曲草稿请求增加可选 `coverMediaObjectId` 和 `lyricsMediaObjectId`；歌曲响应增加：

- `coverMediaObjectId`
- `lyricsMediaObjectId`
- 可用时的 `artistStageName`

音频对象 ID 仍为必填。前端未取得封面 ID 时显示项目自制默认封面；未取得歌词 ID 时不显示歌词入口。P4 不增加跨服务外键或 Catalog 对 Media schema 的直接查询。

## 5. music-web 信息架构

公开路由：

- `/login`
- `/register`
- `/activate`

受保护路由：

- `/discover`
- `/search`
- `/tracks/:trackId`
- `/artists/:userId`
- `/library/favorites`
- `/library/playlists`
- `/library/playlists/:playlistId`
- `/library/history`
- `/messages`
- `/artist/profile`
- `/artist/studio`

一级导航为发现、搜索、歌单、我的和歌手工作台。歌手工作台只对 `SINGER` 显示；普通用户注册固定产生 `USER`，歌手和管理员使用既有授权账号。

开发环境激活流程：注册成功后，用户端按注册邮箱从开发收件箱中精确选择激活消息，将 token 只保留在当前页面内存并提交激活接口。不得将激活 token 写入控制台、sessionStorage、截图或测试汇总。真实 SMTP 继续后置。

## 6. admin-web 信息架构

路由：

- `/login`
- `/users`
- `/tracks`
- `/messages`

除登录外全部要求 `ADMIN`。页面使用侧栏与表格布局：

- 用户管理：读取用户列表，客户端提供必要分页与状态空态；锁定操作需要确认。
- 歌曲管理：按状态读取管理列表，发布或锁定歌曲；操作需要确认并显示服务端结果。
- 消息发布：提交简单广播或目标角色消息，显示成功结果或可理解错误。

独立歌手管理、概览统计、审计日志和高级筛选不进入 P4。

## 7. 前端状态与会话

music-web Vuex 模块：

- `auth`：token、到期时间、用户 ID、角色和状态。
- `catalog`：推荐、搜索结果和当前歌曲。
- `library`：收藏、歌单、历史和消息。
- `player`：单曲队列、播放状态、进度、音量和歌词行。

admin-web 只建立 `auth` 和各页面局部列表状态，不复制 music-web 的播放器与音乐库 store。

bearer 和非秘密会话元数据保存在 `sessionStorage`，刷新后可恢复同一标签页会话；不使用 `localStorage` 做长期登录。收到 `401` 时清空会话并返回登录页；收到 `403` 时保留会话并显示权限不足。

## 8. 播放器与媒体

播放器使用单一 `HTMLAudioElement`：

- 播放、暂停、当前时间、总时长和音量。
- 单曲队列，不实现复杂队列编排。
- 用户拖动进度时设置 `currentTime`，由浏览器通过媒体 Cookie 发起真实 Range 请求。
- 切换歌曲时释放旧事件监听和临时对象 URL。
- 可用歌词按受限 UTF-8 LRC 文本解析；按当前时间选择同步行。

最近播放位置在以下时机写入 Interaction：

- 暂停
- 拖动完成
- 切歌
- 页面离开
- 播放期间每 15 秒一次

不得在每个 `timeupdate` 事件写数据库。写入失败不打断音频播放，但在界面保留非阻塞错误状态。

## 9. 歌手上传流程

歌手工作台按顺序执行：

1. 维护艺名和简介。
2. 上传必填 MP3 音频。
3. 可选上传合法图片封面。
4. 可选上传 UTF-8 歌词。
5. 使用媒体对象 ID 创建歌曲草稿。
6. 可编辑草稿标题、时长、封面和歌词关联。
7. 明确提交审核。

上传进度、客户端扩展名提示和服务端错误均可见；客户端检查不替代 Media 的魔数、MIME、大小和路径安全校验。

## 10. 视觉与响应式

已批准视觉方向：

- 中性灰白页面、深色稳定导航、黄色播放与主动作、绿色健康状态、红色危险动作。
- 用户端重内容和播放，不使用营销首页、超大标题或装饰性卡片堆叠。
- 后台端紧凑、可扫描，使用 Element UI 表格、分页、表单和确认对话框。
- 使用兼容 Vue 2 的 Lucide 图标；不熟悉的图标提供 tooltip。
- 卡片圆角不超过 8px；固定播放器、控件和封面使用稳定尺寸。
- 不按视口宽度缩放字体，不使用负字距。

桌面端使用固定底部播放栏。普通用户手机端使用不遮挡内容的迷你播放器与底部导航；独立手机全屏播放器、平板专项和像素级多设备适配后置。

旧项目素材不再使用，且用户已明确跳过 Image Gen。P4 只下载少量可合法复用的音乐摄影素材到前端静态资源目录，并在同目录记录来源 URL、作者和许可说明；运行时不依赖远程图片。真实歌曲封面优先显示用户上传媒体，没有封面时才使用本地默认摄影素材。

### 10.1 视觉实现清单

实现以已批准方向稿 `.superpowers/brainstorm/782-1787105445/content/visual-direction.html` 为唯一视觉基线。方向稿只负责视觉，不作为生产代码或远程资源依赖。

颜色锁定：

- 页面画布 `#f3f5f2`，表面 `#ffffff`，深色导航和播放器 `#202428`。
- 主文字 `#171a1d`，次文字 `#697078`，分隔线 `#d9ddd8`。
- 黄色主动作 `#f5c842`，绿色健康状态 `#2f9b68`，红色危险动作 `#cf4a45`。
- 不增加渐变、彩色遮罩、装饰光斑或单色主题变体。

排版与尺寸：

- 字体使用系统无衬线栈 `Inter, "PingFang SC", "Microsoft YaHei", sans-serif`，字距固定为 `0`。
- 页面标题桌面端 `24px/32px`，移动端 `20px/28px`；区块标题 `16px/24px`；正文 `14px/22px`；辅助文字 `12px/18px`；控件文字 `14px/20px`。
- music-web 桌面侧栏 `216px`，底部播放器 `88px`；封面保持 `1:1`，圆角 `6px`。
- admin-web 侧栏 `184px`，表格行高不低于 `44px`；卡片和面板圆角均不超过 `8px`。
- 动画仅用于播放状态、页面状态切换和确认反馈，时长 `120-180ms`，并遵守 `prefers-reduced-motion`。

首屏和可见文案：

- 品牌只显示 `MUSIC DREAMER`；用户端一级导航为 `发现音乐`、`搜索`、`我的收藏`、`我的歌单`、`最近播放`、`消息`、`歌手工作台`。
- 发现页首屏允许 `早上好`、`按最新发布排序，发现今天可以播放的音乐。`、`搜索歌曲或歌手`、`今日新声`、`查看全部`、`热门歌曲`、`按播放量排序`、`平台公告`。
- 移动端一级导航只显示 `发现`、`搜索`、`歌单`、`我的`。
- 后台品牌显示 `MD 管理`，一级导航只显示 `歌曲审核`、`用户管理`、`消息发布`；歌曲页标题为 `待审核歌曲`。
- 不增加营销标语、功能说明、虚构统计、装饰徽章或未批准的首屏文案。

组件与图标：

- 用户端使用开放内容区、媒体栅格、紧凑歌曲列表和固定播放器；后台使用侧栏、表格、分页、表单和确认对话框，不把表格改成卡片网格。
- 使用兼容 Vue 2 的 Lucide 图标：播放/暂停、上一首/下一首、搜索、收藏、歌单、历史、消息、用户、上传、音量、更多、退出、审核和锁定。
- 图标默认 `20px`、`2px` 描边；播放器主按钮 `24px`，图标按钮有可见焦点和 `aria-label`，不熟悉的动作提供 tooltip。
- 危险操作使用红色文字或描边并必须确认；绿色只表达服务或内容可用，黄色只表达当前选中、播放或主动作。

媒体与响应式：

- 摄影素材无颜色遮罩，使用 `object-fit: cover` 和稳定比例；来源记录与文件一同交付。
- 桌面首屏保持左侧导航、内容区和底部播放器同时可见；播放器不覆盖正文，长标题使用省略并保留完整 tooltip。
- `<= 768px` 隐藏桌面侧栏，使用底部四项导航和位于其上的迷你播放器；正文预留两者高度，不出现横向溢出。
- admin-web 仅保证桌面可扫描；窄屏允许表格容器水平滚动，但操作按钮和表头不得互相覆盖。

## 11. API 与错误处理

共享 Axios 客户端：

- 开发时通过各应用的 `/api` 代理访问 `http://127.0.0.1:8080`。
- 普通 API 自动附加 bearer。
- 媒体元素直接使用同源 `/api/media/objects/{id}` URL，不把 bearer 拼进地址。
- 请求超时、网络错误和后端 `ProblemDetail` 转换为稳定前端错误对象。

展示规则：

- 字段错误显示在对应输入旁。
- `401` 清会话并跳转登录。
- `403` 显示权限不足，不清会话。
- `409` 显示状态冲突并刷新相关数据。
- `503` 保留用户输入并提供重试。
- 未知错误显示通用消息和可用的 trace ID，不展示堆栈。

## 12. TDD 与验证

所有新增生产行为遵循 RED -> GREEN -> REFACTOR，并保存 RED/GREEN 命令与结果。

后端聚焦测试：

- 登录响应包含服务端用户元数据。
- 登录与退出正确设置/过期媒体 Cookie。
- Gateway 仅对无 bearer 的媒体 GET 接受 Cookie；错误 bearer 不回退；写方法拒绝 Cookie。
- 管理员歌曲列表的角色、状态过滤和分页。
- 歌手资料只能由本人维护。
- 草稿封面/歌词关联和响应映射。

shared 测试：

- bearer 注入、401/403/409/503 转换、角色和状态常量。

music-web 聚焦测试：

- 注册、开发激活、登录和路由守卫。
- 推荐与搜索不是个性化文案。
- 播放、暂停、拖动、音量、单曲切换和歌词同步。
- 历史写入节流。
- 收藏、歌单、消息和歌手上传提交。

admin-web 聚焦测试：

- 非管理员守卫。
- 用户、歌曲和消息页面的加载、空态和错误态。
- 危险操作确认、取消、成功和失败反馈。

P4 checkpoint：

1. 两个前端测试独立通过。
2. 两个生产构建独立通过。
3. Playwright 桌面完成普通用户、歌手和管理员三条真实流程。
4. 手机视口重跑普通用户主流程。
5. 检查浏览器控制台错误、关键截图、空白、重叠和文字溢出。
6. 不清理用户 15-18，不自动退出或删除新会话。

P4 不自动运行 P5 完整 Maven reactor、全服务重启持久化矩阵或最终主体报告。

## 13. 本地运行

- music-web 开发端口：`5173`
- admin-web 开发端口：`5174`
- 两者都将 `/api` 代理到 Gateway `8080`

P4 提供各应用标准 `dev`、`test` 和 `build` 脚本。完整的五服务加双前端统一生命周期脚本属于 P5 集成加固范围。

## 14. 明确非目标

以下功能不因用户提供的概览表而成为 P4 验收项：

- JWT 认证
- 个性化推荐算法或推荐降级编排
- 歌单关注、收藏他人歌单和公开分享
- 评论与点赞
- 歌曲硬删除
- 播放计数 outbox
- 可靠审计事件
- 高级筛选、统计和独立审计页面
- 真实 SMTP
- CodeArts、公网部署、课程报告和答辩材料

## 15. 文件所有权与发布边界

P4 由 Codex 在主工作树单写者串行实施。用户/P2/P3 现有未提交修改不得回滚、清理或覆盖。

本设计批准不授权提交、合并、推送或发布。依赖安装、生产构建、Playwright、可能覆盖生成物的命令及任何删除行为，仍按对应执行边界单独报告和确认。
