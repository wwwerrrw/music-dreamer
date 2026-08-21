# 悦享音乐 Stitch 双前端重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不修改后端源码和现有 API 语义的前提下，将 Stitch `Vibrant Lyricism` 浅色方案完整落地到 `music-web` 与 `admin-web`。

**Architecture:** 保留现有 Vue 2.7 路由、Vuex、API adapter、权限和播放器行为，先在 `frontend/shared` 建立统一设计令牌，再分别重构两个应用的壳层、共享组件和页面 DOM。视觉契约使用 Node 内置测试读取源文件验证，功能回归沿用现有测试，最终使用浏览器截图对照 Stitch 正式页面。

**Tech Stack:** Vue 2.7.16、Vue Router 3.6.5、Vuex 3.6.2、Vite 4.5.5、Vue CLI 5.0.8、Element UI 2.15.14、lucide-vue 0.517.0、Node 24.18.0。

## Global Constraints

- 用户可见项目名统一为“悦享音乐”。
- 唯一视觉基线为 Stitch 项目 `4059305951146804381` 的 `Vibrant Lyricism` 浅色设计系统。
- 不修改 `backend/`、数据库迁移、共享后端契约、Gateway 配置或 API 路径。
- 不直接复制 Stitch HTML；只提取布局、层级、文案、颜色、字体、间距和响应式行为。
- 保留唯一 `HTMLAudioElement`、同源媒体 URL、SessionStorage 和 401/403/409/503 语义。
- 所有新增行为遵循 RED -> GREEN -> REFACTOR。
- 测试、构建、截图和报告会写入现有输出目录，执行前按项目规则报告精确范围。
- 不清理或删除现有 `dist`、`node_modules`、测试结果或用户数据。

---

### Task 1: 共享 Vibrant Lyricism 设计令牌

**Files:**
- Modify: `frontend/shared/src/styles/tokens.css`
- Create: `frontend/shared/tests/design-system.test.mjs`
- Verify: `frontend/music-web/src/main.js`
- Verify: `frontend/admin-web/src/main.js`

**Interfaces:**
- Produces: CSS custom properties `--md-canvas`, `--md-surface`, `--md-primary`, `--md-secondary`, `--md-ink`, `--md-muted`, `--md-line`, `--md-success`, `--md-danger`, `--md-sidebar-width`, `--md-player-height`。
- Consumes: 两个应用现有的 `@music-dreamer/shared/styles/tokens.css` 导入。

- [ ] **Step 1: 写失败的设计令牌契约测试**

```js
test('shared tokens lock the approved light Stitch palette', () => {
  const css = readFileSync(new URL('../src/styles/tokens.css', import.meta.url), 'utf8');
  for (const declaration of [
    '--md-canvas: #fff8f7', '--md-surface: #ffffff', '--md-primary: #e63946',
    '--md-secondary: #1d3557', '--md-ink: #1a1a1a', '--md-muted: #6c757d'
  ]) assert.match(css, new RegExp(declaration.replace('#', '\\#')));
  assert.doesNotMatch(css, /#0d0d11|Sonic Obsidian/i);
});
```

- [ ] **Step 2: 运行 RED**

Run: `npm --workspace @music-dreamer/shared test`

Expected: FAIL，因为当前仍是灰黄 P4 令牌，缺少 `--md-primary`。

- [ ] **Step 3: 最小实现共享浅色令牌**

将 `tokens.css` 改为 Stitch 浅粉白画布、纯白表面、绯红主色、深蓝辅助色和稳定尺寸；保留全局盒模型、中文字体回退、可见焦点和 768px 断点。

- [ ] **Step 4: 运行 GREEN 与共享回归**

Run: `npm --workspace @music-dreamer/shared test`

Expected: 所有 shared 测试通过。

- [ ] **Step 5: 提交**

```powershell
git add -- frontend/shared/src/styles/tokens.css frontend/shared/tests/design-system.test.mjs
git commit -m "style: add Vibrant Lyricism frontend tokens"
```

### Task 2: music-web 壳层与认证页面

**Files:**
- Modify: `frontend/music-web/src/components/AppShell.vue`
- Modify: `frontend/music-web/src/components/AuthLayout.vue`
- Modify: `frontend/music-web/src/components/MiniPlayer.vue`
- Modify: `frontend/music-web/src/components/StateMessage.vue`
- Modify: `frontend/music-web/src/styles/app.css`
- Modify: `frontend/music-web/src/views/LoginView.vue`
- Modify: `frontend/music-web/src/views/RegisterView.vue`
- Modify: `frontend/music-web/src/views/ActivateView.vue`
- Create: `frontend/music-web/tests/visual-contract.test.mjs`

**Interfaces:**
- Consumes: Task 1 设计令牌、现有 `navigationFor()`、现有 `player` Vuex module。
- Produces: 桌面侧栏、移动底栏、固定/悬浮播放器、认证布局和公共页面视觉契约。

- [ ] **Step 1: 写失败的壳层契约测试**

```js
test('music shell and auth copy use the approved brand and responsive chrome', () => {
  assert.match(appShell, /悦享音乐/);
  assert.match(appCss, /var\(--md-primary\)/);
  assert.match(appCss, /\.mobile-navigation/);
  assert.match(appCss, /padding-bottom:\s*calc\(/);
  assert.doesNotMatch(appShell, /MUSIC\s*<span>DREAMER/);
});
```

- [ ] **Step 2: 运行 RED**

Run: `npm --workspace music-web test`

Expected: FAIL，因为当前品牌仍是 `MUSIC DREAMER`，移动正文没有播放器与导航合计预留。

- [ ] **Step 3: 重构壳层和播放器外观**

保持所有现有事件和 methods；将品牌替换为“悦享音乐”，侧栏使用浅色表面与绯红活动态，播放器采用白色悬浮表面和稳定进度控件，移动端预留播放器与导航总高度。

- [ ] **Step 4: 重构登录、注册和启用结果页面**

按 Stitch 认证页面重排标题、输入框、密码可见性容器、主按钮、切换入口和反馈状态；不得记录密码或激活令牌，不改变现有 submit workflow。

- [ ] **Step 5: 运行 GREEN 与构建检查**

Run: `npm --workspace music-web test`

Run: `npm --workspace music-web run build`

Expected: 测试与构建均通过。

- [ ] **Step 6: 提交**

```powershell
git add -- frontend/music-web/src/components frontend/music-web/src/styles/app.css frontend/music-web/src/views/LoginView.vue frontend/music-web/src/views/RegisterView.vue frontend/music-web/src/views/ActivateView.vue frontend/music-web/tests/visual-contract.test.mjs
git commit -m "style: rebuild music shell and auth views"
```

### Task 3: 发现、搜索、歌曲、歌手与播放器详情

**Files:**
- Modify: `frontend/music-web/src/components/TrackRow.vue`
- Modify: `frontend/music-web/src/styles/content.css`
- Modify: `frontend/music-web/src/views/DiscoverView.vue`
- Modify: `frontend/music-web/src/views/SearchView.vue`
- Modify: `frontend/music-web/src/views/TrackView.vue`
- Modify: `frontend/music-web/src/views/ArtistView.vue`
- Modify: `frontend/music-web/tests/visual-contract.test.mjs`

**Interfaces:**
- Consumes: 现有 discovery、catalog、player 和 media helpers。
- Produces: 统一音乐卡片、歌曲行、搜索栏、歌词区、歌手头部和移动播放详情。

- [ ] **Step 1: 扩展失败的页面结构契约**

```js
test('catalog views expose Stitch page landmarks without personalization claims', () => {
  assert.match(discover, /今日新声/);
  assert.match(discover, /热门单曲/);
  assert.match(search, /搜索结果/);
  assert.match(track, /歌词|暂无歌词/);
  assert.match(artist, /热门歌曲/);
  assert.doesNotMatch(discover, /个性化推荐/);
});
```

- [ ] **Step 2: 运行 RED**

Run: `npm --workspace music-web test`

Expected: FAIL 于尚未落地的正式页面结构类名或标题。

- [ ] **Step 3: 重构发现和搜索**

保持真实查询和排序；实现封面主区、最新/热门分区、圆角搜索框、筛选视觉、紧凑结果行和底部播放器避让。

- [ ] **Step 4: 重构歌曲和歌手详情**

桌面使用封面、元数据、动作、歌词/列表分区；移动使用单列封面和触控播放控件。保留收藏、歌单、LRC 和真实媒体行为。

- [ ] **Step 5: 运行 GREEN**

Run: `npm --workspace music-web test`

Expected: 所有 music-web 测试通过。

- [ ] **Step 6: 提交**

```powershell
git add -- frontend/music-web/src/components/TrackRow.vue frontend/music-web/src/styles/content.css frontend/music-web/src/views/DiscoverView.vue frontend/music-web/src/views/SearchView.vue frontend/music-web/src/views/TrackView.vue frontend/music-web/src/views/ArtistView.vue frontend/music-web/tests/visual-contract.test.mjs
git commit -m "style: rebuild music discovery and detail views"
```

### Task 4: 音乐库、消息与歌手工作台

**Files:**
- Modify: `frontend/music-web/src/views/FavoritesView.vue`
- Modify: `frontend/music-web/src/views/HistoryView.vue`
- Modify: `frontend/music-web/src/views/PlaylistsView.vue`
- Modify: `frontend/music-web/src/views/PlaylistView.vue`
- Modify: `frontend/music-web/src/views/MessagesView.vue`
- Modify: `frontend/music-web/src/views/ArtistProfileView.vue`
- Modify: `frontend/music-web/src/views/ArtistStudioView.vue`
- Modify: `frontend/music-web/src/styles/content.css`
- Modify: `frontend/music-web/tests/visual-contract.test.mjs`

**Interfaces:**
- Consumes: 现有 library API/model、artist studio API/model、统一 `TrackRow`。
- Produces: 收藏/历史/歌单标签化信息架构、消息列表、歌手资料和正式上传工作台。

- [ ] **Step 1: 写失败的工作流视觉契约**

```js
test('library and studio views keep all approved workflow landmarks', () => {
  for (const label of ['收藏歌曲', '最近播放', '我的歌单', '消息中心', '歌曲资料', '提交审核']) {
    assert.equal(allViews.includes(label), true, `missing ${label}`);
  }
});
```

- [ ] **Step 2: 运行 RED**

Run: `npm --workspace music-web test`

Expected: FAIL 于尚未统一的页面文案或工作台分区。

- [ ] **Step 3: 重构音乐库和消息**

共享歌曲行和状态组件；保留收藏幂等、歌单增删、历史读取和消息已读行为。移动端以单列标签、横向歌单和悬浮播放器呈现。

- [ ] **Step 4: 重构歌手资料与上传工作台**

正式中文分区为“媒体文件、基本信息、发布设置、草稿”；上传失败保留输入，主操作固定为“保存草稿”和“提交审核”。

- [ ] **Step 5: 运行 GREEN 与 music-web 构建**

Run: `npm --workspace music-web test`

Run: `npm --workspace music-web run build`

Expected: 测试与构建均通过。

- [ ] **Step 6: 提交**

```powershell
git add -- frontend/music-web/src/views frontend/music-web/src/styles/content.css frontend/music-web/tests/visual-contract.test.mjs
git commit -m "style: rebuild music library and artist studio"
```

### Task 5: admin-web 全页面重构

**Files:**
- Modify: `frontend/admin-web/src/components/AdminShell.vue`
- Modify: `frontend/admin-web/src/components/AdminState.vue`
- Modify: `frontend/admin-web/src/styles/admin.css`
- Modify: `frontend/admin-web/src/views/LoginView.vue`
- Modify: `frontend/admin-web/src/views/TracksView.vue`
- Modify: `frontend/admin-web/src/views/UsersView.vue`
- Modify: `frontend/admin-web/src/views/MessagesView.vue`
- Create: `frontend/admin-web/tests/visual-contract.test.mjs`

**Interfaces:**
- Consumes: 现有 `createAdminApi()`、store actions、Element UI 表格/分页/确认行为。
- Produces: 浅色后台壳层、紧凑审核表、详情抽屉、用户表、消息编辑与预览布局。

- [ ] **Step 1: 写失败的后台视觉契约**

```js
test('admin source exposes the approved brand and review workflow', () => {
  assert.match(shell, /悦享音乐/);
  assert.match(tracks, /待审核队列/);
  assert.match(tracks, /查看详情/);
  assert.match(users, /用户管理/);
  assert.match(messages, /发布消息/);
  assert.doesNotMatch(shell, />MD\s*<span>管理/);
});
```

- [ ] **Step 2: 运行 RED**

Run: `npm --workspace admin-web test`

Expected: FAIL，因为当前后台品牌和审核详情结构未达到正式方案。

- [ ] **Step 3: 重构后台壳层与登录**

使用浅色侧栏、绯红活动指示、统一品牌和工作台标题；管理员登录沿用认证视觉但保留管理员专属文案。

- [ ] **Step 4: 重构歌曲审核与详情状态**

表格为唯一视觉中心，操作权重为详情文字、通过实心、退回描边；详情使用不新增路由的右侧抽屉，保留确认与反馈。

- [ ] **Step 5: 重构用户管理和消息发布**

用户页保持高密度搜索、筛选、状态和分页；消息页使用编辑、目标范围、预览与发布布局，保留取消确认不调用 API。

- [ ] **Step 6: 运行 GREEN 与后台构建**

Run: `npm --workspace admin-web test`

Run: `npm --workspace admin-web run build`

Expected: 测试与构建均通过。

- [ ] **Step 7: 提交**

```powershell
git add -- frontend/admin-web/src frontend/admin-web/tests/visual-contract.test.mjs
git commit -m "style: rebuild admin frontend from Stitch"
```

### Task 6: 双前端回归与视觉验收

**Files:**
- Verify only: `frontend/**`
- Temporary evidence: system temp directory only，不写入仓库。

**Interfaces:**
- Consumes: Tasks 1-5 完成的两个前端。
- Produces: 测试、构建、桌面/移动截图、控制台和交互证据。

- [ ] **Step 1: 运行全量前端测试**

Run: `npm test`

Expected: shared、music-web、admin-web 全部通过，0 failures。

- [ ] **Step 2: 运行两个生产构建**

Run: `npm run build`

Expected: 两个构建退出码为 0。该命令会覆盖 `frontend/music-web/dist/**` 与 `frontend/admin-web/dist/**`，执行前单独报告范围。

- [ ] **Step 3: 启动开发服务器**

Run: `npm --workspace music-web run dev`

Run: `npm --workspace admin-web run dev`

Expected: `http://127.0.0.1:5173` 与 `http://127.0.0.1:5174` 可访问；如端口占用则使用明确的备用端口。

- [ ] **Step 4: 浏览器桌面验证**

验证 `music-web` 认证、发现、搜索、歌曲、音乐库、消息和歌手工作台；验证 `admin-web` 登录、审核详情、用户和消息。检查 URL/title、非空 DOM、无框架错误层、控制台错误和至少一个交互状态变化。

- [ ] **Step 5: 浏览器移动验证**

以 390x844 或 Stitch 780x1768 的等比例视口验证登录、发现、搜索、播放、我的音乐和消息；检查播放器与底栏不遮挡、无横向溢出。

- [ ] **Step 6: Stitch 忠实度台账**

至少核对：品牌文案、画布/表面颜色、导航、主色按钮、封面比例、播放器、表格密度、详情抽屉、响应式避让和字体等级。每项记录概念证据、渲染证据以及修正结果。

- [ ] **Step 7: 最终差异检查**

Run: `git diff --check`

Run: `git status --short`

确认 `backend/` 没有由本次新增的改动，且没有临时截图、报告或调试文件写入仓库。
