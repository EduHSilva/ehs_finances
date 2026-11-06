import { parseDate } from '@internationalized/date'

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
  const { locale: appLocale } = useI18n()
  const locale = appLocale.value === 'pt' ? 'pt-BR' : 'en-US'
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
  const datePart = /^(\d{4}-\d{2}-\d{2})/.exec(value)?.[1]
  return parseDate(datePart || value.slice(0, 10))
}
