<script setup lang="ts">
import DashLayout from '~/layouts/DashLayout.vue'
import CustomHeader from '~/components/CustomHeader.vue'
import ListCategories from '~/components/categories/ListCategories.vue'
import type { Category } from '~/types'
import AddCategoryModal from '~/components/categories/AddCategoryModal.vue'

const { $financesService } = useNuxtApp()

const data = ref()

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
        <ListCategories
          :data="data"
          :title="$t('categories')"
          @edit="openEditModal"
          @delete="confirmDelete"
        />
      </template>
    </UDashboardPanel>
  </DashLayout>
</template>
