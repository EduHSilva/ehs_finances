import type { CashflowProjection, Category, FinancePreferences, FinancialAccount, GoalContribution, MonthlyGoal, MonthlyGoalSummary, Recurrence, Transaction } from '~/types'
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
  periodMonth: string
  active?: boolean
}

export interface CreateTransactionRequest {
  income: boolean
  value: number
  categoryID: string
  title: string
  recurrencyID?: string
  executionDate: string
  accountId: string
}

export interface UpdateTransactionRequest {
  value: number
  title: string
  executionDate?: string
}

export interface CreateAccountRequest {
  name: string
  type: 'PF' | 'PJ'
  openingBalance: number
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
  editTransaction(id: string, transaction: UpdateTransactionRequest): Promise<DefaultResponse<Transaction>>
  createTransaction(transaction: CreateTransactionRequest): Promise<DefaultResponse<Transaction>>
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
        title: category.title, color: category.color, goal: category.goal, classification: category.classification
      }
    }),
    deleteCategory: id => $fetch(`${baseUrl}categories/${id}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    editCategory: (id, category) => $fetch(`${baseUrl}categories/${id}`, {
      headers: authHeaders(), method: 'PUT', body: {
        title: category.title, goal: category.goal, color: category.color
      }
    }),
    getRecurrences: accountId => $fetch(`${baseUrl}recurrences`, {
      headers: authHeaders(), query: { accountId, page: 0, size: 200 }
    }),
    createRecurrence: recurrence => $fetch(`${baseUrl}recurrences`, {
      headers: authHeaders(), method: 'POST', body: {
        day: recurrence.day, income: recurrence.income, categoryID: recurrence.categoryID,
        type: recurrence.type, installments: recurrence.installments, payed: recurrence.payed,
        value: recurrence.value, title: recurrence.title, accountId: recurrence.accountId
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
        payed: recurrence.payed
      }
    }),
    getTransactions: (accountId, page = 0, size = 200) => $fetch(`${baseUrl}transactions`, {
      headers: authHeaders(), query: { accountId, page, size }
    }),
    deleteTransaction: id => $fetch(`${baseUrl}transactions/${id}`, {
      headers: authHeaders(), method: 'DELETE'
    }),
    editTransaction: (id, transaction) => $fetch(`${baseUrl}transactions/${id}`, {
      headers: authHeaders(), method: 'PUT', body: {
        title: transaction.title,
        value: transaction.value,
        executionDate: transaction.executionDate
      }
    }),
    createTransaction: transaction => $fetch(`${baseUrl}transactions`, {
      headers: authHeaders(), method: 'POST', body: {
        income: transaction.income, value: transaction.value, categoryID: transaction.categoryID,
        title: transaction.title, recurrencyID: transaction.recurrencyID,
        executionDate: transaction.executionDate, accountId: transaction.accountId
      }
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
    })
  }
}
