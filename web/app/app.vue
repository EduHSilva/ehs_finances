<script setup lang="ts">
const colorMode = useColorMode()
const appBase = useRuntimeConfig().app.baseURL

const color = computed(() => colorMode.value === 'dark' ? '#18181B' : '#4F46E5')

useHead({
  titleTemplate: (title) => (title ? `${title} · EHS Finances` : 'EHS Finances — Controle financeiro'),
  meta: [
    { charset: 'utf-8' },
    { name: 'viewport', content: 'width=device-width, initial-scale=1' },
    { key: 'theme-color', name: 'theme-color', content: color },
    { name: 'apple-mobile-web-app-title', content: 'EHS Finances' }
  ],
  link: [
    { rel: 'icon', type: 'image/svg+xml', href: `${appBase}favicon.svg` },
    { rel: 'icon', href: `${appBase}favicon.ico`, sizes: '32x32' },
    { rel: 'manifest', href: `${appBase}manifest.webmanifest` },
    { rel: 'apple-touch-icon', href: `${appBase}apple-touch-icon.png` }
  ],
  htmlAttrs: {
    lang: 'pt-BR'
  }
})

const title = 'EHS Finances — Controle financeiro'
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
