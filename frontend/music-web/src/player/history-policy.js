const IMMEDIATE_EVENTS = new Set(['pause', 'seeked', 'track-change', 'pagehide']);

export function shouldPersistHistory({ event, now, lastWriteAt }) {
  if (IMMEDIATE_EVENTS.has(event)) return true;
  return event === 'timeupdate' && now - lastWriteAt >= 15000;
}
