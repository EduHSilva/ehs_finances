export function getTokenCookie() {
  // Persistente: sem maxAge o cookie morria ao fechar o navegador. A validade real
  // da sessão é a do JWT, renovada por `/users/auth/me` a cada carregamento.
  return useCookie<{ token: string | null } | null>('token', { maxAge: 60 * 60 * 24 * 30 })
}

export function formatCurrency(value: number, appLanguage?: string) {
  const language = appLanguage ?? useI18n().locale.value
  const locale = language === 'pt' ? 'pt-BR' : 'en-US'
  const currency = language === 'pt' ? 'BRL' : 'USD'
  return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(value)
}

export function formatDate(value: string | Date, appLanguage?: string) {
  const language = appLanguage ?? useI18n().locale.value
  const locale = language === 'pt' ? 'pt-BR' : 'en-US'
  const datePart = typeof value === 'string' ? /^(\d{4}-\d{2}-\d{2})/.exec(value)?.[1] : undefined
  const date = datePart
    ? new Date(`${datePart}T12:00:00`)
    : new Date(value)
  return date.toLocaleDateString(locale)
}

export function toExecutionDate(value: { toString(): string } | Date) {
  const date = value instanceof Date
    ? `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`
    : value.toString()
  return `${date.slice(0, 10)}T12:00:00`
}

export function parseLocalDate(value: string) {
  const [year, month, day] = (/^(\d{4}-\d{2}-\d{2})/.exec(value)?.[1] || value.slice(0, 10)).split('-').map(Number)
  return new Date(year!, month! - 1, day!)
}
