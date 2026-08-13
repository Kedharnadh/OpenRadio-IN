(function () {
  function isHlsUrl(url) {
    return typeof url === 'string' && /\.m3u8(?:[?#]|$)/i.test(url);
  }

  function normalizeCastContentType(contentType, streamUrl) {
    const ct = String(contentType || '').toLowerCase();
    if (ct.includes('mpegurl') || isHlsUrl(streamUrl)) {
      return 'application/vnd.apple.mpegurl';
    }
    if (ct === 'audio/aac') return 'audio/aac';
    if (ct === 'audio/ogg') return 'audio/ogg';
    if (ct === 'video/mp2t') return 'video/mp2t';
    return ct || 'audio/mpeg';
  }

  window.OpenRadioCast = { isHlsUrl, normalizeCastContentType };
})();
