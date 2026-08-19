export function resolveAdminAccess(path, session) {
  if (session?.role === 'ADMIN' && session.status === 'ACTIVE') return { allow: true };
  return { allow: false, redirect: `/login?redirect=${encodeURIComponent(path)}` };
}
