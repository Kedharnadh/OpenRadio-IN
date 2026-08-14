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

  // EPG proxy mode: fetch + parse a Prasar Bharati cuesheet (no ?url= needed)
  if (url.searchParams.has('epg')) {
    return handleEpgRequest(url.searchParams.get('epg'));
  }

  const hlsUrl = url.searchParams.get('url');
  if (!hlsUrl) {
    return new Response('Missing ?url=<HLS_URL>', { status: 400, headers: CORS_HEADERS });
  }

  try { new URL(hlsUrl); } catch {
    return new Response('Invalid URL', { status: 400, headers: CORS_HEADERS });
  }

  // Metadata proxy mode
  if (url.searchParams.has('meta')) {
    const metaUrl = url.searchParams.get('metaUrl');
    return handleMetadataRequest(hlsUrl, metaUrl);
  }

  // Allow caller to force a content type (e.g. video/MP2T)
  const forcedContentType = url.searchParams.get('contentType');
  const probeOnly = url.searchParams.has('probe');

  // Resolve the (possibly multi-variant) playlist down to a media playlist
  let resolution;
  try {
    resolution = await resolvePlaylist(hlsUrl);
  } catch (err) {
    return new Response('Failed to fetch HLS manifest', { status: 502, headers: CORS_HEADERS });
  }

  const contentType = forcedContentType || resolution.contentType;

  // probe mode: report the resolved URL and content type without streaming
  if (probeOnly) {
    return new Response(JSON.stringify({
      url: resolution.url,
      contentType,
      type: resolution.fromMaster ? 'hls-master' : 'hls-media',
    }), {
      headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' },
    });
  }

  const { readable, writable } = new TransformStream();
  streamSegments(resolution, writable);

  return new Response(readable, {
    headers: {
      ...CORS_HEADERS,
      'Content-Type': contentType,
      'Cache-Control': 'no-cache',
    },
  });
}

async function handleMetadataRequest(streamUrl, metaUrl) {
  let icy = {};
  try {
    icy = await fetchIcyMetadata(streamUrl);
  } catch (err) {
    // fall through to the station's status endpoint, if provided
  }

  let status = {};
  if (metaUrl) {
    try {
      status = await fetchStatusMetadata(metaUrl);
    } catch (err) {
      // fall through
    }
  }

  const result = {
    streamTitle: icy.streamTitle || status.streamTitle || '',
    art: status.art || '',
  };
  if (result.streamTitle || result.art) {
    return new Response(JSON.stringify(result), {
      headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' },
    });
  }
  return new Response(JSON.stringify({ streamTitle: '' }), {
    headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' },
  });
}

function concatBytes(a, b) {
  if (!a || !a.length) return b;
  if (!b || !b.length) return a;
  const out = new Uint8Array(a.length + b.length);
  out.set(a);
  out.set(b);
  return out;
}

// ICY metadata framing: every `metaInt` audio bytes is followed by a 1-byte
// length (L) and then L*16 bytes of metadata. Network chunks are arbitrary
// sizes, so the parser must not assume the first metadata block lines up with
// a chunk boundary (Cloudflare's fetch delivers multi-KB chunks that often
// swallow the boundary and several blocks at once).
async function fetchIcyMetadata(streamUrl) {
  const resp = await fetch(streamUrl, {
    headers: { 'Icy-MetaData': '1', 'Cache-Control': 'no-cache' },
  });
  const metaInt = parseInt(resp.headers.get('icy-metaint') || '0', 10);
  if (!metaInt || metaInt <= 0) {
    return { streamTitle: '', error: 'no-icy' };
  }
  const reader = resp.body.getReader();
  let buffer = new Uint8Array(0);
  let toSkip = metaInt; // audio bytes remaining before the next metadata block
  let metaLen = -1; // metadata block length in bytes; -1 = length byte not read yet
  let metaBytes = new Uint8Array(0);

  const readChunk = async () => {
    const { done, value } = await reader.read();
    if (done) return false;
    buffer = concatBytes(buffer, value);
    return true;
  };

  while (true) {
    while (buffer.length > 0) {
      if (toSkip > 0) {
        const n = Math.min(toSkip, buffer.length);
        toSkip -= n;
        buffer = buffer.slice(n);
      } else if (metaLen < 0) {
        metaLen = buffer[0] * 16;
        buffer = buffer.slice(1);
      } else {
        const n = Math.min(metaLen - metaBytes.length, buffer.length);
        metaBytes = concatBytes(metaBytes, buffer.slice(0, n));
        buffer = buffer.slice(n);
        if (metaBytes.length === metaLen) {
          try { await reader.cancel(); } catch {}
          const metaStr = new TextDecoder().decode(metaBytes);
          const match = metaStr.match(/StreamTitle='([^']*)'/i);
          return { streamTitle: match ? match[1].trim() : '' };
        }
      }
    }
    if (!(await readChunk())) break;
  }
  try { await reader.cancel(); } catch {}
  return { streamTitle: '', error: 'incomplete' };
}

async function fetchStatusMetadata(metaUrl) {
  const resp = await fetch(metaUrl, { headers: { 'Cache-Control': 'no-cache' } });
  if (!resp.ok) throw new Error(`Status fetch failed: ${resp.status}`);
  const json = await resp.json();

  // AzuraCast "now playing" API: { now_playing: { song: { text, title, art } } }
  if (json && json.now_playing && json.now_playing.song) {
    const song = json.now_playing.song;
    const streamTitle = String(song.text || song.title || '').trim();
    const art = /^https?:\/\//.test(String(song.art || '')) ? song.art : '';
    return { streamTitle, art };
  }

  // Icecast status-json.xsl
  const source = json && json.icestats ? json.icestats.source : null;
  const sources = Array.isArray(source) ? source : source ? [source] : [];
  const mount = sources.find((entry) => entry && (entry.song || entry.title)) || sources[0] || {};
  const streamTitle = String(mount.song || mount.title || mount.server_name || '').trim();
  return { streamTitle, art: '' };
}

function segmentLines(manifest) {
  return manifest
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l && !l.startsWith('#'));
}

function isMasterPlaylist(manifest) {
  return /#EXT-X-STREAM-INF:/i.test(manifest);
}

async function fetchText(target) {
  const resp = await fetch(target, { headers: { 'Cache-Control': 'no-cache' } });
  if (!resp.ok) throw new Error('Manifest fetch failed');
  return resp.text();
}

function pickVariant(manifest, baseUrl) {
  let bestUrl = null;
  let bestBw = -1;
  const lines = manifest.split('\n').map((l) => l.trim()).filter(Boolean);
  for (let i = 0; i < lines.length; i++) {
    if (!lines[i].startsWith('#EXT-X-STREAM-INF:')) continue;
    const variant = lines[i + 1];
    if (!variant || variant.startsWith('#')) continue;
    const bw = parseInt((/BANDWIDTH=(\d+)/i.exec(lines[i]) || [])[1] || '0', 10);
    if (bw > bestBw) {
      bestBw = bw;
      bestUrl = new URL(variant, baseUrl).href;
    }
  }
  if (!bestUrl) {
    const fallback = segmentLines(manifest)[0];
    if (fallback) bestUrl = new URL(fallback, baseUrl).href;
  }
  if (!bestUrl) throw new Error('No variant found');
  return bestUrl;
}

async function resolvePlaylist(hlsUrl) {
  let baseUrl = hlsUrl;
  let manifest = await fetchText(baseUrl);
  let fromMaster = false;
  if (isMasterPlaylist(manifest)) {
    baseUrl = pickVariant(manifest, hlsUrl);
    manifest = await fetchText(baseUrl);
    fromMaster = true;
  }
  return {
    url: baseUrl,
    manifest,
    fromMaster,
    contentType: guessContentType(manifest, baseUrl),
  };
}

function guessContentType(manifest, baseUrl) {
  const segment = segmentLines(manifest)[0];
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

async function fetchAndWrite(writer, target) {
  const resp = await fetch(target);
  if (!resp.ok) throw new Error(`Fetch failed: ${resp.status}`);
  const reader = resp.body.getReader();
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    await writer.write(value);
  }
}

function parseInitMap(manifest, baseUrl) {
  const match = /#EXT-X-MAP:URI="([^"]+)"/i.exec(manifest);
  if (!match) return null;
  try {
    return { url: new URL(match[1], baseUrl).href };
  } catch {}
  return null;
}

async function streamSegments(resolution, writable) {
  const writer = writable.getWriter();
  const seenSegments = new Set();
  let manifest = resolution.manifest;
  const baseUrl = resolution.url;
  const live = /#EXT-X-MEDIA-SEQUENCE/i.test(manifest);
  const initMap = parseInitMap(manifest, baseUrl);
  let initWritten = false;
  let retries = 0;

  try {
    while (true) {
      const lines = segmentLines(manifest);
      // For live playlists, join near the live edge instead of the oldest segment.
      const startIndex = live && seenSegments.size === 0 ? Math.max(0, lines.length - 2) : 0;

      for (let i = startIndex; i < lines.length; i++) {
        let segmentUrl;
        try {
          segmentUrl = new URL(lines[i], baseUrl).href;
        } catch {
          continue;
        }

        if (seenSegments.has(segmentUrl)) continue;
        seenSegments.add(segmentUrl);

        if (seenSegments.size > 200) {
          const entries = [...seenSegments];
          for (let k = 0; k < 100; k++) seenSegments.delete(entries[k]);
        }

        try {
          if (!initWritten && initMap) {
            await fetchAndWrite(writer, initMap.url);
            initWritten = true;
          }
          await fetchAndWrite(writer, segmentUrl);
        } catch {
          // skip failed segment
        }
      }

      // Poll for updated manifest (live HLS)
      await sleep(4000);
      try {
        const resp = await fetch(baseUrl, { headers: { 'Cache-Control': 'no-cache' } });
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

/* ---------- EPG (Prasar Bharati cuesheet) ---------- */

function decodeEntities(str) {
  return str
    .replace(/&#0*39;/g, "'")
    .replace(/&#0*34;/g, '"')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>');
}

function cellText(cellHtml) {
  return decodeEntities(cellHtml.replace(/<[^>]*>/g, ' '))
    .replace(/\s+/g, ' ')
    .trim();
}

function parseCuesheet(html) {
  // Date appears in an <h4> as DD-MM-YYYY; normalise to YYYY-MM-DD
  const dateMatch = html.match(/(\d{2})-(\d{2})-(\d{4})/);
  const date = dateMatch ? `${dateMatch[3]}-${dateMatch[2]}-${dateMatch[1]}` : '';

  const tableMatch = html.match(/<table[^>]*id="st"[^>]*>([\s\S]*?)<\/table>/i);
  const programs = [];
  if (!tableMatch) return { date, programs };

  const rowRe = /<tr[^>]*>([\s\S]*?)<\/tr>/gi;
  const tableHtml = tableMatch[1].replace(/<!--[\s\S]*?-->/g, '');
  let rowMatch;
  while ((rowMatch = rowRe.exec(tableHtml)) !== null) {
    const cells = [];
    const tdRe = /<t[dh][^>]*>([\s\S]*?)<\/t[dh]>/gi;
    let tdMatch;
    while ((tdMatch = tdRe.exec(rowMatch[1])) !== null) cells.push(tdMatch[1]);

    if (cells.length < 8) continue;
    const start = cellText(cells[1]);
    const end = cellText(cells[2]);
    const title = cellText(cells[4]);
    if (!/^\d{1,2}:\d{2}\s*(AM|PM)/i.test(start)) continue;
    if (!/^\d{1,2}:\d{2}\s*(AM|PM)/i.test(end)) continue;
    if (!title) continue;

    programs.push({
      start,
      end,
      title,
      language: cellText(cells[5]),
      type: cellText(cells[6]),
    });
    if (programs.length >= 200) break;
  }
  return { date, programs };
}

function secondsUntilIstMidnight() {
  const nowMs = Date.now();
  const istNow = new Date(nowMs + 5.5 * 3600 * 1000);
  const nextIstMidnight = Date.UTC(
    istNow.getUTCFullYear(),
    istNow.getUTCMonth(),
    istNow.getUTCDate() + 1
  );
  return Math.max(60, Math.floor((nextIstMidnight - nowMs) / 1000));
}

async function handleEpgRequest(epgId) {
  const upstream = `https://cuesheets.prasarbharati.org/viewsheet/${encodeURIComponent(epgId)}`;
  const headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36',
    'Accept-Language': 'en-IN,en;q=0.9',
    'Cache-Control': 'no-cache',
  };
  try {
    const resp = await fetch(upstream, { headers });
    if (!resp.ok) {
      return new Response(JSON.stringify({ error: `HTTP ${resp.status}` }), {
        status: resp.status,
        headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' },
      });
    }
    const html = await resp.text();
    const parsed = parseCuesheet(html);
    return new Response(JSON.stringify(parsed), {
      headers: {
        ...CORS_HEADERS,
        'Content-Type': 'application/json',
        'Cache-Control': `max-age=${secondsUntilIstMidnight()}`,
      },
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 502,
      headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' },
    });
  }
}
