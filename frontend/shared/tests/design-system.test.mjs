import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const tokens = readFileSync(new URL('../src/styles/tokens.css', import.meta.url), 'utf8');

test('shared tokens lock the approved light Stitch palette', () => {
  for (const declaration of [
    '--md-canvas: #fff8f7',
    '--md-surface: #ffffff',
    '--md-primary: #e63946',
    '--md-secondary: #1d3557',
    '--md-ink: #1a1a1a',
    '--md-muted: #6c757d'
  ]) {
    assert.equal(tokens.includes(declaration), true, `missing ${declaration}`);
  }

  assert.doesNotMatch(tokens, /#0d0d11|Sonic Obsidian/i);
});

test('shared tokens define stable navigation, player, focus, and typography primitives', () => {
  for (const token of [
    '--md-sidebar-width:',
    '--md-player-height:',
    '--md-mobile-nav-height:',
    '--md-control-height:',
    '--md-font-display:',
    '--md-font-body:'
  ]) {
    assert.equal(tokens.includes(token), true, `missing ${token}`);
  }

  assert.match(tokens, /:focus-visible\s*\{/);
  assert.match(tokens, /letter-spacing:\s*0/);
});
