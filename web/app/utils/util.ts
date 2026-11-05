export function getTokenCookie() {
  return useCookie<{ token: string | null } | null>('token')
}

export function formatCurrency(value: number) {
  const locale = typeof navigator === 'undefined' ? 'pt-BR' : navigator.language
  if (locale == 'en') {
    return 'U$ ' + value.toFixed(2)
  }
  return 'R$ ' + value.toFixed(2)
}

export function formatDate(value: string | Date) {
  const locale = typeof navigator === 'undefined' ? 'pt-BR' : navigator.language
  if (locale == 'en') {
    return new Date(value).toLocaleDateString()
  }
  return new Date(value).toLocaleDateString()
}
