import { api } from '../api.js';
import { createArtistStudioApi } from './studio-api.js';

export const artistStudio = createArtistStudioApi(api);
