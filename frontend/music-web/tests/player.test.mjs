import test from 'node:test';
import assert from 'node:assert/strict';

import { loadDiscovery } from '../src/catalog/discovery.js';
import { parseLrc, lyricAt } from '../src/player/lrc.js';
import { createPlayerController } from '../src/player/controller.js';
import { shouldPersistHistory } from '../src/player/history-policy.js';
import { lyricsAvailable, trackMediaSource } from '../src/player/media.js';

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

test('history writes on lifecycle events and throttles passive progress to 15 seconds', () => {
  for (const event of ['pause', 'seeked', 'track-change', 'pagehide']) {
    assert.equal(shouldPersistHistory({ event, now: 20000, lastWriteAt: 19000 }), true);
  }
  assert.equal(shouldPersistHistory({ event: 'timeupdate', now: 14999, lastWriteAt: 0 }), false);
  assert.equal(shouldPersistHistory({ event: 'timeupdate', now: 15000, lastWriteAt: 0 }), true);
});
