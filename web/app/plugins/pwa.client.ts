export default defineNuxtPlugin(() => {
  if (!('serviceWorker' in navigator)) return

  const appBase = useRuntimeConfig().app.baseURL

  window.addEventListener('load', () => {
    navigator.serviceWorker.register(`${appBase}sw.js`)
  })
})
