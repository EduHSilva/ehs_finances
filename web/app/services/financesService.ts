import type { CashflowProjection, Category, CreditCard, CreditCardInvoice, CreditCardRequest, DashboardPending, DashboardSummary, FinancePreferences, FinancialAccount, GoalContribution, InvestmentSummary, InvoicePaymentRequest, MonthlyGoal, MonthlyGoalSummary, Recurrence, Transaction, AccountMember, AccountMembers, IncomingAccountInvite, TransferRequest, TransferResponse, TransferUpdateRequest } from '~/types'
import type { DefaultResponse, Page } from '~/utils/types'
import { getTokenCookie } from '~/utils/util'
import { apiFetch } from '~/utils/apiFetch'

export interface FinancesServiceOptions {
  apiUrl: string | unknown
}

export interface MonthlyGoalRequest {
  title: string
  accountId: string
  mode: 'FIXED' | 'PERCENT_INCOME'
  amount: number
  initialBalance?: number | null
  objective?: number | null
  periodMonth: string
  dtInicio?: string | null
  dtFim?: string | null
  active?: boolean
  reserve?: boolean
}

export interface CreateTransactionRequest {
  income: boolean
  value: number
  categoryID?: string
  title: string
  recurrencyID?: string
  executionDate: string
  accountId: string
  status?: 'OK' | 'PENDING'
  creditCardId?: string | null
}

export interface UpdateTransactionRequest {
  value: number
  title: string
  executionDate?: string
  categoryID?: string
  income?: boolean
  creditCardId?: string | null
  clearCreditCard?: boolean
}

/**
 * Campos de cartão dos lançamentos/recorrências. `creditCardId` vazio/ausente
 * com `clear` verdadeiro envia `clearCreditCard` para desvincular.
 */
function creditCardFields(creditCardId?: string | null, currentCreditCardId?: string | null) {
  const next = creditCardId || null
  const current = currentCreditCardId || null
  if (!next && current) return { clearCreditCard: true }
  if (current && next === current) return {}
  return { creditCardId: next ?? undefined }
}

export interface CreateAccountRequest {
  name: string
  type: 'PF' | 'PJ'
  openingBalance: number
}

export interface UpdateAccountRequest {
  name: string
  openingBalance?: number
}

export interface StatementImportLineRequest {
  description: string
  amount: number
  income: boolean
  date: string
  categoryId?: string
  sourceTransactionId?: string
}

export interface StatementImportRequest {
  accountId: string
  source: string
  lines: StatementImportLineRequest[]
}

export interface StatementImportPreviewLine {
  row: number
  key: string
  description: string
  amount: number
  income: boolean
  date: string
  duplicate: boolean
}

export interface StatementImportPreview {
  lines: StatementImportPreviewLine[]
  importable: number
  duplicates: number
}

export interface StatementImportResult {
  imported: number
  duplicates: number
}

export interface ReviewImportedTransactionRequest {
  approved: boolean
  categoryId?: string
}

export interface TransactionImportAudit {
  id: string
  transactionId: string
  event: string
  createdAt: string
}

export interface FinancesService {
  getPreferences(): Promise<DefaultResponse<FinancePreferences>>
  updatePreferences(preferences: FinancePreferences): Promise<DefaultResponse<FinancePreferences>>
  getAccounts(): Promise<DefaultResponse<FinancialAccount[]>>
  createAccount(account: CreateAccountRequest): Promise<DefaultResponse<FinancialAccount>>
  updateAccount(id: string, account: UpdateAccountRequest): Promise<DefaultResponse<FinancialAccount>>
  getCategories(): Promise<DefaultResponse<Page<Category>>>
  createCategory(category: Category): Promise<DefaultResponse<Category>>
  deleteCategory(id: string): Promise<DefaultResponse<void>>
  editCategory(id: string, category: Category): Promise<DefaultResponse<Category>>
  getRecurrences(accountId?: string): Promise<DefaultResponse<Page<Recurrence>>>
  createRecurrence(recurrence: Recurrence): Promise<DefaultResponse<Recurrence>>
  deleteRecurrence(id: string): Promise<DefaultResponse<void>>
  editRecurrence(id: string, recurrence: Recurrence): Promise<DefaultResponse<Recurrence>>
  getTransactions(accountId?: string, page?: number, size?: number): Promise<DefaultResponse<Page<Transaction>>>
  deleteTransaction(id: string): Promise<DefaultResponse<void>>
  confirmTransaction(id: string, value?: number): Promise<DefaultResponse<Transaction>>
  editTransaction(id: string, transaction: UpdateTransactionRequest): Promise<DefaultResponse<Transaction>>
  createTransaction(transaction: CreateTransactionRequest): Promise<DefaultResponse<Transaction>>
  previewStatementImport(request: StatementImportRequest): Promise<DefaultResponse<StatementImportPreview>>
  confirmStatementImport(request: StatementImportRequest): Promise<DefaultResponse<StatementImportResult>>
  reviewImportedTransaction(id: string, review: ReviewImportedTransactionRequest): Promise<DefaultResponse<Transaction | null>>
  getTransactionImportAudit(id: string): Promise<DefaultResponse<TransactionImportAudit[]>>
  confirmRecurrence(id: string, value?: number): Promise<DefaultResponse<Recurrence>>
  getPending(accountId: string, month?: string): Promise<DefaultResponse<DashboardPending>>
  getInvestmentSummary(accountId: string): Promise<DefaultResponse<InvestmentSummary>>
  getDashboardSummary(accountId: string, month?: string): Promise<DefaultResponse<DashboardSummary>>
  getGoals(accountId: string, periodMonth: string): Promise<DefaultResponse<Page<MonthlyGoal>>>
  createGoal(goal: MonthlyGoalRequest): Promise<DefaultResponse<MonthlyGoal>>
  updateGoal(id: string, goal: MonthlyGoalRequest): Promise<DefaultResponse<MonthlyGoal>>
  deleteGoal(id: string): Promise<DefaultResponse<void>>
  getGoalSummary(id: string, periodMonth: string): Promise<DefaultResponse<MonthlyGoalSummary>>
  contributeToGoal(id: string, contribution: GoalContribution): Promise<DefaultResponse<void>>
  getProjection(accountId: string, month?: string): Promise<DefaultResponse<CashflowProjection>>
  getAccountMembers(accountId: string): Promise<DefaultResponse<AccountMembers>>
  inviteAccountMember(accountId: string, email: string): Promise<DefaultResponse<AccountMember>>
  resendAccountInvite(accountId: string, memberId: string): Promise<DefaultResponse<AccountMember>>
  revokeAccountMember(accountId: string, memberId: string): Promise<DefaultResponse<void>>
  getAccountInvites(): Promise<DefaultResponse<IncomingAccountInvite[]>>
  acceptAccountInvite(token: string): Promise<DefaultResponse<void>>
  declineAccountInvite(token: string): Promise<DefaultResponse<void>>
  getCards(accountId?: string): Promise<DefaultResponse<CreditCard[]>>
  getCard(id: string): Promise<DefaultResponse<CreditCard>>
  createCard(card: CreditCardRequest): Promise<DefaultResponse<CreditCard>>
  updateCard(id: string, card: CreditCardRequest): Promise<DefaultResponse<CreditCard>>
  deleteCard(id: string): Promise<DefaultResponse<void>>
  getCardInvoices(id: string): Promise<DefaultResponse<CreditCardInvoice[]>>
  payInvoice(id: string, invoiceMonth: string, payment: InvoicePaymentRequest): Promise<DefaultResponse<CreditCardInvoice>>
  deleteInvoicePayment(id: string, invoiceMonth: string, paymentId: string): Promise<DefaultResponse<CreditCardInvoice>>
  createTransfer(transfer: TransferRequest): Promise<DefaultResponse<TransferResponse>>
  updateTransfer(transferId: string, transfer: TransferUpdateRequest): Promise<DefaultResponse<TransferResponse>>
  deleteTransfer(transferId: string): Promise<DefaultResponse<void>>
}

export default function financesService({ apiUrl }: FinancesServiceOptions): FinancesService {
  const baseUrl = String(apiUrl)
  const authHeaders = () => ({ Authorization: `Bearer ${getTokenCookie().value?.token || ''}` })

  return {
    getPreferences: () => apiFetch(`${baseUrl}preferences`, { headers: authHeaders() }),
    updatePreferences: preferences => apiFetch(`${baseUrl}preferences`, {
      headers: authHeaders(), method: 'PUT', body: preferences
    }),
    getAccounts: () => apiFetch(`${baseUrl}accounts`, { headers: authHeaders() }),
    createAccount: account => apiFetch(`${baseUrl}accounts`, {
      headers: authHeaders(), method: 'POST', body: account
    }),
    updateAccount: (id, account) => apiFetch(`${baseUrl}accounts/${id}`, {
      headers: authHeaders(), method: 'PUT', body: account
    }),
    getCategories: () => apiFetch(`${baseUrl}categories?page=0&size=200`, { headers: authHeaders() }),
    createCategory: category => apiFetch(`${baseUrl}categories`, {
      headers: authHeaders(), method: 'POST', body: {
        title: category.title, color: category.color, goal: category.goal,
        classification: category.classification, income: category.income
      }
    }),
    deleteCategory: id => apiFetch(`${baseUrl}categories/${id}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    editCategory: (id, category) => apiFetch(`${baseUrl}categories/${id}`, {
      headers: authHeaders(), method: 'PUT', body: {
        title: category.title, goal: category.goal, color: category.color,
        classification: category.classification, income: category.income
      }
    }),
    getRecurrences: accountId => apiFetch(`${baseUrl}recurrences`, {
      headers: authHeaders(), query: { accountId, page: 0, size: 200 }
    }),
    createRecurrence: recurrence => apiFetch(`${baseUrl}recurrences`, {
      headers: authHeaders(), method: 'POST', body: {
        day: recurrence.day, income: recurrence.income, categoryID: recurrence.categoryID,
        type: recurrence.type, installments: recurrence.installments, payed: recurrence.payed,
        value: recurrence.value, title: recurrence.title, accountId: recurrence.accountId,
        status: recurrence.status, creditCardId: recurrence.creditCardId || undefined
      }
    }),
    deleteRecurrence: id => apiFetch(`${baseUrl}recurrences/${id}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    editRecurrence: (id, recurrence) => apiFetch(`${baseUrl}recurrences/${id}`, {
      headers: authHeaders(), method: 'PUT', body: {
        title: recurrence.title,
        day: recurrence.day,
        value: recurrence.value,
        installments: recurrence.installments,
        payed: recurrence.payed,
        categoryID: recurrence.categoryID,
        income: recurrence.income,
        status: recurrence.status,
        ...creditCardFields(recurrence.creditCardId, recurrence.currentCreditCardId)
      }
    }),
    confirmRecurrence: (id, value) => apiFetch(`${baseUrl}recurrences/${id}/confirm`, {
      headers: authHeaders(), method: 'POST', body: value == null ? undefined : { value }
    }),
    getTransactions: (accountId, page = 0, size = 200) => apiFetch(`${baseUrl}transactions`, {
      headers: authHeaders(), query: { accountId, page, size }
    }),
    deleteTransaction: id => apiFetch(`${baseUrl}transactions/${id}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    confirmTransaction: (id, value) => apiFetch(`${baseUrl}transactions/${id}/confirm`, {
      headers: authHeaders(), method: 'POST', body: value == null ? undefined : { value }
    }),
    editTransaction: (id, transaction) => apiFetch(`${baseUrl}transactions/${id}`, {
      headers: authHeaders(), method: 'PUT', body: {
        title: transaction.title,
        value: transaction.value,
        executionDate: transaction.executionDate,
        categoryID: transaction.categoryID,
        income: transaction.income,
        ...(transaction.creditCardId
          ? { creditCardId: transaction.creditCardId }
          : transaction.clearCreditCard ? { clearCreditCard: true } : {})
      }
    }),
    createTransaction: transaction => apiFetch(`${baseUrl}transactions`, {
      headers: authHeaders(), method: 'POST', body: {
        income: transaction.income, value: transaction.value, categoryID: transaction.categoryID,
        title: transaction.title, recurrencyID: transaction.recurrencyID,
        executionDate: transaction.executionDate, accountId: transaction.accountId,
        status: transaction.status, creditCardId: transaction.creditCardId || undefined
      }
    }),
    previewStatementImport: request => apiFetch(`${baseUrl}transactions/import/preview`, {
      headers: authHeaders(), method: 'POST', body: request
    }),
    confirmStatementImport: request => apiFetch(`${baseUrl}transactions/import/confirm`, {
      headers: authHeaders(), method: 'POST', body: request
    }),
    reviewImportedTransaction: (id, review) => apiFetch(`${baseUrl}transactions/${id}/review`, {
      headers: authHeaders(), method: 'PUT', body: review
    }),
    getTransactionImportAudit: id => apiFetch(`${baseUrl}transactions/${id}/import-audit`, {
      headers: authHeaders()
    }),
    getGoals: (accountId, periodMonth) => apiFetch(`${baseUrl}goals`, {
      headers: authHeaders(), query: { accountId, periodMonth, page: 0, size: 200 }
    }),
    createGoal: goal => apiFetch(`${baseUrl}goals`, {
      headers: authHeaders(), method: 'POST', body: { ...goal, active: goal.active ?? true }
    }),
    updateGoal: (id, goal) => apiFetch(`${baseUrl}goals/${id}`, {
      headers: authHeaders(), method: 'PUT', body: { ...goal, active: goal.active ?? true }
    }),
    deleteGoal: id => apiFetch(`${baseUrl}goals/${id}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    getGoalSummary: (id, periodMonth) => apiFetch(`${baseUrl}goals/${id}/summary`, {
      headers: authHeaders(), query: { periodMonth }
    }),
    contributeToGoal: (id, contribution) => apiFetch(`${baseUrl}goals/${id}/contributions`, {
      headers: authHeaders(), method: 'POST', body: contribution
    }),
    getProjection: (accountId, month) => apiFetch(`${baseUrl}dashboard/projection`, {
      headers: authHeaders(), query: { accountId, month }
    }),
    getPending: (accountId, month) => apiFetch(`${baseUrl}dashboard/pending`, {
      headers: authHeaders(), query: { accountId, month }
    }),
    getInvestmentSummary: accountId => apiFetch(`${baseUrl}dashboard/investments`, {
      headers: authHeaders(), query: { accountId }
    }),
    getDashboardSummary: (accountId, month) => apiFetch(`${baseUrl}dashboard/summary`, {
      headers: authHeaders(), query: { accountId, month }
    }),
    getAccountMembers: accountId => apiFetch(`${baseUrl}accounts/${accountId}/members`, {
      headers: authHeaders()
    }),
    inviteAccountMember: (accountId, email) => apiFetch(`${baseUrl}accounts/${accountId}/members`, {
      headers: authHeaders(), method: 'POST', body: { email }
    }),
    resendAccountInvite: (accountId, memberId) => apiFetch(`${baseUrl}accounts/${accountId}/members/${memberId}/resend`, {
      headers: authHeaders(), method: 'POST'
    }),
    revokeAccountMember: (accountId, memberId) => apiFetch(`${baseUrl}accounts/${accountId}/members/${memberId}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    getAccountInvites: () => apiFetch(`${baseUrl}account-invites`, {
      headers: authHeaders()
    }),
    acceptAccountInvite: token => apiFetch(`${baseUrl}account-invites/accept`, {
      headers: authHeaders(), method: 'POST', body: { token }
    }),
    declineAccountInvite: token => apiFetch(`${baseUrl}account-invites/decline`, {
      headers: authHeaders(), method: 'POST', body: { token }
    }),
    getCards: accountId => apiFetch(`${baseUrl}cards`, {
      headers: authHeaders(), query: { accountId }
    }),
    getCard: id => apiFetch(`${baseUrl}cards/${id}`, { headers: authHeaders() }),
    createCard: card => apiFetch(`${baseUrl}cards`, {
      headers: authHeaders(), method: 'POST', body: {
        accountId: card.accountId,
        name: card.name,
        cardLimit: card.cardLimit ?? null,
        closingDay: card.closingDay,
        dueDay: card.dueDay,
        initialBalance: card.initialBalance ?? null
      }
    }),
    updateCard: (id, card) => apiFetch(`${baseUrl}cards/${id}`, {
      headers: authHeaders(), method: 'PUT', body: {
        name: card.name,
        cardLimit: card.cardLimit ?? null,
        closingDay: card.closingDay,
        dueDay: card.dueDay
      }
    }),
    deleteCard: id => apiFetch(`${baseUrl}cards/${id}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    getCardInvoices: id => apiFetch(`${baseUrl}cards/${id}/invoices`, { headers: authHeaders() }),
    payInvoice: (id, invoiceMonth, payment) => apiFetch(`${baseUrl}cards/${id}/invoices/${invoiceMonth}/payments`, {
      headers: authHeaders(), method: 'POST', body: { value: payment.value, date: payment.date }
    }),
    deleteInvoicePayment: (id, invoiceMonth, paymentId) => apiFetch(`${baseUrl}cards/${id}/invoices/${invoiceMonth}/payments/${paymentId}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    createTransfer: transfer => apiFetch(`${baseUrl}transfers`, {
      headers: authHeaders(), method: 'POST', body: {
        fromAccountId: transfer.fromAccountId,
        toAccountId: transfer.toAccountId,
        value: transfer.value,
        date: transfer.date,
        title: transfer.title || undefined
      }
    }),
    updateTransfer: (transferId, transfer) => apiFetch(`${baseUrl}transfers/${transferId}`, {
      headers: authHeaders(), method: 'PUT', body: {
        value: transfer.value,
        date: transfer.date,
        title: transfer.title || undefined
      }
    }),
    deleteTransfer: transferId => apiFetch(`${baseUrl}transfers/${transferId}`, {
      headers: authHeaders(), method: 'DELETE'
    })
  }
}
