// O PWA foi removido do site. Este arquivo permanece apenas para que
// navegadores que registraram o service worker antigo o substituam,
// apaguem os caches e se desregistrem.
self.addEventListener('install', () => self.skipWaiting())

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const keys = await caches.keys()
    await Promise.all(keys.map(key => caches.delete(key)))
    await self.registration.unregister()
    const clients = await self.clients.matchAll({ type: 'window' })
    await Promise.all(clients.map(client => client.navigate(client.url)))
  })())
})
