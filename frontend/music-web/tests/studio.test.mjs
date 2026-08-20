import test from 'node:test';
import assert from 'node:assert/strict';

import { createArtistStudioApi } from '../src/artist/studio-api.js';
import { draftStatusLabel, preserveStudioForm } from '../src/artist/studio-model.js';

test('artist profile loads and saves through the current singer endpoints', async () => {
  const calls = [];
  const api = {
    get: async (path) => { calls.push(['GET', path]); return { data: { userId: 16, stageName: '林屿' } }; },
    put: async (path, body) => { calls.push(['PUT', path, body]); return { data: body }; }
  };
  const studio = createArtistStudioApi(api);
  await studio.profile(16);
  await studio.saveProfile({ stageName: '林屿', biography: '独立音乐人' });
  assert.deepEqual(calls, [
    ['GET', '/api/catalog/artists/16'],
    ['PUT', '/api/catalog/artists/me', { stageName: '林屿', biography: '独立音乐人' }]
  ]);
});

test('draft creation requires audio and carries optional uploaded media ids', async () => {
  const calls = [];
  let nextId = 70;
  const api = {
    post: async (path, body, options) => {
      calls.push({ path, body, options });
      if (path === '/api/media/objects') return { data: { id: ++nextId } };
      return { data: { id: 8, ...body, status: 'DRAFT' } };
    }
  };
  const studio = createArtistStudioApi(api);
  await assert.rejects(studio.createDraft({ title: '凌晨两点', durationMs: 208000 }), /audio file is required/);

  const result = await studio.createDraft({
    title: '凌晨两点', durationMs: 208000,
    audio: new Blob(['audio']), cover: new Blob(['cover']), lyrics: new Blob(['lyrics'])
  });
  assert.deepEqual(calls.map((call) => [call.path, call.options?.params?.mediaType]), [
    ['/api/media/objects', 'AUDIO'],
    ['/api/media/objects', 'IMAGE'],
    ['/api/media/objects', 'LYRICS'],
    ['/api/catalog/tracks/drafts', undefined]
  ]);
  assert.deepEqual(calls.at(-1).body, {
    title: '凌晨两点', durationMs: 208000, mediaObjectId: 71,
    coverMediaObjectId: 72, lyricsMediaObjectId: 73
  });
  assert.equal(result.id, 8);
});

test('draft update and submit use the current catalog contract', async () => {
  const calls = [];
  const api = { put: async (path, body) => { calls.push(['PUT', path, body]); return { data: body }; }, post: async (path) => { calls.push(['POST', path]); return { data: { id: 8, status: 'PENDING_REVIEW' } }; } };
  const studio = createArtistStudioApi(api);
  const payload = { title: '凌晨三点', mediaObjectId: 71, durationMs: 210000, coverMediaObjectId: 72, lyricsMediaObjectId: null };
  await studio.updateDraft(8, payload);
  await studio.submit(8);
  assert.deepEqual(calls, [
    ['PUT', '/api/catalog/tracks/8', payload],
    ['POST', '/api/catalog/tracks/8/submit']
  ]);
});

test('draft update uploads newly selected media before persisting replacement ids', async () => {
  const calls = [];
  let nextId = 80;
  const api = {
    post: async (path, body, options) => {
      calls.push({ method: 'POST', path, body, options });
      return { data: { id: ++nextId } };
    },
    put: async (path, body) => {
      calls.push({ method: 'PUT', path, body });
      return { data: { id: 8, ...body, status: 'DRAFT' } };
    }
  };
  const studio = createArtistStudioApi(api);
  const result = await studio.updateDraft(8, {
    title: '替换后的版本', durationMs: 215000,
    mediaObjectId: 71, coverMediaObjectId: 72, lyricsMediaObjectId: 73,
    audio: new Blob(['new-audio']), cover: new Blob(['new-cover']), lyrics: new Blob(['new-lyrics'])
  });

  assert.deepEqual(calls.slice(0, 3).map((call) => [call.path, call.options?.params?.mediaType]), [
    ['/api/media/objects', 'AUDIO'],
    ['/api/media/objects', 'IMAGE'],
    ['/api/media/objects', 'LYRICS']
  ]);
  assert.deepEqual(calls.at(-1), {
    method: 'PUT', path: '/api/catalog/tracks/8',
    body: { title: '替换后的版本', durationMs: 215000, mediaObjectId: 81, coverMediaObjectId: 82, lyricsMediaObjectId: 83 }
  });
  assert.equal(result.mediaObjectId, 81);
});

test('upload errors preserve entered studio values and status labels are accurate', () => {
  const form = { title: '未完成的歌', durationMs: 180000, audio: { name: 'demo.mp3' } };
  assert.deepEqual(preserveStudioForm(form, { status: 503 }), { ...form, errorCode: 'RETRY' });
  assert.equal(draftStatusLabel('DRAFT'), '草稿');
  assert.equal(draftStatusLabel('PENDING_REVIEW'), '已提交');
  assert.equal(draftStatusLabel('PUBLISHED'), '已发布');
  assert.equal(draftStatusLabel('LOCKED'), '已锁定');
});
