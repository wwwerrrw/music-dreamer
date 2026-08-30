import { loadTracksForEntries } from './track-batch.js';

export async function loadHistoryTracks({ api, library }) {
  const history = await library.history();
  return loadTracksForEntries(api, history);
}
