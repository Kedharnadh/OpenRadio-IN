(function () {
  function isHlsUrl(url) {
    if (typeof url !== 'string') return false;
    try {
      return /\.m3u8(?:[?#]|$)/i.test(new URL(url).pathname);
    } catch {
      return /\.m3u8(?:[?#]|$)/i.test(url);
    }
  }

  function resolveOriginalUrl(streamUrl) {
    if (typeof streamUrl !== 'string') return streamUrl;
    try {
      const parsed = new URL(streamUrl);
      const proxiedUrl = parsed.searchParams.get('url');
      if (proxiedUrl) return decodeURIComponent(proxiedUrl);
    } catch {}
    return streamUrl;
  }

  function normalizeCastContentType(contentType, streamUrl) {
    const ct = String(contentType || '')
      .trim()
      .toLowerCase();
    if (ct.includes('mpegurl')) return 'application/vnd.apple.mpegurl';
    if (ct === 'audio/mpeg' || ct === 'audio/mp3') return 'audio/mpeg';
    if (ct === 'audio/aac' || ct === 'audio/aacp') return 'audio/aac';
    if (ct === 'audio/ogg' || ct === 'application/ogg') return 'audio/ogg';
    if (ct === 'video/mp2t' || ct === 'video/mpeg') return 'video/mp2t';
    if (ct === 'video/mp4' || ct === 'audio/mp4') return ct;
    if (ct === 'audio/ac3' || ct === 'audio/eac3') return ct;
    if (ct === 'audio/wav') return 'audio/wav';
    if (ct === 'audio/flac') return 'audio/flac';
    if (!ct) {
      const originalUrl = resolveOriginalUrl(streamUrl);
      if (isHlsUrl(originalUrl)) return 'application/vnd.apple.mpegurl';
    }
    return ct || 'audio/mpeg';
  }

  window.OpenRadioCast = { isHlsUrl, normalizeCastContentType, resolveOriginalUrl };
})();
