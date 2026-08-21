export async function confirmAction(confirmed, action) {
  if (!confirmed) return false;
  await action();
  return true;
}

export async function publishConfirmedMessage({ confirm, publish, payload }) {
  try {
    await confirm();
  } catch (problem) {
    if (problem === 'cancel' || problem === 'close') return null;
    throw problem;
  }
  return publish(payload);
}

export function createSingleFlight() {
  let pending = false;
  return async (action) => {
    if (pending) return undefined;
    pending = true;
    try {
      return await action();
    } finally {
      pending = false;
    }
  };
}

export function viewState(page, value) {
  if (typeof value === 'string') return { page, kind: value };
  if (value?.status === 503) return { page, kind: 'error', code: 'RETRY' };
  if (value?.status === 403) return { page, kind: 'error', code: 'FORBIDDEN' };
  return { page, kind: 'error', code: 'ERROR' };
}

const MESSAGES = Object.freeze({
  401: '登录状态已失效，请重新登录。',
  403: '管理员权限不足。',
  409: '服务端状态已变化，请刷新后重试。',
  503: '服务暂时不可用，请稍后重试。'
});

export function adminProblemMessage(problem) {
  return MESSAGES[problem?.status] || problem?.detail || (problem?.traceId ? `请求失败（追踪号：${problem.traceId}）` : '请求失败，请稍后重试。');
}
