import type { AvatarProps } from '@nuxt/ui'

export type TransactionStatus = 'OK' | 'REVIEW' | 'DELAY' | 'DELETED'
export type SaleStatus = 'paid' | 'failed' | 'refunded'
export type FinancialAccountType = 'PF' | 'PJ'

export interface FinancialAccount {
  id: string
  type: FinancialAccountType
  name: string
  currency: string
  openingBalance: number
  balance: number
}

export interface FinancePreferences {
  showBusinessAccount: boolean
  currency: string
}

export type MonthlyGoalMode = 'FIXED' | 'PERCENT_INCOME'

export interface MonthlyGoal {
  id: string
  accountId: string
  title: string
  mode: MonthlyGoalMode
  amount: number
  periodMonth: string
  active?: boolean
}

export interface GoalContribution {
  amount: number
  date: string
  transactionId?: string
}

export interface MonthlyGoalSummary {
  goalId: string
  accountId: string
  title: string
  periodMonth: string
  incomeBase: number
  expected: number
  realized: number
  difference: number
  progress: number
}

export interface CashflowProjection {
  accountId: string
  horizonDays: number
  calculatedAt: string
  currentBalance: number
  closingBalance: number
  confidence: string
  assumptions: string[]
  series: Array<{
    date: string
    balance: number
    knownIncome: number
    knownExpenses: number
    estimatedIncome: number
    estimatedExpenses: number
  }>
  items: Array<{
    date: string
    title: string
    amount: number
    income: boolean
    kind: string
  }>
}

export interface Transaction {
  id: string
  title: string
  value: number
  executionDate: string
  income: boolean
  status: TransactionStatus
  categoryID?: string
  category?: string
  accountId?: string
  classification?: string
}

export interface Category {
  id: string
  title: string
  classification: string
  goal: number
  color: string
}

export interface Recurrence {
  id?: string
  title: string
  categoryID?: string
  category?: string
  value: number
  status?: string
  income: boolean
  type: string
  day: number
  installments?: number
  payed?: number
  accountId?: string
}

export interface InstallmentItem {
  id: string
  title: string
  category: string
  value: number
  installments: number
  payed: number
}

export interface Mail {
  id: number
  unread?: boolean
  from: User
  subject: string
  body: string
  date: string
}

export interface Member {
  name: string
  username: string
  role: 'member' | 'owner'
  avatar: AvatarProps
}

export interface Sale {
  id: string
  date: string
  status: SaleStatus
  email: string
  amount: number
}

export interface Notification {
  id: number
  unread?: boolean
  sender: User
  body: string
  date: string
}

