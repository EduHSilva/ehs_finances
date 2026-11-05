export function apiOrigin() {
  return import.meta.client ? window.location.origin : ''
}
