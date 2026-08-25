import test from 'node:test';
import assert from 'node:assert/strict';

import { loadHistoryTracks } from '../src/library/history-loader.js';

test('history loading skips tracks that are no longer available', async () => {
  const calls = [];
  const api = {
    get: async (path) => {
      calls.push(path);
      if (path.endsWith('/12')) throw { status: 409 };
      return {
        data: {
          id: 11,
          title: 'Still Available',
          artistUserId: 4,
          mediaObjectId: 21,
          durationMs: 180000,
          playCount: 3,
          status: 'PUBLISHED'
        }
      };
    }
  };
  const library = { history: async () => [{ trackId: 12 }, { trackId: 11 }] };

  const tracks = await loadHistoryTracks({ api, library });

  assert.deepEqual(calls, ['/api/catalog/tracks/12', '/api/catalog/tracks/11']);
  assert.deepEqual(tracks.map((track) => track.id), [11]);
});

test('history loading keeps infrastructure failures visible', async () => {
  const problem = { status: 503 };
  const api = { get: async () => { throw problem; } };
  const library = { history: async () => [{ trackId: 11 }] };

  await assert.rejects(
    () => loadHistoryTracks({ api, library }),
    (failure) => failure === problem
  );
});
