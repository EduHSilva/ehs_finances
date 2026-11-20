export type TransactionStatus = 'OK' | 'PENDING' | 'REVIEW' | 'DELAY' | 'DELETED'
export type RecurrenceStatus = 'ACTIVE' | 'PENDING' | 'INACTIVE' | 'PAYED' | 'DELETED'
export type SaleStatus = 'paid' | 'failed' | 'refunded'
export type FinancialAccountType = 'PF' | 'PJ'
export type FinancialAccountRole = 'OWNER' | 'MEMBER'
export type TransactionEntryType = 'NORMAL' | 'CARD' | 'TRANSFER' | 'INVOICE_PAYMENT'
export type CreditCardStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED'
export type CreditCardInvoiceStatus = 'OPEN' | 'PARTIAL' | 'PAID'
export type TransferStatus = 'OK' | 'PENDING' | 'DELETED'

export interface FinancialAccount {
  id: string
  type: FinancialAccountType
  name: string
  currency: string
  openingBalance: number
  balance: number
  role?: FinancialAccountRole
}

export type AccountMemberStatus = 'PENDING' | 'ACTIVE' | 'DECLINED' | 'REVOKED' | 'EXPIRED'

export interface AccountMember {
  id: string
  accountId: string
  email: string
  name?: string | null
  userId?: string | null
  status: AccountMemberStatus
  expiresAt?: string | null
  acceptedAt?: string | null
  createdAt?: string | null
  token?: string | null
}

export interface AccountMembers {
  accountId: string
  accountName: string
  ownerId: string
  ownerName?: string | null
  ownerEmail?: string | null
  members: AccountMember[]
}

export interface IncomingAccountInvite {
  id: string
  accountId: string
  accountName: string
  ownerId: string
  ownerName?: string | null
  ownerEmail?: string | null
  email: string
  expiresAt?: string | null
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
  initialBalance?: number | null
  objective?: number | null
  periodMonth: string
  dtInicio?: string | null
  dtFim?: string | null
  recurring?: boolean
  active?: boolean
  reserve?: boolean
}

export interface GoalContribution {
  amount: number
  date: string
  transactionId?: string
  adjustment?: boolean
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
  initialBalance?: number
  objective?: number | null
  saved?: number
  objectiveProgress?: number | null
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
    recurrenceId?: string
    installmentNumber?: number
    installments?: number
    payed?: number
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
  categoryKey?: string | null
  recurrence?: string
  accountId?: string
  classification?: string
  entryType?: TransactionEntryType
  creditCardId?: string | null
  creditCardName?: string | null
  transferId?: string | null
  counterpartAccountId?: string | null
  counterpartAccountName?: string | null
  invoiceDueDate?: string | null
}

export interface CreditCard {
  id: string
  accountId: string
  name: string
  cardLimit?: number | null
  closingDay: number
  dueDay: number
  status: CreditCardStatus
  openInvoiceTotal: number
}

export interface CreditCardRequest {
  accountId?: string
  name: string
  cardLimit?: number | null
  closingDay: number
  dueDay: number
  /** Só na criação: vira uma compra CARD confirmada na fatura em aberto. */
  initialBalance?: number | null
}

export interface CreditCardInvoice {
  cardId: string
  invoiceMonth: string
  closingDate: string
  dueDate: string
  total: number
  paid: number
  remaining: number
  status: CreditCardInvoiceStatus
  payments?: CreditCardInvoicePayment[]
}

export interface CreditCardInvoicePayment {
  id: string
  invoiceMonth?: string
  value: number
  date: string
  transactionId?: string | null
}

export interface InvoicePaymentRequest {
  value: number
  date: string
}

export interface TransferRequest {
  fromAccountId: string
  toAccountId: string
  value: number
  date: string
  title?: string
}

export interface TransferUpdateRequest {
  value: number
  date: string
  title?: string
}

export interface TransferResponse {
  transferId: string
  fromAccountId: string
  toAccountId: string
  value: number
  date: string
  title?: string | null
  status: TransferStatus
  outTransactionId?: string | null
  inTransactionId?: string | null
}

export interface Category {
  id: string
  title: string
  classification: string
  goal: number
  color: string
  income: boolean
  defaultKey?: string | null
}

export interface Recurrence {
  id?: string
  title: string
  categoryID?: string
  category?: string
  categoryKey?: string | null
  value: number
  status?: RecurrenceStatus
  income: boolean
  type: string
  day: number
  installments?: number
  payed?: number
  accountId?: string
  creditCardId?: string | null
  /** Cartão gravado no servidor, usado para montar `clearCreditCard` na edição. */
  currentCreditCardId?: string | null
  creditCardName?: string | null
}

export interface InvestmentSummary {
  accountId: string
  totalContributions: number
  totalWithdrawals: number
  balance: number
  currency: string
}

export interface DashboardSummary {
  accountId: string
  horizonDays: number
  calculatedAt: string
  cash: {
    currentBalance: number
    monthIncome: number
    monthExpenses: number
    monthNet: number
    projectedClosingBalance: number
  }
  investments: {
    total: number
    monthContributions: number
    monthWithdrawals: number
    monthNet: number
    projectedClosingBalance: number
  }
  netWorth: {
    current: number
    projected: number
  }
  cards: {
    openInvoicesTotal: number
    cards: Array<{
      cardId: string
      name: string
      total: number
    }>
  }
  livingCost: {
    monthlyAverage: number | null
    months: number
    total: number
    reserve?: {
      goalId?: string | null
      title?: string | null
      total: number
      months: number | null
    } | null
  }
  topCategories: Array<{
    categoryId?: string | null
    title?: string | null
    color?: string | null
    amount: number
    share: number
  }>
}

export type PendingItemKind = 'TRANSACTION' | 'RECURRENCE' | 'TRANSFER' | 'INVOICE'

export interface PendingItem {
  id: string
  kind: PendingItemKind
  title: string
  amount: number
  income: boolean
  date: string
  status: TransactionStatus | RecurrenceStatus
  cardId?: string | null
  cardName?: string | null
  invoiceMonth?: string | null
}

export interface DashboardPending {
  accountId: string
  horizonDays: number
  items: PendingItem[]
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
