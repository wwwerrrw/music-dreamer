import { API_PATHS, mapLoginSession } from '@music-dreamer/shared';
import { selectActivationMessage } from './activation.js';
import { saveSession } from './session.js';
import { defaultRouteFor } from '../router/access.js';

export async function login({ api, storage, credentials }) {
  const response = await api.post(API_PATHS.identity.login, credentials);
  const session = saveSession(storage, mapLoginSession(response.data));
  return { session, redirect: defaultRouteFor(session.role) };
}

export async function registerAndActivate({ api, registration }) {
  const registered = await api.post(API_PATHS.identity.register, registration);
  const inbox = await api.get(API_PATHS.identity.developmentInbox);
  const activation = selectActivationMessage(inbox.data, registration.email);
  if (!activation) throw new Error('ACTIVATION_MESSAGE_NOT_FOUND');
  await api.post(API_PATHS.identity.activate, { token: activation.rawToken });
  return { userId: registered.data.userId, email: registration.email };
}
