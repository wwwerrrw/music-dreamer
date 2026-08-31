import test from 'node:test';
import assert from 'node:assert/strict';
import { selectHeroTracks } from '../src/catalog/discovery.js';

const track = (id) => ({ id, title: id });

test('selectHeroTracks prioritizes latest tracks and fills to five with unique popular tracks', () => {
  const discovery = {
    latest: { tracks: [track('latest-1'), track('shared'), track('latest-3')] },
    popular: { tracks: [track('shared'), track('popular-2'), track('popular-3'), track('popular-4')] }
  };

  assert.deepEqual(
    selectHeroTracks(discovery).map(({ id }) => id),
    ['latest-1', 'shared', 'latest-3', 'popular-2', 'popular-3']
  );
});

test('selectHeroTracks returns the available tracks when fewer than five exist', () => {
  const discovery = {
    latest: { tracks: [track('latest-1')] },
    popular: { tracks: [track('latest-1'), track('popular-2')] }
  };

  assert.deepEqual(selectHeroTracks(discovery).map(({ id }) => id), ['latest-1', 'popular-2']);
});
