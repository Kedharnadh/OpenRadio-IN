(() => {
  const DATA_URL = './data/stations.json';
  const state = {
    stations: [],
    filteredStations: [],
    favorites: new Set(JSON.parse(localStorage.getItem('openradio-favorites') || '[]')),
    activeCategory: 'all',
    search: '',
    currentStation: null,
    playing: false
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
    install: document.getElementById('install-app'),
    cast: document.getElementById('cast-button'),
    audio: document.getElementById('audio-player')
  };
  let installPrompt;
  let castContext;
  let castPlayer;
  let castPlayerController;

  // Custom Cast receiver App ID registered in Google Cast SDK Console.
  // The receiver (cast-receiver.html) handles HLS audio-only streams
  // by setting the correct hlsSegmentFormat for TS segments with AAC.
  const CUSTOM_CAST_APP_ID = '45881BB0'; // e.g. 'ABCD1234'

  // Set this to your deployed Cloudflare Worker URL when using HLS proxy.
  // Deploy hls-proxy-worker.js to https://workers.cloudflare.com (free tier).
  // Example: const HLS_PROXY_URL = 'https://hls-proxy.username.workers.dev';
  const HLS_PROXY_URL = '';

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

  function updatePlayer() {
    const station = state.currentStation;
    const hasMultiple = state.filteredStations.length > 1;
    if (!station) {
      elements.playerTitle.textContent = 'Choose a station';
      elements.playerMeta.textContent = 'Your selected radio station will appear here.';
      elements.playToggle.disabled = true;
      elements.playToggle.textContent = '\u25b6 Play';
      elements.prevBtn.disabled = true;
      elements.nextBtn.disabled = true;
      return;
    }

    elements.playerTitle.textContent = station.name;
    const destination = isCasting() ? 'Casting' : (station.streams?.[0]?.codec || 'Stream');
    elements.playerMeta.textContent = `${station.language || 'Unknown language'} \u2022 ${destination}`;
    elements.playToggle.disabled = false;
    elements.playToggle.textContent = state.playing ? '\u23f9 Stop' : '\u25b6 Play';
    elements.prevBtn.disabled = !hasMultiple;
    elements.nextBtn.disabled = !hasMultiple;
  }

  function createStationCard(station, featured) {
    const card = makeElement('article', `station-card${featured ? ' featured-card' : ''}`);
    const top = makeElement('div', 'station-card__top');
    const titleBlock = document.createElement('div');
    titleBlock.append(makeElement('h3', '', station.name), makeElement('p', '', stationTags(station)));

    const favorite = makeElement('button', `icon-btn${state.favorites.has(station.id) ? ' active' : ''}`, state.favorites.has(station.id) ? '\u2665' : '\u2661');
    favorite.type = 'button';
    favorite.dataset.action = 'favorite';
    favorite.dataset.id = station.id;
    favorite.setAttribute('aria-label', `Favorite ${station.name}`);
    top.append(titleBlock, favorite);

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
    const stream = [...(station.streams || [])].filter((entry) => entry.url).sort((first, second) => (first.priority || Infinity) - (second.priority || Infinity))[0];
    const session = castContext?.getCurrentSession();
    if (!stream || !session) return;

    const isHls = String(stream.codec || '').toLowerCase() === 'hls' || stream.url.includes('.m3u8');

    // Use the HLS proxy for audio-only HLS streams: the proxy fetches HLS
    // segments and streams them as continuous audio/mpeg, which the
    // Default Media Receiver can play reliably.
    const useProxy = isHls && HLS_PROXY_URL;
    const castUrl = useProxy ? `${HLS_PROXY_URL}?url=${encodeURIComponent(stream.url)}` : stream.url;

    const media = new chrome.cast.media.MediaInfo(castUrl, streamContentType(stream, useProxy));
    media.streamType = chrome.cast.media.StreamType.LIVE;
    const metadata = new chrome.cast.media.MusicTrackMediaMetadata();
    metadata.title = station.name;
    metadata.artist = station.language || 'OpenRadio-IN';
    if (station.logo) metadata.images = [new chrome.cast.Image(station.logo)];
    media.metadata = metadata;

    state.currentStation = station;
    elements.audio.pause();
    try {
      await session.loadMedia(new chrome.cast.media.LoadRequest(media));
      state.playing = true;
      setStatus(`Casting ${station.name}`);
    } catch (error) {
      state.playing = false;
      setStatus('Unable to cast this stream');
      console.error(error);
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
    if (!window.cast || castContext) return;
    castContext = cast.framework.CastContext.getInstance();
    // When the HLS proxy is configured, use the Default Media Receiver
    // since the proxy delivers plain audio/mpeg. Otherwise use the custom
    // receiver for native HLS handling.
    const appId = HLS_PROXY_URL ? chrome.cast.media.DEFAULT_MEDIA_RECEIVER_APP_ID : CUSTOM_CAST_APP_ID;
    castContext.setOptions({ receiverApplicationId: appId, autoJoinPolicy: chrome.cast.AutoJoinPolicy.ORIGIN_SCOPED });
    castContext.addEventListener(cast.framework.CastContextEventType.CAST_STATE_CHANGED, (event) => {
      if (event.castState === cast.framework.CastState.CONNECTED) {
        setupCastPlayer();
        if (state.currentStation) castStation(state.currentStation);
      }
      if (event.castState === cast.framework.CastState.NOT_CONNECTED || event.castState === cast.framework.CastState.NO_DEVICES_AVAILABLE) {
        castPlayer = undefined;
        castPlayerController = undefined;
      }
      updatePlayer();
    });
  }

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
    } catch (error) {
      state.playing = false;
      setStatus('Unable to start this stream');
      console.error(error);
    }
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
      if (state.hls) {
        state.hls.destroy();
        state.hls = null;
      }
      elements.audio.pause();
      elements.audio.src = '';
      return;
    }
    await playStation(state.currentStation);
  }

  function playAdjacentStation(direction) {
    if (!state.currentStation || state.filteredStations.length < 2) return;
    const currentIndex = state.filteredStations.findIndex((s) => s.id === state.currentStation.id);
    if (currentIndex === -1) return;
    const nextIndex = (currentIndex + direction + state.filteredStations.length) % state.filteredStations.length;
    playStation(state.filteredStations[nextIndex]);
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
    if (state.currentStation?.id === station.id && state.playing) await togglePlayback();
    else await playStation(station);
  }

  async function loadStations() {
    try {
      let response = await fetch(DATA_URL, { cache: 'no-store' });
      // This fallback keeps the checked-out repository usable before the Pages
      // workflow copies the database into website/data.
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
  elements.stations.addEventListener('click', handleStationAction);
  elements.playToggle.addEventListener('click', togglePlayback);
  elements.prevBtn.addEventListener('click', () => playAdjacentStation(-1));
  elements.nextBtn.addEventListener('click', () => playAdjacentStation(1));
  elements.audio.addEventListener('play', () => { state.playing = true; updatePlayer(); renderStationLists(); });
  elements.audio.addEventListener('pause', () => { state.playing = false; updatePlayer(); renderStationLists(); });
  elements.audio.addEventListener('ended', () => {
    if (state.hls) { state.hls.destroy(); state.hls = null; }
    state.playing = false;
    setStatus('Playback ended');
    updatePlayer();
    renderStationLists();
  });
  elements.audio.addEventListener('error', () => {
    if (state.hls) { state.hls.destroy(); state.hls = null; }
    state.playing = false;
    setStatus('Unable to stream this station');
    updatePlayer();
    renderStationLists();
  });

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
    window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js').catch(console.error));
  }

  loadStations();
})();
