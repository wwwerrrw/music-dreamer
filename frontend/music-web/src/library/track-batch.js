import { API_PATHS, mapTrackSummary } from '@music-dreamer/shared';

const MAX_BATCH_IDS = 100;

export async function loadTracksForEntries(api, entries) {
  const trackIds = entries.map((entry) => entry.trackId);
  if (!trackIds.length) return [];
  const requests = [];
  for (let start = 0; start < trackIds.length; start += MAX_BATCH_IDS) {
    requests.push(api.get(API_PATHS.catalog.tracksBatch(trackIds.slice(start, start + MAX_BATCH_IDS))));
  }
  const results = await Promise.all(requests);
  const byId = new Map(results.flatMap((result) => result.data)
    .map((item) => [item.id, mapTrackSummary(item)]));
  return trackIds.flatMap((trackId) => byId.get(trackId) ?? []);
}
