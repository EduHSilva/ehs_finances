<script setup lang="ts">
import { CalendarDate } from '@internationalized/date'

const props = defineProps<{
  modelValue?: Date | null
  locale?: string
}>()

const emit = defineEmits<{ 'update:modelValue': [Date | undefined] }>()

const { t } = useI18n()
const calendarOpen = ref(false)

function toDate(value: unknown): Date | undefined {
  if (!value) return undefined
  if (value instanceof Date) return value
  const date = value as { year?: number, month?: number, day?: number }
  return date.year && date.month && date.day
    ? new Date(date.year, date.month - 1, date.day)
    : undefined
}

const calendarValue = computed({
  get: () => {
    const date = props.modelValue
    return date ? new CalendarDate(date.getFullYear(), date.getMonth() + 1, date.getDate()) : undefined
  },
  set: (value) => {
    emit('update:modelValue', value ? new Date(value.year, value.month - 1, value.day) : undefined)
    calendarOpen.value = false
  }
})
</script>

<template>
  <div class="flex w-full items-center gap-1.5">
    <UInputDate
      :model-value="calendarValue"
      :locale="locale"
      class="w-full"
      @update:model-value="emit('update:modelValue', toDate($event))"
    />
    <UPopover v-model:open="calendarOpen">
      <UButton
        icon="i-lucide-calendar-days"
        color="neutral"
        variant="outline"
        :aria-label="t('openCalendar')"
      />
      <template #content>
        <UCalendar
          v-model="calendarValue"
          :locale="locale"
          class="p-2"
        />
      </template>
    </UPopover>
  </div>
</template>
