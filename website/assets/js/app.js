(() => {
  const DATA_URL = './data/stations.json';

  const I18N = {
    en: {
      'app.eyebrow': 'Community radio \u2022 PWA',
      'app.tagline': 'Discover and play Indian online radio stations with instant search, filters, and a polished app-style experience.',
      'search.placeholder': 'Search stations, language, or category',
      'install': 'Install app',
      'section.recent': 'Recently Played',
      'section.favorites': 'Favorite Stations',
      'section.all': 'All stations',
      'filter.all': 'All',
      'filter.allLanguages': 'All languages',
      'player.defaultTitle': 'Choose a station',
      'player.defaultMeta': 'Your selected radio station will appear here.',
      'controls.play': 'Play',
      'controls.resume': 'Resume',
      'controls.pause': 'Pause',
      'controls.stop': 'Stop',
      'controls.previous': 'Previous',
      'controls.next': 'Next',
      'controls.volume': 'Mute/unmute',
      'np.title': 'Now Playing',
      'np.share': 'Share',
      'np.timer': 'Timer',
      'np.alarm': 'Alarm',
      'np.sleeptimer': 'Sleeping in {time}',
      'np.now': 'Now',
      'np.next': 'Next',
      'np.noSchedule': 'No schedule available',
      'theme.light': 'Light',
      'theme.dark': 'Dark',
      'theme.auto': 'Auto',
      'theme.toggle': 'Toggle theme',
      'update.available': 'Update available',
      'update.refresh': 'Refresh',
      'status.loading': 'Loading stations\u2026',
      'status.loaded': '{n} stations loaded',
      'status.playing': 'Playing {name}',
      'status.resumed': 'Playback resumed',
      'status.playingCast': 'Casting {name}',
      'status.casting': 'Casting',
      'status.noCastSession': 'No Cast session available',
      'status.castBlocked': 'Cast blocked for this HLS stream. Deploy the proxy worker and set HLS_PROXY_URL in app.js.',
      'status.castError': 'Cast error: {error}',
      'status.appInstalled': 'App installed',
      'player.unknownLanguage': 'Unknown language',
      'player.stream': 'Stream',
      'share.text': 'Listen to {name} on OpenRadio-IN',
      'status.retrying': 'Retrying in {sec}s ({attempt}/{max})\u2026',
      'status.noStream': 'No stream available',
      'status.hlsUnsupported': 'HLS playback not supported in this browser',
      'status.loadingHls': 'Loading HLS stream\u2026',
      'status.loadFailed': 'Unable to load this stream',
      'status.tryingBackup': 'Main stream failed \u2014 trying backup\u2026',
      'status.playFailed': 'Unable to start playback',
      'status.streamFailed': 'Unable to start this stream',
      'status.ended': 'Playback ended',
      'status.error': 'Unable to load station data',
      'status.dataUnavailable': 'Station data is unavailable right now.',
      'status.emptyData': 'The station database could not be loaded. Please refresh and try again.',
      'status.emptyList': 'No stations match this search yet. Try a different keyword.',
      'status.sleeptimerStopped': 'Sleep timer: playback stopped',
      'status.copied': 'Link copied!',
      'results.shown.one': '1 station shown',
      'results.shown.many': '{n} stations shown',
      'verified': 'Verified',
      'community': 'Community',
      'status.online': 'Online',
      'status.offline': 'Offline',
      'status.unknown': 'Status unknown',
      'alarm.timeLabel': 'Time',
      'alarm.stationLabel': 'Station',
      'alarm.set': 'Set alarm',
      'alarm.off': 'Turn off',
      'alarm.status': 'Alarm at {time}',
      'alarm.fired': 'Wake up \u2014 playing {name}'
    },
    te: {
      'app.eyebrow': '\u0C15\u0C2E\u0C4D\u0C2F\u0C42\u0C28\u0C3F\u0C1F\u0C40 \u0C30\u0C47\u0C21\u0C3F\u0C2F\u0C4B \u2022 PWA',
      'app.tagline': '\u0C2D\u0C3E\u0C30\u0C24\u0C40\u0C2F \u0C06\u0C28\u0C4D\u0C32\u0C48\u0C28\u0C4D \u0C30\u0C47\u0C21\u0C3F\u0C2F\u0C4B \u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D\u0C32\u0C28\u0C41 \u0C24\u0C15\u0C4D\u0C37\u0C23 \u0C36\u0C4B\u0C27\u0C28, \u0C2B\u0C3F\u0C32\u0C4D\u0C1F\u0C30\u0C4D\u0C32\u0C24\u0C4B \u0C35\u0C3F\u0C28\u0C02\u0C21\u0C3F.',
      'search.placeholder': '\u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D\u0C32\u0C41, \u0C2D\u0C3E\u0C37 \u0C32\u0C47\u0C26\u0C3E \u0C35\u0C30\u0C4D\u0C17\u0C02 \u0C15\u0C4B\u0C38\u0C02 \u0C35\u0C46\u0C24\u0C15\u0C02\u0C21\u0C3F',
      'install': '\u0C2F\u0C3E\u0C2A\u0C4D\u0C28\u0C41 \u0C07\u0C28\u0C4D\u0C38\u0C4D\u0C1F\u0C3E\u0C32\u0C4D \u0C1A\u0C47\u0C2F\u0C02\u0C21\u0C3F',
      'section.recent': '\u0C07\u0C24\u0C40\u0C35\u0C32 \u0C35\u0C3F\u0C28\u0C4D\u0C28\u0C35\u0C3F',
      'section.favorites': '\u0C07\u0C37\u0C4D\u0C1F\u0C2E\u0C48\u0C28 \u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D\u0C32\u0C41',
      'section.all': '\u0C05\u0C28\u0C4D\u0C28\u0C3F \u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D\u0C32\u0C41',
      'filter.all': '\u0C05\u0C28\u0C4D\u0C28\u0C40',
      'filter.allLanguages': '\u0C05\u0C28\u0C4D\u0C28\u0C3F \u0C2D\u0C3E\u0C37\u0C32\u0C41',
      'player.defaultTitle': '\u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D\u0C28\u0C41 \u0C0E\u0C02\u0C1A\u0C41\u0C15\u0C02\u0C21\u0C3F',
      'player.defaultMeta': '\u0C2E\u0C40\u0C30\u0C41 \u0C0E\u0C02\u0C1A\u0C41\u0C15\u0C41\u0C28\u0C4D\u0C28 \u0C30\u0C47\u0C21\u0C3F\u0C2F\u0C4B \u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D \u0C07\u0C15\u0C4D\u0C15\u0C21 \u0C15\u0C28\u0C3F\u0C2A\u0C3F\u0C38\u0C4D\u0C24\u0C41\u0C02\u0C26\u0C3F.',
      'controls.play': '\u0C2A\u0C4D\u0C32\u0C47',
      'controls.resume': '\u0C15\u0C4A\u0C28\u0C38\u0C3E\u0C17\u0C3F\u0C02\u0C1A\u0C41',
      'controls.pause': '\u0C2A\u0C3E\u0C1C\u0C4D',
      'controls.stop': '\u0C06\u0C2A\u0C41',
      'controls.previous': '\u0C2E\u0C41\u0C28\u0C41\u0C2A\u0C1F\u0C3F',
      'controls.next': '\u0C24\u0C30\u0C41\u0C35\u0C3E\u0C24',
      'controls.volume': '\u0C2E\u0C4D\u0C2F\u0C42\u0C1F\u0C4D/\u0C05\u0C28\u0C4D\u0C2E\u0C4D\u0C2F\u0C42\u0C1F\u0C4D',
      'np.title': '\u0C2A\u0C4D\u0C30\u0C38\u0C4D\u0C24\u0C41\u0C24\u0C02 \u0C2A\u0C4D\u0C32\u0C47 \u0C05\u0C35\u0C41\u0C24\u0C4B\u0C02\u0C26\u0C3F',
      'np.share': '\u0C37\u0C47\u0C30\u0C4D',
      'np.timer': '\u0C1F\u0C48\u0C2E\u0C30\u0C4D',
      'np.alarm': '\u0C05\u0C32\u0C3E\u0C30\u0C02',
      'np.sleeptimer': '{time} \u0C32\u0C4B \u0C28\u0C3F\u0C26\u0C4D\u0C30',
      'np.now': '\u0C07\u0C2A\u0C4D\u0C2A\u0C41\u0C21\u0C41',
      'np.next': '\u0C24\u0C30\u0C4D\u0C35\u0C3E\u0C24',
      'np.noSchedule': '\u0C36\u0C47\u0C26\u0C4D\u0C2F\u0C42\u0C32\u0C4D \u0C05\u0C02\u0C26\u0C41\u0C2C\u0C3E\u0C1F\u0C41\u0C32\u0C4B \u0C32\u0C47\u0C26\u0C41',
      'theme.light': '\u0C32\u0C48\u0C1F\u0C4D',
      'theme.dark': '\u0C21\u0C3E\u0C30\u0C4D\u0C15\u0C4D',
      'theme.auto': '\u0C06\u0C1F\u0C4B',
      'theme.toggle': '\u0C25\u0C3F\u0C2E\u0C4D \u0C2E\u0C3E\u0C30\u0C4D\u0C1A\u0C02\u0C21\u0C3F',
      'update.available': '\u0C05\u0C2A\u0C4D\u0C21\u0C47\u0C1F\u0C4D \u0C05\u0C02\u0C26\u0C41\u0C2C\u0C3E\u0C1F\u0C41\u0C32\u0C4B \u0C09\u0C02\u0C26\u0C3F',
      'update.refresh': '\u0C30\u0C3F\u0C2B\u0C4D\u0C30\u0C47\u0C37\u0C4D',
      'status.loading': '\u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D\u0C32\u0C41 \u0C32\u0C4B\u0C21\u0C4D \u0C05\u0C35\u0C41\u0C24\u0C41\u0C28\u0C4D\u0C28\u0C3E\u0C2F\u0C3F\u2026',
      'status.loaded': '{n} \u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D\u0C32\u0C41 \u0C32\u0C4B\u0C21\u0C4D \u0C05\u0C2F\u0C4D\u0C2F\u0C3E\u0C2F\u0C3F',
      'status.playing': '{name} \u0C2A\u0C4D\u0C32\u0C47 \u0C05\u0C35\u0C41\u0C24\u0C4B\u0C02\u0C26\u0C3F',
      'status.playingCast': '{name} \u0C15\u0C3E\u0C38\u0C4D\u0C1F\u0C3F\u0C02\u0C17\u0C4D \u0C05\u0C35\u0C41\u0C24\u0C4B\u0C02\u0C26\u0C3F',
      'status.casting': '\u0C15\u0C3E\u0C38\u0C4D\u0C1F\u0C3F\u0C02\u0C17\u0C4D',
      'status.noCastSession': '\u0C15\u0C3E\u0C38\u0C4D\u0C1F\u0C4D \u0C38\u0C47\u0C37\u0C28\u0C4D \u0C05\u0C02\u0C26\u0C41\u0C2C\u0C3E\u0C1F\u0C41\u0C32\u0C4B \u0C32\u0C47\u0C26\u0C41',
      'status.castBlocked': '\u0C08 HLS \u0C38\u0C4D\u0C1F\u0C4D\u0C30\u0C40\u0C02\u0C15\u0C3F \u0C15\u0C3E\u0C38\u0C4D\u0C1F\u0C4D \u0C28\u0C3F\u0C30\u0C4B\u0C26\u0C4D\u0C27\u0C3F\u0C02\u0C1A\u0C2C\u0C21\u0C3F\u0C02\u0C26\u0C3F. HLS_PROXY_URL \u0C38\u0C46\u0C1F\u0C4D \u0C1A\u0C47\u0C2F\u0C02\u0C21\u0C3F.',
      'status.castError': '\u0C15\u0C3E\u0C38\u0C4D\u0C1F\u0C4D \u0C26\u0C4B\u0C37\u0C02: {error}',
      'status.appInstalled': '\u0C2F\u0C3E\u0C2A\u0C4D \u0C07\u0C28\u0C4D\u0C38\u0C4D\u0C1F\u0C3E\u0C32\u0C4D \u0C05\u0C2F\u0C4D\u0C2F\u0C3F\u0C02\u0C26\u0C3F',
      'player.unknownLanguage': '\u0C24\u0C46\u0C32\u0C3F\u0C2F\u0C28\u0C3F \u0C2D\u0C3E\u0C37',
      'player.stream': '\u0C38\u0C4D\u0C1F\u0C4D\u0C30\u0C40\u0C02',
      'share.text': 'OpenRadio-IN \u0C32\u0C4B {name} \u0C35\u0C3F\u0C28\u0C02\u0C21\u0C3F',
      'status.resumed': '\u0C2A\u0C4D\u0C32\u0C47\u0C2C\u0C4D\u0C2F\u0C3E\u0C15\u0C4D \u0C2A\u0C41\u0C28\u0C03\u0C2A\u0C4D\u0C30\u0C3E\u0C30\u0C02\u0C2D\u0C3F\u0C02\u0C1A\u0C2C\u0C21\u0C3F\u0C02\u0C26\u0C3F',
      'status.noStream': '\u0C38\u0C4D\u0C1F\u0C4D\u0C30\u0C40\u0C02 \u0C05\u0C02\u0C26\u0C41\u0C2C\u0C3E\u0C1F\u0C41\u0C32\u0C4B \u0C32\u0C47\u0C26\u0C41',
      'status.hlsUnsupported': '\u0C08 \u0C2C\u0C4D\u0C30\u0C4C\u0C1C\u0C30\u0C4D\u0C32\u0C4B HLS \u0C2A\u0C4D\u0C32\u0C47\u0C2C\u0C4D\u0C2F\u0C3E\u0C15\u0C4D \u0C38\u0C2A\u0C4B\u0C30\u0C4D\u0C1F\u0C4D \u0C1A\u0C47\u0C2F\u0C2C\u0C21\u0C26\u0C41',
      'status.loadingHls': 'HLS \u0C38\u0C4D\u0C1F\u0C4D\u0C30\u0C40\u0C02 \u0C32\u0C4B\u0C21\u0C4D \u0C05\u0C35\u0C41\u0C24\u0C4B\u0C02\u0C26\u0C3F\u2026',
      'status.loadFailed': '\u0C08 \u0C38\u0C4D\u0C1F\u0C4D\u0C30\u0C40\u0C02\u0C28\u0C3F \u0C32\u0C4B\u0C21\u0C4D \u0C1A\u0C47\u0C2F\u0C32\u0C47\u0C15\u0C2A\u0C4B\u0C2F\u0C3E\u0C2E\u0C41',
      'status.tryingBackup': '\u0C2E\u0C42\u0C32 \u0C38\u0C4D\u0C1F\u0C4D\u0C30\u0C40\u0C02 \u0C2C\u0C3F\u0C32\u0C4D\u0C32\u0C2F\u0C48\u0C02\u0A26\u0C3F \u2014 \u0C2C\u0C48\u0C15\u0C4D\u0C2A\u0C4D \u0C2A\u0C4D\u0C30\u0C2F\u0C24\u0C4D\u0C28\u0C3F\u0C38\u0C4D\u0C24\u0C41\u0C28\u0C4D\u0C28\u0C3E\u0C2E\u0C41\u2026',
      'status.playFailed': '\u0C2A\u0C4D\u0C32\u0C47\u0C2C\u0C4D\u0C2F\u0C3E\u0C15\u0C4D \u0C2A\u0C4D\u0C30\u0C3E\u0C30\u0C02\u0C2D\u0C3F\u0C02\u0C1A\u0C32\u0C47\u0C15\u0C2A\u0C4B\u0C2F\u0C3E\u0C2E\u0C41',
      'status.streamFailed': '\u0C08 \u0C38\u0C4D\u0C1F\u0C4D\u0C30\u0C40\u0C02\u0C28\u0C3F \u0C2A\u0C4D\u0C30\u0C3E\u0C30\u0C02\u0C2D\u0C3F\u0C02\u0C1A\u0C32\u0C47\u0C15\u0C2A\u0C4B\u0C2F\u0C3E\u0C2E\u0C41',
      'status.ended': '\u0C2A\u0C4D\u0C32\u0C47\u0C2C\u0C4D\u0C2F\u0C3E\u0C15\u0C4D \u0C2E\u0C41\u0C17\u0C3F\u0C38\u0C3F\u0C02\u0C26\u0C3F',
      'status.error': '\u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D \u0C21\u0C3E\u0C1F\u0C3E\u0C28\u0C41 \u0C32\u0C4B\u0C21\u0C4D \u0C1A\u0C47\u0C2F\u0C32\u0C47\u0C15\u0C2A\u0C4B\u0C2F\u0C3E\u0C2E\u0C41',
      'status.dataUnavailable': '\u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D \u0C21\u0C3E\u0C1F\u0C3E \u0C2A\u0C4D\u0C30\u0C38\u0C4D\u0C24\u0C41\u0C24\u0C02 \u0C05\u0C02\u0C26\u0C41\u0C2C\u0C3E\u0C1F\u0C41\u0C32\u0C4B \u0C32\u0C47\u0C26\u0C41.',
      'status.emptyData': '\u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D \u0C21\u0C3E\u0C1F\u0C3E\u0C2C\u0C47\u0C38\u0C4D \u0C32\u0C4B\u0C21\u0C4D \u0C05\u0C35\u0C41\u0C24\u0C4B\u0C32\u0C47\u0C26\u0C41. \u0C26\u0C2F\u0C1A\u0C47\u0C38\u0C3F \u0C2E\u0C33\u0C4D\u0C33\u0C40 \u0C2A\u0C4D\u0C30\u0C2F\u0C24\u0C4D\u0C28\u0C3F\u0C02\u0C1A\u0C02\u0C21\u0C3F.',
      'status.emptyList': '\u0C08 \u0C36\u0C4B\u0C27\u0C28\u0C15\u0C3F \u0C38\u0C30\u0C3F\u0C2A\u0C4B\u0C32\u0C47 \u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D\u0C32\u0C41 \u0C32\u0C47\u0C35\u0C41.',
      'status.sleeptimerStopped': '\u0C28\u0C3F\u0C26\u0C4D\u0C30 \u0C1F\u0C48\u0C2E\u0C30\u0C4D: \u0C2A\u0C4D\u0C32\u0C47\u0C2C\u0C4D\u0C2F\u0C3E\u0C15\u0C4D \u0C06\u0C2A\u0C3F\u0C02\u0C26\u0C3F',
      'status.copied': '\u0C32\u0C3F\u0C02\u0C15\u0C4D \u0C15\u0C3E\u0C2A\u0C40 \u0C05\u0C2F\u0C4D\u0C2F\u0C3F\u0C02\u0C26\u0C3F!',
      'results.shown.one': '1 \u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D \u0C1A\u0C42\u0C2A\u0C2C\u0C21\u0C41\u0C24\u0C41\u0C02\u0C26\u0C3F',
      'results.shown.many': '{n} \u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D\u0C32\u0C41 \u0C1A\u0C42\u0C2A\u0C2C\u0C21\u0C41\u0C24\u0C41\u0C28\u0C4D\u0C28\u0C3E\u0C2F\u0C3F',
      'verified': '\u0C27\u0C43\u0C35\u0C40\u0C15\u0C30\u0C3F\u0C02\u0C1A\u0C2C\u0C21\u0C3F\u0C02\u0C26\u0C3F',
      'community': '\u0C15\u0C2E\u0C4D\u0C2F\u0C42\u0C28\u0C3F\u0C1F\u0C40',
      'status.online': '\u0C06\u0C28\u0C4D\u0C32\u0C48\u0C28\u0C4D',
      'status.offline': '\u0C06\u0C2B\u0C4D\u0C32\u0C48\u0C28\u0C4D',
      'status.unknown': '\u0C38\u0C4D\u0C25\u0C3F\u0C24\u0C3F \u0C24\u0C46\u0C32\u0C3F\u0C2F\u0C26\u0C41',
      'alarm.timeLabel': '\u0C38\u0C2E\u0C2F\u0C02',
      'alarm.stationLabel': '\u0C38\u0C4D\u0C1F\u0C47\u0C37\u0C28\u0C4D',
      'alarm.set': '\u0C05\u0C32\u0C3E\u0C30\u0C02 \u0C38\u0C46\u0C1F\u0C4D',
      'alarm.off': '\u0C06\u0C2B\u0C4D \u0C1A\u0C47\u0C2F\u0C3F',
      'alarm.status': '{time} \u0C15\u0C3F \u0C05\u0C32\u0C3E\u0C30\u0C02',
      'alarm.fired': '\u0C32\u0C46\u0C02\u0C1A\u0C02\u0C21\u0C3F \u2014 {name} \u0C2A\u0C4D\u0C32\u0C47 \u0C05\u0C35\u0C41\u0C24\u0C4B\u0C02\u0C26\u0C3F'
    },
    hi: {
      'app.eyebrow': '\u0915\u092E\u094D\u092F\u0942\u0928\u093F\u091F\u0940 \u0930\u0947\u0921\u093F\u092F\u094B \u2022 PWA',
      'app.tagline': '\u0924\u0941\u0930\u0902\u0924 \u0916\u094B\u091C \u0914\u0930 \u092B\u093C\u093F\u0932\u094D\u091F\u0930 \u0915\u0947 \u0938\u093E\u0925 \u092D\u093E\u0930\u0924\u0940\u092F \u0911\u0928\u0932\u093E\u0907\u0928 \u0930\u0947\u0921\u093F\u092F\u094B \u0938\u094D\u091F\u0947\u0936\u0928 \u0938\u0941\u0928\u0947\u0902\u0964',
      'search.placeholder': '\u0938\u094D\u091F\u0947\u0936\u0928, \u092D\u093E\u0937\u093E \u092F\u093E \u0936\u094D\u0930\u0947\u0923\u0940 \u0916\u094B\u091C\u0947\u0902',
      'install': '\u0910\u092A \u0907\u0902\u0938\u094D\u091F\u093E\u0932 \u0915\u0930\u0947\u0902',
      'section.recent': '\u0939\u093E\u0932 \u092E\u0947\u0902 \u0938\u0941\u0928\u0947 \u0917\u090F',
      'section.favorites': '\u092A\u0938\u0902\u0926\u0940\u0926\u093E \u0938\u094D\u091F\u0947\u0936\u0928',
      'section.all': '\u0938\u092D\u0940 \u0938\u094D\u091F\u0947\u0936\u0928',
      'filter.all': '\u0938\u092D\u0940',
      'filter.allLanguages': '\u0938\u092D\u0940 \u092D\u093E\u0937\u093E\u090F\u0901',
      'player.defaultTitle': '\u0938\u094D\u091F\u0947\u0936\u0928 \u091A\u0941\u0928\u0947\u0902',
      'player.defaultMeta': '\u0906\u092A\u0915\u093E \u091A\u0941\u0928\u093E \u0939\u0941\u0906 \u0930\u0947\u0921\u093F\u092F\u094B \u0938\u094D\u091F\u0947\u0936\u0928 \u092F\u0939\u093E\u0902 \u0926\u093F\u0916\u0947\u0917\u093E\u0964',
      'controls.play': '\u092A\u094D\u0932\u0947',
      'controls.resume': '\u092B\u093C\u093F\u0930 \u0936\u0941\u0930\u0942 \u0915\u0930\u0947\u0902',
      'controls.pause': '\u092A\u0949\u091C\u093C',
      'controls.stop': '\u0930\u094B\u0915\u0947\u0902',
      'controls.previous': '\u092A\u093F\u091B\u0932\u093E',
      'controls.next': '\u0905\u0917\u0932\u093E',
      'controls.volume': '\u092E\u094D\u092F\u0942\u091F/\u0905\u0928\u092E\u094D\u092F\u0942\u091F',
      'np.title': '\u0905\u092D\u0940 \u091A\u0932 \u0930\u0939\u093E \u0939\u0948',
      'np.share': '\u0936\u0947\u092F\u0930',
      'np.timer': '\u091F\u093E\u0907\u092E\u0930',
      'np.alarm': '\u0905\u0932\u093E\u0930\u094D\u092E',
      'np.sleeptimer': '{time} \u092E\u0947\u0902 \u0938\u094B \u091C\u093E\u090F\u0902',
      'np.now': '\u0905\u092D\u0940',
      'np.next': '\u0905\u0917\u0932\u093E',
      'np.noSchedule': '\u0936\u0947\u0921\u094D\u092F\u0942\u0932 \u0909\u092A\u0932\u092C\u094D\u0927 \u0928\u0939\u0940\u0902 \u0939\u0948',
      'theme.light': '\u0932\u093E\u0907\u091F',
      'theme.dark': '\u0921\u093E\u0930\u094D\u0915',
      'theme.auto': '\u0911\u091F\u094B',
      'theme.toggle': '\u0925\u0940\u092E \u092C\u0926\u0932\u0947\u0902',
      'update.available': '\u0905\u092A\u0921\u0947\u091F \u0909\u092A\u0932\u092C\u094D\u0927 \u0939\u0948',
      'update.refresh': '\u0930\u093F\u092B\u093C\u094D\u0930\u0947\u0936',
      'status.loading': '\u0938\u094D\u091F\u0947\u0936\u0928 \u0932\u094B\u0921 \u0939\u094B \u0930\u0939\u0947 \u0939\u0948\u0902\u2026',
      'status.loaded': '{n} \u0938\u094D\u091F\u0947\u0936\u0928 \u0932\u094B\u0921 \u0939\u0941\u090F',
      'status.playing': '{name} \u091A\u0932 \u0930\u0939\u093E \u0939\u0948',
      'status.playingCast': '{name} \u0915\u093E\u0938\u094D\u091F \u0939\u094B \u0930\u0939\u093E \u0939\u0948',
      'status.casting': '\u0915\u093E\u0938\u094D\u091F\u093F\u0902\u0917',
      'status.noCastSession': '\u0915\u094B\u0908 \u0915\u093E\u0938\u094D\u091F \u0938\u0947\u0936\u0928 \u0909\u092A\u0932\u092C\u094D\u0927 \u0928\u0939\u0940\u0902',
      'status.castBlocked': '\u0907\u0938 HLS \u0938\u094D\u091F\u094D\u0930\u0940\u092E \u0915\u0947 \u0932\u093F\u090F \u0915\u093E\u0938\u094D\u091F \u092C\u094D\u0932\u0949\u0915 \u0939\u0948. HLS_PROXY_URL \u0938\u0947\u091F \u0915\u0930\u0947\u0902.',
      'status.castError': '\u0915\u093E\u0938\u094D\u091F \u0924\u094D\u0930\u0941\u091F\u093F: {error}',
      'status.appInstalled': '\u0910\u092A \u0907\u0902\u0938\u094D\u091F\u093E\u0932 \u0939\u094B \u0917\u092F\u093E',
      'player.unknownLanguage': '\u0905\u091C\u094D\u091E\u093E\u0924 \u092D\u093E\u0937\u093E',
      'player.stream': '\u0938\u094D\u091F\u094D\u0930\u0940\u092E',
      'share.text': 'OpenRadio-IN \u092A\u0930 {name} \u0938\u0941\u0928\u0947\u0902',
      'status.resumed': '\u092A\u094D\u0932\u0947\u092C\u0948\u0915 \u092B\u093F\u0930 \u0936\u0941\u0930\u0942 \u0939\u0941\u0906',
      'status.noStream': '\u0915\u094B\u0908 \u0938\u094D\u091F\u094D\u0930\u0940\u092E \u0909\u092A\u0932\u092C\u094D\u0927 \u0928\u0939\u0940\u0902',
      'status.hlsUnsupported': '\u0907\u0938 \u092C\u094D\u0930\u093E\u0909\u091C\u093C\u0930 \u092E\u0947\u0902 HLS \u092A\u094D\u0932\u0947\u092C\u0948\u0915 \u0938\u092E\u0930\u094D\u0925\u093F\u0924 \u0928\u0939\u0940\u0902 \u0939\u0948',
      'status.loadingHls': 'HLS \u0938\u094D\u091F\u094D\u0930\u0940\u092E \u0932\u094B\u0921 \u0939\u094B \u0930\u0939\u0940 \u0939\u0948\u2026',
      'status.loadFailed': '\u092F\u0939 \u0938\u094D\u091F\u094D\u0930\u0940\u092E \u0932\u094B\u0921 \u0928\u0939\u0940\u0902 \u0939\u094B \u0938\u0915\u0940',
      'status.tryingBackup': '\u092E\u0941\u0916\u094D\u092F \u0938\u094D\u091F\u094D\u0930\u0940\u092E \u0935\u093F\u092B\u0932 \u0939\u0941\u0908 \u2014 \u092C\u0948\u0915\u0905\u092A \u0915\u0940 \u0915\u094B\u0936\u093F\u0936 \u0915\u0940 \u091C\u093E \u0930\u0939\u0940 \u0939\u0948\u2026',
      'status.playFailed': '\u092A\u094D\u0932\u0947\u092C\u0948\u0915 \u0936\u0941\u0930\u0942 \u0928\u0939\u0940\u0902 \u0939\u094B \u0938\u0915\u093E',
      'status.streamFailed': '\u092F\u0939 \u0938\u094D\u091F\u094D\u0930\u0940\u092E \u0936\u0941\u0930\u0942 \u0928\u0939\u0940\u0902 \u0939\u094B \u0938\u0915\u0940',
      'status.ended': '\u092A\u094D\u0932\u0947\u092C\u0948\u0915 \u0938\u092E\u093E\u092A\u094D\u0924',
      'status.error': '\u0938\u094D\u091F\u0947\u0936\u0928 \u0921\u0947\u091F\u093E \u0932\u094B\u0921 \u0928\u0939\u0940\u0902 \u0939\u094B \u0938\u0915\u093E',
      'status.dataUnavailable': '\u0938\u094D\u091F\u0947\u0936\u0928 \u0921\u0947\u091F\u093E \u0905\u092D\u0940 \u0905\u0928\u0941\u092A\u0932\u092C\u094D\u0927 \u0939\u0948\u0964',
      'status.emptyData': '\u0938\u094D\u091F\u0947\u0936\u0928 \u0921\u0947\u091F\u093E\u092C\u0947\u0938 \u0932\u094B\u0921 \u0928\u0939\u0940\u0902 \u0939\u094B \u0938\u0915\u0940\u0964 \u0915\u0943\u092A\u092F\u093E \u092B\u093F\u0930 \u0938\u0947 \u0915\u094B\u0936\u093F\u0936 \u0915\u0930\u0947\u0902\u0964',
      'status.emptyList': '\u0907\u0938 \u0916\u094B\u091C \u0938\u0947 \u0915\u094B\u0908 \u0938\u094D\u091F\u0947\u0936\u0928 \u092E\u0947\u0932 \u0928\u0939\u0940\u0902 \u0916\u093E\u0924\u093E\u0964',
      'status.sleeptimerStopped': '\u0938\u094D\u0932\u0940\u092A \u091F\u093E\u0907\u092E\u0930: \u092A\u094D\u0932\u0947\u092C\u0948\u0915 \u0930\u094B\u0915 \u0939\u0941\u0906',
      'status.copied': '\u0932\u093F\u0902\u0915 \u0915\u0949\u092A\u0940 \u0939\u0941\u0906!',
      'results.shown.one': '1 \u0938\u094D\u091F\u0947\u0936\u0928 \u0926\u093F\u0916\u093E\u092F\u093E \u0917\u092F\u093E',
      'results.shown.many': '{n} \u0938\u094D\u091F\u0947\u0936\u0928 \u0926\u093F\u0916\u093E\u090F \u091C\u093E \u0930\u0939\u0947 \u0939\u0948\u0902',
      'verified': '\u0938\u0924\u094D\u092F\u093E\u092A\u093F\u0924',
      'community': '\u0915\u092E\u094D\u092F\u0942\u0928\u093F\u091F\u0940',
      'status.online': '\u0911\u0928\u0932\u093E\u0907\u0928',
      'status.offline': '\u0911\u092B\u093C\u0932\u093E\u0907\u0928',
      'status.unknown': '\u0938\u094D\u0925\u093F\u0924\u093F \u0905\u091C\u094D\u091E\u093E\u0924',
      'alarm.timeLabel': '\u0938\u092E\u092F',
      'alarm.stationLabel': '\u0938\u094D\u091F\u0947\u0936\u0928',
      'alarm.set': '\u0905\u0932\u093E\u0930\u094D\u092E \u0938\u0947\u091F \u0915\u0930\u0947\u0902',
      'alarm.off': '\u092C\u0902\u0926 \u0915\u0930\u0947\u0902',
      'alarm.status': '{time} \u092A\u0930 \u0905\u0932\u093E\u0930\u094D\u092E',
      'alarm.fired': '\u091C\u093E\u0917\u094B \u2014 {name} \u091A\u0932 \u0930\u0939\u093E \u0939\u0948'
    }
  };
  let uiLang = localStorage.getItem('openradio-ui-lang') || 'en';

  function t(key, vars) {
    const dict = I18N[uiLang] || I18N.en;
    let text = dict[key] ?? I18N.en[key] ?? key;
    if (vars) {
      for (const [k, v] of Object.entries(vars)) {
        text = text.split(`{${k}}`).join(String(v));
      }
    }
    return text;
  }

  const state = {
    stations: [],
    filteredStations: [],
    favorites: new Set(JSON.parse(localStorage.getItem('openradio-favorites') || '[]')),
    recentStations: JSON.parse(localStorage.getItem('openradio-recent') || '[]'),
    activeCategory: localStorage.getItem('openradio-category') || 'all',
    activeLanguage: localStorage.getItem('openradio-language') || 'all',
    search: '',
    currentStation: null,
    playing: false,
    nowPlayingTrack: '',
    nowPlayingArt: '',
    currentSource: 'all',
    volume: parseFloat(localStorage.getItem('openradio-volume') || '1'),
    muted: false,
    previousVolume: 1,
    theme: localStorage.getItem('openradio-theme') || 'system',
    alarm: JSON.parse(localStorage.getItem('openradio-alarm') || 'null'),
    retryCount: 0,
    maxRetries: 3,
    sleepTimerId: null,
    sleepTimerEnd: null,
    sleepTimerIntervalId: null,
    castInProgress: false,
    userInitiatedStop: false,
    paused: false,
    pauseIntent: false,
    pendingAutoResume: false,
    resumeAttempts: 0,
    streamIndex: 0,
    playGeneration: 0,
    streamSwitching: false,
    metadataIntervalId: null,
    epgPrograms: null,
    epgDate: null,
    epgRefreshIntervalId: null
  };

  const elements = {
    search: document.getElementById('search'),
    filters: document.getElementById('filters'),
    resultsCount: document.getElementById('results-count'),
    featured: document.getElementById('featured'),
    stations: document.getElementById('stations'),
    playToggle: document.getElementById('play-toggle'),
    stopBtn: document.getElementById('stop-btn'),
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
    nowPlayingEpg: document.getElementById('now-playing-epg'),
    npPrev: document.getElementById('np-prev'),
    npPlayToggle: document.getElementById('np-play-toggle'),
    npStopBtn: document.getElementById('np-stop'),
    npNext: document.getElementById('np-next'),
    npVolumeSlider: document.getElementById('np-volume-slider'),
    npVolumeBtn: document.getElementById('np-volume-btn'),
    shareBtn: document.getElementById('share-btn'),
    sleepTimerBtn: document.getElementById('sleep-timer-btn'),
    sleepTimerPicker: document.getElementById('sleep-timer-picker'),
    sleepTimerStatus: document.getElementById('sleep-timer-status'),
    themeToggle: document.getElementById('theme-toggle'),
    updateBanner: document.getElementById('update-banner'),
    updateBtn: document.getElementById('update-btn'),
    languageSelect: document.getElementById('language-select'),
    uiLang: document.getElementById('ui-lang'),
    alarmBtn: document.getElementById('alarm-btn'),
    alarmPicker: document.getElementById('alarm-picker'),
    alarmTimeInput: document.getElementById('alarm-time-input'),
    alarmStationSelect: document.getElementById('alarm-station-select'),
    alarmSet: document.getElementById('alarm-set'),
    alarmOff: document.getElementById('alarm-off'),
    alarmStatus: document.getElementById('alarm-status')
  };
  let installPrompt;
  let castContext;
  let castPlayer;
  let castPlayerController;

  const CAST_RECEIVER_APP_ID = 'CC1AD845'; // Google Default Media Receiver (plays HLS/MP3/AAC/TS natively)
  const HLS_PROXY_URL = 'https://openradio-hls-proxy.kedharnadh1.workers.dev';
  const RECENT_MAX = 10;
  const RESUME_GRACE_MS = 3000;
  const RESUME_BACKOFF_MS = [5000, 10000, 20000, 30000, 30000];
  const RESUME_MAX_ATTEMPTS = 10;
  const ART_CACHE_KEY = 'openradio-art-cache';
  const ART_CACHE_TTL = 24 * 60 * 60 * 1000;

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
    const languages = String(station.language || '')
      .split(',')
      .map((language) => language.trim().toLowerCase())
      .filter(Boolean);
    const languageOk = state.activeLanguage === 'all' || languages.includes(state.activeLanguage.toLowerCase());
    return languageOk && searchable.includes(query) && hasCategory(station, state.activeCategory);
  }

  /* ---------- Theme ---------- */

  const SYSTEM_DARK_QUERY = window.matchMedia('(prefers-color-scheme: dark)');

  function resolveTheme(theme) {
    return theme === 'system' ? (SYSTEM_DARK_QUERY.matches ? 'dark' : 'light') : theme;
  }

  function setTheme(theme) {
    state.theme = theme;
    document.documentElement.setAttribute('data-theme', resolveTheme(theme));
    localStorage.setItem('openradio-theme', theme);
    const metaTheme = document.getElementById('theme-color');
    if (metaTheme) metaTheme.content = resolveTheme(theme) === 'light' ? '#f1f5f9' : '#0f172a';
    renderThemeToggle();
  }

  function renderThemeToggle() {
    elements.themeToggle.textContent = state.theme === 'light'
      ? '\u{1F319} ' + t('theme.dark')
      : state.theme === 'dark'
        ? '\u2600\uFE0F ' + t('theme.light')
        : '\u{1F319}/\u2600 ' + t('theme.auto');
  }

  function toggleTheme() {
    const order = ['light', 'dark', 'system'];
    const next = order[(order.indexOf(state.theme) + 1) % order.length];
    setTheme(next);
  }

  function watchSystemTheme() {
    SYSTEM_DARK_QUERY.addEventListener('change', () => {
      if (state.theme === 'system') setTheme('system');
    });
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
    try {
      if (typeof navigator.mediaSession.setAudioFocusMode === 'function') {
        navigator.mediaSession.setAudioFocusMode('multicast');
      }
    } catch {}
    navigator.mediaSession.setActionHandler('play', () => playPlayback());
    navigator.mediaSession.setActionHandler('pause', () => pausePlayback());
    navigator.mediaSession.setActionHandler('stop', () => stopPlayback());
    navigator.mediaSession.setActionHandler('previoustrack', () => playAdjacentStation(-1));
    navigator.mediaSession.setActionHandler('nexttrack', () => playAdjacentStation(1));
  }

  function updateMediaSession() {
    if (!('mediaSession' in navigator)) return;
    const station = state.currentStation;
    if (!station) {
      navigator.mediaSession.metadata = null;
      navigator.mediaSession.playbackState = 'none';
      return;
    }
    const track = state.nowPlayingTrack;
    const artSrc = state.nowPlayingArt || station.logo || '';
    const hit = getCurrentEpgProgram();
    const nowEpg = hit && hit.program ? hit.program.title : '';
    navigator.mediaSession.metadata = new MediaMetadata({
      title: nowEpg || track || station.name,
      artist: nowEpg || track ? station.name : (station.language || 'OpenRadio-IN'),
      artwork: artSrc ? [{ src: artSrc, sizes: '512x512' }] : []
    });
    navigator.mediaSession.playbackState = state.playing ? 'playing' : (state.paused ? 'paused' : 'none');
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

  function renderNowPlayingArt() {
    const station = state.currentStation;
    const art = state.nowPlayingArt || station?.logo || '';
    elements.nowPlayingLogo.src = art;
    elements.nowPlayingLogo.alt = station?.name || '';
    elements.nowPlayingLogo.hidden = !art;
    elements.nowPlayingPlaceholder.hidden = Boolean(art);
  }

  function updatePlayer() {
    const station = state.currentStation;
    const list = state.currentSource === 'favorites'
      ? state.stations.filter((s) => state.favorites.has(s.id))
      : state.filteredStations;
    const hasMultiple = list.length > 1;

    if (!station) {
      elements.playerTitle.textContent = t('player.defaultTitle');
      elements.playerMeta.textContent = t('player.defaultMeta');
      elements.playToggle.disabled = true;
      elements.playToggle.textContent = '\u25b6 ' + t('controls.play');
      elements.stopBtn.disabled = true;
      elements.prevBtn.disabled = true;
      elements.nextBtn.disabled = true;
      elements.npPlayToggle.disabled = true;
      elements.npPlayToggle.textContent = '\u25b6 ' + t('controls.play');
      elements.npStopBtn.disabled = true;
      elements.npPrev.disabled = true;
      elements.npNext.disabled = true;
      elements.nowPlayingTitle.textContent = t('player.defaultTitle');
      elements.nowPlayingMeta.textContent = '';
      elements.nowPlayingLogo.hidden = true;
      elements.nowPlayingPlaceholder.hidden = false;
      elements.nowPlayingTrack.hidden = true;
      updateMediaSession();
      return;
    }

    const destination = isCasting() ? t('status.casting') : (station.streams?.[0]?.codec || t('player.stream'));
    const languageLabel = station.language || t('player.unknownLanguage');
    elements.playerTitle.textContent = station.name;
    elements.playerMeta.textContent = `${languageLabel} \u2022 ${destination}`;
    elements.prevBtn.disabled = !hasMultiple;
    elements.nextBtn.disabled = !hasMultiple;
    elements.npPrev.disabled = !hasMultiple;
    elements.npNext.disabled = !hasMultiple;
    elements.nowPlayingTitle.textContent = station.name;
    elements.nowPlayingMeta.textContent = `${languageLabel} \u2022 ${destination}`;
    renderNowPlayingArt();
    if (state.nowPlayingTrack) {
      elements.nowPlayingTrack.hidden = false;
      elements.nowPlayingTrack.textContent = state.nowPlayingTrack;
    } else {
      elements.nowPlayingTrack.hidden = true;
    }

    const isPlaying = state.playing || (!!elements.audio.src && !elements.audio.paused);
    const isPaused = state.paused && !isPlaying;
    elements.playToggle.disabled = false;
    elements.playToggle.textContent = isPlaying ? '\u23f8 ' + t('controls.pause') : (isPaused ? '\u25b6 ' + t('controls.resume') : '\u25b6 ' + t('controls.play'));
    elements.stopBtn.disabled = false;
    elements.npPlayToggle.disabled = false;
    elements.npPlayToggle.textContent = isPlaying ? '\u23f8 ' + t('controls.pause') : (isPaused ? '\u25b6 ' + t('controls.resume') : '\u25b6 ' + t('controls.play'));
    elements.npStopBtn.disabled = false;
    renderEpg();
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

    const status = station.status || 'unknown';
    const dot = makeElement('span', `status-dot status-dot--${status}`, '');
    dot.title = t(`status.${status}`);
    dot.setAttribute('aria-label', t(`status.${status}`));

    const favorite = makeElement('button', `icon-btn${state.favorites.has(station.id) ? ' active' : ''}`, state.favorites.has(station.id) ? '\u2665' : '\u2661');
    favorite.type = 'button';
    favorite.dataset.action = 'favorite';
    favorite.dataset.id = station.id;
    favorite.setAttribute('aria-label', `Favorite ${station.name}`);
    top.append(thumb, thumbFallback, titleBlock, dot, favorite);

    const badges = makeElement('div', 'station-badges');
    (station.categories || []).filter(Boolean).slice(0, featured ? 3 : 4).forEach((category) => badges.append(makeElement('span', '', category)));

    const footer = makeElement('div', 'station-card__footer');
    footer.append(makeElement('span', '', featured ? (station.verified ? t('verified') : t('community')) : (station.country || t('player.stream'))));
    const playLabel = state.currentStation?.id === station.id
      ? (state.playing ? t('controls.pause') : (state.paused ? t('controls.resume') : t('controls.play')))
      : t('controls.play');
    const play = makeElement('button', 'secondary-btn', playLabel);
    play.type = 'button';
    play.dataset.action = 'play';
    play.dataset.id = station.id;
    footer.append(play);

    card.append(top, badges, footer);
    return card;
  }

  function renderFilters() {
    const categories = ['all', ...new Set(state.stations.flatMap((station) => station.categories || []))]
      .filter(Boolean)
      .filter((category) => String(category).toLowerCase() !== 'all')
      .sort((first, second) => String(first).localeCompare(String(second)));
    const availableCategories = new Set(categories.map((category) => String(category).toLowerCase()));
    availableCategories.add('all');
    if (state.stations.length && !availableCategories.has(String(state.activeCategory).toLowerCase())) state.activeCategory = 'all';
    elements.filters.replaceChildren(...categories.map((category) => {
      const button = makeElement('button', `pill${state.activeCategory.toLowerCase() === String(category).toLowerCase() ? ' active' : ''}`, category === 'all' ? t('filter.all') : category);
      button.type = 'button';
      button.dataset.category = category;
      return button;
    }));
    renderLanguageOptions();
  }

  function renderLanguageOptions() {
    const languages = [...new Set(
      state.stations
        .map((station) => String(station.language || ''))
        .flatMap((languages) => languages.split(',').map((language) => language.trim()).filter(Boolean))
    )]
      .sort((first, second) => String(first).localeCompare(String(second)));
    const availableLanguages = new Set(languages.map((language) => String(language).toLowerCase()));
    if (state.stations.length && !availableLanguages.has(String(state.activeLanguage).toLowerCase())) state.activeLanguage = 'all';
    const allOption = makeElement('option', '', t('filter.allLanguages'));
    allOption.value = 'all';
    elements.languageSelect.replaceChildren(
      allOption,
      ...languages.map((language) => {
        const option = makeElement('option', '', language);
        option.value = language;
        return option;
      })
    );
    elements.languageSelect.value = state.activeLanguage;
  }

  function renderStationLists() {
    const favoriteStations = state.stations.filter((s) => state.favorites.has(s.id));
    if (favoriteStations.length) {
      elements.favoritesSection.hidden = false;
      elements.featured.replaceChildren(...favoriteStations.map((station) => createStationCard(station, true)));
    } else {
      elements.favoritesSection.hidden = true;
    }
    elements.stations.replaceChildren(...(state.filteredStations.length ? state.filteredStations.map((station) => createStationCard(station, false)) : [makeElement('div', 'empty-state', t('status.emptyList'))]));
    renderRecent();
  }

  function applyFilters() {
    state.filteredStations = state.stations.filter(stationMatches).sort((first, second) => String(first.name).localeCompare(String(second.name)));
    renderFilters();
    renderStationLists();
    const count = state.filteredStations.length;
    elements.resultsCount.textContent = t(count === 1 ? 'results.shown.one' : 'results.shown.many', { n: count });
  }

  function saveFavorites() {
    localStorage.setItem('openradio-favorites', JSON.stringify([...state.favorites]));
  }

  function saveFilters() {
    localStorage.setItem('openradio-category', state.activeCategory);
    localStorage.setItem('openradio-language', state.activeLanguage);
  }

  /* ---------- Cast ---------- */

  function isCasting() {
    return Boolean(castContext && window.cast && castContext.getCastState() === cast.framework.CastState.CONNECTED);
  }

  function streamContentType(stream) {
    const codec = String(stream.codec || '').toLowerCase();
    if (codec === 'hls' || String(stream.url || '').includes('.m3u8')) return 'application/vnd.apple.mpegurl';
    if (codec === 'aac') return 'audio/aac';
    if (codec === 'ogg') return 'audio/ogg';
    return 'audio/mpeg';
  }

  function normalizeCastContentType(contentType, streamUrl) {
    if (window.OpenRadioCast && typeof window.OpenRadioCast.normalizeCastContentType === 'function') {
      return window.OpenRadioCast.normalizeCastContentType(contentType, streamUrl);
    }
    const ct = String(contentType || '').trim().toLowerCase();
    if (ct.includes('mpegurl')) return 'application/vnd.apple.mpegurl';
    if (ct === 'audio/mpeg' || ct === 'audio/mp3') return 'audio/mpeg';
    if (ct === 'audio/aac' || ct === 'audio/aacp') return 'audio/aac';
    if (ct === 'audio/ogg' || ct === 'application/ogg') return 'audio/ogg';
    if (ct === 'video/mp2t' || ct === 'video/mpeg') return 'video/mp2t';
    if (ct === 'video/mp4' || ct === 'audio/mp4') return ct;
    if (ct === 'audio/ac3' || ct === 'audio/eac3') return ct;
    if (ct === 'audio/wav') return 'audio/wav';
    if (ct === 'audio/flac') return 'audio/flac';
    if (!ct && String(streamUrl || '').includes('.m3u8')) return 'application/vnd.apple.mpegurl';
    return ct || 'audio/mpeg';
  }

  function isHlsStream(stream) {
    return String(stream.codec || '').toLowerCase() === 'hls' || String(stream.url || '').includes('.m3u8');
  }

  function sortStreamsForPlayback(streams) {
    return [...(streams || [])]
      .filter((s) => s.url)
      .sort((a, b) => {
        const aHls = isHlsStream(a);
        const bHls = isHlsStream(b);
        if (aHls !== bHls) return aHls ? 1 : -1;
        return (a.priority || Infinity) - (b.priority || Infinity);
      });
  }

  async function castStation(station) {
    if (state.castInProgress) return;
    if (state.playing && isCasting() && state.currentStation && station && state.currentStation.id === station.id) {
      console.log('OpenRadio-IN: already casting station', station.name, '- skipping redundant load');
      return;
    }
    state.castInProgress = true;
    try {
      await castStationInner(station);
    } finally {
      state.castInProgress = false;
    }
  }

  async function castStationInner(station) {
    const streams = sortStreamsForPlayback(station.streams);
    if (!streams.length) return;
    const stream = streams[0];

    state.currentStation = station;
    if (state.hls) { state.hls.destroy(); state.hls = null; }
    elements.audio.pause();
    elements.audio.src = '';

    let session = castContext?.getCurrentSession();
    if (!session) {
      try {
        console.log('OpenRadio-IN: requesting Cast session for station', station.name, 'appId', CAST_RECEIVER_APP_ID);
        if (castContext && typeof castContext.requestSession === 'function') {
          await castContext.requestSession();
          session = castContext.getCurrentSession();
          console.log('OpenRadio-IN: session created', session && session.getSessionId ? session.getSessionId() : session);
        }
      } catch (error) {
        const message = error && error.message ? error.message : 'session_error';
        console.error('OpenRadio-IN: Cast session request failed', error);
        setStatus(t('status.castError', { error: message }));
        return;
      }
    }

    if (!session) {
      console.warn('OpenRadio-IN: no active Cast session after request');
      setStatus(t('status.noCastSession'));
      return;
    }

    const receiverAppId = session && typeof session.getSessionObj === 'function' ? session.getSessionObj().receiverApplicationId : null;
    console.log('OpenRadio-IN: active Cast session', {
      sessionId: session.getSessionId ? session.getSessionId() : 'n/a',
      receiverAppId,
      expectedAppId: CAST_RECEIVER_APP_ID,
      sessionObj: session && typeof session.getSessionObj === 'function' ? session.getSessionObj() : session
    });

    const directContentType = streamContentType(stream);
    const isHls = directContentType === 'application/vnd.apple.mpegurl';
    const candidates = [{ url: stream.url, contentType: directContentType }];
    if (isHls) {
      const hsf = (cast.framework.messages && cast.framework.messages.HlsSegmentFormat) || {};
      const hlsSegmentFormats = [
        hsf.TS_AAC || 'ts_aac',
        hsf.TS_HE_AAC || 'ts_he_aac',
        hsf.TS || 'ts',
        hsf.AAC || 'aac',
        hsf.MP3 || 'mp3'
      ];
      let resolvedUrl = stream.url;
      try {
        const redirectResponse = await fetch(stream.url, { redirect: 'follow', signal: AbortSignal.timeout(10000) });
        if (redirectResponse.ok && redirectResponse.url && redirectResponse.url !== stream.url) {
          console.log('OpenRadio-IN: resolved HLS redirect', stream.url, '->', redirectResponse.url);
          resolvedUrl = redirectResponse.url;
        }
      } catch (error) {
        console.warn('Cast HLS redirect resolve failed:', error.message || error);
      }
      candidates.length = 0;
      hlsSegmentFormats.forEach((fmt) => candidates.push({ url: resolvedUrl, contentType: directContentType, hlsSegmentFormat: fmt }));
      if (resolvedUrl !== stream.url) {
        candidates.push({ url: stream.url, contentType: directContentType, hlsSegmentFormat: hlsSegmentFormats[0] });
      }
    }
    if (HLS_PROXY_URL && !isHls) {
      try {
        const probeResponse = await fetch(`${HLS_PROXY_URL}?probe=1&url=${encodeURIComponent(stream.url)}`, { signal: AbortSignal.timeout(10000) });
        if (probeResponse.ok) {
          const probe = await probeResponse.json();
          if (probe && probe.contentType && probe.url) {
            candidates.push({
              url: `${HLS_PROXY_URL}?url=${encodeURIComponent(probe.url)}&contentType=${encodeURIComponent(probe.contentType)}`,
              contentType: normalizeCastContentType(probe.contentType, probe.url)
            });
          }
        }
      } catch (error) {
        console.warn('Cast proxy probe failed:', error.message || error);
      }
    }

    async function loadOnCast(candidate) {
      const normalizedCt = normalizeCastContentType(candidate.contentType, candidate.url);
      console.log('OpenRadio-IN: launching Cast media', { url: candidate.url, normalizedCt, hlsSegmentFormat: candidate.hlsSegmentFormat, station: station.name, sessionId: session.getSessionId ? session.getSessionId() : 'n/a' });
      if (normalizedCt === 'application/vnd.apple.mpegurl') {
        const mediaInfo = new cast.framework.messages.MediaInformation();
        mediaInfo.contentId = candidate.url;
        mediaInfo.contentType = normalizedCt;
        mediaInfo.streamType = cast.framework.messages.StreamType.LIVE;
        if (candidate.hlsSegmentFormat) mediaInfo.hlsSegmentFormat = candidate.hlsSegmentFormat;
        mediaInfo.metadata = {
          metadataType: (cast.framework.messages.MetadataType && cast.framework.messages.MetadataType.MUSIC_TRACK) || 1,
          title: station.name,
          artist: station.language || 'OpenRadio-IN'
        };
        if (station.logo) mediaInfo.metadata.images = [{ url: station.logo }];
        const request = new cast.framework.messages.LoadRequestData();
        request.media = mediaInfo;
        await session.loadMedia(request);
        return;
      }
      const media = new chrome.cast.media.MediaInfo(candidate.url, normalizedCt);
      media.streamType = chrome.cast.media.StreamType.LIVE;
      media.metadata = new chrome.cast.media.MusicTrackMediaMetadata();
      media.metadata.title = station.name;
      media.metadata.artist = station.language || 'OpenRadio-IN';
      if (station.logo) media.metadata.images = [new chrome.cast.Image(station.logo)];
      await session.loadMedia(new chrome.cast.media.LoadRequest(media));
    }

    let lastError = '';
    for (const candidate of candidates) {
      try {
        await loadOnCast(candidate);
        state.playing = true;
        state.paused = false;
        setStatus(t('status.playingCast', { name: station.name }));
        updatePlayer();
        renderStationLists();
        return;
      } catch (error) {
        lastError = error.message || String(error);
        console.warn('Cast loadMedia failed:', candidate.url, 'hlsSegmentFormat:', candidate.hlsSegmentFormat, error);
      }
    }

    state.playing = false;
    state.paused = false;
    setStatus(t('status.castError', { error: lastError || t('status.castError') }));
    updatePlayer();
    renderStationLists();
  }

  function setupCastPlayer() {
    if (castPlayerController) return;
    castPlayer = new cast.framework.RemotePlayer();
    castPlayerController = new cast.framework.RemotePlayerController(castPlayer);
    castPlayerController.addEventListener(cast.framework.RemotePlayerEventType.IS_PAUSED_CHANGED, () => {
      state.playing = !castPlayer.isPaused;
      state.paused = castPlayer.isPaused;
      updatePlayer();
      renderStationLists();
    });
  }

  function initializeCast() {
    if (typeof window.cast === 'undefined' || castContext) return;
    castContext = cast.framework.CastContext.getInstance();
    castContext.setOptions({ receiverApplicationId: CAST_RECEIVER_APP_ID, autoJoinPolicy: chrome.cast.AutoJoinPolicy.ORIGIN_SCOPED });
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
          state.paused = false;
          break;
      }
      updatePlayer();
    });
  }

  /* ---------- Playback & Retry ---------- */

  function advanceStream() {
    const station = state.currentStation;
    if (!station || state.userInitiatedStop) return false;
    const streams = sortStreamsForPlayback(station.streams);
    const next = (state.streamIndex || 0) + 1;
    if (next >= streams.length || !streams[next]) return false;
    state.streamSwitching = true;
    setStatus(t('status.tryingBackup'));
    playStation(station, next);
    return true;
  }

  async function playStation(station, streamIndex = 0) {
    const streams = sortStreamsForPlayback(station.streams);
    if (!streams.length) {
      setStatus(t('status.noStream'));
      return;
    }
    const stream = streams[streamIndex];
    if (!stream) {
      state.streamSwitching = false;
      state.playing = false;
      state.pauseIntent = false;
      setStatus(t('status.streamFailed'));
      updatePlayer();
      renderStationLists();
      return;
    }
    state.streamIndex = streamIndex;
    const generation = ++state.playGeneration;

    if (isCasting()) {
      await castStation(station);
      return;
    }

    state.currentStation = station;
    state.retryCount = 0;
    state.userInitiatedStop = false;
    state.paused = false;
    state.pauseIntent = true;
    state.pendingAutoResume = false;
    state.resumeAttempts = 0;
    state.nowPlayingTrack = '';
    state.nowPlayingArt = '';
    elements.nowPlayingTrack.hidden = true;
    clearEpg();
    fetchEpg(station);

    if (state.hls) {
      state.hls.destroy();
      state.hls = null;
    }

    const isHls = isHlsStream(stream);

    if (isHls) {
      if (!window.Hls || !window.Hls.isSupported()) {
        setStatus(t('status.hlsUnsupported'));
        updatePlayer();
        renderStationLists();
        return;
      }
      elements.audio.src = '';
      setStatus(t('status.loadingHls'));
      let started = false;
      let loadReject = null;
      state.hls = new window.Hls({
        startLevel: 0,
        enableWorker: true,
        maxBufferLength: 90,
        maxMaxBufferLength: 120,
        backBufferLength: 30,
        liveSyncDurationCount: 6,
        liveMaxLatencyDurationCount: 12,
        fragLoadingMaxRetry: 8,
        manifestLoadingMaxRetry: 4
      });
      const hls = state.hls;
      hls.on(window.Hls.Events.ERROR, (event, data) => {
        if (!data.fatal) return;
        if (state.playGeneration !== generation) return;
        console.warn('HLS fatal error:', data.type, data.details);
        try { hls.destroy(); } catch {}
        if (state.hls === hls) state.hls = null;
        if (loadReject) {
          // failed during initial load — let the load promise reject and handle fallback
          loadReject(new Error(data.type));
          return;
        }
        if (started && !state.streamSwitching && !advanceStream()) {
          state.streamSwitching = false;
          state.playing = false;
          state.pauseIntent = false;
          setStatus(t('status.streamFailed'));
          updatePlayer();
          renderStationLists();
        }
      });
      hls.loadSource(stream.url);
      hls.attachMedia(elements.audio);
      try {
        await new Promise((resolve, reject) => {
          loadReject = reject;
          const timeout = setTimeout(() => reject(new Error('HLS load timed out')), 20000);
          hls.on(window.Hls.Events.MANIFEST_PARSED, () => { clearTimeout(timeout); resolve(); });
        });
      } catch (error) {
        if (state.playGeneration !== generation) return;
        if (state.userInitiatedStop) {
          updatePlayer();
          renderStationLists();
          return;
        }
        if (!advanceStream()) {
          state.streamSwitching = false;
          state.playing = false;
          state.pauseIntent = false;
          setStatus(t('status.loadFailed'));
          console.error(error);
          updatePlayer();
          renderStationLists();
        }
        return;
      }
      if (state.userInitiatedStop || state.paused) {
        try { hls.destroy(); } catch {}
        if (state.hls === hls) state.hls = null;
        updatePlayer();
        renderStationLists();
        return;
      }
      try {
        await elements.audio.play();
      } catch (error) {
        if (state.playGeneration !== generation) return;
        if (state.userInitiatedStop || state.paused) {
          updatePlayer();
          renderStationLists();
          return;
        }
        if (!advanceStream()) {
          state.streamSwitching = false;
          state.playing = false;
          state.pauseIntent = false;
          setStatus(t('status.playFailed'));
          console.error(error);
          updatePlayer();
          renderStationLists();
        }
        return;
      }
      if (state.playGeneration !== generation || state.userInitiatedStop) return;
      state.streamSwitching = false;
      started = true;
      state.playing = true;
      setStatus(t('status.playing', { name: station.name }));
      localStorage.setItem('openradio-last-station', station.id);
      addRecentStation(station);
      startMetadataPolling(stream.url, station);
      updatePlayer();
      renderStationLists();
      return;
    }

    elements.audio.src = stream.url;
    elements.audio.load();
    try {
      await elements.audio.play();
      if (state.playGeneration !== generation || state.userInitiatedStop) return;
      state.streamSwitching = false;
      state.playing = true;
      setStatus(t('status.playing', { name: station.name }));
      localStorage.setItem('openradio-last-station', station.id);
      addRecentStation(station);
      startMetadataPolling(stream.url, station);
    } catch (error) {
      if (state.playGeneration !== generation) return;
      if (state.userInitiatedStop || state.paused) {
        updatePlayer();
        renderStationLists();
        return;
      }
      if (!advanceStream()) {
        state.streamSwitching = false;
        state.playing = false;
        state.pauseIntent = false;
        setStatus(t('status.streamFailed'));
        console.error(error);
      }
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
    setStatus(t('status.retrying', { sec: Math.round(delay / 1000), attempt: state.retryCount, max: state.maxRetries }));
    setTimeout(() => {
      if (state.userInitiatedStop) return;
      playStation(state.currentStation, state.streamIndex || 0);
    }, delay);
  }

  function stopPlayback() {
    state.userInitiatedStop = true;
    state.paused = false;
    state.pauseIntent = true;
    state.pendingAutoResume = false;
    state.resumeAttempts = 0;
    state.streamSwitching = false;
    if (state.hls) { state.hls.destroy(); state.hls = null; }
    elements.audio.pause();
    elements.audio.src = '';
    state.playing = false;
    state.nowPlayingTrack = '';
    state.nowPlayingArt = '';
    elements.nowPlayingTrack.hidden = true;
    stopMetadataPolling();
    clearEpg();
    updatePlayer();
    renderStationLists();
  }

  function pausePlayback() {
    if (!state.currentStation) return;
    if (isCasting()) {
      if (castPlayerController) castPlayerController.playOrPause();
      return;
    }
    state.paused = true;
    state.pauseIntent = true;
    state.pendingAutoResume = false;
    state.resumeAttempts = 0;
    elements.audio.pause();
    updatePlayer();
    renderStationLists();
  }

  async function playPlayback() {
    if (!state.currentStation) return;
    if (isCasting()) {
      if (castPlayerController) castPlayerController.playOrPause();
      return;
    }
    if (state.playing) return;
    state.userInitiatedStop = false;
    state.pauseIntent = false;
    state.pendingAutoResume = false;
    state.resumeAttempts = 0;
    if (state.paused) {
      state.paused = false;
      try {
        await elements.audio.play();
      } catch (error) {
        console.error(error);
        await playStation(state.currentStation, state.streamIndex || 0);
      }
      updatePlayer();
      renderStationLists();
      return;
    }
    await playStation(state.currentStation, state.streamIndex || 0);
  }

  function scheduleAutoResume() {
    state.pendingAutoResume = true;
    state.resumeAttempts = 0;
    setTimeout(attemptResume, RESUME_GRACE_MS);
  }

  function tryPlayAudio() {
    const promise = elements.audio.play();
    return new Promise((resolve) => {
      const timer = setTimeout(() => resolve(false), 2500);
      promise
        .then(() => { clearTimeout(timer); resolve(true); })
        .catch(() => { clearTimeout(timer); resolve(false); });
    });
  }

  function attemptResume() {
    if (!state.pendingAutoResume || !state.currentStation) return;
    if (state.userInitiatedStop || state.paused || state.pauseIntent) {
      state.pendingAutoResume = false;
      state.resumeAttempts = 0;
      return;
    }
    if (isCasting()) {
      state.pendingAutoResume = false;
      return;
    }

    if (!elements.audio.paused) {
      // Audio recovered on its own (e.g. HLS.js resumed) — reconcile state so the
      // play/stop buttons reflect reality instead of staying stuck on the reset state.
      state.playing = true;
      state.paused = false;
      state.pendingAutoResume = false;
      state.resumeAttempts = 0;
      updatePlayer();
      renderStationLists();
      return;
    }

    const canReplay = Boolean(elements.audio.src) || Boolean(state.hls);
    if (!canReplay) {
      if (document.hidden) {
        setTimeout(attemptResume, RESUME_BACKOFF_MS[RESUME_BACKOFF_MS.length - 1]);
      } else {
        state.pendingAutoResume = false;
        state.resumeAttempts = 0;
        playStation(state.currentStation, state.streamIndex || 0);
      }
      return;
    }

    tryPlayAudio().then((resumed) => {
      if (!state.pendingAutoResume) return;
      if (resumed) {
        state.pendingAutoResume = false;
        state.resumeAttempts = 0;
        setStatus(t('status.resumed'));
        return;
      }
      state.resumeAttempts += 1;
      if (!document.hidden && state.resumeAttempts > RESUME_MAX_ATTEMPTS) {
        state.pendingAutoResume = false;
        state.resumeAttempts = 0;
        return;
      }
      const delay = RESUME_BACKOFF_MS[Math.min(state.resumeAttempts, RESUME_BACKOFF_MS.length - 1)];
      setTimeout(attemptResume, delay);
    });
  }

  async function togglePlayback() {
    if (!state.currentStation) return;
    const actuallyPlaying = !elements.audio.paused;
    if (state.playing || actuallyPlaying) {
      pausePlayback();
    } else {
      await playPlayback();
    }
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
    if (state.currentStation?.id === station.id) {
      const actuallyPlaying = !elements.audio.paused;
      if (state.playing || actuallyPlaying) pausePlayback();
      else if (state.paused) await playPlayback();
      else await playStation(station);
    } else {
      await playStation(station);
    }
  }

  /* ---------- Now Playing Sheet ---------- */

  function openNowPlaying() {
    elements.nowPlaying.hidden = false;
    document.body.style.overflow = 'hidden';
    renderEpg();
  }

  function closeNowPlaying() {
    elements.nowPlaying.hidden = true;
    document.body.style.overflow = '';
  }

  /* ---------- Sleep Timer ---------- */

  function formatSleepTimerRemaining(ms) {
    const totalSeconds = Math.max(0, Math.ceil(ms / 1000));
    const h = Math.floor(totalSeconds / 3600);
    const m = Math.floor((totalSeconds % 3600) / 60);
    const s = totalSeconds % 60;
    if (h > 0) return `${h}h ${m}m ${s}s`;
    if (m > 0) return `${m}m ${s}s`;
    return `${s}s`;
  }

  function updateSleepTimerDisplay() {
    if (!state.sleepTimerEnd) {
      elements.sleepTimerStatus.hidden = true;
      elements.sleepTimerBtn.textContent = '\u23F0 ' + t('np.timer');
      return;
    }
    const time = formatSleepTimerRemaining(state.sleepTimerEnd - Date.now());
    elements.sleepTimerStatus.hidden = false;
    elements.sleepTimerStatus.textContent = t('np.sleeptimer', { time });
    elements.sleepTimerBtn.textContent = `\u23F0 ${time}`;
  }

  function setSleepTimer(minutes) {
    if (state.sleepTimerId) {
      clearTimeout(state.sleepTimerId);
      state.sleepTimerId = null;
    }
    if (state.sleepTimerIntervalId) {
      clearInterval(state.sleepTimerIntervalId);
      state.sleepTimerIntervalId = null;
    }
    state.sleepTimerEnd = null;
    if (minutes <= 0) {
      elements.sleepTimerPicker.hidden = true;
      elements.sleepTimerStatus.hidden = true;
      elements.sleepTimerBtn.textContent = '\u23F0 ' + t('np.timer');
      return;
    }
    state.sleepTimerEnd = Date.now() + minutes * 60 * 1000;
    state.sleepTimerId = setTimeout(() => {
      stopPlayback();
      state.sleepTimerId = null;
      state.sleepTimerEnd = null;
      if (state.sleepTimerIntervalId) {
        clearInterval(state.sleepTimerIntervalId);
        state.sleepTimerIntervalId = null;
      }
      elements.sleepTimerStatus.hidden = true;
      elements.sleepTimerBtn.textContent = '\u23F0 ' + t('np.timer');
      setStatus(t('status.sleeptimerStopped'));
      updatePlayer();
    }, minutes * 60 * 1000);
    state.sleepTimerIntervalId = setInterval(updateSleepTimerDisplay, 1000);
    elements.sleepTimerPicker.hidden = true;
    updateSleepTimerDisplay();
  }

  /* ---------- Share ---------- */

  async function shareStation() {
    const station = state.currentStation;
    if (!station) return;
    const shareData = {
      title: station.name,
      text: t('share.text', { name: station.name }),
      url: `${window.location.origin}${window.location.pathname}?station=${station.id}`
    };
    if (navigator.share) {
      try { await navigator.share(shareData); } catch {}
    } else {
      try { await navigator.clipboard.writeText(shareData.url); setStatus(t('status.copied')); } catch {}
    }
  }

  /* ---------- Stream Metadata ---------- */

  async function fetchStreamMetadata(streamUrl, station) {
    try {
      const metaUrl = `${HLS_PROXY_URL}?meta=1&url=${encodeURIComponent(streamUrl)}`;
      const response = await fetch(metaUrl, { signal: AbortSignal.timeout(5000) });
      const data = await response.json();
      let title = data.streamTitle || '';
      if (!title && station?.metadata_url) {
        const statusUrl = `${HLS_PROXY_URL}?meta=1&metaUrl=${encodeURIComponent(station.metadata_url)}`;
        const statusResponse = await fetch(statusUrl, { signal: AbortSignal.timeout(5000) });
        const statusData = await statusResponse.json();
        title = statusData.streamTitle || '';
      }
      applyTrackMetadata(title, station);
    } catch (error) {
      console.warn('Metadata fetch failed:', error.message || error);
    }
  }

  function applyTrackMetadata(title, station) {
    if (!title || title === state.nowPlayingTrack) return;
    state.nowPlayingTrack = title;
    elements.nowPlayingTrack.textContent = title;
    elements.nowPlayingTrack.hidden = false;
    updateMediaSession();
    lookupArtwork(title, station);
  }

  function splitArtistTrack(title) {
    const separator = title.indexOf(' - ');
    if (separator > 0) {
      const artist = title.slice(0, separator).trim();
      const track = title.slice(separator + 3).trim();
      return [artist, track].filter(Boolean);
    }
    return ['', title.trim()];
  }

  function getCachedArtwork(key) {
    try {
      const store = JSON.parse(localStorage.getItem(ART_CACHE_KEY) || '{}');
      const entry = store[key];
      if (entry && Date.now() - entry.t < ART_CACHE_TTL) return entry.url;
    } catch {}
    return '';
  }

  function setCachedArtwork(key, url) {
    try {
      const store = JSON.parse(localStorage.getItem(ART_CACHE_KEY) || '{}');
      store[key] = { url, t: Date.now() };
      localStorage.setItem(ART_CACHE_KEY, JSON.stringify(store));
    } catch {}
  }

  async function lookupArtwork(title, station) {
    const key = title.toLowerCase().trim();
    const cached = getCachedArtwork(key);
    if (cached) {
      applyArtwork(cached);
      return;
    }
    const [artist, track] = splitArtistTrack(title);
    const term = [artist, track].filter(Boolean).join(' ');
    try {
      const response = await fetch(
        `https://itunes.apple.com/search?term=${encodeURIComponent(term)}&media=music&entity=song&limit=1`,
        { signal: AbortSignal.timeout(6000) }
      );
      const data = await response.json();
      const result = data.results && data.results[0];
      if (result && result.artworkUrl100) {
        const art = result.artworkUrl100.replace('100x100', '600x600');
        setCachedArtwork(key, art);
        applyArtwork(art);
      } else {
        applyArtwork('');
      }
    } catch (error) {
      console.warn('Artwork lookup failed:', error.message || error);
      applyArtwork('');
    }
  }

  function applyArtwork(art) {
    state.nowPlayingArt = art || '';
    renderNowPlayingArt();
    updateMediaSession();
  }

  function startMetadataPolling(url, station) {
    stopMetadataPolling();
    fetchStreamMetadata(url, station);
    state.metadataIntervalId = setInterval(() => fetchStreamMetadata(url, station), 30000);
  }

  function stopMetadataPolling() {
    if (state.metadataIntervalId) {
      clearInterval(state.metadataIntervalId);
      state.metadataIntervalId = null;
    }
  }

  /* ---------- EPG (scheduled programming) ---------- */

  function istNowParts() {
    const ist = new Date(Date.now() + 5.5 * 3600 * 1000);
    const yyyy = ist.getUTCFullYear();
    const mm = String(ist.getUTCMonth() + 1).padStart(2, '0');
    const dd = String(ist.getUTCDate()).padStart(2, '0');
    return { date: `${yyyy}-${mm}-${dd}`, minutes: ist.getUTCHours() * 60 + ist.getUTCMinutes() };
  }

  function parseEpgTime(timeStr) {
    const match = String(timeStr || '').trim().match(/^(\d{1,2}):(\d{2})\s*(AM|PM)$/i);
    if (!match) return null;
    let hours = parseInt(match[1], 10);
    const minutes = parseInt(match[2], 10);
    const period = match[3].toUpperCase();
    if (period === 'AM' && hours === 12) hours = 0;
    if (period === 'PM' && hours !== 12) hours += 12;
    return hours * 60 + minutes;
  }

  function getCurrentEpgProgram() {
    const programs = state.epgPrograms;
    if (!programs || !programs.length) return null;
    const now = istNowParts().minutes;
    for (let i = 0; i < programs.length; i++) {
      const start = parseEpgTime(programs[i].start);
      const end = parseEpgTime(programs[i].end);
      if (start === null || end === null) continue;
      if (now >= start && now < end) {
        return { program: programs[i], next: i + 1 < programs.length ? programs[i + 1] : null };
      }
    }
    return null;
  }

  function renderEpg() {
    if (!elements.nowPlayingEpg) return;
    const hit = getCurrentEpgProgram();
    if (!state.currentStation || !hit) {
      elements.nowPlayingEpg.hidden = true;
      updateMediaSession();
      return;
    }
    const current = hit.program;
    const next = hit.next;

    const rows = [];

    const nowRow = makeElement('div', 'epg-row epg-now');
    nowRow.append(makeElement('span', 'epg-label', t('np.now')), document.createTextNode(`  ${current.title}`));
    rows.push(nowRow);

    if (next && parseEpgTime(next.start) !== null) {
      const nextRow = makeElement('div', 'epg-row epg-next');
      nextRow.append(makeElement('span', 'epg-label', t('np.next')), document.createTextNode(' '));
      nextRow.append(makeElement('span', 'epg-time', next.start));
      nextRow.append(document.createTextNode(`  ${next.title}`));
      rows.push(nextRow);
    }

    elements.nowPlayingEpg.replaceChildren(...rows);
    elements.nowPlayingEpg.hidden = false;
    updateMediaSession();
  }

  function startEpgTimer() {
    stopEpgTimer();
    state.epgRefreshIntervalId = setInterval(renderEpg, 60000);
  }

  function stopEpgTimer() {
    if (state.epgRefreshIntervalId) {
      clearInterval(state.epgRefreshIntervalId);
      state.epgRefreshIntervalId = null;
    }
  }

  function clearEpg() {
    stopEpgTimer();
    state.epgPrograms = null;
    state.epgDate = null;
    if (elements.nowPlayingEpg) elements.nowPlayingEpg.hidden = true;
  }

  function parseCuesheetHtml(html) {
    const doc = new DOMParser().parseFromString(html, 'text/html');
    const dateMatch = html.match(/(\d{2})-(\d{2})-(\d{4})/);
    const date = dateMatch ? `${dateMatch[3]}-${dateMatch[2]}-${dateMatch[1]}` : '';
    const table = doc.getElementById('st');
    const programs = [];
    if (!table) return { date, programs };
    const cellText = (cell) => (cell ? cell.textContent.replace(/\s+/g, ' ').trim() : '');
    table.querySelectorAll('tbody tr').forEach((tr) => {
      const cells = tr.querySelectorAll('td');
      if (cells.length < 8) return;
      const start = cellText(cells[1]);
      const end = cellText(cells[2]);
      const title = cellText(cells[4]);
      if (!/^\d{1,2}:\d{2}\s*(AM|PM)/i.test(start)) return;
      if (!/^\d{1,2}:\d{2}\s*(AM|PM)/i.test(end)) return;
      if (!title) return;
      programs.push({
        start,
        end,
        title,
        language: cellText(cells[5]),
        type: cellText(cells[6]),
      });
    });
    return { date, programs };
  }

  async function fetchEpg(station) {
    const epgId = station && station.epg_id;
    if (!epgId) return;
    const today = istNowParts().date;
    let programs = null;
    try {
      const cacheKey = `openradio-epg-${epgId}`;
      const cached = JSON.parse(localStorage.getItem(cacheKey) || 'null');
      if (cached && cached.date === today && Array.isArray(cached.programs)) {
        programs = cached.programs;
      }
      if (!programs) {
        let data = null;
        // The cuesheet site sends ACAO:*, so the browser can fetch it directly.
        try {
          const response = await fetch(`https://cuesheets.prasarbharati.org/viewsheet/${encodeURIComponent(epgId)}`, { signal: AbortSignal.timeout(8000) });
          if (response.ok) data = parseCuesheetHtml(await response.text());
        } catch {}
        // Fall back to the proxy worker if the direct fetch is blocked/unreachable.
        if (!data || !data.programs || !data.programs.length) {
          try {
            const response = await fetch(`${HLS_PROXY_URL}?epg=${encodeURIComponent(epgId)}`, { signal: AbortSignal.timeout(8000) });
            if (response.ok) data = await response.json();
          } catch {}
        }
        if (data && data.date === today && Array.isArray(data.programs) && data.programs.length) {
          programs = data.programs;
          try {
            localStorage.setItem(cacheKey, JSON.stringify({ date: today, programs }));
          } catch {}
        }
      }
    } catch (error) {
      console.warn('EPG fetch failed:', error.message || error);
    }
    state.epgPrograms = programs;
    state.epgDate = today;
    startEpgTimer();
    renderEpg();
  }

  /* ---------- Keyboard Shortcuts ---------- */

  function handleKeydown(e) {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA' || e.target.tagName === 'SELECT') return;
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
      populateAlarmStations();
      applyFilters();
      updatePlayer();
      setStatus(t('status.loaded', { n: state.stations.length }));
    } catch (error) {
      console.error(error);
      setStatus(t('status.error'));
      elements.resultsCount.textContent = t('status.dataUnavailable');
      elements.stations.replaceChildren(makeElement('div', 'empty-state', t('status.emptyData')));
    }
  }

  /* ---------- Alarm ---------- */

  function populateAlarmStations() {
    const options = state.stations.map((station) => {
      const option = makeElement('option', '', station.name);
      option.value = station.id;
      return option;
    });
    elements.alarmStationSelect.replaceChildren(...options);
    if (state.alarm) {
      elements.alarmTimeInput.value = state.alarm.time || '';
      elements.alarmStationSelect.value = state.stations.some((s) => s.id === state.alarm.stationId) ? state.alarm.stationId : '';
    }
    renderAlarmStatus();
  }

  function renderAlarmStatus() {
    if (!state.alarm) {
      elements.alarmStatus.hidden = true;
      elements.alarmStatus.textContent = '';
      elements.alarmOff.disabled = true;
      elements.alarmBtn.textContent = '\u23F0 ' + t('np.alarm');
      return;
    }
    elements.alarmStatus.hidden = false;
    elements.alarmStatus.textContent = t('alarm.status', { time: state.alarm.time });
    elements.alarmOff.disabled = false;
    elements.alarmBtn.textContent = `\u23F0 ${state.alarm.time}`;
  }

  function saveAlarm() {
    localStorage.setItem('openradio-alarm', JSON.stringify(state.alarm));
    renderAlarmStatus();
  }

  function setAlarm(time, stationId) {
    if (!time || !stationId) return;
    state.alarm = { time, stationId };
    saveAlarm();
    elements.alarmTimeInput.value = time;
    elements.alarmStationSelect.value = stationId;
    elements.alarmPicker.hidden = true;
  }

  function clearAlarm() {
    state.alarm = null;
    localStorage.removeItem('openradio-alarm');
    renderAlarmStatus();
  }

  function checkAlarm() {
    if (!state.alarm) return;
    const now = new Date();
    const nowTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
    if (nowTime !== state.alarm.time) return;
    const station = state.stations.find((s) => s.id === state.alarm.stationId);
    state.alarm = null;
    localStorage.removeItem('openradio-alarm');
    renderAlarmStatus();
    if (station) {
      playStation(station).then(() => setStatus(t('alarm.fired', { name: station.name })));
    }
  }

  /* ---------- UI Language ---------- */

  function applyUiLanguage() {
    localStorage.setItem('openradio-ui-lang', uiLang);
    document.documentElement.lang = uiLang;
    elements.uiLang.value = uiLang;
    document.querySelectorAll('[data-i18n]').forEach((el) => { el.textContent = t(el.dataset.i18n); });
    document.querySelectorAll('[data-i18n-placeholder]').forEach((el) => { el.placeholder = t(el.dataset.i18nPlaceholder); });
    renderThemeToggle();
    applyFilters();
    updatePlayer();
    renderAlarmStatus();
    updateSleepTimerDisplay();
  }

  /* ---------- Init ---------- */

  setTheme(state.theme);
  watchSystemTheme();
  applyVolume(state.volume);
  setupMediaSession();
  restoreCollapsedStates();
  applyUiLanguage();

  elements.search.addEventListener('input', (event) => {
    state.search = event.target.value;
    applyFilters();
  });
  elements.filters.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-category]');
    if (!button) return;
    state.activeCategory = button.dataset.category;
    saveFilters();
    applyFilters();
  });
  elements.languageSelect.addEventListener('change', (event) => {
    state.activeLanguage = event.target.value;
    saveFilters();
    applyFilters();
  });
  elements.uiLang.addEventListener('change', (event) => {
    uiLang = event.target.value;
    applyUiLanguage();
  });
  elements.featured.addEventListener('click', handleStationAction);
  elements.recentStations.addEventListener('click', handleStationAction);
  elements.stations.addEventListener('click', handleStationAction);
  elements.playToggle.addEventListener('click', togglePlayback);
  elements.stopBtn.addEventListener('click', stopPlayback);
  elements.prevBtn.addEventListener('click', () => playAdjacentStation(-1));
  elements.nextBtn.addEventListener('click', () => playAdjacentStation(1));
  elements.npPlayToggle.addEventListener('click', togglePlayback);
  elements.npStopBtn.addEventListener('click', stopPlayback);
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

  elements.alarmBtn.addEventListener('click', () => {
    elements.alarmPicker.hidden = !elements.alarmPicker.hidden;
  });
  elements.alarmSet.addEventListener('click', () => {
    setAlarm(elements.alarmTimeInput.value, elements.alarmStationSelect.value);
  });
  elements.alarmOff.addEventListener('click', clearAlarm);
  setInterval(checkAlarm, 15000);
  checkAlarm();

  elements.audio.addEventListener('play', () => {
    state.playing = true;
    state.paused = false;
    state.pauseIntent = false;
    state.pendingAutoResume = false;
    state.resumeAttempts = 0;
    state.retryCount = 0;
    updatePlayer();
    renderStationLists();
  });
  function resumeInterruptedPlayback() {
    if (state.pendingAutoResume) {
      attemptResume();
      return;
    }
    if (state.currentStation && !state.userInitiatedStop && !state.paused && !state.pauseIntent && elements.audio.paused) {
      scheduleAutoResume();
    }
  }

  elements.audio.addEventListener('pause', () => {
    state.playing = false;
    if (state.pauseIntent) {
      state.pauseIntent = false;
    } else if (state.currentStation && !state.userInitiatedStop) {
      scheduleAutoResume();
    }
    updatePlayer();
    renderStationLists();
  });
  elements.audio.addEventListener('stalled', () => {
    if (state.playing && !state.userInitiatedStop && !state.pauseIntent && !isCasting()) scheduleAutoResume();
  });
  elements.audio.addEventListener('waiting', () => {
    if (state.playing && !state.userInitiatedStop && !state.pauseIntent && !isCasting()) scheduleAutoResume();
  });
  elements.audio.addEventListener('ended', () => {
    if (state.hls) { state.hls.destroy(); state.hls = null; }
    state.playing = false;
    state.paused = false;
    state.nowPlayingTrack = '';
    elements.nowPlayingTrack.hidden = true;
    stopMetadataPolling();
    setStatus(t('status.ended'));
    updatePlayer();
    renderStationLists();
  });
  elements.audio.addEventListener('error', () => {
    if (state.streamSwitching) {
      updatePlayer();
      renderStationLists();
      return;
    }
    if (state.hls) { state.hls.destroy(); state.hls = null; }
    state.playing = false;
    state.paused = false;
    state.pendingAutoResume = false;
    state.nowPlayingTrack = '';
    elements.nowPlayingTrack.hidden = true;
    stopMetadataPolling();
    if (state.currentStation && !state.userInitiatedStop && !advanceStream()) {
      state.streamSwitching = false;
      state.userInitiatedStop = false;
      retryPlayback();
      updatePlayer();
      renderStationLists();
      return;
    }
    state.userInitiatedStop = false;
    updatePlayer();
    renderStationLists();
  });

  document.addEventListener('keydown', handleKeydown);

  document.addEventListener('visibilitychange', () => {
    if (document.hidden) return;
    resumeInterruptedPlayback();
  });
  window.addEventListener('focus', resumeInterruptedPlayback);
  document.addEventListener('pageshow', resumeInterruptedPlayback);

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
  window.addEventListener('appinstalled', () => { elements.install.hidden = true; setStatus(t('status.appInstalled')); });

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
