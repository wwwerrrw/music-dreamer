import { API_PATHS } from '@music-dreamer/shared';

export async function loadProfile({ api, library }) {
  const [account, favorites, playlists, history] = await Promise.all([
    api.get(API_PATHS.identity.me),
    library.favorites(),
    library.playlists(),
    library.history()
  ]);
  return {
    account: account.data,
    counts: {
      favorites: favorites.length,
      playlists: playlists.length,
      history: history.length
    }
  };
}
