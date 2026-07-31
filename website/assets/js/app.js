(() => {
  const DATA_URL = './data/stations.json';
  const state = {
    stations: [],
    filteredStations: [],
    favorites: new Set(JSON.parse(localStorage.getItem('openradio-favorites') || '[]')),
    recentStations: JSON.parse(localStorage.getItem('openradio-recent') || '[]'),
    activeCategory: 'all',
    search: '',
    currentStation: null,
    playing: false,
    nowPlayingTrack: '',
    currentSource: 'all',
    volume: parseFloat(localStorage.getItem('openradio-volume') || '1'),
    muted: false,
    previousVolume: 1,
    theme: localStorage.getItem('openradio-theme') || 'dark',
    retryCount: 0,
    maxRetries: 3,
    sleepTimerId: null,
    sleepTimerEnd: null,
    userInitiatedStop: false,
    metadataIntervalId: null
  };

  const elements = {
    search: document.getElementById('search'),
    filters: document.getElementById('filters'),
    resultsCount: document.getElementById('results-count'),
    featured: document.getElementById('featured'),
    stations: document.getElementById('stations'),
    playToggle: document.getElementById('play-toggle'),
    playerTitle: document.getElementById('player-title'),
    playerMeta: document.getElementById('player-meta'),
    status: document.getElementById('status-pill'),
    prevBtn: document.getElementById('prev-btn'),
    nextBtn: document.getElementById('next-btn'),
    favoritesSection: document.getElementById('favorites-section'),
    recentSection: document.getElementById('recent-section'),
    recentStations: document.getElementById('recent-stations'),
    allSection: document.getElementById('all-section'),
    install: document.getElementById('install-app'),
    cast: document.getElementById('cast-button'),
    audio: document.getElementById('audio-player'),
    playerBar: document.getElementById('player-bar'),
    playerInfo: document.getElementById('player-info'),
    volumeSlider: document.getElementById('volume-slider'),
    volumeBtn: document.getElementById('volume-btn'),
    nowPlaying: document.getElementById('now-playing'),
    nowPlayingBackdrop: document.getElementById('now-playing-backdrop'),
    nowPlayingClose: document.getElementById('now-playing-close'),
    nowPlayingLogo: document.getElementById('now-playing-logo'),
    nowPlayingPlaceholder: document.getElementById('now-playing-placeholder'),
    nowPlayingTitle: document.getElementById('now-playing-title'),
    nowPlayingMeta: document.getElementById('now-playing-meta'),
    nowPlayingTrack: document.getElementById('now-playing-track'),
    npPrev: document.getElementById('np-prev'),
    npPlayToggle: document.getElementById('np-play-toggle'),
    npNext: document.getElementById('np-next'),
    npVolumeSlider: document.getElementById('np-volume-slider'),
    npVolumeBtn: document.getElementById('np-volume-btn'),
    shareBtn: document.getElementById('share-btn'),
    sleepTimerBtn: document.getElementById('sleep-timer-btn'),
    sleepTimerPicker: document.getElementById('sleep-timer-picker'),
    sleepTimerStatus: document.getElementById('sleep-timer-status'),
    themeToggle: document.getElementById('theme-toggle'),
    updateBanner: document.getElementById('update-banner'),
    updateBtn: document.getElementById('update-btn')
  };
  let installPrompt;
  let castContext;
  let castPlayer;
  let castPlayerController;

  const CUSTOM_CAST_APP_ID = '45881BB0';
  const HLS_PROXY_URL = 'https://openradio-hls-proxy.kedharnadh1.workers.dev';
  const RECENT_MAX = 10;

  function setStatus(message) {
    elements.status.textContent = message;
  }

  function makeElement(tag, className, text) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (text !== undefined) element.textContent = text;
    return element;
  }

  function stationTags(station) {
    return [...new Set([station.language, ...(station.categories || [])].filter(Boolean))].slice(0, 3).join(' \u2022 ');
  }

  function hasCategory(station, category) {
    if (category === 'all') return true;
    return [station.language, ...(station.categories || [])]
      .some((value) => String(value).toLowerCase() === category.toLowerCase());
  }

  function stationMatches(station) {
    const query = state.search.trim().toLowerCase();
    const searchable = [station.name, station.language, station.country, station.state, station.city, ...(station.categories || [])]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    return searchable.includes(query) && hasCategory(station, state.activeCategory);
  }

  /* ---------- Theme ---------- */

  function setTheme(theme) {
    state.theme = theme;
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('openradio-theme', theme);
    const metaTheme = document.getElementById('theme-color');
    if (theme === 'light') {
      elements.themeToggle.textContent = '\u{1F319} Dark';
      if (metaTheme) metaTheme.content = '#f1f5f9';
    } else {
      elements.themeToggle.textContent = '\u2600\uFE0F Light';
      if (metaTheme) metaTheme.content = '#0f172a';
    }
  }

  function toggleTheme() {
    setTheme(state.theme === 'dark' ? 'light' : 'dark');
  }

  /* ---------- Volume ---------- */

  function applyVolume(value) {
    const vol = Math.max(0, Math.min(1, value));
    state.volume = vol;
    elements.audio.volume = vol;
    elements.volumeSlider.value = vol;
    elements.npVolumeSlider.value = vol;
    localStorage.setItem('openradio-volume', String(vol));
    updateVolumeIcon();
  }

  function updateVolumeIcon() {
    const icon = state.muted || state.volume === 0 ? '\u{1F507}' : state.volume < 0.5 ? '\u{1F509}' : '\u{1F50A}';
    elements.volumeBtn.textContent = icon;
    elements.npVolumeBtn.textContent = icon;
  }

  function toggleMute() {
    if (state.muted) {
      state.muted = false;
      applyVolume(state.previousVolume);
    } else {
      state.muted = true;
      state.previousVolume = state.volume;
      applyVolume(0);
    }
    updateVolumeIcon();
  }

  /* ---------- Media Session ---------- */

  function setupMediaSession() {
    if (!('mediaSession' in navigator)) return;
    navigator.mediaSession.setActionHandler('play', () => togglePlayback());
    navigator.mediaSession.setActionHandler('pause', () => togglePlayback());
    navigator.mediaSession.setActionHandler('stop', () => stopPlayback());
    navigator.mediaSession.setActionHandler('previoustrack', () => playAdjacentStation(-1));
    navigator.mediaSession.setActionHandler('nexttrack', () => playAdjacentStation(1));
  }

  function updateMediaSession() {
    if (!('mediaSession' in navigator)) return;
    const station = state.currentStation;
    if (!station) {
      navigator.mediaSession.metadata = null;
      return;
    }
    navigator.mediaSession.metadata = new MediaMetadata({
      title: station.name,
      artist: station.language || 'OpenRadio-IN',
      artwork: station.logo ? [{ src: station.logo, sizes: '512x512', type: 'image/png' }] : []
    });
  }

  /* ---------- Collapse Sections ---------- */

  function getCollapsedSections() {
    try { return JSON.parse(localStorage.getItem('openradio-collapsed') || '[]'); } catch { return []; }
  }

  function saveCollapsedSections(sections) {
    localStorage.setItem('openradio-collapsed', JSON.stringify(sections));
  }

  function toggleSection(sectionId) {
    const collapsed = getCollapsedSections();
    const section = document.getElementById(sectionId);
    if (!section) return;
    const idx = collapsed.indexOf(sectionId);
    if (idx > -1) {
      collapsed.splice(idx, 1);
      section.classList.remove('collapsed');
    } else {
      collapsed.push(sectionId);
      section.classList.add('collapsed');
    }
    saveCollapsedSections(collapsed);
  }

  function restoreCollapsedStates() {
    const collapsed = getCollapsedSections();
    collapsed.forEach((sectionId) => {
      const section = document.getElementById(sectionId);
      if (section) section.classList.add('collapsed');
    });
  }

  /* ---------- Update Player ---------- */

  function updatePlayer() {
    const station = state.currentStation;
    const list = state.currentSource === 'favorites'
      ? state.stations.filter((s) => state.favorites.has(s.id))
      : state.filteredStations;
    const hasMultiple = list.length > 1;

    if (!station) {
      elements.playerTitle.textContent = 'Choose a station';
      elements.playerMeta.textContent = 'Your selected radio station will appear here.';
      elements.playToggle.disabled = true;
      elements.playToggle.textContent = '\u25b6 Play';
      elements.prevBtn.disabled = true;
      elements.nextBtn.disabled = true;
      elements.npPlayToggle.disabled = true;
      elements.npPlayToggle.textContent = '\u25b6 Play';
      elements.npPrev.disabled = true;
      elements.npNext.disabled = true;
      elements.nowPlayingTitle.textContent = 'Choose a station';
      elements.nowPlayingMeta.textContent = '';
      elements.nowPlayingLogo.hidden = true;
      elements.nowPlayingPlaceholder.hidden = false;
      elements.nowPlayingTrack.hidden = true;
      updateMediaSession();
      return;
    }

    const stationText = state.playing ? '\u23f9 Stop' : '\u25b6 Play';
    elements.playerTitle.textContent = station.name;
    const destination = isCasting() ? 'Casting' : (station.streams?.[0]?.codec || 'Stream');
    elements.playerMeta.textContent = `${station.language || 'Unknown language'} \u2022 ${destination}`;
    elements.playToggle.disabled = false;
    elements.playToggle.textContent = stationText;
    elements.prevBtn.disabled = !hasMultiple;
    elements.nextBtn.disabled = !hasMultiple;
    elements.npPlayToggle.disabled = false;
    elements.npPlayToggle.textContent = stationText;
    elements.npPrev.disabled = !hasMultiple;
    elements.npNext.disabled = !hasMultiple;
    elements.nowPlayingTitle.textContent = station.name;
    elements.nowPlayingMeta.textContent = `${station.language || 'Unknown language'} \u2022 ${destination}`;
    elements.nowPlayingLogo.src = station.logo || '';
    elements.nowPlayingLogo.alt = station.name;
    elements.nowPlayingLogo.hidden = !station.logo;
    elements.nowPlayingPlaceholder.hidden = Boolean(station.logo);
    if (state.nowPlayingTrack) {
      elements.nowPlayingTrack.hidden = false;
      elements.nowPlayingTrack.textContent = state.nowPlayingTrack;
    } else {
      elements.nowPlayingTrack.hidden = true;
    }
    updateMediaSession();
  }

  /* ---------- Recent Stations ---------- */

  function addRecentStation(station) {
    state.recentStations = state.recentStations.filter((s) => s.id !== station.id);
    state.recentStations.unshift({ id: station.id, name: station.name, language: station.language, categories: station.categories });
    if (state.recentStations.length > RECENT_MAX) state.recentStations.pop();
    localStorage.setItem('openradio-recent', JSON.stringify(state.recentStations));
    renderRecent();
  }

  function renderRecent() {
    if (!state.recentStations.length) {
      elements.recentSection.hidden = true;
      return;
    }
    elements.recentSection.hidden = false;
    const recentList = state.recentStations
      .map((r) => state.stations.find((s) => s.id === r.id))
      .filter(Boolean);
    elements.recentStations.replaceChildren(...recentList.map((s) => createStationCard(s, true)));
  }

  /* ---------- Station Card, Filters, Render ---------- */

  function createStationCard(station, featured) {
    const card = makeElement('article', `station-card${featured ? ' featured-card' : ''}`);
    const top = makeElement('div', 'station-card__top');

    const initials = (station.name.match(/\b\w/g) || []).slice(0, 2).join('').toUpperCase() || station.name[0] || '?';
    const thumb = makeElement('img', 'station-thumb');
    thumb.loading = 'lazy';
    thumb.alt = station.name;
    thumb.dataset.fallback = initials;
    const thumbFallback = makeElement('span', 'station-thumb-fallback', initials);
    if (station.logo) {
      thumb.src = station.logo;
      thumbFallback.hidden = true;
    } else {
      thumb.style.display = 'none';
    }
    thumb.addEventListener('error', () => { thumb.style.display = 'none'; thumbFallback.hidden = false; });

    const titleBlock = document.createElement('div');
    titleBlock.className = 'station-card__title';
    titleBlock.append(makeElement('h3', '', station.name), makeElement('p', '', stationTags(station)));

    const favorite = makeElement('button', `icon-btn${state.favorites.has(station.id) ? ' active' : ''}`, state.favorites.has(station.id) ? '\u2665' : '\u2661');
    favorite.type = 'button';
    favorite.dataset.action = 'favorite';
    favorite.dataset.id = station.id;
    favorite.setAttribute('aria-label', `Favorite ${station.name}`);
    top.append(thumb, thumbFallback, titleBlock, favorite);

    const badges = makeElement('div', 'station-badges');
    (station.categories || []).filter(Boolean).slice(0, featured ? 3 : 4).forEach((category) => badges.append(makeElement('span', '', category)));

    const footer = makeElement('div', 'station-card__footer');
    footer.append(makeElement('span', '', featured ? (station.verified ? 'Verified' : 'Community') : (station.country || 'India')));
    const play = makeElement('button', 'secondary-btn', state.currentStation?.id === station.id && state.playing ? 'Stop' : 'Play');
    play.type = 'button';
    play.dataset.action = 'play';
    play.dataset.id = station.id;
    footer.append(play);

    card.append(top, badges, footer);
    return card;
  }

  function renderFilters() {
    const categories = ['all', ...new Set(state.stations.flatMap((station) => [station.language, ...(station.categories || [])]))]
      .filter(Boolean)
      .filter((category) => String(category).toLowerCase() !== 'all')
      .sort((first, second) => String(first).localeCompare(String(second)));
    elements.filters.replaceChildren(...categories.map((category) => {
      const button = makeElement('button', `pill${state.activeCategory.toLowerCase() === String(category).toLowerCase() ? ' active' : ''}`, category === 'all' ? 'All' : category);
      button.type = 'button';
      button.dataset.category = category;
      return button;
    }));
  }

  function renderStationLists() {
    const favoriteStations = state.stations.filter((s) => state.favorites.has(s.id));
    if (favoriteStations.length) {
      elements.favoritesSection.hidden = false;
      elements.featured.replaceChildren(...favoriteStations.map((station) => createStationCard(station, true)));
    } else {
      elements.favoritesSection.hidden = true;
    }
    elements.stations.replaceChildren(...(state.filteredStations.length ? state.filteredStations.map((station) => createStationCard(station, false)) : [makeElement('div', 'empty-state', 'No stations match this search yet. Try a different keyword.')]));
    renderRecent();
  }

  function applyFilters() {
    state.filteredStations = state.stations.filter(stationMatches).sort((first, second) => first.name.localeCompare(second.name));
    renderFilters();
    renderStationLists();
    elements.resultsCount.textContent = `${state.filteredStations.length} station${state.filteredStations.length === 1 ? '' : 's'} shown`;
  }

  function saveFavorites() {
    localStorage.setItem('openradio-favorites', JSON.stringify([...state.favorites]));
  }

  /* ---------- Cast ---------- */

  function isCasting() {
    return Boolean(castContext && window.cast && castContext.getCastState() === cast.framework.CastState.CONNECTED);
  }

  function streamContentType(stream, useProxy) {
    if (useProxy) return 'audio/mpeg';
    const codec = String(stream.codec || '').toLowerCase();
    if (codec === 'hls' || stream.url.includes('.m3u8')) return 'application/vnd.apple.mpegurl';
    if (codec === 'aac') return 'audio/aac';
    if (codec === 'ogg') return 'audio/ogg';
    return 'audio/mpeg';
  }

  async function castStation(station) {
    const streams = [...(station.streams || [])].filter((s) => s.url).sort((a, b) => (a.priority || Infinity) - (b.priority || Infinity));
    if (!streams.length) return;
    const stream = streams[0];
    const isHls = String(stream.codec || '').toLowerCase() === 'hls' || stream.url.includes('.m3u8');
    const useProxy = isHls && HLS_PROXY_URL;
    const contentType = streamContentType(stream, useProxy);
    const castUrl = useProxy ? `${HLS_PROXY_URL}?url=${encodeURIComponent(stream.url)}&contentType=${encodeURIComponent(contentType)}` : stream.url;

    state.currentStation = station;
    if (state.hls) { state.hls.destroy(); state.hls = null; }
    elements.audio.pause();
    elements.audio.src = '';

    const session = castContext?.getCurrentSession();
    if (!session) {
      setStatus('No Cast session available');
      return;
    }

    async function loadOnCast(ct) {
      const media = new chrome.cast.media.MediaInfo(castUrl, ct);
      media.streamType = chrome.cast.media.StreamType.LIVE;
      media.metadata = new chrome.cast.media.MusicTrackMediaMetadata();
      media.metadata.title = station.name;
      media.metadata.artist = station.language || 'OpenRadio-IN';
      if (station.logo) media.metadata.images = [new chrome.cast.Image(station.logo)];
      await session.loadMedia(new chrome.cast.media.LoadRequest(media));
    }

    try {
      await loadOnCast(contentType);
      state.playing = true;
      setStatus(`Casting ${station.name}`);
    } catch (error) {
      state.playing = false;
      console.error('Cast loadMedia error:', error.message || error);
      if (isHls && !useProxy) {
        setStatus(`Cast blocked for this HLS stream. Deploy the proxy worker (see hls-proxy-worker.js) and set HLS_PROXY_URL in app.js.`);
      } else {
        setStatus(`Cast error: ${error.message || 'Unable to cast'}`);
      }
    }
    updatePlayer();
    renderStationLists();
  }

  function setupCastPlayer() {
    if (castPlayerController) return;
    castPlayer = new cast.framework.RemotePlayer();
    castPlayerController = new cast.framework.RemotePlayerController(castPlayer);
    castPlayerController.addEventListener(cast.framework.RemotePlayerEventType.IS_PAUSED_CHANGED, () => {
      state.playing = !castPlayer.isPaused;
      updatePlayer();
      renderStationLists();
    });
  }

  function initializeCast() {
    if (typeof window.cast === 'undefined' || castContext) return;
    castContext = cast.framework.CastContext.getInstance();
    const appId = HLS_PROXY_URL ? chrome.cast.media.DEFAULT_MEDIA_RECEIVER_APP_ID : CUSTOM_CAST_APP_ID;
    castContext.setOptions({ receiverApplicationId: appId, autoJoinPolicy: chrome.cast.AutoJoinPolicy.TAB_AND_ORIGIN_SCOPED });
    castContext.addEventListener(cast.framework.CastContextEventType.CAST_STATE_CHANGED, (event) => {
      switch (event.castState) {
        case cast.framework.CastState.CONNECTED:
          setupCastPlayer();
          if (state.currentStation) castStation(state.currentStation);
          break;
        case cast.framework.CastState.NOT_CONNECTED:
        case cast.framework.CastState.NO_DEVICES_AVAILABLE:
          castPlayer = undefined;
          castPlayerController = undefined;
          state.playing = false;
          break;
      }
      updatePlayer();
    });
  }

  /* ---------- Playback & Retry ---------- */

  async function playStation(station) {
    const streams = [...(station.streams || [])].filter((stream) => stream.url).sort((first, second) => (first.priority || Infinity) - (second.priority || Infinity));
    if (!streams.length) {
      setStatus('No stream available');
      return;
    }

    if (isCasting()) {
      await castStation(station);
      return;
    }

    state.currentStation = station;
    state.retryCount = 0;
    state.userInitiatedStop = false;
    const stream = streams[0];

    if (state.hls) {
      state.hls.destroy();
      state.hls = null;
    }

    const isHls = (stream.codec || '').toLowerCase() === 'hls' || stream.url.includes('.m3u8');

    if (isHls) {
      if (!window.Hls || !window.Hls.isSupported()) {
        setStatus('HLS playback not supported in this browser');
        updatePlayer();
        renderStationLists();
        return;
      }
      elements.audio.src = '';
      setStatus('Loading HLS stream...');
      state.hls = new window.Hls({ startLevel: 0 });
      state.hls.loadSource(stream.url);
      state.hls.attachMedia(elements.audio);
      try {
        await new Promise((resolve, reject) => {
          const timeout = setTimeout(() => reject(new Error('HLS load timed out')), 20000);
          state.hls.on(window.Hls.Events.MANIFEST_PARSED, () => { clearTimeout(timeout); resolve(); });
          state.hls.on(window.Hls.Events.ERROR, (event, data) => {
            console.warn('HLS error:', data.type, data.details);
            if (data.fatal) { clearTimeout(timeout); reject(new Error(data.type)); }
          });
        });
      } catch (error) {
        state.hls.destroy();
        state.hls = null;
        state.playing = false;
        setStatus('Unable to load this stream');
        console.error(error);
        updatePlayer();
        renderStationLists();
        return;
      }
      try {
        await elements.audio.play();
      } catch (error) {
        state.hls.destroy();
        state.hls = null;
        state.playing = false;
        setStatus('Unable to start playback');
        console.error(error);
        updatePlayer();
        renderStationLists();
        return;
      }
      state.playing = true;
      setStatus(`Playing ${station.name}`);
      localStorage.setItem('openradio-last-station', station.id);
      addRecentStation(station);
      startMetadataPolling(stream.url);
      updatePlayer();
      renderStationLists();
      return;
    }

    elements.audio.src = stream.url;
    elements.audio.load();
    try {
      await elements.audio.play();
      state.playing = true;
      setStatus(`Playing ${station.name}`);
      localStorage.setItem('openradio-last-station', station.id);
      addRecentStation(station);
      startMetadataPolling(stream.url);
    } catch (error) {
      state.playing = false;
      setStatus('Unable to start this stream');
      console.error(error);
    }
    updatePlayer();
    renderStationLists();
  }

  function retryPlayback() {
    if (!state.currentStation || state.retryCount >= state.maxRetries) {
      state.retryCount = 0;
      return;
    }
    state.retryCount++;
    const delay = Math.min(1000 * Math.pow(2, state.retryCount), 15000);
    setStatus(`Retrying in ${Math.round(delay / 1000)}s (${state.retryCount}/${state.maxRetries})...`);
    setTimeout(() => playStation(state.currentStation), delay);
  }

  function stopPlayback() {
    state.userInitiatedStop = true;
    if (state.hls) { state.hls.destroy(); state.hls = null; }
    elements.audio.pause();
    elements.audio.src = '';
    state.playing = false;
    state.nowPlayingTrack = '';
    elements.nowPlayingTrack.hidden = true;
    stopMetadataPolling();
    updatePlayer();
    renderStationLists();
  }

  async function togglePlayback() {
    if (!state.currentStation) return;
    if (isCasting()) {
      if (castPlayerController) castPlayerController.playOrPause();
      return;
    }
    if (state.playing) {
      stopPlayback();
      return;
    }
    await playStation(state.currentStation);
  }

  function playAdjacentStation(direction) {
    if (!state.currentStation) return;
    const list = state.currentSource === 'favorites'
      ? state.stations.filter((s) => state.favorites.has(s.id))
      : state.filteredStations;
    if (list.length < 2) return;
    const currentIndex = list.findIndex((s) => s.id === state.currentStation.id);
    if (currentIndex === -1) return;
    const nextIndex = (currentIndex + direction + list.length) % list.length;
    playStation(list[nextIndex]);
  }

  async function handleStationAction(event) {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const station = state.stations.find((entry) => entry.id === button.dataset.id);
    if (!station) return;
    if (button.dataset.action === 'favorite') {
      state.favorites.has(station.id) ? state.favorites.delete(station.id) : state.favorites.add(station.id);
      saveFavorites();
      renderStationLists();
      return;
    }
    state.currentSource = event.currentTarget === elements.featured || event.currentTarget === elements.recentStations ? 'favorites' : 'all';
    if (state.currentStation?.id === station.id && state.playing) await togglePlayback();
    else await playStation(station);
  }

  /* ---------- Now Playing Sheet ---------- */

  function openNowPlaying() {
    elements.nowPlaying.hidden = false;
    document.body.style.overflow = 'hidden';
  }

  function closeNowPlaying() {
    elements.nowPlaying.hidden = true;
    document.body.style.overflow = '';
  }

  /* ---------- Sleep Timer ---------- */

  function setSleepTimer(minutes) {
    if (state.sleepTimerId) {
      clearTimeout(state.sleepTimerId);
      state.sleepTimerId = null;
    }
    state.sleepTimerEnd = null;
    if (minutes <= 0) {
      elements.sleepTimerPicker.hidden = true;
      elements.sleepTimerStatus.hidden = true;
      elements.sleepTimerBtn.textContent = '\u23F0 Timer';
      return;
    }
    state.sleepTimerEnd = Date.now() + minutes * 60 * 1000;
    state.sleepTimerId = setTimeout(() => {
      stopPlayback();
      state.sleepTimerId = null;
      state.sleepTimerEnd = null;
      elements.sleepTimerStatus.hidden = true;
      elements.sleepTimerBtn.textContent = '\u23F0 Timer';
      setStatus('Sleep timer: playback stopped');
      updatePlayer();
    }, minutes * 60 * 1000);
    elements.sleepTimerPicker.hidden = true;
    elements.sleepTimerStatus.hidden = false;
    const mins = minutes >= 60 ? `${Math.floor(minutes / 60)}h ${minutes % 60}m` : `${minutes}m`;
    elements.sleepTimerStatus.textContent = `Sleeping in ${mins}`;
    elements.sleepTimerBtn.textContent = `\u23F0 ${mins}`;
  }

  /* ---------- Share ---------- */

  async function shareStation() {
    const station = state.currentStation;
    if (!station) return;
    const shareData = {
      title: station.name,
      text: `Listen to ${station.name} on OpenRadio-IN`,
      url: `${window.location.origin}${window.location.pathname}?station=${station.id}`
    };
    if (navigator.share) {
      try { await navigator.share(shareData); } catch {}
    } else {
      try { await navigator.clipboard.writeText(shareData.url); setStatus('Link copied!'); } catch {}
    }
  }

  /* ---------- Stream Metadata ---------- */

  function fetchStreamMetadata(streamUrl) {
    const metaUrl = `${HLS_PROXY_URL}?meta=1&url=${encodeURIComponent(streamUrl)}`;
    fetch(metaUrl, { signal: AbortSignal.timeout(5000) })
      .then((res) => res.json())
      .then((data) => {
        if (data.streamTitle) {
          state.nowPlayingTrack = data.streamTitle;
          elements.nowPlayingTrack.textContent = state.nowPlayingTrack;
          elements.nowPlayingTrack.hidden = false;
        }
      })
      .catch(() => {});
  }

  function startMetadataPolling(url) {
    stopMetadataPolling();
    fetchStreamMetadata(url);
    state.metadataIntervalId = setInterval(() => fetchStreamMetadata(url), 30000);
  }

  function stopMetadataPolling() {
    if (state.metadataIntervalId) {
      clearInterval(state.metadataIntervalId);
      state.metadataIntervalId = null;
    }
  }

  /* ---------- Keyboard Shortcuts ---------- */

  function handleKeydown(e) {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
    if (!elements.nowPlaying.hidden && e.key === 'Escape') { closeNowPlaying(); return; }
    switch (e.key) {
      case ' ':
        e.preventDefault();
        togglePlayback();
        break;
      case 'ArrowLeft':
        playAdjacentStation(-1);
        break;
      case 'ArrowRight':
        playAdjacentStation(1);
        break;
    }
  }

  /* ---------- Load Stations ---------- */

  async function loadStations() {
    try {
      let response = await fetch(DATA_URL, { cache: 'no-store' });
      if (!response.ok) response = await fetch('../database/stations.json', { cache: 'no-store' });
      if (!response.ok) throw new Error(`Station data request failed: ${response.status}`);
      state.stations = (await response.json()).filter(Boolean);
      const lastStationId = localStorage.getItem('openradio-last-station');
      state.currentStation = state.stations.find((station) => station.id === lastStationId) || null;
      applyFilters();
      updatePlayer();
      setStatus(`${state.stations.length} stations loaded`);
    } catch (error) {
      console.error(error);
      setStatus('Unable to load station data');
      elements.resultsCount.textContent = 'Station data is unavailable right now.';
      elements.stations.replaceChildren(makeElement('div', 'empty-state', 'The station database could not be loaded. Please refresh and try again.'));
    }
  }

  /* ---------- Init ---------- */

  setTheme(state.theme);
  applyVolume(state.volume);
  setupMediaSession();
  restoreCollapsedStates();

  elements.search.addEventListener('input', (event) => {
    state.search = event.target.value;
    applyFilters();
  });
  elements.filters.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-category]');
    if (!button) return;
    state.activeCategory = button.dataset.category;
    applyFilters();
  });
  elements.featured.addEventListener('click', handleStationAction);
  elements.recentStations.addEventListener('click', handleStationAction);
  elements.stations.addEventListener('click', handleStationAction);
  elements.playToggle.addEventListener('click', togglePlayback);
  elements.prevBtn.addEventListener('click', () => playAdjacentStation(-1));
  elements.nextBtn.addEventListener('click', () => playAdjacentStation(1));
  elements.npPlayToggle.addEventListener('click', togglePlayback);
  elements.npPrev.addEventListener('click', () => playAdjacentStation(-1));
  elements.npNext.addEventListener('click', () => playAdjacentStation(1));

  document.querySelector('.app-shell').addEventListener('click', (e) => {
    const sectionHead = e.target.closest('.section-head');
    if (!sectionHead) return;
    const section = sectionHead.closest('.card-section');
    if (!section) return;
    toggleSection(section.id);
  });

  elements.playerBar.addEventListener('click', (e) => {
    if (e.target.closest('button') || e.target.closest('input')) return;
    openNowPlaying();
  });
  elements.nowPlayingBackdrop.addEventListener('click', closeNowPlaying);
  elements.nowPlayingClose.addEventListener('click', closeNowPlaying);

  elements.volumeSlider.addEventListener('input', (e) => { state.muted = false; applyVolume(parseFloat(e.target.value)); updateVolumeIcon(); });
  elements.npVolumeSlider.addEventListener('input', (e) => { state.muted = false; applyVolume(parseFloat(e.target.value)); updateVolumeIcon(); });
  elements.volumeBtn.addEventListener('click', toggleMute);
  elements.npVolumeBtn.addEventListener('click', toggleMute);

  elements.shareBtn.addEventListener('click', shareStation);

  elements.sleepTimerBtn.addEventListener('click', () => {
    elements.sleepTimerPicker.hidden = !elements.sleepTimerPicker.hidden;
  });
  elements.sleepTimerPicker.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-minutes]');
    if (!btn) return;
    setSleepTimer(parseInt(btn.dataset.minutes, 10));
  });

  elements.themeToggle.addEventListener('click', toggleTheme);

  elements.audio.addEventListener('play', () => {
    state.playing = true;
    state.retryCount = 0;
    updatePlayer();
    renderStationLists();
  });
  elements.audio.addEventListener('pause', () => { state.playing = false; updatePlayer(); renderStationLists(); });
  elements.audio.addEventListener('ended', () => {
    if (state.hls) { state.hls.destroy(); state.hls = null; }
    state.playing = false;
    state.nowPlayingTrack = '';
    elements.nowPlayingTrack.hidden = true;
    stopMetadataPolling();
    setStatus('Playback ended');
    updatePlayer();
    renderStationLists();
  });
  elements.audio.addEventListener('error', () => {
    if (state.hls) { state.hls.destroy(); state.hls = null; }
    state.playing = false;
    state.nowPlayingTrack = '';
    elements.nowPlayingTrack.hidden = true;
    stopMetadataPolling();
    if (state.currentStation && !state.userInitiatedStop) retryPlayback();
    state.userInitiatedStop = false;
    updatePlayer();
    renderStationLists();
  });

  document.addEventListener('keydown', handleKeydown);

  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault();
    installPrompt = event;
    elements.install.hidden = false;
  });
  elements.install.addEventListener('click', async () => {
    if (!installPrompt) return;
    installPrompt.prompt();
    await installPrompt.userChoice;
    installPrompt = null;
    elements.install.hidden = true;
  });
  window.addEventListener('appinstalled', () => { elements.install.hidden = true; setStatus('App installed'); });

  window.addEventListener('openradio-cast-api', (event) => {
    if (event.detail) initializeCast();
  });
  if (window.__castApiAvailable) initializeCast();

  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('./sw.js').then((reg) => {
        reg.addEventListener('updatefound', () => {
          const newWorker = reg.installing;
          newWorker.addEventListener('statechange', () => {
            if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
              elements.updateBanner.hidden = false;
            }
          });
        });
      }).catch(console.error);
    });
    elements.updateBtn.addEventListener('click', () => {
      elements.updateBanner.hidden = true;
      window.location.reload();
    });
  }

  loadStations();
})();
