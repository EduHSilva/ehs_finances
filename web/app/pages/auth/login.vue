<script setup lang="ts">
import * as z from 'zod'
import type { AuthFormField, FormSubmitEvent } from '@nuxt/ui'
import { useUser } from '~/plugins/userService'

const toast = useToast()
const loading = ref(false)

const fields: AuthFormField[] = [{
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
  email: z.email('Invalid email'),
  password: z.string('Password is required').min(8, 'Must be at least 8 characters')
})

type Schema = z.output<typeof schema>

async function onSubmit(payload: FormSubmitEvent<Schema>) {
  const { $userService } = useNuxtApp()
  loading.value = true
  try {
    const response = await $userService.login(payload.data.email, payload.data.password)
    getTokenCookie().value = { token: response.data.token }
    useUser().value = response.data
    await navigateTo('/dashboard')
  } catch {
    toast.add({ title: $t('attention'), description: $t('loginError'), color: 'error' })
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
        <UAuthForm
          :schema="schema"
          :title="$t('loginTitle')"
          :description="$t('loginDescription')"
          icon="i-lucide-user"
          :fields="fields"
          :submit="{ label: $t('loginSubmit'), loading }"
          @submit="onSubmit"
        >
          <template #footer>
            <p class="text-sm text-muted">
              {{ $t('noAccount') }}
              <NuxtLink to="/auth/register" class="font-medium text-primary hover:underline">
                {{ $t('createAccount') }}
              </NuxtLink>
            </p>
          </template>
        </UAuthForm>
      </UPageCard>
    </div>
  </div>
</template>
