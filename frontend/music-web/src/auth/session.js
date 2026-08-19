const SESSION_KEY = 'music-dreamer.session';
const SESSION_FIELDS = ['token', 'expiresAt', 'userId', 'role', 'status'];

function sanitizeSession(value) {
  if (!value || !value.token || !value.expiresAt || !value.role || value.status !== 'ACTIVE') return null;
  return Object.fromEntries(SESSION_FIELDS.map((field) => [field, value[field]]));
}

export function saveSession(storage, value) {
  const session = sanitizeSession(value);
  if (!session) throw new TypeError('A complete active session is required');
  storage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}

export function loadSession(storage, now = Date.now()) {
  const stored = storage.getItem(SESSION_KEY);
  if (!stored) return null;
  try {
    const session = sanitizeSession(JSON.parse(stored));
    if (!session || Date.parse(session.expiresAt) <= now) {
      clearSession(storage);
      return null;
    }
    return session;
  } catch {
    clearSession(storage);
    return null;
  }
}

export function clearSession(storage) {
  storage.removeItem(SESSION_KEY);
}

export function sessionErrorAction(problem, storage) {
  if (problem?.status === 401) {
    clearSession(storage);
    return 'LOGIN';
  }
  if (problem?.status === 403) return 'FORBIDDEN';
  if (problem?.status === 409) return 'REFRESH';
  if (problem?.status === 503) return 'RETRY';
  return 'ERROR';
}

export { SESSION_KEY };
