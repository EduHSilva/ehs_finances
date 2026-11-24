<script setup lang="ts">
import { designTokens } from '~/design-system/tokens'

const colorMode = useColorMode()
const appBase = useRuntimeConfig().app.baseURL

const color = computed(() => colorMode.value === 'dark' ? designTokens.neutrals.dark.bg : designTokens.accents.light.primary)

useHead({
  titleTemplate: (title) => (title ? `${title} · Finanças` : 'Finanças — Controle financeiro'),
  meta: [
    { charset: 'utf-8' },
    { name: 'viewport', content: 'width=device-width, initial-scale=1' },
    { key: 'theme-color', name: 'theme-color', content: color }
  ],
  link: [
    { rel: 'icon', type: 'image/svg+xml', href: `${appBase}favicon.svg` },
    { rel: 'icon', href: `${appBase}favicon.ico`, sizes: '32x32' },
    { rel: 'apple-touch-icon', href: `${appBase}apple-touch-icon.png` }
  ],
  htmlAttrs: {
    lang: 'pt-BR'
  }
})

const title = 'Finanças — Controle financeiro'
const description = 'Controle contas, transações, metas mensais e previsibilidade de caixa em um só painel.'

useSeoMeta({
  description,
  ogTitle: title,
  ogDescription: description,
  ogLocale: 'pt_BR',
  twitterCard: 'summary_large_image'
})
</script>

<template>
  <UApp>
    <NuxtLoadingIndicator />

    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </UApp>
</template>
