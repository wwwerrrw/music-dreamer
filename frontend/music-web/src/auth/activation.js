export function selectActivationMessage(messages, email) {
  return messages
    .filter((message) => message.email === email)
    .sort((left, right) => Date.parse(right.expiresAt) - Date.parse(left.expiresAt))[0] || null;
}
