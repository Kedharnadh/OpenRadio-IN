// Cloudflare Worker: HLS-to-audio proxy for Chromecast
// Deploy this to https://workers.cloudflare.com (free tier)
// Then set HLS_PROXY_URL in app.js to your worker URL
//
// How it works:
// Chromecast cannot play audio-only HLS on the Default Media Receiver.
// This worker fetches HLS segments and streams them as a continuous
// audio/mpeg stream, which the Default Media Receiver CAN play.

addEventListener('fetch', (event) => {
  event.respondWith(handleRequest(event.request));
});

async function handleRequest(request) {
  const url = new URL(request.url);
  const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, OPTIONS',
    'Access-Control-Allow-Headers': '*',
  };

  if (request.method === 'OPTIONS') {
    return new Response(null, { headers: corsHeaders });
  }

  const hlsUrl = url.searchParams.get('url');
  if (!hlsUrl) {
    return new Response('Missing ?url=<HLS_URL>', {
      status: 400,
      headers: corsHeaders,
    });
  }

  // Validate URL
  try {
    new URL(hlsUrl);
  } catch {
    return new Response('Invalid URL', {
      status: 400,
      headers: corsHeaders,
    });
  }

  const { readable, writable } = new TransformStream();
  streamHls(hlsUrl, writable);

  return new Response(readable, {
    headers: {
      ...corsHeaders,
      'Content-Type': 'audio/mpeg',
      'Cache-Control': 'no-cache',
      'Transfer-Encoding': 'chunked',
    },
  });
}

async function streamHls(hlsUrl, writable) {
  const writer = writable.getWriter();
  const baseUrl = new URL(hlsUrl);
  const seenSegments = new Set();

  try {
    while (true) {
      let manifest;
      try {
        const resp = await fetch(hlsUrl, {
          headers: { 'Cache-Control': 'no-cache' },
        });
        manifest = await resp.text();
      } catch {
        await sleep(5000);
        continue;
      }

      const segmentLines = manifest
        .split('\n')
        .map((line) => line.trim())
        .filter((line) => line && !line.startsWith('#'));

      for (const segment of segmentLines) {
        let segmentUrl;
        try {
          segmentUrl = new URL(segment, hlsUrl).href;
        } catch {
          continue;
        }

        if (seenSegments.has(segmentUrl)) continue;
        seenSegments.add(segmentUrl);

        // Keep set from growing unbounded
        if (seenSegments.size > 100) {
          const entries = [...seenSegments];
          for (let i = 0; i < 50; i++) seenSegments.delete(entries[i]);
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
          // skip failed segment, continue to next
        }
      }

      // Wait before checking for playlist updates
      await sleep(4000);
    }
  } catch (err) {
    console.error('Stream error:', err);
  } finally {
    try {
      writer.close();
    } catch {}
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
