export function mergeRecordedTrack(queue, currentTrack, recordedTrack) {
  if (!recordedTrack || !Number.isInteger(recordedTrack.id)) return currentTrack;
  const queued = Array.isArray(queue) ? queue.find((track) => track.id === recordedTrack.id) : null;
  if (queued) {
    Object.assign(queued, recordedTrack);
    return currentTrack?.id === recordedTrack.id ? queued : currentTrack;
  }
  if (currentTrack?.id === recordedTrack.id) Object.assign(currentTrack, recordedTrack);
  return currentTrack;
}
