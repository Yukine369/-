// Офлайн-кэш для сайта «Ежедневки». При выпуске новой версии меняйте имя кэша.
const CACHE = 'ezhednevki-v2.2.0';
const FILES = ['./', './index.html', './manifest.webmanifest', './icons/icon-192.png', './icons/icon-512.png'];
self.addEventListener('install', e => { e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES))); self.skipWaiting(); });
self.addEventListener('activate', e => { e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k))))); self.clients.claim(); });
self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);
  // APK и version.json всегда берём из сети, чтобы проверка обновлений видела свежую версию
  if (e.request.method !== 'GET' || url.pathname.endsWith('.apk') || url.pathname.endsWith('version.json')) return;
  e.respondWith(fetch(e.request).then(r => { if (url.origin === location.origin) { const cp = r.clone(); caches.open(CACHE).then(c => c.put(e.request, cp)); } return r; })
    .catch(() => caches.match(e.request).then(r => r || caches.match('./index.html'))));
});
