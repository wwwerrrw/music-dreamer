import Vue from 'vue';
import VueRouter from 'vue-router';
import { loadSession } from '../auth/session.js';
import { defaultRouteFor, resolveRouteAccess } from './access.js';
import { routeRecords } from './routes.js';

Vue.use(VueRouter);

const router = new VueRouter({ mode: 'history', routes: routeRecords, scrollBehavior: () => ({ x: 0, y: 0 }) });

router.beforeEach((to, from, next) => {
  const session = loadSession(window.sessionStorage);
  if (to.meta.publicOnly && session) return next(defaultRouteFor(session.role));
  const decision = resolveRouteAccess({ path: to.fullPath, ...to.meta }, session);
  return decision.allow ? next() : next(decision.redirect);
});

export default router;
