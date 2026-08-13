(function () {
  function isHlsUrl(url) {
    return typeof url === 'string' && /\.m3u8(?:[?#]|$)/i.test(url);
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
    const originalUrl = resolveOriginalUrl(streamUrl);
    const ct = String(contentType || '').toLowerCase();
    if (ct.includes('mpegurl') || isHlsUrl(originalUrl)) {
      return 'application/vnd.apple.mpegurl';
    }
    if (ct === 'audio/aac') return 'audio/aac';
    if (ct === 'audio/ogg') return 'audio/ogg';
    if (ct === 'video/mp2t') return 'video/mp2t';
    return ct || 'audio/mpeg';
  }

  window.OpenRadioCast = { isHlsUrl, normalizeCastContentType, resolveOriginalUrl };
})();
