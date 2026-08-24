<template>
  <div class="page-shell messages-page">
    <header class="page-header compact">
      <div><h1>消息中心</h1><p class="page-subtitle">平台通知与账号动态。</p></div>
      <button v-if="unreadCount" class="messages-read-all" type="button" @click="readAll"><CheckCheck :size="17" aria-hidden="true" /><span>全部标记已读</span></button>
    </header>
    <StateMessage v-if="state !== 'ready'" :state="state" :message="message" />
    <div v-else-if="messages.length" class="message-center-layout">
      <section class="message-inbox" aria-label="收件箱">
        <div class="message-inbox-heading"><h2>收件箱</h2><span>{{ unreadCount }} 条未读</span></div>
        <button v-for="item in messages" :key="item.id" class="message-list-item" :class="{ unread: !item.readAt, selected: item.id === selectedId }" type="button" @click="select(item.id)">
          <span class="message-icon"><Mail :size="19" aria-hidden="true" /></span>
          <span class="message-list-copy"><strong>{{ item.subject }}</strong><small>{{ item.body }}</small></span>
          <time>{{ formatDate(item.publishedAt) }}</time>
          <span v-if="!item.readAt" class="unread-dot" aria-label="未读"></span>
        </button>
      </section>

      <article v-if="selectedMessage" class="message-detail-panel">
        <header><span class="message-detail-icon"><Bell :size="22" aria-hidden="true" /></span><div><h2>{{ selectedMessage.subject }}</h2><p>悦享音乐 · {{ formatDate(selectedMessage.publishedAt) }}</p></div></header>
        <div class="message-detail-body">{{ selectedMessage.body }}</div>
        <button v-if="!selectedMessage.readAt" class="primary-button" type="button" @click="read(selectedMessage.id)"><Check :size="17" aria-hidden="true" /><span>标记已读</span></button>
        <span v-else class="message-read-state"><Check :size="16" aria-hidden="true" />已读</span>
      </article>
      <p v-if="feedback" class="message-feedback" role="status">{{ feedback }}</p>
    </div>
    <p v-else class="empty-state">暂时没有消息。</p>
  </div>
</template>
<script>
import { Bell, Check, CheckCheck, Mail } from 'lucide-vue';
import StateMessage from '../components/StateMessage.vue';
import { library } from '../library/client.js';
import { markMessageRead } from '../library/models.js';
import { problemMessage } from '../ui/problem-message.js';

export default {
  name: 'MessagesView',
  components: { Bell, Check, CheckCheck, Mail, StateMessage },
  data: () => ({ state: 'loading', message: '正在加载消息…', feedback: '', messages: [], selectedId: null }),
  computed: {
    selectedMessage() { return this.messages.find((item) => item.id === this.selectedId) || this.messages[0] || null; },
    unreadCount() { return this.messages.filter((item) => !item.readAt).length; }
  },
  async mounted() {
    try {
      this.messages = await library.messages();
      this.selectedId = this.messages[0] ? this.messages[0].id : null;
      this.state = 'ready';
    } catch (problem) {
      this.state = 'error';
      this.message = problemMessage(problem);
    }
  },
  methods: {
    formatDate(value) { return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit' }).format(new Date(value)); },
    select(id) { this.selectedId = id; },
    async read(id) {
      this.feedback = '';
      try {
        await library.markRead(id);
        this.messages = markMessageRead(this.messages, id, new Date().toISOString());
      } catch (problem) {
        this.feedback = problemMessage(problem);
      }
    },
    async readAll() {
      this.feedback = '';
      try {
        const unread = this.messages.filter((item) => !item.readAt);
        await Promise.all(unread.map((item) => library.markRead(item.id)));
        const readAt = new Date().toISOString();
        this.messages = unread.reduce((items, item) => markMessageRead(items, item.id, readAt), this.messages);
      } catch (problem) {
        this.feedback = problemMessage(problem);
      }
    }
  }
};
</script>
