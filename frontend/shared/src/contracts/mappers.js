import { TRACK_STATUS } from './constants.js';

export function mapLoginSession(value) {
  return {
    token: value.token,
    expiresAt: value.expiresAt,
    userId: value.userId,
    role: value.role,
    status: value.status
  };
}

export function mapTrackSummary(value) {
  return {
    id: value.id,
    title: value.title,
    artistUserId: value.artistUserId,
    artistStageName: value.artistStageName || `歌手 #${value.artistUserId}`,
    mediaObjectId: value.mediaObjectId,
    coverMediaObjectId: value.coverMediaObjectId ?? null,
    lyricsMediaObjectId: value.lyricsMediaObjectId ?? null,
    durationMs: value.durationMs,
    playCount: value.playCount,
    createdAt: value.createdAt,
    status: fromCatalogTrackStatus(value.status)
  };
}

export function toCatalogTrackStatus(value) {
  return value === TRACK_STATUS.SUBMITTED ? TRACK_STATUS.PENDING_REVIEW : value;
}

export function fromCatalogTrackStatus(value) {
  return value === TRACK_STATUS.PENDING_REVIEW ? TRACK_STATUS.SUBMITTED : value;
}
