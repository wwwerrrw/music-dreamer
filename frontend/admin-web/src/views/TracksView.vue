<template>
  <div class="admin-page tracks-admin-page">
    <header class="admin-page-header"><div><span class="admin-eyebrow">内容管理 / 歌曲审核</span><h1>待审核队列</h1><p>核对歌曲信息后执行发布或锁定。</p></div><button class="admin-icon-button" type="button" aria-label="刷新队列" title="刷新队列" @click="reload()"><RefreshCw :size="18" aria-hidden="true" /></button></header>
    <div class="admin-filter-bar">
      <el-select v-model="status" size="small" aria-label="歌曲状态" @change="reload(0)"><el-option label="待审核" value="SUBMITTED" /><el-option label="已发布" value="PUBLISHED" /><el-option label="已锁定" value="LOCKED" /><el-option label="草稿" value="DRAFT" /></el-select>
      <label class="admin-search-input"><Search :size="17" aria-hidden="true" /><input v-model.trim="query" placeholder="搜索歌曲或歌手" aria-label="搜索歌曲或歌手"></label>
    </div>

    <AdminState v-if="loading" kind="loading" message="正在加载歌曲…" />
    <AdminState v-else-if="error" kind="error" :message="error" />
    <AdminState v-else-if="!filteredItems.length" kind="empty" message="当前筛选下没有歌曲。" />
    <template v-else>
      <el-table class="admin-data-table review-table" :data="filteredItems">
        <el-table-column label="歌曲" min-width="260">
          <template #default="scope"><div class="admin-track-cell"><img v-if="scope.row.coverMediaObjectId" :src="coverFor(scope.row)" :alt="`${scope.row.title} 封面`"><span v-else><Music2 :size="20" aria-hidden="true" /></span><div><strong>{{ scope.row.title }}</strong><small>#{{ scope.row.id }}</small></div></div></template>
        </el-table-column>
        <el-table-column label="艺人" min-width="150"><template #default="scope"><span>{{ scope.row.artistStageName || `歌手 #${scope.row.artistUserId}` }}</span></template></el-table-column>
        <el-table-column label="时长" width="100"><template #default="scope"><span class="mono-value">{{ formatDuration(scope.row.durationMs) }}</span></template></el-table-column>
        <el-table-column label="提交时间" min-width="150"><template #default="scope"><span>{{ formatDate(scope.row.createdAt) }}</span></template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="scope"><span class="admin-status" :class="`is-${scope.row.status.toLowerCase()}`">{{ statusText(scope.row.status) }}</span></template></el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope"><div class="review-actions"><el-button type="text" @click="openDetail(scope.row)">查看详情</el-button><el-button v-if="scope.row.status === 'PENDING_REVIEW'" class="approve-button" size="mini" @click="publish(scope.row)">通过</el-button><el-button v-if="scope.row.status !== 'LOCKED'" class="lock-button" size="mini" @click="lock(scope.row)">锁定</el-button></div></template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="prev, pager, next" :current-page="page.page + 1" :page-size="page.size" :total="page.totalItems" @current-change="changePage" />
    </template>

    <el-drawer custom-class="review-drawer" :visible.sync="drawerOpen" size="420px" :with-header="false">
      <article v-if="selectedTrack" class="review-detail">
        <header><h2>歌曲详情</h2><button type="button" aria-label="关闭详情" title="关闭详情" @click="drawerOpen = false"><X :size="19" aria-hidden="true" /></button></header>
        <img v-if="selectedTrack.coverMediaObjectId" class="review-cover" :src="coverFor(selectedTrack)" :alt="`${selectedTrack.title} 封面`"><div v-else class="review-cover review-cover-placeholder"><Music2 :size="44" aria-hidden="true" /></div>
        <dl><div><dt>歌曲名称</dt><dd>{{ selectedTrack.title }}</dd></div><div><dt>创作者</dt><dd>{{ selectedTrack.artistStageName || `歌手 #${selectedTrack.artistUserId}` }}</dd></div><div><dt>歌曲时长</dt><dd>{{ formatDuration(selectedTrack.durationMs) }}</dd></div><div><dt>提交时间</dt><dd>{{ formatDate(selectedTrack.createdAt) }}</dd></div><div><dt>当前状态</dt><dd><span class="admin-status">{{ statusText(selectedTrack.status) }}</span></dd></div></dl>
        <footer><button v-if="selectedTrack.status === 'PENDING_REVIEW'" class="drawer-approve" type="button" @click="publish(selectedTrack)">通过审核</button><button v-if="selectedTrack.status !== 'LOCKED'" class="drawer-lock" type="button" @click="lock(selectedTrack)">锁定歌曲</button></footer>
      </article>
    </el-drawer>
  </div>
</template>

<script>
import { Music2, RefreshCw, Search, X } from 'lucide-vue';
import { mediaObjectUrl } from '@music-dreamer/shared';
import { adminApi } from '../api/index.js';
import AdminState from '../components/AdminState.vue';
import { adminProblemMessage } from '../ui/admin-model.js';

export default {
  name: 'TracksView',
  components: { AdminState, Music2, RefreshCw, Search, X },
  data: () => ({ status: 'SUBMITTED', query: '', loading: true, error: '', drawerOpen: false, selectedTrack: null, page: { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 } }),
  computed: {
    filteredItems() {
      const query = this.query.toLocaleLowerCase('zh-CN');
      if (!query) return this.page.items;
      return this.page.items.filter((item) => `${item.title} ${item.artistStageName || ''}`.toLocaleLowerCase('zh-CN').includes(query));
    }
  },
  async mounted() { await this.reload(); },
  methods: {
    statusText(value) { return { DRAFT: '草稿', PENDING_REVIEW: '待审核', PUBLISHED: '已发布', LOCKED: '已锁定' }[value] || value; },
    coverFor(row) { return mediaObjectUrl(row.coverMediaObjectId); },
    formatDuration(ms) { const total = Math.max(0, Math.round((ms || 0) / 1000)); return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`; },
    formatDate(value) { return value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—'; },
    openDetail(row) { this.selectedTrack = row; this.drawerOpen = true; },
    async reload(page = this.page.page || 0) {
      this.loading = true;
      this.error = '';
      try { this.page = await adminApi.tracks({ status: this.status, page, size: 20 }); }
      catch (problem) { this.error = adminProblemMessage(problem); }
      finally { this.loading = false; }
    },
    changePage(value) { this.reload(value - 1); },
    async publish(row) {
      try {
        await this.$confirm(`确认通过并发布“${row.title}”？`, '通过审核', { confirmButtonText: '通过', cancelButtonText: '取消', type: 'warning' });
        await adminApi.publishTrack(row.id);
        this.drawerOpen = false;
        await this.reload();
        this.$message.success('歌曲已发布。');
      } catch (problem) { if (problem !== 'cancel' && problem !== 'close') this.$message.error(adminProblemMessage(problem)); }
    },
    async lock(row) {
      try {
        await this.$confirm(`锁定后“${row.title}”将无法播放，确认继续？`, '锁定歌曲', { confirmButtonText: '锁定', cancelButtonText: '取消', type: 'warning' });
        await adminApi.lockTrack(row.id);
        this.drawerOpen = false;
        await this.reload();
        this.$message.success('歌曲已锁定。');
      } catch (problem) { if (problem !== 'cancel' && problem !== 'close') this.$message.error(adminProblemMessage(problem)); }
    }
  }
};
</script>
