export function createPlaybackQueue(tracks, startTrackId) {
  const items = Array.isArray(tracks) ? tracks.filter(Boolean) : [];
  const requestedIndex = items.findIndex((track) => track.id === startTrackId);
  return { tracks: items, index: items.length ? Math.max(0, requestedIndex) : -1 };
}

export function movePlaybackQueue(queue, direction) {
  const step = direction < 0 ? -1 : 1;
  const index = queue.index + step;
  if (index < 0 || index >= queue.tracks.length) return null;
  return { tracks: queue.tracks, index, track: queue.tracks[index] };
}
