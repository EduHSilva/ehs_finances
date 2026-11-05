import type { GoalContribution, MonthlyGoal, MonthlyGoalSummary } from '~/types'
import type { MonthlyGoalRequest } from '~/services/financesService'
import { useUser } from '~/plugins/userService'

export function useMonthlyGoals() {
  const { $financesService } = useNuxtApp()
  const user = useUser()
  const stateKey = `monthly-goals:${user.value?.id || 'anonymous'}`
  const goals = useState<MonthlyGoal[]>(stateKey, () => [])
  const summaries = useState<Record<string, MonthlyGoalSummary>>(`${stateKey}:summaries`, () => ({}))
  const loading = useState<boolean>(`${stateKey}:loading`, () => false)

  async function load(accountId: string, periodMonth: string) {
    loading.value = true
    try {
      const response = await $financesService.getGoals(accountId, periodMonth)
      goals.value = response.data.items.filter(goal => goal.active !== false)
      summaries.value = {}
      const summaryEntries = await Promise.all(goals.value.map(async (goal) => {
        const summary = await $financesService.getGoalSummary(goal.id, periodMonth)
        return [goal.id, summary.data] as const
      }))
      summaries.value = Object.fromEntries(summaryEntries)
    } finally {
      loading.value = false
    }
  }

  async function create(goal: MonthlyGoalRequest, accountId: string, periodMonth: string) {
    await $financesService.createGoal(goal)
    await load(accountId, periodMonth)
  }

  async function update(id: string, goal: MonthlyGoalRequest, accountId: string, periodMonth: string) {
    await $financesService.updateGoal(id, goal)
    await load(accountId, periodMonth)
  }

  async function remove(id: string, accountId: string, periodMonth: string) {
    await $financesService.deleteGoal(id)
    await load(accountId, periodMonth)
  }

  async function contribute(id: string, contribution: GoalContribution, accountId: string, periodMonth: string) {
    await $financesService.contributeToGoal(id, contribution)
    await load(accountId, periodMonth)
  }

  return { goals, summaries, loading, load, create, update, remove, contribute }
}
