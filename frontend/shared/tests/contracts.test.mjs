import test from 'node:test';
import assert from 'node:assert/strict';

import { API_PATHS, mediaObjectUrl } from '../src/api/paths.js';
import { mapLoginSession, mapTrackSummary, toCatalogTrackStatus } from '../src/contracts/mappers.js';

test('catalog search path encodes user input and deterministic sorting', () => {
  assert.equal(
    API_PATHS.catalog.search({ keyword: '林 屿/现场', sort: 'POPULAR' }),
    '/api/catalog/tracks/search?keyword=%E6%9E%97+%E5%B1%BF%2F%E7%8E%B0%E5%9C%BA&sort=POPULAR'
  );
});

test('admin track path maps the submitted frontend state and keeps paging explicit', () => {
  assert.equal(
    API_PATHS.catalog.adminTracks({ status: 'SUBMITTED', page: 2, size: 20 }),
    '/api/catalog/admin/tracks?status=SUBMITTED&page=2&size=20'
  );
  assert.equal(toCatalogTrackStatus('SUBMITTED'), 'PENDING_REVIEW');
});

test('media object URL remains same-origin and carries no bearer query', () => {
  assert.equal(mediaObjectUrl(42), '/api/media/objects/42');
  assert.throws(() => mediaObjectUrl(0), /positive integer/);
});

test('login mapping keeps only the server-issued session contract', () => {
  assert.deepEqual(mapLoginSession({
    token: 'opaque-token',
    expiresAt: '2026-08-19T08:00:00Z',
    userId: 15,
    role: 'USER',
    status: 'ACTIVE',
    password: 'must-not-survive'
  }), {
    token: 'opaque-token',
    expiresAt: '2026-08-19T08:00:00Z',
    userId: 15,
    role: 'USER',
    status: 'ACTIVE'
  });
});

test('track mapping presents pending review as submitted without losing media links', () => {
  assert.deepEqual(mapTrackSummary({
    id: 8,
    title: '凌晨两点',
    artistUserId: 16,
    artistStageName: '林屿',
    mediaObjectId: 71,
    coverMediaObjectId: 72,
    lyricsMediaObjectId: null,
    durationMs: 208000,
    playCount: 37,
    createdAt: '2026-08-19T08:00:00Z',
    status: 'PENDING_REVIEW'
  }), {
    id: 8,
    title: '凌晨两点',
    artistUserId: 16,
    artistStageName: '林屿',
    mediaObjectId: 71,
    coverMediaObjectId: 72,
    lyricsMediaObjectId: null,
    durationMs: 208000,
    playCount: 37,
    createdAt: '2026-08-19T08:00:00Z',
    status: 'SUBMITTED'
  });
});
