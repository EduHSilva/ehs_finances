<script setup lang="ts">
import type { TableColumn } from '@nuxt/ui'
import { upperFirst } from 'scule'
import type { Row } from '@tanstack/table-core'
import { getPaginationRowModel } from '@tanstack/table-core'
import type { Category, Transaction } from '~/types'
import DashLayout from '~/layouts/DashLayout.vue'
import CustomHeader from '~/components/CustomHeader.vue'
import AddTransactionModal from '~/components/transactions/AddTransactionModal.vue'
import ImportStatementModal from '~/components/transactions/ImportStatementModal.vue'
import ConfirmValueModal from '~/components/ConfirmValueModal.vue'

const UButton = resolveComponent('UButton')
const UBadge = resolveComponent('UBadge')
const UDropdownMenu = resolveComponent('UDropdownMenu')
const UCheckbox = resolveComponent('UCheckbox')

const table = useTemplateRef('table')

const columnFilters = ref([{
  id: 'title',
  value: ''
}])
const rowSelection = ref({ 1: true })

const { $financesService } = useNuxtApp()
const { activeAccount, loadAccounts } = useFinancialAccounts()
const categoryLabel = useCategoryLabel()
const { t, locale } = useI18n()
const toast = useToast()

const data = ref<Transaction[]>([])

async function confirmDelete(transaction: Transaction) {
  await $financesService.deleteTransaction(transaction.id)
  await refresh()
}

const confirmModalRef = ref<InstanceType<typeof ConfirmValueModal> | null>(null)

function askConfirm(transaction: Transaction) {
  confirmModalRef.value?.openModal(transaction.value, {
    title: t('confirmTransactionTitle'),
    description: `${transaction.title} · ${formatDate(transaction.executionDate, locale.value)}`,
    onConfirm: async (value) => {
      await $financesService.confirmTransaction(transaction.id, value)
      await refresh()
      toast.add({ title: t('transactionConfirmed'), color: 'success' })
    }
  })
}

const modalRef = ref<InstanceType<typeof AddTransactionModal> | null>(null)
const importModalRef = ref<InstanceType<typeof ImportStatementModal> | null>(null)

const selectedTransaction = ref<Transaction | null>(null)
const reviewCount = computed(() => data.value.filter(transaction => transaction.status === 'REVIEW').length)

function openModal() {
  modalRef.value?.openModal()
}

const categories = ref<Category[]>([])

function openEditModal(transaction: Transaction) {
  const matchingCategory = categories.value.find(category => category.title === transaction.category)
  selectedTransaction.value = { ...transaction, categoryID: matchingCategory?.id }
  openModal()
}

function showNewTransaction() {
  selectedTransaction.value = null
  openModal()
}

async function refresh() {
  if (!activeAccount.value) {
    data.value = []
    return
  }
  if (!categories.value.length) {
    const categoryResponse = await $financesService.getCategories()
    categories.value = categoryResponse.data.items
  }
  const accountId = activeAccount.value.id
  const size = 200
  const all: Transaction[] = []
  let page = 0
  let total = Infinity
  while (all.length < total) {
    const response = await $financesService.getTransactions(accountId, page, size)
    const items = response.data.items as Transaction[]
    all.push(...items)
    total = response.data.total
    if (!items.length) break
    page++
  }
  const categoryByTitle = new Map(categories.value.map(category => [category.title, category.id]))
  const categoryByKey = new Map(categories.value.filter(category => category.defaultKey).map(category => [category.defaultKey!, category.id]))
  for (const transaction of all) {
    if (!transaction.categoryID) {
      transaction.categoryID = categoryByTitle.get(transaction.category || '')
        ?? (transaction.categoryKey ? categoryByKey.get(transaction.categoryKey) : undefined)
    }
  }
  data.value = all
}

watch(() => activeAccount.value?.id, () => refresh(), { immediate: true })
onMounted(() => loadAccounts().then(refresh))

function getRowItems(row: Row<Transaction>) {
  return [
    {
      type: 'label',
      label: $t('actions')
    },
    ...(row.original.status === 'PENDING'
      ? [{
          label: $t('confirm'),
          icon: 'i-lucide-check',
          onSelect() {
            askConfirm(row.original)
          }
        }]
      : []),
    ...(row.original.status === 'REVIEW'
      ? [{
          label: $t('reviewImport'),
          icon: 'i-lucide-search-check',
          onSelect() {
            importModalRef.value?.openReview()
          }
        }]
      : []),
    {
      label: $t('edit'),
      icon: 'i-lucide-edit',
      onSelect() {
        openEditModal(row.original)
      }
    },
    {
      label: $t('delete'),
      icon: 'i-lucide-trash',
      color: 'error',
      onSelect() {
        confirmDelete(row.original)
      }
    }
  ]
}

const columnVisibility = ref({
  id: false
})

const columns = computed<TableColumn<Transaction>[]>(() => [
  {
    id: 'select',
    header: ({ table }) =>
      h(UCheckbox, {
        'modelValue': table.getIsSomePageRowsSelected()
          ? 'indeterminate'
          : table.getIsAllPageRowsSelected(),
        'onUpdate:modelValue': (value: boolean | 'indeterminate') =>
          table.toggleAllPageRowsSelected(!!value),
        'ariaLabel': 'Select all'
      }),
    cell: ({ row }) =>
      h(UCheckbox, {
        'modelValue': row.getIsSelected(),
        'onUpdate:modelValue': (value: boolean | 'indeterminate') =>
          row.toggleSelected(!!value),
        'ariaLabel': 'Select row'
      })
  },

  {
    accessorKey: 'id',
    header: 'ID'
  },

  {
    accessorKey: 'title',
    header: $t('title')
  },
  {
    accessorKey: 'executionDate',
    header: $t('date'),
    cell: ({ row }) => `${formatDate(row.original.executionDate)}`
  },
  {
    accessorKey: 'category',
    header: $t('category'),
    cell: ({ row }) => categoryLabel(row.original.category, row.original.categoryKey)
  },
  {
    accessorKey: 'classification',
    header: $t('classification'),
    cell: ({ row }) => row.original.classification ? $t(row.original.classification.toLowerCase()) : ''
  },
  {
    accessorKey: 'value',
    header: $t('value'),
    cell: ({ row }) => `R$ ${row.original.value}`
  },

  {
    accessorKey: 'income',
    header: $t('type'),
    cell: ({ row }) => (row.original.income ? $t('income') : $t('expanse'))
  },

  {
    accessorKey: 'status',
    header: $t('status'),
    filterFn: 'equals',
    cell: ({ row }) => {
      const color = {
        OK: 'success' as const,
        PENDING: 'warning' as const,
        REVIEW: 'warning' as const,
        DELAY: 'error' as const,
        DELETED: 'neutral' as const
      }[row.original.status]

      return h(
        UBadge,
        { class: 'capitalize', variant: 'subtle', color },
        () => $t(`transactionStatus.${row.original.status.toLowerCase()}`)
      )
    }
  },

  {
    id: 'actions',
    cell: ({ row }) =>
      h(
        'div',
        { class: 'text-right' },
        h(
          UDropdownMenu,
          {
            content: { align: 'end' },
            items: getRowItems(row)
          },
          () =>
            h(UButton, {
              icon: 'i-lucide-ellipsis-vertical',
              color: 'neutral',
              variant: 'ghost',
              class: 'ml-auto'
            })
        )
      )
  }
])

const categoryFilter = ref('all')
const categoryFilterOptions = computed(() => [
  { label: t('all'), value: 'all' },
  ...categories.value.map(category => ({
    label: categoryLabel(category.title, category.defaultKey),
    value: category.id
  })),
  { label: t('noCategory'), value: 'none' }
])

const filteredData = computed(() => {
  if (categoryFilter.value === 'all') return data.value
  if (categoryFilter.value === 'none') return data.value.filter(transaction => !transaction.categoryID)
  return data.value.filter(transaction => transaction.categoryID === categoryFilter.value)
})

const statusFilter = ref('all')

watch(() => statusFilter.value, (newVal) => {
  if (!table?.value?.tableApi) return

  const statusColumn = table.value.tableApi.getColumn('status')
  if (!statusColumn) return

  if (newVal === 'all') {
    statusColumn.setFilterValue(undefined)
  } else {
    statusColumn.setFilterValue(newVal)
  }
})

const title = computed({
  get: (): string => {
    return (table.value?.tableApi?.getColumn('title')?.getFilterValue() as string) || ''
  },
  set: (value: string) => {
    table.value?.tableApi?.getColumn('title')?.setFilterValue(value || undefined)
  }
})

const pagination = ref({
  pageIndex: 0,
  pageSize: 10
})
</script>

<template>
  <DashLayout>
    <UDashboardPanel id="transactions">
      <template #header>
        <CustomHeader>
          <div v-if="activeAccount" class="flex flex-wrap items-center gap-2">
            <UButton
              :label="$t('importStatement')"
              icon="i-lucide-upload"
              color="neutral"
              variant="outline"
              @click="importModalRef?.openModal()"
            />
            <UButton
              v-if="reviewCount"
              :label="$t('reviewImportsCount', { count: reviewCount })"
              icon="i-lucide-search-check"
              color="warning"
              variant="subtle"
              @click="importModalRef?.openReview()"
            />
            <UButton
              :label="$t('newTransaction')"
              class="bg-primary dark:bg-primary-dark"
              @click="showNewTransaction"
            />
          </div>
        </CustomHeader>
      </template>

      <template #body>
        <AddTransactionModal
          ref="modalRef"
          :categories="categories"
          :refresh="refresh"
          :transaction="selectedTransaction"
        />
        <ImportStatementModal
          ref="importModalRef"
          :categories="categories"
          :refresh="refresh"
        />
        <ConfirmValueModal ref="confirmModalRef" />
        <div class="flex flex-wrap items-center justify-between gap-1.5">
          <UInput
            v-model="title"
            class="max-w-sm"
            icon="i-lucide-search"
            :placeholder="$t('filterTransactions')"
          />

          <div class="flex flex-wrap items-center gap-1.5">
            <USelect
              v-model="categoryFilter"
              :items="categoryFilterOptions"
              :placeholder="$t('filterCategory')"
              class="min-w-44"
            />
            <USelect
              v-model="statusFilter"
              :items="[
                { label: $t('all'), value: 'all' },
                { label: $t('transactionStatus.pending'), value: 'PENDING' },
                { label: $t('transactionStatus.ok'), value: 'OK' },
                { label: $t('transactionStatus.review'), value: 'REVIEW' },
                { label: $t('transactionStatus.delay'), value: 'DELAY' },
                { label: $t('transactionStatus.deleted'), value: 'DELETED' }
              ]"
              :ui="{ trailingIcon: 'group-data-[state=open]:rotate-180 transition-transform duration-200' }"
              :placeholder="$t('filterStatus')"
              class="min-w-28"
            />
            <UDropdownMenu
              :items="
                table?.tableApi
                  ?.getAllColumns()
                  .filter((column: any) => column.getCanHide())
                  .map((column: any) => ({
                    label: upperFirst(column.id),
                    type: 'checkbox' as const,
                    checked: column.getIsVisible(),
                    onUpdateChecked(checked: boolean) {
                      table?.tableApi?.getColumn(column.id)?.toggleVisibility(checked)
                    },
                    onSelect(e?: Event) {
                      e?.preventDefault()
                    }
                  }))
              "
              :content="{ align: 'end' }"
            >
              <UButton
                label="Display"
                color="neutral"
                variant="outline"
                trailing-icon="i-lucide-settings-2"
              />
            </UDropdownMenu>
          </div>
        </div>

        <UTable
          ref="table"
          v-model:column-filters="columnFilters"
          v-model:column-visibility="columnVisibility"
          v-model:row-selection="rowSelection"
          v-model:pagination="pagination"
          :pagination-options="{
            getPaginationRowModel: getPaginationRowModel()
          }"
          class="shrink-0"
          :data="filteredData"
          :columns="columns"
          :empty="$t('noData')"
          :ui="{
            base: 'table-fixed border-separate border-spacing-0',
            thead: '[&>tr]:bg-elevated/50 [&>tr]:after:content-none',
            tbody: '[&>tr]:last:[&>td]:border-b-0',
            th: 'py-2 first:rounded-l-lg last:rounded-r-lg border-y border-default first:border-l last:border-r',
            td: 'border-b border-default',
            separator: 'h-0'
          }"
        />

        <div class="flex items-center justify-between gap-3 border-t border-default pt-4 mt-auto">
          <div class="text-sm text-muted">
            {{ table?.tableApi?.getFilteredSelectedRowModel().rows.length || 0 }} of
            {{ table?.tableApi?.getFilteredRowModel().rows.length || 0 }} row(s) selected.
          </div>

          <div class="flex items-center gap-1.5">
            <UPagination
              :default-page="(table?.tableApi?.getState().pagination.pageIndex || 0) + 1"
              :items-per-page="table?.tableApi?.getState().pagination.pageSize"
              :total="table?.tableApi?.getFilteredRowModel().rows.length"
              @update:page="(p: number) => table?.tableApi?.setPageIndex(p - 1)"
            />
          </div>
        </div>
      </template>
    </UDashboardPanel>
  </DashLayout>
</template>
