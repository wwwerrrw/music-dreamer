import test from 'node:test';
import assert from 'node:assert/strict';

const asyncControl = await import('../src/ui/async-control.js').catch(() => ({}));

function deferred() {
  let resolve;
  const promise = new Promise((next) => { resolve = next; });
  return { promise, resolve };
}

test('latest-only applies only the newest overlapping request result', async () => {
  assert.equal(typeof asyncControl.createLatestOnly, 'function');
  const first = deferred();
  const second = deferred();
  const applied = [];
  const runLatest = asyncControl.createLatestOnly();

  const firstRun = runLatest(() => first.promise, (value) => applied.push(value));
  const secondRun = runLatest(() => second.promise, (value) => applied.push(value));
  second.resolve('newest');
  await secondRun;
  first.resolve('stale');
  await firstRun;

  assert.deepEqual(applied, ['newest']);
});

test('single-flight ignores overlapping submissions and unlocks after completion', async () => {
  assert.equal(typeof asyncControl.createSingleFlight, 'function');
  const pending = deferred();
  let calls = 0;
  const runOnce = asyncControl.createSingleFlight();
  const action = () => { calls += 1; return pending.promise; };

  const first = runOnce(action);
  const duplicate = runOnce(action);
  assert.equal(calls, 1);
  assert.equal(await duplicate, undefined);

  pending.resolve('done');
  assert.equal(await first, 'done');
  assert.equal(await runOnce(async () => { calls += 1; return 'again'; }), 'again');
  assert.equal(calls, 2);
});
