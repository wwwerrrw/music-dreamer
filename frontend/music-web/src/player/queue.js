function uniqueTracks(tracks) {
  const seen = new Set();
  return tracks.filter((track) => {
    if (!track || !Number.isInteger(track.id) || seen.has(track.id)) return false;
    seen.add(track.id);
    return true;
  });
}

export const PLAYBACK_MODES = Object.freeze({
  ORDER: 'ORDER',
  SHUFFLE: 'SHUFFLE',
  REPEAT_ONE: 'REPEAT_ONE'
});

export function cyclePlaybackMode(mode) {
  if (mode === PLAYBACK_MODES.ORDER) return PLAYBACK_MODES.SHUFFLE;
  if (mode === PLAYBACK_MODES.SHUFFLE) return PLAYBACK_MODES.REPEAT_ONE;
  return PLAYBACK_MODES.ORDER;
}

export function normalizePlaybackSelection(payload, currentQueue = []) {
  const wrapped = payload && payload.track;
  const track = wrapped ? payload.track : payload;
  if (!track || !Number.isInteger(track.id)) throw new TypeError('A valid track is required');

  const requestedQueue = wrapped && Array.isArray(payload.queue) ? payload.queue : currentQueue;
  const queue = uniqueTracks(Array.isArray(requestedQueue) ? requestedQueue : []);
  let index = queue.findIndex((item) => item.id === track.id);
  if (index === -1) {
    queue.push(track);
    index = queue.length - 1;
  }
  return { track: queue[index], queue, index };
}

export function adjacentTrack(queue, currentIndex, offset) {
  if (!Array.isArray(queue) || !queue.length) return null;
  const step = offset < 0 ? -1 : 1;
  const index = Number.isInteger(currentIndex) ? currentIndex : 0;
  return queue[(index + step + queue.length) % queue.length];
}

export function trackForPlaybackEnd(queue, currentIndex, mode, random = Math.random) {
  if (!Array.isArray(queue) || !queue.length) return null;
  const index = Number.isInteger(currentIndex) && currentIndex >= 0 && currentIndex < queue.length
    ? currentIndex
    : 0;
  if (mode === PLAYBACK_MODES.REPEAT_ONE) return queue[index];
  if (mode !== PLAYBACK_MODES.SHUFFLE || queue.length === 1) {
    return adjacentTrack(queue, index, 1);
  }
  const candidates = queue.filter((_track, candidateIndex) => candidateIndex !== index);
  const candidateIndex = Math.min(
    candidates.length - 1,
    Math.floor(Math.max(0, Number(random())) * candidates.length)
  );
  return candidates[candidateIndex];
}
