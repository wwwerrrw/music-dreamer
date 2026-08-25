import { API_PATHS, mapTrackSummary } from '@music-dreamer/shared';

const UNAVAILABLE_TRACK_STATUSES = new Set([404, 409]);

export async function loadHistoryTracks({ api, library }) {
  const history = await library.history();
  const tracks = await Promise.all(history.map(async (item) => {
    try {
      const response = await api.get(API_PATHS.catalog.track(item.trackId));
      return mapTrackSummary(response.data);
    } catch (problem) {
      if (UNAVAILABLE_TRACK_STATUSES.has(problem?.status)) return null;
      throw problem;
    }
  }));
  return tracks.filter(Boolean);
}
