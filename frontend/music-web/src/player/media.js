import { mediaObjectUrl } from '@music-dreamer/shared';

export function trackMediaSource(track) { return mediaObjectUrl(track.mediaObjectId); }
export function lyricsAvailable(track) { return Number.isInteger(track?.lyricsMediaObjectId) && track.lyricsMediaObjectId > 0; }
