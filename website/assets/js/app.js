(() => {
  const state = {
    stations: [],
    filteredStations: [],
    favorites: new Set(JSON.parse(localStorage.getItem('openradio-favorites') || '[]')),
    activeCategory: 'all',
    search: '',
    currentStation: null,
    playing: false
  };

  const searchInput = document.getElementById('search');
  const filtersContainer = document.getElementById('filters');
  const resultsCount = document.getElementById('results-count');
  const featuredContainer = document.getElementById('featured');
  const stationsContainer = document.getElementById('stations');
  const playToggle = document.getElementById('play-toggle');
  const playerTitle = document.getElementById('player-title');
  const playerMeta = document.getElementById('player-meta');
  const statusPill = document.getElementById('status-pill');
  const audio = document.getElementById('audio-player');

  function updateStatus(text) {
    statusPill.textContent = text;
  }

  function updatePlayerUI() {
    if (!state.currentStation) {
      playerTitle.textContent = 'Choose a station';
      playerMeta.textContent = 'Your selected radio station will appear here.';
      playToggle.disabled = true;
      playToggle.textContent = '▶ Play';
      return;
    }

    playerTitle.textContent = state.currentStation.name;
    const streamLabel = state.currentStation.streams?.[0]?.codec || 'Stream';
    playerMeta.textContent = `${state.currentStation.language || 'Unknown language'} • ${streamLabel}`;
    playToggle.disabled = false;
    playToggle.textContent = state.playing ? '⏸ Pause' : '▶ Play';
  }

  function getStationLabel(station) {
    const categories = (station.categories || []).filter(Boolean);
    const tags = [...new Set([station.language, ...categories].filter(Boolean))].slice(0, 3);
    return tags.join(' • ');
  }

  function matchesCategory(station, category) {
    if (category === 'all') return true;
    const target = category.toLowerCase();
    return [station.language, ...(station.categories || [])].some((value) => String(value).toLowerCase() === target);
  }

  function applyFilters() {
    const query = state.search.trim().toLowerCase();
    state.filteredStations = state.stations.filter((station) => {
      const haystack = [station.name, station.language, station.country, station.state, station.city, ...(station.categories || [])]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return haystack.includes(query) && matchesCategory(station, state.activeCategory);
    });

    state.filteredStations.sort((a, b) => a.name.localeCompare(b.name));
    renderFilters();
    renderFeatured();
    renderStations();
    resultsCount.textContent = `${state.filteredStations.length} station${state.filteredStations.length === 1 ? '' : 's'} shown`;
  }

  function renderFilters() {
    const categories = ['all', ...new Set(state.stations.flatMap((station) => [station.language, ...(station.categories || [])]))]
      .filter(Boolean)
      .filter((value) => value !== 'ALL')
      .sort((a, b) => a.localeCompare(b));

    filtersContainer.innerHTML = categories
      .map((category) => {
        const label = category === 'all' ? 'All' : category;
        const activeClass = state.activeCategory.toLowerCase() === category.toLowerCase() ? 'active' : '';
        return `<button class="pill ${activeClass}" data-category="${category}">${label}</button>`;
      })
      .join('');
  }

  function renderFeatured() {
    const featured = state.filteredStations.slice(0, 6);
    if (!featured.length) {
      featuredContainer.innerHTML = '<div class="empty-state">No stations match this search yet.</div>';
      return;
    }

    featuredContainer.innerHTML = featured
      .map((station) => {
        const isFavorite = state.favorites.has(station.id);
        const isCurrent = state.currentStation?.id === station.id;
        const buttonLabel = isCurrent && state.playing ? 'Pause' : 'Play';
        return `
          <article class="station-card featured-card">
            <div class="station-card__top">
              <div>
                <h3>${station.name}</h3>
                <p>${getStationLabel(station)}</p>
              </div>
              <button class="icon-btn ${isFavorite ? 'active' : ''}" data-action="favorite" data-id="${station.id}" aria-label="Favorite ${station.name}">${isFavorite ? '♥' : '♡'}</button>
            </div>
            <div class="station-badges">${(station.categories || []).slice(0, 3).map((category) => `<span>${category}</span>`).join('')}</div>
            <div class="station-card__footer">
              <span>${station.verified ? 'Verified' : 'Community'}</span>
              <button class="secondary-btn" data-action="play" data-id="${station.id}">${buttonLabel}</button>
            </div>
          </article>`;
      })
      .join('');
  }

  function renderStations() {
    if (!state.filteredStations.length) {
      stationsContainer.innerHTML = '<div class="empty-state">No stations match this search yet. Try a different keyword.</div>';
      return;
    }

    stationsContainer.innerHTML = state.filteredStations
      .map((station) => {
        const isFavorite = state.favorites.has(station.id);
        const isCurrent = state.currentStation?.id === station.id;
        const buttonLabel = isCurrent && state.playing ? 'Pause' : 'Play';
        return `
          <article class="station-card">
            <div class="station-card__top">
              <div>
                <h3>${station.name}</h3>
                <p>${getStationLabel(station)}</p>
              </div>
              <button class="icon-btn ${isFavorite ? 'active' : ''}" data-action="favorite" data-id="${station.id}" aria-label="Favorite ${station.name}">${isFavorite ? '♥' : '♡'}</button>
            </div>
            <div class="station-badges">${(station.categories || []).slice(0, 4).map((category) => `<span>${category}</span>`).join('')}</div>
            <div class="station-card__footer">
              <span>${station.country || 'India'}</span>
              <button class="secondary-btn" data-action="play" data-id="${station.id}">${buttonLabel}</button>
            </div>
          </article>`;
      })
      .join('');
  }

  function persistFavorites() {
    localStorage.setItem('openradio-favorites', JSON.stringify([...state.favorites]));
  }

  function toggleFavorite(stationId) {
    if (state.favorites.has(stationId)) {
      state.favorites.delete(stationId);
    } else {
      state.favorites.add(stationId);
    }
    persistFavorites();
    applyFilters();
  }

  async function playStation(station) {
    if (!station?.streams?.length) {
      updateStatus('No stream available');
      playerMeta.textContent = 'This station does not have a playable stream yet.';
      return;
    }

    state.currentStation = station;
    const stream = station.streams[0];
    audio.src = stream.url;
    audio.load();

    try {
      await audio.play();
      state.playing = true;
      updateStatus(`Playing ${station.name}`);
      localStorage.setItem('openradio-last-station', station.id);
    } catch (error) {
      state.playing = false;
      updateStatus('Playback blocked');
      playerMeta.textContent = 'The browser blocked playback. Please tap again or use a supported audio stream.';
      console.error(error);
    }

    updatePlayerUI();
  }

  async function togglePlayback() {
    if (!state.currentStation) return;
    if (state.playing) {
      audio.pause();
      state.playing = false;
      updateStatus(`Paused ${state.currentStation.name}`);
      updatePlayerUI();
      return;
    }

    await playStation(state.currentStation);
  }

  async function handleCardClick(event) {
    const button = event.target.closest('button');
    if (!button) return;

    const stationId = button.getAttribute('data-id');
    const action = button.getAttribute('data-action');
    const station = state.stations.find((entry) => entry.id === stationId);

    if (!station) return;

    if (action === 'favorite') {
      toggleFavorite(station.id);
      return;
    }

    if (action === 'play') {
      if (state.currentStation?.id === station.id && state.playing) {
        await togglePlayback();
      } else {
        await playStation(station);
      }
    }
  }

  searchInput.addEventListener('input', (event) => {
    state.search = event.target.value;
    applyFilters();
  });

  filtersContainer.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-category]');
    if (!button) return;
    state.activeCategory = button.getAttribute('data-category');
    applyFilters();
  });

  featuredContainer.addEventListener('click', handleCardClick);
  stationsContainer.addEventListener('click', handleCardClick);
  playToggle.addEventListener('click', togglePlayback);

  audio.addEventListener('pause', () => {
    state.playing = false;
    updatePlayerUI();
  });

  audio.addEventListener('play', () => {
    state.playing = true;
    updatePlayerUI();
  });

  audio.addEventListener('error', () => {
    state.playing = false;
    updateStatus('Unable to stream this station');
    updatePlayerUI();
  });

  window.addEventListener('online', () => updateStatus('Online • ready to stream'));
  window.addEventListener('offline', () => updateStatus('Offline • cached shell available'));

  async function loadStations() {
    try {
      const response = await fetch('../database/stations.json', { cache: 'no-store' });
      if (!response.ok) throw new Error('Stations data could not be loaded');
      const stations = await response.json();
      state.stations = stations.filter(Boolean);
      const lastStationId = localStorage.getItem('openradio-last-station');
      state.currentStation = state.stations.find((station) => station.id === lastStationId) || null;
      applyFilters();
      updateStatus(`${state.stations.length} stations loaded`);
      updatePlayerUI();
    } catch (error) {
      console.error(error);
      updateStatus('Unable to load station data');
      resultsCount.textContent = 'Station data is unavailable right now.';
      stationsContainer.innerHTML = '<div class="empty-state">The station database could not be loaded. Please refresh or check the repository files.</div>';
    }
  }

  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('./sw.js').catch((error) => console.error('Service worker registration failed', error));
    });
  }

  loadStations();
})();
