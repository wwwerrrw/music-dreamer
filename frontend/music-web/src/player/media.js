import { mediaObjectUrl } from '@music-dreamer/shared';

export function trackMediaSource(track) { return mediaObjectUrl(track.mediaObjectId); }
export function lyricsAvailable(track) { return Number.isInteger(track?.lyricsMediaObjectId) && track.lyricsMediaObjectId > 0; }
export function trackDetailLocation(track) {
  return { name: 'track', params: { trackId: String(track.id) } };
}

export function syncTrackDetailRoute({ route, track, replace }) {
  if (route?.name !== 'track' || !Number.isInteger(track?.id)) return false;
  if (String(route.params?.trackId) === String(track.id)) return false;
  replace(trackDetailLocation(track));
  return true;
}

export function trackRouteViewKey(route) {
  if (route?.name === 'track') return `track:${String(route.params?.trackId ?? '')}`;
  return route?.name || route?.path || 'route';
}
