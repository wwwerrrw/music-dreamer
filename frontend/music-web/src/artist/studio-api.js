import { API_PATHS } from '@music-dreamer/shared';

export function createArtistStudioApi(api) {
  const upload = async (file, mediaType) => {
    const form = new FormData();
    form.append('file', file);
    return (await api.post(API_PATHS.media.objects, form, { params: { mediaType } })).data.id;
  };
  return {
    async profile(userId) { return (await api.get(API_PATHS.catalog.artist(userId))).data; },
    async saveProfile(value) { return (await api.put(API_PATHS.catalog.artistMe, value)).data; },
    async createDraft(form) {
      if (!form.audio) throw new TypeError('audio file is required');
      const mediaObjectId = await upload(form.audio, 'AUDIO');
      const coverMediaObjectId = form.cover ? await upload(form.cover, 'IMAGE') : null;
      const lyricsMediaObjectId = form.lyrics ? await upload(form.lyrics, 'LYRICS') : null;
      const payload = {
        title: form.title,
        durationMs: form.durationMs,
        mediaObjectId,
        coverMediaObjectId,
        lyricsMediaObjectId
      };
      return (await api.post(API_PATHS.catalog.drafts, payload)).data;
    },
    async updateDraft(trackId, payload) { return (await api.put(API_PATHS.catalog.updateDraft(trackId), payload)).data; },
    async submit(trackId) { return (await api.post(API_PATHS.catalog.submit(trackId))).data; }
  };
}
