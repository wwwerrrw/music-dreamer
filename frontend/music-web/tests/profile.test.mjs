import test from 'node:test';
import assert from 'node:assert/strict';

import { loadProfile } from '../src/profile/load-profile.js';

test('profile overview loads current account and existing library counts once', async () => {
  const calls = [];
  const api = {
    get: async (path) => {
      calls.push(path);
      return { data: { id: 15, username: 'listener', email: 'listener@example.test', role: 'USER', status: 'ACTIVE' } };
    }
  };
  const library = {
    favorites: async () => [{ trackId: 1 }, { trackId: 2 }],
    playlists: async () => [{ id: 3 }],
    history: async () => [{ trackId: 2 }, { trackId: 1 }, { trackId: 4 }]
  };

  const result = await loadProfile({ api, library });

  assert.deepEqual(calls, ['/api/identity/users/me']);
  assert.deepEqual(result, {
    account: { id: 15, username: 'listener', email: 'listener@example.test', role: 'USER', status: 'ACTIVE' },
    counts: { favorites: 2, playlists: 1, history: 3 }
  });
});
