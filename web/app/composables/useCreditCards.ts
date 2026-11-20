import type { CreditCard } from '~/types'

/**
 * Cartões da conta ativa. O cartão pertence à conta financeira, então a lista
 * é recarregada sempre que a conta ativa muda.
 */
export function useCreditCards() {
  const { $financesService } = useNuxtApp()
  const { activeAccount } = useFinancialAccounts()
  const cards = shallowRef<CreditCard[]>([])
  const loading = ref(false)
  const error = ref(false)

  async function loadCards(accountId?: string | null) {
    const id = accountId ?? activeAccount.value?.id
    if (!id) {
      cards.value = []
      return
    }
    loading.value = true
    error.value = false
    try {
      const response = await $financesService.getCards(id)
      cards.value = (response.data || []).filter(card => card.status !== 'DELETED')
    } catch {
      cards.value = []
      error.value = true
    } finally {
      loading.value = false
    }
  }

  watch(() => activeAccount.value?.id, id => loadCards(id), { immediate: true })

  const cardOptions = computed(() => cards.value.map(card => ({
    label: card.name,
    value: card.id
  })))

  function cardName(cardId?: string | null) {
    if (!cardId) return undefined
    return cards.value.find(card => card.id === cardId)?.name
  }

  return { cards, cardOptions, loading, error, loadCards, cardName }
}

export function lastDayOfMonth(year: number, month: number) {
  return new Date(year, month + 1, 0).getDate()
}

export function clampDay(year: number, month: number, day: number) {
  return Math.min(Math.max(1, day), lastDayOfMonth(year, month))
}

export function isoDate(year: number, month: number, day: number) {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

export function localIsoDate(value: Date) {
  return isoDate(value.getFullYear(), value.getMonth(), value.getDate())
}

/**
 * Ciclo da fatura identificada por `invoiceMonth` (AAAA-MM do vencimento):
 * fecha no `closingDay` anterior ao vencimento e começa no dia seguinte ao
 * fechamento do mês anterior. Cada compra do intervalo pertence à fatura.
 */
export function invoiceCycle(closingDay: number, dueDay: number, invoiceMonth: string) {
  const [dueYear, dueMonthNumber] = invoiceMonth.split('-').map(Number)
  const year = dueYear || new Date().getFullYear()
  const dueMonth = Math.max(0, (dueMonthNumber || 1) - 1)
  const closingDayInDueMonth = clampDay(year, dueMonth, closingDay)

  // O vencimento é o primeiro `dueDay` após o fechamento: se o dia de
  // vencimento é menor/igual ao de fechamento, o fechamento é do mês anterior.
  const closesPreviousMonth = dueDay <= closingDayInDueMonth
  const closingYear = closesPreviousMonth && dueMonth === 0 ? year - 1 : year
  const closingMonth = closesPreviousMonth ? (dueMonth === 0 ? 11 : dueMonth - 1) : dueMonth
  const closingDate = isoDate(closingYear, closingMonth, clampDay(closingYear, closingMonth, closingDay))

  const previousYear = closingMonth === 0 ? closingYear - 1 : closingYear
  const previousMonth = closingMonth === 0 ? 11 : closingMonth - 1
  const previousClosingDay = clampDay(previousYear, previousMonth, closingDay)
  const isLastDay = previousClosingDay === lastDayOfMonth(previousYear, previousMonth)
  const startDate = isLastDay
    ? isoDate(previousYear, previousMonth, previousClosingDay)
    : isoDate(previousYear, previousMonth, previousClosingDay + 1)

  return { startDate, closingDate }
}

/**
 * Mês (`AAAA-MM`) do ciclo atualmente em aberto para o cartão: o ciclo cujo
 * fechamento ainda não passou (ou ocorreu hoje). Compras feitas hoje entram nele.
 */
export function openInvoiceMonth(card: CreditCard, todayIso?: string) {
  const today = todayIso || localIsoDate(new Date())
  let year = Number(today.slice(0, 4))
  let month = Number(today.slice(5, 7)) - 1
  for (let i = 0; i < 3; i++) {
    const candidate = `${year}-${String(month + 1).padStart(2, '0')}`
    const { closingDate } = invoiceCycle(card.closingDay, card.dueDay, candidate)
    if (today <= closingDate) return candidate
    month++
    if (month > 11) {
      month = 0
      year++
    }
  }
  return `${year}-${String(month + 1).padStart(2, '0')}`
}
