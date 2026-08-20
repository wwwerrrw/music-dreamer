function viewState(value) {
  if (value === 'loading' || value === 'empty') return { kind: value, visible: true };
  if (value?.status === 403) return { kind: 'error', visible: true, code: 'FORBIDDEN' };
  if (value?.status === 503) return { kind: 'error', visible: true, code: 'RETRY' };
  return { kind: 'ready', visible: true };
}

export function playlistState(value) { return viewState(value); }
export function messageState(value) { return viewState(value); }
export function markMessageRead(messages, messageId, readAt) {
  return messages.map((message) => message.id === messageId ? { ...message, readAt } : message);
}

export function openMessageDetail(messageId) {
  return { selectedId: messageId, detailOpen: true };
}

export function closeMessageDetail(state) {
  return { selectedId: state.selectedId, detailOpen: false };
}
