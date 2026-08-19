import {createApiClient} from '@music-dreamer/shared'; import {loadAdminSession} from '../auth/session.js';
export const api=createApiClient({tokenProvider:()=>loadAdminSession(window.sessionStorage)?.token});
