<script setup lang="ts">
import type { Category, Transaction } from '~/types'
import type { StatementImportRequest } from '~/services/financesService'
import {
  StatementImportParseError,
  parseStatementFile,
  sanitizeImportSource,
  type ParsedStatementLine,
  type StatementFormat
} from '~/utils/statementImport'

const props = defineProps({
  refresh: { type: Function, required: true },
  categories: { type: Array as PropType<Category[]>, required: true }
})

const MAX_LINES = 1000
const MAX_FILE_SIZE = 5 * 1024 * 1024

interface ImportRow extends ParsedStatementLine {
  include: boolean
  duplicate: boolean
  categoryId?: string
}

const { t } = useI18n()
const toast = useToast()
const { $financesService } = useNuxtApp()
const { activeAccount, accountOptions, loadAccounts } = useFinancialAccounts()
const categoryLabel = useCategoryLabel()
const ofxConverterUrl = computed(() => String(useRuntimeConfig().public.ofxConverterUrl || ''))

const open = ref(false)
const step = ref<'upload' | 'preview' | 'result'>('upload')
const selectedAccountId = ref('')
const fileName = ref('')
const format = ref<StatementFormat>('CSV')
const source = ref('import')
const rows = ref<ImportRow[]>([])
const preview = ref<{ importable: number, duplicates: number } | null>(null)
const result = ref<{ imported: number, duplicates: number } | null>(null)
const reviewing = ref<Transaction[]>([])
const reviewCategories = ref<Record<string, string | undefined>>({})
const categoryList = ref<Category[]>(props.categories)
const loadingPreview = ref(false)
const confirming = ref(false)
const reviewingId = ref('')
const discardTarget = ref<Transaction | null>(null)
const discardLoading = ref(false)
const pdfFileName = ref('')

const title = computed(() => step.value === 'result' && !result.value ? t('reviewImports') : t('importStatement'))
const description = computed(() => step.value === 'result' && !result.value ? t('reviewImportsDesc') : t('importStatementDesc'))
const invalidCount = computed(() => rows.value.filter(row => row.errors.length > 0).length)
const includedCount = computed(() => rows.value.filter(row => !row.errors.length && row.include).length)
const canConfirm = computed(() => includedCount.value > 0 && !!selectedAccountId.value)
const typeItems = computed(() => [
  { label: t('income'), value: true },
  { label: t('expanse'), value: false }
])

watch(() => props.categories, (categories) => {
  categoryList.value = categories
}, { deep: true })

function categoryOptions(income: boolean) {
  return [
    { label: t('noCategory'), value: '' },
    ...categoryList.value
      .filter(category => category.income === income)
      .map(category => ({ label: categoryLabel(category.title, category.defaultKey), value: category.id }))
  ]
}

function resetFlow() {
  step.value = 'upload'
  fileName.value = ''
  format.value = 'CSV'
  source.value = 'import'
  rows.value = []
  preview.value = null
  result.value = null
  reviewing.value = []
  reviewCategories.value = {}
  pdfFileName.value = ''
}

async function ensureReferenceData() {
  const tasks: Promise<unknown>[] = [loadAccounts()]
  if (!categoryList.value.length) {
    tasks.push($financesService.getCategories().then((response) => {
      categoryList.value = response.data.items
    }))
  }
  await Promise.allSettled(tasks)
}

async function openModal() {
  await ensureReferenceData()
  selectedAccountId.value = activeAccount.value?.id || selectedAccountId.value
  resetFlow()
  open.value = true
}

async function openReview() {
  await ensureReferenceData()
  selectedAccountId.value = activeAccount.value?.id || selectedAccountId.value
  resetFlow()
  step.value = 'result'
  open.value = true
  await loadReviewing()
}

function closeModal() {
  open.value = false
  resetFlow()
}

async function readStatementFile(file: File): Promise<string> {
  const buffer = await file.arrayBuffer()
  const utf8 = new TextDecoder('utf-8').decode(buffer)
  if (!utf8.includes('\uFFFD')) return utf8
  return new TextDecoder('windows-1252').decode(buffer)
}

async function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  if (!selectedAccountId.value) {
    toast.add({ title: t('importAccountRequired'), color: 'error' })
    return
  }
  if (file.size > MAX_FILE_SIZE) {
    toast.add({ title: t('importFileTooLarge'), color: 'error' })
    return
  }

  rows.value = []
  preview.value = null
  pdfFileName.value = ''

  if (file.name.toLowerCase().endsWith('.pdf') || file.type === 'application/pdf') {
    pdfFileName.value = file.name
    return
  }

  try {
    const content = await readStatementFile(file)
    const parsed = parseStatementFile(content, file.name)
    fileName.value = file.name
    format.value = parsed.format
    source.value = sanitizeImportSource(file.name)
    rows.value = parsed.lines.slice(0, MAX_LINES).map(line => ({
      ...line,
      include: line.errors.length === 0,
      duplicate: false,
      categoryId: undefined
    }))

    if (parsed.lines.length > MAX_LINES) {
      toast.add({ title: t('importLimitReached', { count: MAX_LINES }), color: 'warning' })
    }

    await runPreview()
  } catch (error) {
    toast.add({
      title: t('importFileReadError'),
      description: error instanceof StatementImportParseError
        ? t(`importParseError.${error.code}`)
        : undefined,
      color: 'error'
    })
  }
}

function buildRequest(lines: ImportRow[]): StatementImportRequest {
  return {
    accountId: selectedAccountId.value,
    source: source.value,
    lines: lines.map(line => ({
      description: line.description,
      amount: line.amount!,
      income: line.income,
      date: line.date!,
      categoryId: line.categoryId || undefined,
      sourceTransactionId: line.sourceTransactionId
    }))
  }
}

async function runPreview() {
  step.value = 'preview'
  preview.value = null
  const validRows = rows.value.filter(row => !row.errors.length)

  if (!validRows.length) {
    preview.value = { importable: 0, duplicates: 0 }
    return
  }

  loadingPreview.value = true
  try {
    const response = await $financesService.previewStatementImport(buildRequest(validRows))
    preview.value = {
      importable: response.data.importable,
      duplicates: response.data.duplicates
    }
    const byRow = new Map(response.data.lines.map(line => [line.row, line]))
    validRows.forEach((row, index) => {
      row.duplicate = byRow.get(index + 1)?.duplicate ?? false
      row.include = !row.duplicate
    })
  } catch {
    toast.add({ title: t('importPreviewFailed'), color: 'error' })
    step.value = 'upload'
  } finally {
    loadingPreview.value = false
  }
}

async function confirmImport() {
  if (!canConfirm.value) return
  const includedRows = rows.value.filter(row => !row.errors.length && row.include)
  confirming.value = true
  try {
    const response = await $financesService.confirmStatementImport(buildRequest(includedRows))
    result.value = response.data
    step.value = 'result'
    await Promise.all([loadReviewing(), props.refresh()])
  } catch {
    toast.add({ title: t('importConfirmFailed'), color: 'error' })
  } finally {
    confirming.value = false
  }
}

function changeRowIncome(row: ImportRow) {
  const selected = categoryList.value.find(category => category.id === row.categoryId)
  if (selected && selected.income !== row.income) row.categoryId = undefined
}

function rowErrorLabel(row: ImportRow) {
  return row.errors.map(error => t(`importLineError.${error}`)).join(', ')
}

async function loadReviewing() {
  if (!selectedAccountId.value) {
    reviewing.value = []
    return
  }
  try {
    const accountId = selectedAccountId.value
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
    reviewing.value = all.filter(transaction => transaction.status === 'REVIEW')
    const categories: Record<string, string | undefined> = {}
    reviewing.value.forEach((transaction) => {
      categories[transaction.id] = transaction.categoryID
    })
    reviewCategories.value = categories
  } catch {
    toast.add({ title: t('importReviewLoadFailed'), color: 'error' })
  }
}

async function approveReview(transaction: Transaction) {
  reviewingId.value = transaction.id
  try {
    await $financesService.reviewImportedTransaction(transaction.id, {
      approved: true,
      categoryId: reviewCategories.value[transaction.id] || undefined
    })
    toast.add({ title: t('importReviewApproved'), color: 'success' })
    await Promise.all([loadReviewing(), props.refresh()])
  } catch {
    toast.add({ title: t('importReviewFailed'), color: 'error' })
  } finally {
    reviewingId.value = ''
  }
}

async function confirmDiscard() {
  if (!discardTarget.value) return
  const transaction = discardTarget.value
  discardLoading.value = true
  try {
    await $financesService.reviewImportedTransaction(transaction.id, { approved: false })
    discardTarget.value = null
    toast.add({ title: t('importReviewDiscarded'), color: 'success' })
    await Promise.all([loadReviewing(), props.refresh()])
  } catch {
    toast.add({ title: t('importReviewFailed'), color: 'error' })
  } finally {
    discardLoading.value = false
  }
}

defineExpose({ openModal, openReview })
</script>

<template>
  <UModal
    v-model:open="open"
    :title="title"
    :description="description"
    :ui="{ content: 'sm:max-w-5xl' }"
  >
    <template #body>
      <div v-if="step === 'upload'" class="space-y-4">
        <UFormField :label="$t('importAccount')" required>
          <USelect
            v-model="selectedAccountId"
            :items="accountOptions"
            :placeholder="$t('importSelectAccount')"
            class="w-full"
          />
        </UFormField>

        <UAlert
          color="neutral"
          variant="subtle"
          icon="i-lucide-info"
          :title="$t('importFileHelpTitle')"
          :description="$t('importFileHelp')"
        />

        <UAlert
          v-if="pdfFileName"
          color="warning"
          variant="subtle"
          icon="i-lucide-file-text"
          :title="$t('importPdfDetectedTitle')"
          :description="$t('importPdfDetectedDesc', { file: pdfFileName })"
        >
          <template #actions>
            <UButton
              :to="ofxConverterUrl"
              target="_blank"
              rel="noopener noreferrer"
              :label="$t('importPdfConvert')"
              icon="i-lucide-external-link"
              color="warning"
              variant="subtle"
              size="sm"
            />
          </template>
        </UAlert>

        <label
          class="flex cursor-pointer flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-default bg-elevated/40 px-6 py-10 text-center transition hover:border-primary hover:bg-elevated/70"
          :class="{ 'pointer-events-none opacity-50': !selectedAccountId }"
        >
          <UIcon name="i-lucide-upload-cloud" class="size-9 text-primary" />
          <span class="font-medium text-highlighted">{{ $t('importChooseFile') }}</span>
          <span class="text-sm text-muted">{{ $t('importFormats') }}</span>
          <input
            type="file"
            accept=".csv,.ofx,.pdf,text/csv,application/x-ofx,application/pdf"
            class="sr-only"
            @change="onFileChange"
          >
        </label>

        <p class="text-center text-sm text-muted">
          {{ $t('importPdfHint') }}
          <UButton
            :to="ofxConverterUrl"
            target="_blank"
            rel="noopener noreferrer"
            :label="$t('importPdfConvert')"
            variant="link"
            size="sm"
            trailing-icon="i-lucide-external-link"
            class="p-0"
          />
        </p>

        <div class="flex justify-end">
          <UButton :label="$t('cancel')" color="neutral" variant="subtle" @click="closeModal" />
        </div>
      </div>

      <div v-else-if="step === 'preview'" class="space-y-4">
        <div class="flex flex-wrap items-center gap-2">
          <UBadge color="neutral" variant="subtle">
            {{ format }}
          </UBadge>
          <span class="text-sm text-muted">{{ fileName }}</span>
          <UBadge color="success" variant="subtle">
            {{ $t('importImportableCount', { count: preview?.importable ?? 0 }) }}
          </UBadge>
          <UBadge color="warning" variant="subtle">
            {{ $t('importDuplicateCount', { count: preview?.duplicates ?? 0 }) }}
          </UBadge>
          <UBadge v-if="invalidCount" color="error" variant="subtle">
            {{ $t('importInvalidCount', { count: invalidCount }) }}
          </UBadge>
        </div>

        <UAlert
          v-if="invalidCount"
          color="warning"
          variant="subtle"
          icon="i-lucide-triangle-alert"
          :description="$t('importInvalidHelp', { count: invalidCount })"
        />

        <div class="max-h-[50vh] overflow-auto rounded-lg border border-default">
          <table class="w-full min-w-[880px] text-sm">
            <thead class="sticky top-0 z-10 bg-elevated/95 text-left text-xs uppercase text-muted">
              <tr>
                <th class="w-12 px-3 py-2">
                  <span class="sr-only">{{ $t('importInclude') }}</span>
                </th>
                <th class="px-3 py-2">
                  {{ $t('date') }}
                </th>
                <th class="px-3 py-2">
                  {{ $t('title') }}
                </th>
                <th class="px-3 py-2 text-right">
                  {{ $t('value') }}
                </th>
                <th class="px-3 py-2">
                  {{ $t('type') }}
                </th>
                <th class="px-3 py-2">
                  {{ $t('category') }}
                </th>
                <th class="px-3 py-2">
                  {{ $t('status') }}
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-default">
              <tr v-for="row in rows" :key="row.row" :class="{ 'bg-error/5': row.errors.length }">
                <td class="px-3 py-2 align-middle">
                  <UCheckbox v-model="row.include" :disabled="row.errors.length > 0 || row.duplicate" />
                </td>
                <td class="whitespace-nowrap px-3 py-2 align-middle">
                  {{ row.date ? formatDate(row.date) : '—' }}
                </td>
                <td class="max-w-[260px] px-3 py-2 align-middle">
                  <span class="line-clamp-2" :title="row.description">{{ row.description || '—' }}</span>
                </td>
                <td class="whitespace-nowrap px-3 py-2 text-right align-middle tabular-nums">
                  {{ row.amount !== null ? formatCurrency(row.amount) : '—' }}
                </td>
                <td class="px-3 py-2 align-middle">
                  <USelect
                    v-model="row.income"
                    :items="typeItems"
                    size="sm"
                    class="w-28"
                    @update:model-value="changeRowIncome(row)"
                  />
                </td>
                <td class="px-3 py-2 align-middle">
                  <USelect
                    v-model="row.categoryId"
                    :items="categoryOptions(row.income)"
                    size="sm"
                    class="min-w-40"
                  />
                </td>
                <td class="px-3 py-2 align-middle">
                  <UBadge
                    v-if="row.errors.length"
                    color="error"
                    variant="subtle"
                    :title="rowErrorLabel(row)"
                  >
                    {{ $t('importLineInvalid') }}
                  </UBadge>
                  <UBadge v-else-if="row.duplicate" color="warning" variant="subtle">
                    {{ $t('importLineDuplicate') }}
                  </UBadge>
                  <UBadge v-else color="success" variant="subtle">
                    {{ $t('importLineValid') }}
                  </UBadge>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex flex-wrap items-center justify-between gap-2">
          <UButton
            :label="$t('importChooseAnotherFile')"
            icon="i-lucide-arrow-left"
            color="neutral"
            variant="ghost"
            :disabled="confirming"
            @click="resetFlow"
          />
          <div class="flex items-center gap-2">
            <UButton
              :label="$t('cancel')"
              color="neutral"
              variant="subtle"
              :disabled="confirming"
              @click="closeModal"
            />
            <UButton
              :label="$t('importConfirmCount', { count: includedCount })"
              icon="i-lucide-check"
              :loading="confirming || loadingPreview"
              :disabled="!canConfirm"
              @click="confirmImport"
            />
          </div>
        </div>
      </div>

      <div v-else class="space-y-4">
        <UAlert
          v-if="result"
          color="success"
          variant="subtle"
          icon="i-lucide-check-circle"
          :title="$t('importDoneTitle')"
          :description="$t('importDoneDescription', { imported: result.imported, duplicates: result.duplicates })"
        />

        <div class="flex flex-wrap items-center justify-between gap-2">
          <div>
            <p class="font-medium text-highlighted">
              {{ $t('importReviewTitle') }}
            </p>
            <p class="text-sm text-muted">
              {{ $t('importReviewHelp') }}
            </p>
          </div>
          <UBadge color="warning" variant="subtle">
            {{ reviewing.length }}
          </UBadge>
        </div>

        <div v-if="reviewing.length" class="max-h-[45vh] overflow-auto rounded-lg border border-default">
          <table class="w-full min-w-[760px] text-sm">
            <thead class="sticky top-0 z-10 bg-elevated/95 text-left text-xs uppercase text-muted">
              <tr>
                <th class="px-3 py-2">
                  {{ $t('date') }}
                </th>
                <th class="px-3 py-2">
                  {{ $t('title') }}
                </th>
                <th class="px-3 py-2 text-right">
                  {{ $t('value') }}
                </th>
                <th class="px-3 py-2">
                  {{ $t('category') }}
                </th>
                <th class="px-3 py-2 text-right">
                  {{ $t('actions') }}
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-default">
              <tr v-for="transaction in reviewing" :key="transaction.id">
                <td class="whitespace-nowrap px-3 py-2">
                  {{ formatDate(transaction.executionDate) }}
                </td>
                <td class="max-w-[260px] px-3 py-2">
                  <span class="line-clamp-2" :title="transaction.title">{{ transaction.title }}</span>
                </td>
                <td class="whitespace-nowrap px-3 py-2 text-right tabular-nums">
                  {{ formatCurrency(transaction.value) }}
                </td>
                <td class="px-3 py-2">
                  <USelect
                    v-model="reviewCategories[transaction.id]"
                    :items="categoryOptions(transaction.income)"
                    size="sm"
                    class="min-w-40"
                  />
                </td>
                <td class="px-3 py-2">
                  <div class="flex items-center justify-end gap-1">
                    <UButton
                      icon="i-lucide-check"
                      color="success"
                      variant="ghost"
                      size="sm"
                      :loading="reviewingId === transaction.id"
                      :aria-label="$t('importApprove')"
                      :title="$t('importApprove')"
                      @click="approveReview(transaction)"
                    />
                    <UButton
                      icon="i-lucide-trash"
                      color="error"
                      variant="ghost"
                      size="sm"
                      :aria-label="$t('importDiscard')"
                      :title="$t('importDiscard')"
                      @click="discardTarget = transaction"
                    />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <UAlert
          v-else
          color="neutral"
          variant="subtle"
          icon="i-lucide-check"
          :description="$t('importReviewEmpty')"
        />

        <div class="flex justify-end gap-2">
          <UButton
            v-if="result"
            :label="$t('importAnother')"
            icon="i-lucide-upload"
            color="neutral"
            variant="subtle"
            @click="resetFlow"
          />
          <UButton :label="$t('close')" @click="closeModal" />
        </div>
      </div>
    </template>
  </UModal>

  <UModal
    :open="!!discardTarget"
    :title="$t('importDiscardTitle')"
    :description="discardTarget ? $t('importDiscardDesc', { title: discardTarget.title }) : undefined"
    @update:open="value => { if (!value) discardTarget = null }"
  >
    <template #body>
      <div class="flex justify-end gap-2">
        <UButton :label="$t('cancel')" color="neutral" variant="subtle" @click="discardTarget = null" />
        <UButton
          :label="$t('importDiscard')"
          color="error"
          :loading="discardLoading"
          @click="confirmDiscard"
        />
      </div>
    </template>
  </UModal>
</template>
