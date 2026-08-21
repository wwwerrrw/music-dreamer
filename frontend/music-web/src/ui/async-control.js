export function createLatestOnly() {
  let latestRequest = 0;
  return async (request, apply) => {
    const requestId = ++latestRequest;
    try {
      const value = await request();
      if (requestId === latestRequest) return apply(value);
    } catch (problem) {
      if (requestId === latestRequest) throw problem;
    }
    return undefined;
  };
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
