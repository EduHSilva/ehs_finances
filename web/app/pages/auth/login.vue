<script setup lang="ts">
import * as z from 'zod'
import type { AuthFormField, FormSubmitEvent } from '@nuxt/ui'
import { useUser } from '~/plugins/userService'

const toast = useToast()
const loading = ref(false)
const config = useRuntimeConfig()
const { t } = useI18n()

type GoogleTokenResponse = {
  credential?: string
}

type GoogleWindow = Window & {
  google?: {
    accounts?: {
      id?: {
        initialize: (options: {
          client_id: string
          callback: (response: GoogleTokenResponse) => void
          auto_select?: boolean
          use_fedcm_for_prompt?: boolean
          cancel_on_tap_outside?: boolean
        }) => void
        prompt: () => void
      }
    }
  }
}

async function ensureGoogleScriptLoaded() {
  if (!import.meta.client) return false
  const w = window as GoogleWindow
  if (w.google?.accounts?.id) return true

  const existing = document.querySelector('script[src="https://accounts.google.com/gsi/client"]')
  if (existing) {
    await new Promise<void>(resolve => existing.addEventListener('load', () => resolve(), { once: true }))
    return Boolean((window as GoogleWindow).google?.accounts?.id)
  }

  await new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true
    script.defer = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Failed to load Google script'))
    document.head.appendChild(script)
  })

  return Boolean((window as GoogleWindow).google?.accounts?.id)
}

async function loginWithGoogle() {
  if (!import.meta.client) return
  const clientId = config.public.googleClientId
  if (!clientId) {
    toast.add({ title: t('attention'), description: t('googleUnavailable'), color: 'error' })
    return
  }

  const loaded = await ensureGoogleScriptLoaded().catch(() => false)
  if (!loaded) {
    toast.add({ title: t('attention'), description: t('googleUnavailable'), color: 'error' })
    return
  }

  const w = window as GoogleWindow
  w.google?.accounts?.id?.initialize({
    client_id: clientId,
    callback: async (response) => {
      const token = response.credential
      if (!token) return
      await submitGoogle(token)
    },
    auto_select: false,
    use_fedcm_for_prompt: false,
    cancel_on_tap_outside: true
  })

  w.google?.accounts?.id?.prompt()
}

async function submitGoogle(idToken: string) {
  const { $userService } = useNuxtApp()
  loading.value = true
  try {
    const response = await $userService.login(null, null, idToken)
    getTokenCookie().value = { token: response.data.token }
    useUser().value = response.data
    await navigateTo('/dashboard')
  } catch {
    toast.add({ title: t('attention'), description: t('googleLoginFailed'), color: 'error' })
  } finally {
    loading.value = false
  }
}

const providers = computed(() => config.public.googleClientId
  ? [{
      label: t('googleLoginAction'),
      icon: 'i-simple-icons-google',
      onClick: loginWithGoogle
    }]
  : [])

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
          :providers="providers"
          :separator="providers.length ? $t('or') : undefined"
          :submit="{ label: $t('loginSubmit'), loading }"
          @submit="onSubmit"
        >
          <template #password-hint>
            <a href="/user/reset-password" class="text-sm font-medium text-primary hover:underline">
              {{ $t('forgotPassword') }}
            </a>
          </template>

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
