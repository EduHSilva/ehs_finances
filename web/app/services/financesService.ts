import type { CashflowProjection, Category, DashboardPending, FinancePreferences, FinancialAccount, GoalContribution, InvestmentSummary, MonthlyGoal, MonthlyGoalSummary, Recurrence, Transaction } from '~/types'
import type { DefaultResponse, Page } from '~/utils/types'
import { getTokenCookie } from '~/utils/util'

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
}

export interface UpdateTransactionRequest {
  value: number
  title: string
  executionDate?: string
  categoryID?: string
  income?: boolean
}

export interface CreateAccountRequest {
  name: string
  type: 'PF' | 'PJ'
  openingBalance: number
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
  getPending(accountId: string, days?: number): Promise<DefaultResponse<DashboardPending>>
  getInvestmentSummary(accountId: string): Promise<DefaultResponse<InvestmentSummary>>
  getGoals(accountId: string, periodMonth: string): Promise<DefaultResponse<Page<MonthlyGoal>>>
  createGoal(goal: MonthlyGoalRequest): Promise<DefaultResponse<MonthlyGoal>>
  updateGoal(id: string, goal: MonthlyGoalRequest): Promise<DefaultResponse<MonthlyGoal>>
  deleteGoal(id: string): Promise<DefaultResponse<void>>
  getGoalSummary(id: string, periodMonth: string): Promise<DefaultResponse<MonthlyGoalSummary>>
  contributeToGoal(id: string, contribution: GoalContribution): Promise<DefaultResponse<void>>
  getProjection(accountId: string, days?: number): Promise<DefaultResponse<CashflowProjection>>
}

export default function financesService({ apiUrl }: FinancesServiceOptions): FinancesService {
  const baseUrl = String(apiUrl)
  const authHeaders = () => ({ Authorization: `Bearer ${getTokenCookie().value?.token || ''}` })

  return {
    getPreferences: () => $fetch(`${baseUrl}preferences`, { headers: authHeaders() }),
    updatePreferences: preferences => $fetch(`${baseUrl}preferences`, {
      headers: authHeaders(), method: 'PUT', body: preferences
    }),
    getAccounts: () => $fetch(`${baseUrl}accounts`, { headers: authHeaders() }),
    createAccount: account => $fetch(`${baseUrl}accounts`, {
      headers: authHeaders(), method: 'POST', body: account
    }),
    getCategories: () => $fetch(`${baseUrl}categories?page=0&size=200`, { headers: authHeaders() }),
    createCategory: category => $fetch(`${baseUrl}categories`, {
      headers: authHeaders(), method: 'POST', body: {
        title: category.title, color: category.color, goal: category.goal,
        classification: category.classification, income: category.income
      }
    }),
    deleteCategory: id => $fetch(`${baseUrl}categories/${id}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    editCategory: (id, category) => $fetch(`${baseUrl}categories/${id}`, {
      headers: authHeaders(), method: 'PUT', body: {
        title: category.title, goal: category.goal, color: category.color,
        classification: category.classification, income: category.income
      }
    }),
    getRecurrences: accountId => $fetch(`${baseUrl}recurrences`, {
      headers: authHeaders(), query: { accountId, page: 0, size: 200 }
    }),
    createRecurrence: recurrence => $fetch(`${baseUrl}recurrences`, {
      headers: authHeaders(), method: 'POST', body: {
        day: recurrence.day, income: recurrence.income, categoryID: recurrence.categoryID,
        type: recurrence.type, installments: recurrence.installments, payed: recurrence.payed,
        value: recurrence.value, title: recurrence.title, accountId: recurrence.accountId,
        status: recurrence.status
      }
    }),
    deleteRecurrence: id => $fetch(`${baseUrl}recurrences/${id}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    editRecurrence: (id, recurrence) => $fetch(`${baseUrl}recurrences/${id}`, {
      headers: authHeaders(), method: 'PUT', body: {
        title: recurrence.title,
        day: recurrence.day,
        value: recurrence.value,
        installments: recurrence.installments,
        payed: recurrence.payed,
        categoryID: recurrence.categoryID,
        income: recurrence.income,
        status: recurrence.status
      }
    }),
    confirmRecurrence: (id, value) => $fetch(`${baseUrl}recurrences/${id}/confirm`, {
      headers: authHeaders(), method: 'POST', body: value == null ? undefined : { value }
    }),
    getTransactions: (accountId, page = 0, size = 200) => $fetch(`${baseUrl}transactions`, {
      headers: authHeaders(), query: { accountId, page, size }
    }),
    deleteTransaction: id => $fetch(`${baseUrl}transactions/${id}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    confirmTransaction: (id, value) => $fetch(`${baseUrl}transactions/${id}/confirm`, {
      headers: authHeaders(), method: 'POST', body: value == null ? undefined : { value }
    }),
    editTransaction: (id, transaction) => $fetch(`${baseUrl}transactions/${id}`, {
      headers: authHeaders(), method: 'PUT', body: {
        title: transaction.title,
        value: transaction.value,
        executionDate: transaction.executionDate,
        categoryID: transaction.categoryID,
        income: transaction.income
      }
    }),
    createTransaction: transaction => $fetch(`${baseUrl}transactions`, {
      headers: authHeaders(), method: 'POST', body: {
        income: transaction.income, value: transaction.value, categoryID: transaction.categoryID,
        title: transaction.title, recurrencyID: transaction.recurrencyID,
        executionDate: transaction.executionDate, accountId: transaction.accountId,
        status: transaction.status
      }
    }),
    previewStatementImport: request => $fetch(`${baseUrl}transactions/import/preview`, {
      headers: authHeaders(), method: 'POST', body: request
    }),
    confirmStatementImport: request => $fetch(`${baseUrl}transactions/import/confirm`, {
      headers: authHeaders(), method: 'POST', body: request
    }),
    reviewImportedTransaction: (id, review) => $fetch(`${baseUrl}transactions/${id}/review`, {
      headers: authHeaders(), method: 'PUT', body: review
    }),
    getTransactionImportAudit: id => $fetch(`${baseUrl}transactions/${id}/import-audit`, {
      headers: authHeaders()
    }),
    getGoals: (accountId, periodMonth) => $fetch(`${baseUrl}goals`, {
      headers: authHeaders(), query: { accountId, periodMonth, page: 0, size: 200 }
    }),
    createGoal: goal => $fetch(`${baseUrl}goals`, {
      headers: authHeaders(), method: 'POST', body: { ...goal, active: goal.active ?? true }
    }),
    updateGoal: (id, goal) => $fetch(`${baseUrl}goals/${id}`, {
      headers: authHeaders(), method: 'PUT', body: { ...goal, active: goal.active ?? true }
    }),
    deleteGoal: id => $fetch(`${baseUrl}goals/${id}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    getGoalSummary: (id, periodMonth) => $fetch(`${baseUrl}goals/${id}/summary`, {
      headers: authHeaders(), query: { periodMonth }
    }),
    contributeToGoal: (id, contribution) => $fetch(`${baseUrl}goals/${id}/contributions`, {
      headers: authHeaders(), method: 'POST', body: contribution
    }),
    getProjection: (accountId, days = 90) => $fetch(`${baseUrl}dashboard/projection`, {
      headers: authHeaders(), query: { accountId, days }
    }),
    getPending: (accountId, days = 30) => $fetch(`${baseUrl}dashboard/pending`, {
      headers: authHeaders(), query: { accountId, days }
    }),
    getInvestmentSummary: accountId => $fetch(`${baseUrl}dashboard/investments`, {
      headers: authHeaders(), query: { accountId }
    })
  }
}
