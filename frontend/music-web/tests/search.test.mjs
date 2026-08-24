import test from 'node:test';
import assert from 'node:assert/strict';

import { loadSearchResults } from '../src/catalog/search.js';

test('search loads tracks and artist home-page matches for a non-empty keyword', async () => {
  const calls = [];
  const api = {
    get: async (path) => {
      calls.push(path);
      if (path.startsWith('/api/catalog/artists/search')) {
        return { data: [{ userId: 16, stageName: '林屿', biography: '独立音乐人' }] };
      }
      return { data: [{ id: 8, title: '晚风', artistUserId: 16, mediaObjectId: 71, durationMs: 180000, playCount: 3, status: 'PUBLISHED' }] };
    }
  };

  const result = await loadSearchResults(api, { keyword: '林屿', sort: 'LATEST' });

  assert.deepEqual(calls, [
    '/api/catalog/tracks/search?keyword=%E6%9E%97%E5%B1%BF&sort=LATEST',
    '/api/catalog/artists/search?keyword=%E6%9E%97%E5%B1%BF'
  ]);
  assert.equal(result.tracks[0].artistStageName, '歌手 #16');
  assert.deepEqual(result.artists, [{ userId: 16, stageName: '林屿', biography: '独立音乐人' }]);
});

test('blank search avoids an unnecessary artist request', async () => {
  const calls = [];
  const api = { get: async (path) => { calls.push(path); return { data: [] }; } };

  const result = await loadSearchResults(api, { keyword: '  ', sort: 'POPULAR' });

  assert.deepEqual(calls, ['/api/catalog/tracks/search?keyword=&sort=POPULAR']);
  assert.deepEqual(result.artists, []);
});
