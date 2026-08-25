<template>
  <div class="admin-page users-admin-page">
    <header class="admin-page-header"><div><span class="admin-eyebrow">账号与权限</span><h1>用户管理</h1><p>查找账号并处理异常状态。</p></div><button class="admin-icon-button" type="button" aria-label="刷新用户" title="刷新用户" @click="load"><RefreshCw :size="18" aria-hidden="true" /></button></header>
    <div class="admin-filter-bar users-filter-bar">
      <label class="admin-search-input user-search"><Search :size="17" aria-hidden="true" /><input v-model.trim="query" placeholder="搜索用户名或邮箱" aria-label="搜索用户名或邮箱"></label>
      <el-select v-model="role" size="small" aria-label="用户角色"><el-option label="所有角色" value="ALL" /><el-option label="普通用户" value="USER" /><el-option label="歌手" value="SINGER" /><el-option label="管理员" value="ADMIN" /></el-select>
    </div>
    <AdminState v-if="loading" kind="loading" message="正在加载用户…" />
    <AdminState v-else-if="error" kind="error" :message="error" />
    <AdminState v-else-if="!filteredUsers.length" kind="empty" message="当前筛选下没有用户。" />
    <el-table v-else class="admin-data-table users-table" :data="filteredUsers">
      <el-table-column label="用户" min-width="250"><template #default="scope"><div class="admin-user-cell"><span>{{ scope.row.username.slice(0, 1).toUpperCase() }}</span><div><strong>{{ scope.row.username }}</strong><small>{{ scope.row.email }}</small></div></div></template></el-table-column>
      <el-table-column label="角色" width="130"><template #default="scope"><span class="admin-role">{{ roleText(scope.row.role) }}</span></template></el-table-column>
      <el-table-column label="账号状态" width="130"><template #default="scope"><span class="admin-account-status" :class="scope.row.status === 'ACTIVE' ? 'is-active' : 'is-locked'">{{ statusText(scope.row.status) }}</span></template></el-table-column>
      <el-table-column label="数据版本" width="110"><template #default="scope"><span class="mono-value">v{{ scope.row.version }}</span></template></el-table-column>
      <el-table-column label="操作" width="240" fixed="right"><template #default="scope"><div class="user-actions"><el-button v-if="scope.row.role === 'USER'" class="promote-user-button" type="text" :loading="promotingId === scope.row.id" :disabled="promotingId !== null" @click="promote(scope.row)">提升为歌手</el-button><el-button v-if="scope.row.status === 'LOCKED'" class="restore-user-button" type="text" @click="restore(scope.row)">恢复账号</el-button><el-button v-else class="lock-user-button" type="text" @click="lock(scope.row)">锁定账号</el-button></div></template></el-table-column>
    </el-table>
    <p v-if="!loading && !error" class="admin-result-count">共 {{ filteredUsers.length }} 个账号</p>
  </div>
</template>

<script>
import { RefreshCw, Search } from 'lucide-vue';
import { adminApi } from '../api/index.js';
import AdminState from '../components/AdminState.vue';
import { adminProblemMessage } from '../ui/admin-model.js';

export default {
  name: 'UsersView',
  components: { AdminState, RefreshCw, Search },
  data: () => ({ loading: true, error: '', query: '', role: 'ALL', users: [], promotingId: null }),
  computed: {
    filteredUsers() {
      const query = this.query.toLocaleLowerCase('zh-CN');
      return this.users.filter((user) => (this.role === 'ALL' || user.role === this.role) && (!query || `${user.username} ${user.email}`.toLocaleLowerCase('zh-CN').includes(query)));
    }
  },
  async mounted() { await this.load(); },
  methods: {
    roleText(value) { return { USER: '普通用户', SINGER: '歌手', ADMIN: '管理员' }[value] || value; },
    statusText(value) { return { ACTIVE: '启用', LOCKED: '已锁定', PENDING: '待启用' }[value] || value; },
    async load() {
      this.loading = true;
      this.error = '';
      try { this.users = await adminApi.users(); }
      catch (problem) { this.error = adminProblemMessage(problem); }
      finally { this.loading = false; }
    },
    async lock(user) {
      try {
        await this.$confirm(`确认锁定用户“${user.username}”？`, '锁定用户', { confirmButtonText: '锁定', cancelButtonText: '取消', type: 'warning' });
        await adminApi.lockUser(user.id);
        await this.load();
        this.$message.success('用户已锁定。');
      } catch (problem) { if (problem !== 'cancel' && problem !== 'close') this.$message.error(adminProblemMessage(problem)); }
    },
    async restore(user) {
      try {
        await this.$confirm(`确认恢复用户“${user.username}”？`, '恢复账号', { confirmButtonText: '恢复', cancelButtonText: '取消', type: 'warning' });
        await adminApi.restoreUser(user.id);
        await this.load();
        this.$message.success('用户已恢复。');
      } catch (problem) { if (problem !== 'cancel' && problem !== 'close') this.$message.error(adminProblemMessage(problem)); }
    },
    async promote(user) {
      try {
        await this.$confirm(`确认将用户“${user.username}”提升为歌手？该用户需要重新登录。`, '提升为歌手', { confirmButtonText: '确认提升', cancelButtonText: '取消', type: 'warning' });
        this.promotingId = user.id;
        await adminApi.promoteSinger(user.id);
        await this.load();
        this.$message.success('用户已提升为歌手。');
      } catch (problem) {
        if (problem !== 'cancel' && problem !== 'close') this.$message.error(adminProblemMessage(problem));
      } finally {
        this.promotingId = null;
      }
    }
  }
};
</script>
