import { createApiClient } from '@music-dreamer/shared';
import { loadSession } from './auth/session.js';

export const api = createApiClient({
  tokenProvider: () => loadSession(window.sessionStorage)?.token
});
