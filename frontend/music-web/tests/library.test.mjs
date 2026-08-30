import test from 'node:test';
import assert from 'node:assert/strict';

import { createLibraryApi } from '../src/library/api.js';
import { loadTracksForEntries } from '../src/library/track-batch.js';
import { markMessageRead, messageState, playlistState } from '../src/library/models.js';
import { resolveTrackByName } from '../src/library/track-lookup.js';

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
  await library.deletePlaylist(3);

  assert.deepEqual(calls, [
    ['POST', '/api/interaction/playlists', { name: '清晨', description: '', visibility: 'PRIVATE' }],
    ['GET', '/api/interaction/playlists'],
    ['GET', '/api/interaction/playlists/3'],
    ['POST', '/api/interaction/playlists/3/tracks', { trackId: 8, sortOrder: 0 }],
    ['DELETE', '/api/interaction/playlists/3/tracks/8'],
    ['DELETE', '/api/interaction/playlists/3']
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

test('playlist track lookup requires one exact song-name match', () => {
  const tracks = [
    { id: 1, title: '晚风', artistStageName: '林屿' },
    { id: 2, title: '晚风现场版', artistStageName: '白昼' }
  ];

  assert.equal(resolveTrackByName(tracks, '  晚风  ').id, 1);
  assert.throws(() => resolveTrackByName(tracks, '不存在'), /未找到/);
  assert.throws(() => resolveTrackByName([...tracks, { id: 3, title: '晚风' }], '晚风'), /同名/);
});

test('library track lookup batches catalog requests and keeps entry order', async () => {
  const calls = [];
  const api = {
    get: async (path) => {
      calls.push(path);
      return {
        data: [
          { id: 3, title: 'Third', artistUserId: 2, mediaObjectId: 4, durationMs: 1000, playCount: 0, status: 'PUBLISHED' },
          { id: 8, title: 'Eighth', artistUserId: 2, mediaObjectId: 5, durationMs: 1000, playCount: 0, status: 'PUBLISHED' }
        ]
      };
    }
  };

  const tracks = await loadTracksForEntries(api, [{ trackId: 8 }, { trackId: 3 }]);

  assert.deepEqual(calls, ['/api/catalog/tracks/batch?ids=8%2C3']);
  assert.deepEqual(tracks.map((track) => track.id), [8, 3]);
  assert.equal(tracks[0].artistStageName, '歌手 #2');
});

test('library track lookup skips entries the catalog no longer serves', async () => {
  const api = {
    get: async () => ({
      data: [
        { id: 3, title: 'Third', artistUserId: 2, mediaObjectId: 4, durationMs: 1000, playCount: 0, status: 'PUBLISHED' }
      ]
    })
  };

  const tracks = await loadTracksForEntries(api, [{ trackId: 9 }, { trackId: 3 }]);

  assert.deepEqual(tracks.map((track) => track.id), [3]);
});

test('library track lookup sends no request without entries', async () => {
  const calls = [];
  const api = { get: async (path) => { calls.push(path); return { data: [] }; } };

  const tracks = await loadTracksForEntries(api, []);

  assert.deepEqual(tracks, []);
  assert.deepEqual(calls, []);
});

test('library track lookup splits requests at the catalog batch limit', async () => {
  const calls = [];
  const entries = Array.from({ length: 101 }, (_, index) => ({ trackId: index + 1 }));
  const api = {
    get: async (path) => {
      const ids = new URL(path, 'http://localhost').searchParams.get('ids').split(',').map(Number);
      if (ids.length > 100) throw new Error('catalog batch limit exceeded');
      calls.push(ids);
      return {
        data: [...ids].reverse().map((id) => ({
          id,
          title: `Track ${id}`,
          artistUserId: 2,
          mediaObjectId: id + 1000,
          durationMs: 1000,
          playCount: 0,
          status: 'PUBLISHED'
        }))
      };
    }
  };

  const tracks = await loadTracksForEntries(api, entries);

  assert.deepEqual(calls.map((ids) => ids.length), [100, 1]);
  assert.deepEqual(tracks.map((track) => track.id), entries.map((entry) => entry.trackId));
});
