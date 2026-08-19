import Vue from 'vue'; import VueRouter from 'vue-router'; import {loadAdminSession} from '../auth/session.js'; import {resolveAdminAccess} from './access.js';
import LoginView from '../views/LoginView.vue'; import TracksView from '../views/TracksView.vue'; import UsersView from '../views/UsersView.vue'; import MessagesView from '../views/MessagesView.vue';
Vue.use(VueRouter);
const routes=[{path:'/',redirect:'/tracks'},{path:'/login',component:LoginView,meta:{public:true}},{path:'/tracks',component:TracksView},{path:'/users',component:UsersView},{path:'/messages',component:MessagesView},{path:'*',redirect:'/tracks'}];
const router=new VueRouter({mode:'history',routes});
router.beforeEach((to,from,next)=>{const session=loadAdminSession(window.sessionStorage);if(to.meta.public)return session?next('/tracks'):next();const decision=resolveAdminAccess(to.fullPath,session);return decision.allow?next():next(decision.redirect);});
export default router;
