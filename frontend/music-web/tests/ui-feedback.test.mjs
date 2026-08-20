import test from 'node:test';
import assert from 'node:assert/strict';

import { clearActionFeedback, createActionFeedback, failActionFeedback } from '../src/ui/action-feedback.js';

test('track actions keep independent visible failure messages', () => {
  const initial = createActionFeedback();
  const favoriteFailed = failActionFeedback(initial, 'favorite', '收藏失败');
  const bothFailed = failActionFeedback(favoriteFailed, 'lyrics', '歌词加载失败');

  assert.deepEqual(initial, { favorite: '', lyrics: '' });
  assert.deepEqual(bothFailed, { favorite: '收藏失败', lyrics: '歌词加载失败' });
  assert.deepEqual(clearActionFeedback(bothFailed, 'favorite'), { favorite: '', lyrics: '歌词加载失败' });
});
