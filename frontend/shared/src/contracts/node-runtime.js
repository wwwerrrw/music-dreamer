const SUPPORTED_EXECUTABLE = 'E:\\nodejs-24.18.0\\node.exe';
const SUPPORTED_VERSION = 'v24.18.0';

export function assertSupportedNode({ execPath = process.execPath, version = process.version } = {}) {
  if (String(execPath).toLowerCase() !== SUPPORTED_EXECUTABLE.toLowerCase()) {
    throw new Error(`Unsupported Node executable. Expected ${SUPPORTED_EXECUTABLE}, received ${execPath}`);
  }
  if (version !== SUPPORTED_VERSION) {
    throw new Error(`Unsupported Node version. Expected ${SUPPORTED_VERSION}, received ${version}`);
  }
  return true;
}

export { SUPPORTED_EXECUTABLE, SUPPORTED_VERSION };
