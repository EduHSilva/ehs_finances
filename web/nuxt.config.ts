// https://nuxt.com/docs/api/configuration/nuxt-config
const publicApiUrl = (globalThis as unknown as {
  process?: { env?: Record<string, string | undefined> }
}).process?.env?.NUXT_PUBLIC_API_URL
const appBaseURL = (globalThis as unknown as {
  process?: { env?: Record<string, string | undefined> }
}).process?.env?.NUXT_APP_BASE_URL || '/'

export default defineNuxtConfig({
  modules: [
    '@nuxt/eslint',
    '@nuxt/ui',
    '@vueuse/nuxt',
    '@nuxtjs/i18n'
  ],

  ssr: false,

  app: {
    baseURL: appBaseURL
  },

  devtools: {
    enabled: true
  },

  css: ['~/assets/css/main.css'],

  runtimeConfig: {
    public: {
      apiUrl: publicApiUrl
    }
  },

  routeRules: {
    '/api/**': {
      cors: true
    }
  },

  compatibilityDate: '2024-07-11',

  eslint: {
    config: {
      stylistic: {
        commaDangle: 'never',
        braceStyle: '1tbs'
      }
    }
  },
  i18n: {
    defaultLocale: 'pt',
    locales: [
      { code: 'en', name: 'English', file: 'en.json' },
      { code: 'pt', name: 'Pt', file: 'pt.json' }
    ],
    strategy: 'no_prefix',
    detectBrowserLanguage: {
      useCookie: true,
      cookieKey: 'i18n_redirected',
      redirectOn: 'root'
    }
  }
})
