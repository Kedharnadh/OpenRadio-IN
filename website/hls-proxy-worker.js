addEventListener('fetch', (event) => {
  event.respondWith(handleRequest(event.request));
});

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, OPTIONS',
  'Access-Control-Allow-Headers': '*',
};

async function handleRequest(request) {
  if (request.method === 'OPTIONS') {
    return new Response(null, { headers: CORS_HEADERS });
  }

  const url = new URL(request.url);
  const hlsUrl = url.searchParams.get('url');
  if (!hlsUrl) {
    return new Response('Missing ?url=<HLS_URL>', { status: 400, headers: CORS_HEADERS });
  }

  try { new URL(hlsUrl); } catch {
    return new Response('Invalid URL', { status: 400, headers: CORS_HEADERS });
  }

  // Allow caller to force a content type (e.g. audio/mpeg)
  const forcedContentType = url.searchParams.get('contentType');

  // 1. Fetch manifest first to determine segment type and validate
  let manifest;
  try {
    const resp = await fetch(hlsUrl, { headers: { 'Cache-Control': 'no-cache' } });
    if (!resp.ok) throw new Error('Manifest fetch failed');
    manifest = await resp.text();
  } catch (err) {
    return new Response('Failed to fetch HLS manifest', { status: 502, headers: CORS_HEADERS });
  }

  // 2. Determine correct content type from first segment URL
  let contentType;
  if (forcedContentType) {
    contentType = forcedContentType;
  } else {
    const segmentLines = manifest
      .split('\n')
      .map((l) => l.trim())
      .filter((l) => l && !l.startsWith('#'));
    const firstSegment = segmentLines[0];
    contentType = guessContentType(firstSegment, hlsUrl);
  }

  // 3. Stream segments with the correct content type
  const { readable, writable } = new TransformStream();
  streamSegments(hlsUrl, manifest, writable);

  return new Response(readable, {
    headers: {
      ...CORS_HEADERS,
      'Content-Type': contentType,
      'Cache-Control': 'no-cache',
    },
  });
}

function guessContentType(segment, baseUrl) {
  if (!segment) return 'audio/mpeg';
  try {
    const ext = new URL(segment, baseUrl).pathname.split('.').pop()?.toLowerCase() || '';
    if (ext === 'ts') return 'video/MP2T';
    if (ext === 'aac' || ext === 'm4a') return 'audio/aac';
    if (ext === 'mp3') return 'audio/mpeg';
    if (ext === 'mp4' || ext === 'm4s') return 'video/mp4';
    if (ext === 'ogg' || ext === 'oga') return 'audio/ogg';
    if (ext === 'wav') return 'audio/wav';
    if (ext === 'ac3' || ext === 'eac3') return 'audio/ac3';
  } catch {}
  return 'audio/mpeg';
}

async function streamSegments(hlsUrl, manifest, writable) {
  const writer = writable.getWriter();
  const seenSegments = new Set();
  let retries = 0;

  try {
    while (true) {
      const lines = manifest
        .split('\n')
        .map((l) => l.trim())
        .filter((l) => l && !l.startsWith('#'));

      for (const segment of lines) {
        let segmentUrl;
        try {
          segmentUrl = new URL(segment, hlsUrl).href;
        } catch {
          continue;
        }

        if (seenSegments.has(segmentUrl)) continue;
        seenSegments.add(segmentUrl);

        if (seenSegments.size > 200) {
          const entries = [...seenSegments];
          for (let i = 0; i < 100; i++) seenSegments.delete(entries[i]);
        }

        try {
          const segResp = await fetch(segmentUrl);
          if (!segResp.ok) continue;
          const reader = segResp.body.getReader();
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            await writer.write(value);
          }
        } catch {
          // skip failed segment
        }
      }

      // Poll for updated manifest (live HLS)
      await sleep(4000);
      try {
        const resp = await fetch(hlsUrl, { headers: { 'Cache-Control': 'no-cache' } });
        if (resp.ok) {
          manifest = await resp.text();
          retries = 0;
        }
      } catch {
        retries++;
        if (retries > 10) break;
      }
    }
  } catch (err) {
    console.error('Stream error:', err);
  } finally {
    try { writer.close(); } catch {}
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
