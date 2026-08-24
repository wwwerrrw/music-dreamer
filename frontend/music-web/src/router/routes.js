const views = {
  LoginView: () => import('../views/LoginView.vue'),
  RegisterView: () => import('../views/RegisterView.vue'),
  ActivateView: () => import('../views/ActivateView.vue'),
  DiscoverView: () => import('../views/DiscoverView.vue'),
  SearchView: () => import('../views/SearchView.vue'),
  TrackView: () => import('../views/TrackView.vue'),
  ArtistView: () => import('../views/ArtistView.vue'),
  ProfileView: () => import('../views/ProfileView.vue'),
  FavoritesView: () => import('../views/FavoritesView.vue'),
  PlaylistsView: () => import('../views/PlaylistsView.vue'),
  PlaylistView: () => import('../views/PlaylistView.vue'),
  HistoryView: () => import('../views/HistoryView.vue'),
  MessagesView: () => import('../views/MessagesView.vue'),
  ArtistProfileView: () => import('../views/ArtistProfileView.vue'),
  ArtistStudioView: () => import('../views/ArtistStudioView.vue')
};

export const routeRecords = [
  { path: '/', redirect: '/discover' },
  { path: '/login', name: 'login', component: views.LoginView, meta: { requiresAuth: false, publicOnly: true } },
  { path: '/register', name: 'register', component: views.RegisterView, meta: { requiresAuth: false, publicOnly: true } },
  { path: '/activate', name: 'activate', component: views.ActivateView, meta: { requiresAuth: false, publicOnly: true } },
  { path: '/discover', name: 'discover', component: views.DiscoverView, meta: { requiresAuth: true } },
  { path: '/search', name: 'search', component: views.SearchView, meta: { requiresAuth: true } },
  { path: '/tracks/:trackId', name: 'track', component: views.TrackView, meta: { requiresAuth: true } },
  { path: '/artists/:userId', name: 'artist', component: views.ArtistView, meta: { requiresAuth: true } },
  { path: '/profile', name: 'profile', component: views.ProfileView, meta: { requiresAuth: true } },
  { path: '/library/favorites', name: 'favorites', component: views.FavoritesView, meta: { requiresAuth: true } },
  { path: '/library/playlists', name: 'playlists', component: views.PlaylistsView, meta: { requiresAuth: true } },
  { path: '/library/playlists/:playlistId', name: 'playlist', component: views.PlaylistView, meta: { requiresAuth: true } },
  { path: '/library/history', name: 'history', component: views.HistoryView, meta: { requiresAuth: true } },
  { path: '/messages', name: 'messages', component: views.MessagesView, meta: { requiresAuth: true } },
  { path: '/artist/profile', name: 'artist-profile', component: views.ArtistProfileView, meta: { requiresAuth: true, role: 'SINGER' } },
  { path: '/artist/studio', name: 'artist-studio', component: views.ArtistStudioView, meta: { requiresAuth: true, role: 'SINGER' } },
  { path: '*', redirect: '/discover' }
];
