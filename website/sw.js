const CACHE_NAME = 'openradio-in-v4';
const APP_SHELL = [
  './',
  './index.html',
  './manifest.webmanifest',
  './assets/css/style.css',
  './assets/js/app.js',
  './icons/icon.svg',
  './data/stations.json'
];

self.addEventListener('install', (event) => {
  // data/stations.json is created by the GitHub Pages workflow. Keeping an
  // individual cache failure non-fatal also makes local development work.
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => Promise.all(APP_SHELL.map((asset) => cache.add(asset).catch(() => undefined)))));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key)))));
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;
  if (event.request.mode === 'navigate') {
    event.respondWith(fetch(event.request).catch(() => caches.match('./index.html')));
    return;
  }
  event.respondWith(caches.match(event.request).then((cached) => cached || fetch(event.request)));
});
