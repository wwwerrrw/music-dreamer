import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import { loadDiscovery } from '../src/catalog/discovery.js';
import { parseLrc, lyricAt } from '../src/player/lrc.js';
import { createPlayerController } from '../src/player/controller.js';
import { shouldPersistHistory } from '../src/player/history-policy.js';
import * as media from '../src/player/media.js';
import { adjacentTrack, normalizePlaybackSelection } from '../src/player/queue.js';
import * as playbackQueue from '../src/player/queue.js';
import { mergeRecordedTrack } from '../src/player/play-count.js';

const { lyricsAvailable, trackMediaSource } = media;

test('discovery loads latest and popular ordering without personalization claims', async () => {
  const calls = [];
  const api = { get: async (path) => { calls.push(path); return { data: [{ id: calls.length }] }; } };
  const result = await loadDiscovery(api);

  assert.equal(calls[0], '/api/catalog/tracks/search?keyword=&sort=LATEST');
  assert.equal(calls[1], '/api/catalog/tracks/search?keyword=&sort=POPULAR');
  assert.equal(result.latest.title, '今日新声');
  assert.equal(result.popular.subtitle, '按播放量排序');
  assert.doesNotMatch(JSON.stringify(result), /个性化/);
});

test('media source is same-origin and lyrics entry follows the media association', () => {
  assert.equal(trackMediaSource({ mediaObjectId: 71 }), '/api/media/objects/71');
  assert.doesNotMatch(trackMediaSource({ mediaObjectId: 71 }), /token|bearer/i);
  assert.equal(lyricsAvailable({ lyricsMediaObjectId: null }), false);
  assert.equal(lyricsAvailable({ lyricsMediaObjectId: 73 }), true);
});

test('mini player cover resolves the current track detail route', () => {
  assert.equal(typeof media.trackDetailLocation, 'function');
  assert.deepEqual(media.trackDetailLocation({ id: 42 }), {
    name: 'track',
    params: { trackId: '42' }
  });
});

test('playing another queued track updates an open track detail route only', () => {
  assert.equal(typeof media.syncTrackDetailRoute, 'function');
  const replacements = [];
  const replace = (location) => replacements.push(location);

  assert.equal(media.syncTrackDetailRoute({
    route: { name: 'track', params: { trackId: '41' } },
    track: { id: 42 },
    replace
  }), true);
  assert.deepEqual(replacements, [{ name: 'track', params: { trackId: '42' } }]);

  assert.equal(media.syncTrackDetailRoute({
    route: { name: 'track', params: { trackId: '42' } },
    track: { id: 42 },
    replace
  }), false);
  assert.equal(media.syncTrackDetailRoute({
    route: { name: 'search', params: {} },
    track: { id: 43 },
    replace
  }), false);
  assert.equal(replacements.length, 1);
});

test('track route view key changes between songs without remounting unrelated views', () => {
  assert.equal(typeof media.trackRouteViewKey, 'function');
  assert.equal(media.trackRouteViewKey({ name: 'track', params: { trackId: '41' } }), 'track:41');
  assert.equal(media.trackRouteViewKey({ name: 'track', params: { trackId: '42' } }), 'track:42');
  assert.equal(media.trackRouteViewKey({ name: 'search', fullPath: '/search?keyword=a' }), 'search');
  assert.equal(media.trackRouteViewKey({ name: 'search', fullPath: '/search?keyword=b' }), 'search');
});

test('player queue keeps the selected list and wraps previous and next tracks', () => {
  const first = { id: 1, title: '第一首' };
  const second = { id: 2, title: '第二首' };
  const third = { id: 3, title: '第三首' };
  const selection = normalizePlaybackSelection({
    track: second,
    queue: [first, second, second, third]
  });

  assert.deepEqual(selection.queue, [first, second, third]);
  assert.equal(selection.index, 1);
  assert.equal(adjacentTrack(selection.queue, selection.index, -1), first);
  assert.equal(adjacentTrack(selection.queue, 0, -1), third);
  assert.equal(adjacentTrack(selection.queue, 2, 1), first);
});

test('playback mode cycles through order, shuffle, and repeat one', () => {
  assert.equal(playbackQueue.cyclePlaybackMode('ORDER'), 'SHUFFLE');
  assert.equal(playbackQueue.cyclePlaybackMode('SHUFFLE'), 'REPEAT_ONE');
  assert.equal(playbackQueue.cyclePlaybackMode('REPEAT_ONE'), 'ORDER');
});

test('playback end follows order, avoids the current track in shuffle, and repeats one', () => {
  const queue = [{ id: 1 }, { id: 2 }, { id: 3 }];

  assert.strictEqual(playbackQueue.trackForPlaybackEnd(queue, 1, 'ORDER'), queue[2]);
  assert.strictEqual(playbackQueue.trackForPlaybackEnd(queue, 1, 'SHUFFLE', () => 0), queue[0]);
  assert.strictEqual(playbackQueue.trackForPlaybackEnd(queue, 1, 'SHUFFLE', () => 0.99), queue[2]);
  assert.strictEqual(playbackQueue.trackForPlaybackEnd(queue, 1, 'REPEAT_ONE'), queue[1]);
});

test('mini player previous and next events are wired into the player store', () => {
  const shell = readFileSync(new URL('../src/components/AppShell.vue', import.meta.url), 'utf8');
  assert.match(shell, /@previous="previous"/);
  assert.match(shell, /@next="next"/);
  assert.match(shell, /dispatch\('player\/previous'\)/);
  assert.match(shell, /dispatch\('player\/next'\)/);
});

test('recorded playback response updates the visible list track reference', () => {
  const visibleTrack = { id: 8, title: '晚风', playCount: 0 };
  const currentTrack = { id: 8, title: '晚风', playCount: 0 };
  const merged = mergeRecordedTrack(
    [visibleTrack],
    currentTrack,
    { id: 8, title: '晚风', playCount: 1 }
  );

  assert.equal(visibleTrack.playCount, 1);
  assert.strictEqual(merged, visibleTrack);
});

test('lrc parser orders timestamps and selects the active line', () => {
  const lines = parseLrc('[00:10.50]第二句\n[00:02.00]第一句\n[ar:歌手]\n[00:10.50]并列');
  assert.deepEqual(lines, [
    { timeMs: 2000, text: '第一句' },
    { timeMs: 10500, text: '第二句' },
    { timeMs: 10500, text: '并列' }
  ]);
  assert.equal(lyricAt(lines, 10499)?.text, '第一句');
  assert.equal(lyricAt(lines, 10500)?.text, '并列');
});

test('player controller reuses one audio element across track changes', async () => {
  const events = [];
  const audio = {
    src: '', currentTime: 0, duration: 240, volume: 1, paused: true,
    play: async () => { audio.paused = false; events.push('play'); },
    pause: () => { audio.paused = true; events.push('pause'); }
  };
  const player = createPlayerController({ audio });

  player.load({ id: 1, mediaObjectId: 71, title: '第一首' });
  await player.toggle();
  player.seek(30);
  player.setVolume(0.4);
  player.load({ id: 2, mediaObjectId: 72, title: '第二首' });

  assert.equal(player.audio, audio);
  assert.equal(audio.src, '/api/media/objects/72');
  assert.equal(audio.currentTime, 0);
  assert.equal(audio.volume, 0.4);
  assert.deepEqual(events, ['play', 'pause']);
});

test('player controller restarts an ended track from the beginning', async () => {
  const events = [];
  const audio = {
    src: '/api/media/objects/71', currentTime: 208, duration: 208, volume: 1, paused: true,
    play: async () => { audio.paused = false; events.push('play'); },
    pause: () => { audio.paused = true; events.push('pause'); }
  };
  const player = createPlayerController({ audio });

  await player.restart();

  assert.equal(audio.currentTime, 0);
  assert.equal(audio.paused, false);
  assert.deepEqual(events, ['play']);
});

test('player store cycles modes and restarts the current track on repeat-one end', async () => {
  const previousWindow = globalThis.window;
  const previousNodeEnv = process.env.NODE_ENV;
  const storage = { getItem: () => null, setItem: () => {}, removeItem: () => {} };
  globalThis.window = { sessionStorage: storage, navigator: { userAgent: 'node-test' } };
  process.env.NODE_ENV = 'production';
  try {
    const { default: store } = await import('../src/store/index.js?playback-mode-test');
    const events = [];
    const audio = {
      src: '', currentTime: 208, duration: 208, volume: 1, paused: true,
      play: async () => { audio.paused = false; events.push('play'); },
      pause: () => { audio.paused = true; events.push('pause'); }
    };
    const track = { id: 8, title: '凌晨两点', mediaObjectId: 71 };
    store.commit('player/initialize', audio);
    store.commit('player/setSelection', { track, queue: [track], index: 0 });

    store.commit('player/cycleMode');
    store.commit('player/cycleMode');
    await store.dispatch('player/ended');

    assert.equal(store.state.player.playbackMode, 'REPEAT_ONE');
    assert.equal(audio.currentTime, 0);
    assert.deepEqual(events, ['play']);
  } finally {
    globalThis.window = previousWindow;
    process.env.NODE_ENV = previousNodeEnv;
  }
});

test('history writes on lifecycle events and throttles passive progress to 15 seconds', () => {
  for (const event of ['pause', 'seeked', 'track-change', 'pagehide']) {
    assert.equal(shouldPersistHistory({ event, now: 20000, lastWriteAt: 19000 }), true);
  }
  assert.equal(shouldPersistHistory({ event: 'timeupdate', now: 14999, lastWriteAt: 0 }), false);
  assert.equal(shouldPersistHistory({ event: 'timeupdate', now: 15000, lastWriteAt: 0 }), true);
});
