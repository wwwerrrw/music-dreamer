import { API_PATHS, mapTrackSummary } from '@music-dreamer/shared';

export function selectHeroTracks(discovery, limit = 5) {
  if (!discovery || limit <= 0) return [];
  const latest = discovery.latest?.tracks || [];
  const popular = discovery.popular?.tracks || [];
  const tracks = [];
  const seen = new Set();

  for (const track of [...latest, ...popular]) {
    if (!track || seen.has(track.id)) continue;
    seen.add(track.id);
    tracks.push(track);
    if (tracks.length === limit) break;
  }

  return tracks;
}

export async function loadDiscovery(api) {
  const [latest, popular] = await Promise.all([
    api.get(API_PATHS.catalog.search({ keyword: '', sort: 'LATEST' })),
    api.get(API_PATHS.catalog.search({ keyword: '', sort: 'POPULAR' }))
  ]);
  return {
    latest: {
      title: '今日新声',
      subtitle: '按最新发布排序，发现今天可以播放的音乐。',
      tracks: latest.data.map(mapTrackSummary)
    },
    popular: {
      title: '热门歌曲',
      subtitle: '按播放量排序',
      tracks: popular.data.map(mapTrackSummary)
    }
  };
}
