import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source = fs.readFileSync(new URL('./cast-utils.js', import.meta.url), 'utf8');
const context = { window: {}, globalThis: {} };
vm.runInNewContext(source, context);
const castUtils = context.window.OpenRadioCast;

test('native HLS streams keep the HLS MIME type', () => {
  assert.equal(
    castUtils.normalizeCastContentType('application/vnd.apple.mpegurl', 'https://example.com/live/stream.m3u8'),
    'application/vnd.apple.mpegurl'
  );

  assert.equal(
    castUtils.normalizeCastContentType('', 'https://example.com/live/stream.m3u8?token=abc'),
    'application/vnd.apple.mpegurl'
  );
});

test('proxy MPEG-TS output is NOT mislabeled as HLS just because .m3u8 appears in the proxy query string', () => {
  const proxyUrl = 'https://openradio-hls-proxy.example.workers.dev?url=https%3A%2F%2Fexample.com%2Flive%2Fstream.m3u8&contentType=video%2FMP2T';

  assert.equal(
    castUtils.normalizeCastContentType('video/MP2T', proxyUrl),
    'video/mp2t'
  );
});

test('concrete content types are trusted over URL sniffing', () => {
  assert.equal(
    castUtils.normalizeCastContentType('audio/aac', 'https://example.com/playlist.m3u8?token=abc'),
    'audio/aac'
  );

  assert.equal(
    castUtils.normalizeCastContentType('audio/ogg', 'https://example.com/playlist.m3u8'),
    'audio/ogg'
  );

  assert.equal(
    castUtils.normalizeCastContentType('audio/mpeg', 'https://example.com/stream.mp3'),
    'audio/mpeg'
  );
});
