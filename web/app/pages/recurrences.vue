<script setup lang="ts">
import DashLayout from '~/layouts/DashLayout.vue'
import CustomHeader from '~/components/CustomHeader.vue'
import ItemRecurrence from '~/components/recurrences/ItemRecurrence.vue'
import ItemInstallments from '~/components/recurrences/ItemInstallments.vue'
import type { Recurrence } from '~/types'
import AddRecurrenceModal from '~/components/recurrences/AddRecurrenceModal.vue'

const data = ref()
const recurrences = ref()
const installmentsPurchases = ref()
const { $financesService } = useNuxtApp()
const { activeAccount, loadAccounts } = useFinancialAccounts()
const categories = await $financesService.getCategories()

const modalRef = ref<InstanceType<typeof AddRecurrenceModal> | null>(null)

const selectedRecurrence = ref<Recurrence | null>(null)

function openModal() {
  modalRef.value?.openModal()
}

function openEditModal(category: Recurrence) {
  selectedRecurrence.value = category
  openModal()
}

function showNewRecurrence() {
  selectedRecurrence.value = null
  openModal()
}

async function refresh() {
  if (!activeAccount.value) {
    data.value = []
    recurrences.value = []
    installmentsPurchases.value = []
    return
  }
  const response = await $financesService.getRecurrences(activeAccount.value.id)
  data.value = response.data.items as Recurrence[]

  recurrences.value = data.value.filter((ele: Recurrence) => ele.type === 'MONTH')
  installmentsPurchases.value = data.value.filter((ele: Recurrence) => ele.type !== 'MONTH')
}

async function confirmDelete(recurrence: Recurrence) {
  await $financesService.deleteRecurrence(recurrence.id ?? '')
  await refresh()
}

async function confirmRecurrence(recurrence: Recurrence) {
  if (!recurrence.id) return
  await $financesService.confirmRecurrence(recurrence.id)
  await refresh()
}

watch(() => activeAccount.value?.id, refresh, { immediate: true })
onMounted(() => loadAccounts().then(refresh))
</script>

<template>
  <DashLayout>
    <UDashboardPanel id="home">
      <template #header>
        <CustomHeader>
          <UButton
            v-if="activeAccount"
            :label="$t('newRecurrence')"
            class="bg-primary dark:bg-primary-dark"
            @click="showNewRecurrence"
          />
        </CustomHeader>
      </template>

      <template #body>
        <AddRecurrenceModal
          ref="modalRef"
          :categories="categories.data.items"
          :refresh="refresh"
          :recurrency="selectedRecurrence"
        />
        <div>
          <UPageCard
            variant="subtle"
            class="mb-4"
            :title="$t('recurrences')"
          >
            <div class="divide-y divide-default">
              <div
                v-for="count in recurrences"
                :key="count.id"
                class="flex items-center justify-between py-3"
              >
                <ItemRecurrence
                  :recurrence="count"
                  @edit="openEditModal"
                  @delete="confirmDelete"
                  @confirm="confirmRecurrence"
                />
              </div>
            </div>
          </UPageCard>
          <UPageCard
            variant="subtle"
            class="mb-4"
            :title="$t('installments')"
          >
            <div class="divide-y divide-default">
              <div
                v-for="count in installmentsPurchases"
                :key="count.id"
                class="flex items-center justify-between py-3"
              >
                <ItemInstallments
                  :recurrence="count"
                  @edit="openEditModal"
                  @delete="confirmDelete"
                  @confirm="confirmRecurrence"
                />
              </div>
            </div>
          </UPageCard>
        </div>
      </template>
    </UDashboardPanel>
  </DashLayout>
</template>
