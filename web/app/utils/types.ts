export type DefaultResponse<T> = {
  data: T
  message: string
  links: object
}

export type Page<T> = {
  items: T[]
  total: number
  page: number
  size: number
}

export type User = {
  email: string
  username: string
  id: string
  token: string
}

export type Client = {
  id?: string
  email: string
  name: string
  phone: string
  observations?: string
}

export type Item = {
  name: string
  description: string
  quantity: number
  price: number
}

export type Budget = {
  id?: string
  title: string
  description: string
  client: string
  clientName?: string
  validate: number
  items: Item[]
  terms?: string
  status?: string
  observations?: string
}

export type DashboardData = {
  totalClients: number
  pendingServices: number
  workingServices: number
  totalReceived: number
  recentServices: Budget[]
}

export type DashboardKanbanData = {
  news: Budget[]
  working: Budget[]
  done: Budget[]
}
