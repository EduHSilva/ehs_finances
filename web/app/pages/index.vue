<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent, AuthFormField } from '@nuxt/ui'
import { useUser } from '~/plugins/userService'

const toast = useToast()

const fields: AuthFormField[] = [{
  name: 'email',
  type: 'email',
  label: 'Email',
  placeholder: 'Enter your email',
  required: true
}, {
  name: 'password',
  label: 'Password',
  type: 'password',
  placeholder: 'Enter your password',
  required: true
}, {
  name: 'remember',
  label: 'Remember me',
  type: 'checkbox'
}]

const providers = [{
  label: 'Google',
  icon: 'i-simple-icons-google',
  onClick: () => {
    toast.add({ title: 'Google', description: 'Login with Google' })
  }
}, {
  label: 'GitHub',
  icon: 'i-simple-icons-github',
  onClick: () => {
    toast.add({ title: 'GitHub', description: 'Login with GitHub' })
  }
}]

const schema = z.object({
  email: z.email('Invalid email'),
  password: z.string('Password is required').min(8, 'Must be at least 8 characters')
})

type Schema = z.output<typeof schema>

async function onSubmit(payload: FormSubmitEvent<Schema>) {
  const { $userService } = useNuxtApp()
  try {
    const data = await $userService.login(payload.data.email, payload.data.password)
    if (!data) {
      toast.add({
        title: $t('attention'),
        description: $t('loginError'),
        icon: 'i-lucide-calendar-days'
      })
    } else {
      const token = getTokenCookie()
      console.log(data.data)
      token.value = { token: data.data.token }
      useUser().value = data.data
      navigateTo('/dashboard')
    }
  } catch (e) {
    console.error(e)
    toast.add({
      title: $t('attention'),
      description: $t('loginError'),
      icon: 'i-lucide-calendar-days'
    })
  }
}
</script>

<template>
  <div class="flex flex-col items-center justify-center gap-4 p-4">
    <UPageCard class="w-full max-w-md">
      <UAuthForm
        :schema="schema"
        title="Login"
        description="Enter your credentials to access your account."
        icon="i-lucide-user"
        :fields="fields"
        :providers="providers"
        @submit="onSubmit"
      />
    </UPageCard>
  </div>
</template>
