import { API_PATHS, mapTrackSummary } from '@music-dreamer/shared';

export async function loadSearchResults(api, { keyword = '', sort = 'LATEST' } = {}) {
  const normalizedKeyword = String(keyword).trim();
  const tracksRequest = api.get(API_PATHS.catalog.search({ keyword: normalizedKeyword, sort }));
  const artistsRequest = normalizedKeyword
    ? api.get(API_PATHS.catalog.artists({ keyword: normalizedKeyword }))
    : Promise.resolve({ data: [] });
  const [tracks, artists] = await Promise.all([tracksRequest, artistsRequest]);
  return {
    tracks: tracks.data.map(mapTrackSummary),
    artists: artists.data
  };
}
