import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source = fs.readFileSync(new URL('./cast-utils.js', import.meta.url), 'utf8');
const context = { window: {}, globalThis: {} };
vm.runInNewContext(source, context);
const castUtils = context.window.OpenRadioCast;

test('HLS content types stay HLS even when the proxy resolves TS or AAC segments', () => {
  assert.equal(
    castUtils.normalizeCastContentType('video/MP2T', 'https://example.com/live/stream.m3u8'),
    'application/vnd.apple.mpegurl'
  );

  assert.equal(
    castUtils.normalizeCastContentType('audio/aac', 'https://example.com/playlist.m3u8?token=abc'),
    'application/vnd.apple.mpegurl'
  );

  assert.equal(
    castUtils.normalizeCastContentType('audio/mpeg', 'https://example.com/stream.mp3'),
    'audio/mpeg'
  );
});
