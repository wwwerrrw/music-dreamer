import { API_PATHS } from '@music-dreamer/shared';

const id = (value, label) => {
  if (!Number.isInteger(value) || value < 1) throw new TypeError(`${label} must be a positive integer`);
  return value;
};

export function createAdminApi(api) {
  return {
    async users() { return (await api.get(API_PATHS.identity.users)).data; },
    async tracks(options) { return (await api.get(API_PATHS.catalog.adminTracks(options))).data; },
    async lockUser(userId) { return api.post(`${API_PATHS.identity.users}/${id(userId, 'userId')}/lock`); },
    async promoteSinger(userId) { return api.post(`${API_PATHS.identity.users}/${id(userId, 'userId')}/promote-singer`); },
    async publishTrack(trackId) { return (await api.post(`/api/catalog/admin/tracks/${id(trackId, 'trackId')}/publish`)).data; },
    async lockTrack(trackId) { return (await api.post(`/api/catalog/admin/tracks/${id(trackId, 'trackId')}/lock`)).data; },
    async publishMessage(request) { return (await api.post(API_PATHS.interaction.adminMessages, request)).data; }
  };
}
