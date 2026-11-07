<script setup lang="ts">
import * as z from 'zod'
import type { AuthFormField, FormSubmitEvent } from '@nuxt/ui'

const toast = useToast()
const loading = ref(false)
const registeredEmail = ref('')

const fields: AuthFormField[] = [{
  name: 'username',
  type: 'text',
  label: $t('username'),
  placeholder: 'Maria Silva',
  required: true
}, {
  name: 'email',
  type: 'email',
  label: $t('email'),
  placeholder: 'voce@email.com',
  required: true
}, {
  name: 'password',
  type: 'password',
  label: $t('password'),
  placeholder: '••••••••',
  required: true
}]

const schema = z.object({
  username: z.string().trim().min(2, 'Must be at least 2 characters'),
  email: z.email('Invalid email'),
  password: z.string('Password is required').min(8, 'Must be at least 8 characters')
})

type Schema = z.output<typeof schema>

async function onSubmit(payload: FormSubmitEvent<Schema>) {
  const { $userService } = useNuxtApp()
  loading.value = true
  try {
    await $userService.register(payload.data.username, payload.data.email, payload.data.password)
    registeredEmail.value = payload.data.email
  } catch {
    toast.add({ title: $t('attention'), description: $t('registerFailed'), color: 'error' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen flex-col bg-elevated/30">
    <header class="flex items-center justify-between px-6 py-4">
      <NuxtLink to="/" aria-label="EHS Finances">
        <BrandLogo />
      </NuxtLink>
      <UButton to="/" variant="ghost" color="neutral" icon="i-lucide-arrow-left" :label="$t('backToHome')" />
    </header>

    <div class="flex flex-1 items-center justify-center p-4">
      <UPageCard class="w-full max-w-md">
        <UAlert
          v-if="registeredEmail"
          color="success"
          variant="subtle"
          icon="i-lucide-mail-check"
          :title="$t('registerSuccessTitle')"
          :description="$t('registerSuccessDescription', { email: registeredEmail })"
        >
          <template #actions>
            <UButton to="/auth/login" :label="$t('login')" size="sm" />
          </template>
        </UAlert>

        <UAuthForm
          v-else
          :schema="schema"
          :title="$t('registerTitle')"
          :description="$t('registerDescription')"
          icon="i-lucide-user-plus"
          :fields="fields"
          :submit="{ label: $t('registerSubmit'), loading }"
          @submit="onSubmit"
        >
          <template #footer>
            <p class="text-sm text-muted">
              {{ $t('haveAccount') }}
              <NuxtLink to="/auth/login" class="font-medium text-primary hover:underline">
                {{ $t('login') }}
              </NuxtLink>
            </p>
          </template>
        </UAuthForm>
      </UPageCard>
    </div>
  </div>
</template>
