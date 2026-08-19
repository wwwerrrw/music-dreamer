import { API_PATHS } from '@music-dreamer/shared';

const id = (value, label) => {
  if (!Number.isInteger(value) || value < 1) throw new TypeError(`${label} must be a positive integer`);
  return value;
};

export function createLibraryApi(api) {
  const favorites = new Set();
  return {
    async addFavorite(trackId) {
      const value = id(trackId, 'trackId');
      if (favorites.has(value)) return;
      await api.put(`${API_PATHS.interaction.favorites}/${value}`);
      favorites.add(value);
    },
    async removeFavorite(trackId) {
      const value = id(trackId, 'trackId');
      if (!favorites.has(value)) return;
      await api.delete(`${API_PATHS.interaction.favorites}/${value}`);
      favorites.delete(value);
    },
    async favorites() {
      const result = await api.get(API_PATHS.interaction.favorites);
      result.data.forEach((item) => favorites.add(item.trackId));
      return result.data;
    },
    async createPlaylist(request) { return (await api.post(API_PATHS.interaction.playlists, request)).data; },
    async playlists() { return (await api.get(API_PATHS.interaction.playlists)).data; },
    async playlist(playlistId) { return (await api.get(`${API_PATHS.interaction.playlists}/${id(playlistId, 'playlistId')}`)).data; },
    async addTrack(playlistId, request) { return api.post(`${API_PATHS.interaction.playlists}/${id(playlistId, 'playlistId')}/tracks`, request); },
    async removeTrack(playlistId, trackId) { return api.delete(`${API_PATHS.interaction.playlists}/${id(playlistId, 'playlistId')}/tracks/${id(trackId, 'trackId')}`); },
    async history() { return (await api.get(API_PATHS.interaction.history)).data; },
    async messages() { return (await api.get(API_PATHS.interaction.messages)).data; },
    async markRead(messageId) { return api.post(`${API_PATHS.interaction.messages}/${id(messageId, 'messageId')}/read`); }
  };
}
