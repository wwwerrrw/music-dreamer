import test from 'node:test';
import assert from 'node:assert/strict';

import { assertSupportedNode } from '../src/contracts/node-runtime.js';

const expectedExecutable = 'E:\\nodejs-24.18.0\\node.exe';

test('node runtime gate accepts the locked executable and version', () => {
  assert.doesNotThrow(() => assertSupportedNode({
    execPath: expectedExecutable,
    version: 'v24.18.0'
  }));
});

test('node runtime gate rejects another executable', () => {
  assert.throws(
    () => assertSupportedNode({ execPath: 'C:\\Program Files\\nodejs\\node.exe', version: 'v24.18.0' }),
    /E:\\nodejs-24\.18\.0\\node\.exe/
  );
});

test('node runtime gate rejects another version', () => {
  assert.throws(
    () => assertSupportedNode({ execPath: expectedExecutable, version: 'v22.0.0' }),
    /v24\.18\.0/
  );
});
