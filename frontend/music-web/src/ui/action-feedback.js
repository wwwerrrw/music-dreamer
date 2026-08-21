const EMPTY_FEEDBACK = Object.freeze({ favorite: '', lyrics: '' });

export function createActionFeedback() {
  return { ...EMPTY_FEEDBACK };
}

export function failActionFeedback(feedback, action, message) {
  return { ...feedback, [action]: message };
}

export function clearActionFeedback(feedback, action) {
  return { ...feedback, [action]: '' };
}
