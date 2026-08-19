const STATUS_MESSAGES = Object.freeze({
  401: '登录状态已失效，请重新登录。',
  403: '当前账号没有执行此操作的权限。',
  409: '内容状态已变化，请刷新后重试。',
  503: '服务暂时不可用，请稍后重试。'
});

export function problemMessage(problem) {
  if (STATUS_MESSAGES[problem?.status]) return STATUS_MESSAGES[problem.status];
  if (problem?.detail && problem.detail !== 'Request failed') return problem.detail;
  return problem?.traceId ? `请求失败（追踪号：${problem.traceId}）` : '请求失败，请稍后重试。';
}
