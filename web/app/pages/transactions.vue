<script setup lang="ts">
import type { TableColumn } from '@nuxt/ui'
import { upperFirst } from 'scule'
import type { Row } from '@tanstack/table-core'
import { getPaginationRowModel } from '@tanstack/table-core'
import type { Category, Transaction } from '~/types'
import DashLayout from '~/layouts/DashLayout.vue'
import CustomHeader from '~/components/CustomHeader.vue'
import AddTransactionModal from '~/components/transactions/AddTransactionModal.vue'

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

const data = ref<Transaction[]>([])

async function confirmDelete(transaction: Transaction) {
  await $financesService.deleteTransaction(transaction.id)
  await refresh()
}

const modalRef = ref<InstanceType<typeof AddTransactionModal> | null>(null)

const selectedTransaction = ref<Transaction | null>(null)

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
  const response = await $financesService.getTransactions(activeAccount.value.id)
  data.value = response.data.items as Transaction[]
}

watch(() => activeAccount.value?.id, () => refresh(), { immediate: true })
onMounted(() => loadAccounts().then(refresh))

function getRowItems(row: Row<Transaction>) {
  return [
    {
      type: 'label',
      label: $t('actions')
    },
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

const columns: TableColumn<Transaction>[] = [
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
    header: $t('category')
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
        REVIEW: 'warning' as const,
        DELAY: 'error' as const,
        DELETED: 'neutral' as const
      }[row.original.status]

      return h(
        UBadge,
        { class: 'capitalize', variant: 'subtle', color },
        () => row.original.status
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
]

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
          <UButton
            v-if="activeAccount"
            :label="$t('newTransaction')"
            class="bg-primary dark:bg-primary-dark"
            @click="showNewTransaction"
          />
        </CustomHeader>
      </template>

      <template #body>
        <AddTransactionModal
          ref="modalRef"
          :categories="categories"
          :refresh="refresh"
          :recurrency="selectedTransaction"
        />
        <div class="flex flex-wrap items-center justify-between gap-1.5">
          <UInput
            v-model="title"
            class="max-w-sm"
            icon="i-lucide-search"
            :placeholder="$t('filterTransactions')"
          />

          <div class="flex flex-wrap items-center gap-1.5">
            <USelect
              v-model="statusFilter"
              :items="[
                { label: 'All', value: 'all' },
                { label: 'OK', value: 'OK' },
                { label: 'Review', value: 'REVIEW' },
                { label: 'Delayed', value: 'DELAY' },
                { label: 'Deleted', value: 'DELETED' }
              ]"
              :ui="{ trailingIcon: 'group-data-[state=open]:rotate-180 transition-transform duration-200' }"
              placeholder="Filter status"
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
          :data="data"
          :columns="columns"
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
