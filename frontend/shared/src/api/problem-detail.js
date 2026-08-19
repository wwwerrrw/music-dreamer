export function toProblemDetail(error) {
  const response = error?.response;
  const data = response?.data;
  if (response) {
    return {
      status: response.status,
      code: data?.code || `HTTP_${response.status}`,
      detail: data?.detail || data?.message || 'Request failed',
      traceId: data?.traceId
    };
  }
  if (error?.request) {
    return {
      status: 0,
      code: 'NETWORK_ERROR',
      detail: 'Unable to reach the Music Dreamer services',
      traceId: undefined
    };
  }
  return {
    status: 0,
    code: 'CLIENT_ERROR',
    detail: error?.message || 'Request failed',
    traceId: undefined
  };
}
