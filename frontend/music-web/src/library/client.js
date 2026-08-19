import { api } from '../api.js';
import { createLibraryApi } from './api.js';

export const library = createLibraryApi(api);
