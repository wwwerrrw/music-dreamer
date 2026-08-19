import axios from 'axios';
import { toProblemDetail } from './problem-detail.js';

export const MEDIA_OBJECTS_PATH = '/api/media/objects';

function withBearer(headers, tokenProvider, mediaRequest, url, method) {
  const next = { ...(headers || {}) };
  const token = typeof tokenProvider === 'function' ? tokenProvider() : undefined;
  const isMediaRead = method === 'GET' && (
    mediaRequest || url === MEDIA_OBJECTS_PATH || url.startsWith(`${MEDIA_OBJECTS_PATH}/`)
  );
  if (token && !isMediaRead) {
    next.Authorization = `Bearer ${token}`;
  }
  return next;
}

export function createApiClient({
  baseURL = '',
  tokenProvider = () => undefined,
  adapter,
  axiosInstance = axios
} = {}) {
  const request = async (method, url, options = {}) => {
    const config = {
      ...options,
      method: method.toUpperCase(),
      url: `${baseURL}${url}`,
      headers: withBearer(options.headers, tokenProvider, options.mediaRequest, url, method.toUpperCase())
    };
    const dispatch = adapter || ((requestConfig) => axiosInstance.request(requestConfig));
    try {
      return await dispatch(config);
    } catch (error) {
      if (error && typeof error.status === 'number' && error.code && error.detail) throw error;
      throw toProblemDetail(error);
    }
  };
  return {
    request,
    get: (url, options) => request('GET', url, options),
    post: (url, body, options = {}) => request('POST', url, { ...options, data: body }),
    put: (url, body, options = {}) => request('PUT', url, { ...options, data: body }),
    delete: (url, options) => request('DELETE', url, options)
  };
}
