import test from 'node:test';
import assert from 'node:assert/strict';

import { createLibraryApi } from '../src/library/api.js';
import { markMessageRead, messageState, playlistState } from '../src/library/models.js';

test('favorite action is UI-idempotent and uses the interaction favorite endpoint', async () => {
  const calls = [];
  const api = { put: async (path) => calls.push(['PUT', path]), delete: async (path) => calls.push(['DELETE', path]) };
  const library = createLibraryApi(api);

  await library.addFavorite(8);
  await library.addFavorite(8);
  await library.removeFavorite(8);

  assert.deepEqual(calls, [
    ['PUT', '/api/interaction/favorites/8'],
    ['DELETE', '/api/interaction/favorites/8']
  ]);
});

test('playlist adapter maps create, list, details, and track mutations', async () => {
  const calls = [];
  const api = {
    post: async (path, body) => { calls.push(['POST', path, body]); return { data: { id: 3, name: body.name } }; },
    get: async (path) => { calls.push(['GET', path]); return { data: [] }; },
    delete: async (path) => calls.push(['DELETE', path])
  };
  const library = createLibraryApi(api);
  await library.createPlaylist({ name: '清晨', description: '', visibility: 'PRIVATE' });
  await library.playlists();
  await library.playlist(3);
  await library.addTrack(3, { trackId: 8, sortOrder: 0 });
  await library.removeTrack(3, 8);

  assert.deepEqual(calls, [
    ['POST', '/api/interaction/playlists', { name: '清晨', description: '', visibility: 'PRIVATE' }],
    ['GET', '/api/interaction/playlists'],
    ['GET', '/api/interaction/playlists/3'],
    ['POST', '/api/interaction/playlists/3/tracks', { trackId: 8, sortOrder: 0 }],
    ['DELETE', '/api/interaction/playlists/3/tracks/8']
  ]);
});

test('library models keep forbidden pages visible and separate loading empty error states', () => {
  assert.deepEqual(playlistState('loading'), { kind: 'loading', visible: true });
  assert.deepEqual(playlistState('empty'), { kind: 'empty', visible: true });
  assert.deepEqual(playlistState({ status: 403 }), { kind: 'error', visible: true, code: 'FORBIDDEN' });
  assert.deepEqual(messageState({ status: 503 }), { kind: 'error', visible: true, code: 'RETRY' });
});

test('marking a message read updates only its read state', () => {
  const messages = [
    { id: 1, subject: 'A', readAt: null },
    { id: 2, subject: 'B', readAt: null }
  ];
  const next = markMessageRead(messages, 2, '2026-08-19T08:00:00Z');
  assert.deepEqual(next, [
    { id: 1, subject: 'A', readAt: null },
    { id: 2, subject: 'B', readAt: '2026-08-19T08:00:00Z' }
  ]);
});
