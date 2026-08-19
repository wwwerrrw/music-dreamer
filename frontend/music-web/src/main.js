import Vue from 'vue';
import App from './App.vue';
import router from './router/index.js';
import store from './store/index.js';
import '@music-dreamer/shared/styles/tokens.css';
import './styles/app.css';
import './styles/content.css';

Vue.config.productionTip = false;

new Vue({ router, store, render: (createElement) => createElement(App) }).$mount('#app');
