const STATUS_LABELS = Object.freeze({ DRAFT: '草稿', PENDING_REVIEW: '已提交', SUBMITTED: '已提交', PUBLISHED: '已发布', LOCKED: '已锁定' });

export function preserveStudioForm(form, problem) {
  const errorCode = problem?.status === 503 ? 'RETRY' : problem?.status === 403 ? 'FORBIDDEN' : 'ERROR';
  return { ...form, errorCode };
}

export function draftStatusLabel(value) { return STATUS_LABELS[value] || value; }
