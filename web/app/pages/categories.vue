<script setup lang="ts">
import DashLayout from '~/layouts/DashLayout.vue'
import CustomHeader from '~/components/CustomHeader.vue'
import ListCategories from '~/components/categories/ListCategories.vue'
import type { Category } from '~/types'
import AddCategoryModal from '~/components/categories/AddCategoryModal.vue'

const { $financesService } = useNuxtApp()
const { t } = useI18n()
const categoryLabel = useCategoryLabel()

const data = ref<Category[]>([])
const search = ref('')
const typeFilter = ref<'all' | 'income' | 'expense'>('all')

const typeOptions = computed(() => [
  { label: t('all'), value: 'all' },
  { label: t('income'), value: 'income' },
  { label: t('expanse'), value: 'expense' }
])

const filteredData = computed(() => {
  const term = search.value.trim().toLowerCase()
  return data.value.filter((category) => {
    const matchesType = typeFilter.value === 'all'
      || (typeFilter.value === 'income' ? category.income : !category.income)
    const matchesSearch = !term
      || categoryLabel(category.title, category.defaultKey).toLowerCase().includes(term)
    return matchesType && matchesSearch
  })
})

async function confirmDelete(category: Category) {
  await $financesService.deleteCategory(category.id)
  await refresh()
}

const modalRef = ref<InstanceType<typeof AddCategoryModal> | null>(null)

const selectedCategory = ref<Category | null>(null)
function openModal() {
  modalRef.value?.openModal()
}

function openEditModal(category: Category) {
  selectedCategory.value = category
  openModal()
}

function showNewCategory() {
  selectedCategory.value = null
  openModal()
}

async function refresh() {
  const response = await $financesService.getCategories()
  data.value = response.data.items as Category[]
}
refresh()
</script>

<template>
  <DashLayout>
    <UDashboardPanel id="home">
      <template #header>
        <CustomHeader>
          <UButton
            :label="$t('newCategory')"
            class="bg-primary dark:bg-primary-dark"
            @click="showNewCategory"
          />
        </CustomHeader>
      </template>

      <template #body>
        <AddCategoryModal
          ref="modalRef"
          :refresh="refresh"
          :category="selectedCategory"
        />
        <div class="mb-4 flex flex-wrap items-center gap-1.5">
          <UInput
            v-model="search"
            class="max-w-sm"
            icon="i-lucide-search"
            :placeholder="$t('filterCategories')"
          />
          <USelect
            v-model="typeFilter"
            :items="typeOptions"
            :placeholder="$t('filterCategoryType')"
            class="min-w-40"
          />
        </div>
        <ListCategories
          :data="filteredData"
          :title="$t('categories')"
          @edit="openEditModal"
          @delete="confirmDelete"
        />
      </template>
    </UDashboardPanel>
  </DashLayout>
</template>
