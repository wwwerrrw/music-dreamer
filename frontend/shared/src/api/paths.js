function query(params) {
  const value = new URLSearchParams();
  Object.entries(params).forEach(([key, entry]) => {
    if (entry !== undefined && entry !== null) value.set(key, String(entry));
  });
  const encoded = value.toString();
  return encoded ? `?${encoded}` : '';
}

export const API_PATHS = Object.freeze({
  identity: {
    login: '/api/identity/sessions',
    register: '/api/identity/registrations',
    activate: '/api/identity/activations',
    developmentInbox: '/api/identity/development-inbox',
    logout: '/api/identity/sessions/current',
    users: '/api/identity/admin/users'
  },
  catalog: {
    search: ({ keyword = '', sort = 'LATEST' } = {}) => `/api/catalog/tracks/search${query({ keyword, sort })}`,
    track: (trackId) => `/api/catalog/tracks/${positiveId(trackId, 'trackId')}`,
    playable: (trackId) => `/api/catalog/tracks/${positiveId(trackId, 'trackId')}/playable`,
    adminTracks: ({ status, page = 0, size = 20 } = {}) => `/api/catalog/admin/tracks${query({ status, page, size })}`,
    artist: (userId) => `/api/catalog/artists/${positiveId(userId, 'userId')}`,
    artistMe: '/api/catalog/artists/me',
    drafts: '/api/catalog/tracks/drafts',
    submit: (trackId) => `/api/catalog/tracks/${positiveId(trackId, 'trackId')}/submit`,
    updateDraft: (trackId) => `/api/catalog/tracks/${positiveId(trackId, 'trackId')}`
  },
  interaction: {
    favorites: '/api/interaction/favorites',
    playlists: '/api/interaction/playlists',
    history: '/api/interaction/history',
    messages: '/api/interaction/messages',
    adminMessages: '/api/interaction/admin/messages'
  },
  media: { objects: '/api/media/objects' }
});

function positiveId(value, label) {
  if (!Number.isInteger(value) || value < 1) throw new TypeError(`${label} must be a positive integer`);
  return value;
}

export function mediaObjectUrl(id) {
  return `${API_PATHS.media.objects}/${positiveId(id, 'mediaObjectId')}`;
}
