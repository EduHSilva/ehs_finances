<script setup lang="ts">
import { useUser } from '~/plugins/userService'

const user = useUser()
const { t } = useI18n()

const links = computed(() => user.value
  ? [{
      label: t('landingDashboard'),
      to: '/dashboard',
      size: 'xl' as const,
      trailingIcon: 'i-lucide-arrow-right'
    }]
  : [{
      label: t('login'),
      to: '/auth/login',
      size: 'xl' as const,
      color: 'neutral' as const,
      variant: 'outline' as const
    }, {
      label: t('createAccount'),
      to: '/auth/register',
      size: 'xl' as const,
      trailingIcon: 'i-lucide-arrow-right'
    }])

const features = computed(() => [{
  icon: 'i-lucide-wallet-cards',
  title: t('landingFeatureAccountsTitle'),
  description: t('landingFeatureAccountsDescription')
}, {
  icon: 'i-lucide-receipt',
  title: t('landingFeatureTransactionsTitle'),
  description: t('landingFeatureTransactionsDescription')
}, {
  icon: 'i-lucide-target',
  title: t('landingFeatureGoalsTitle'),
  description: t('landingFeatureGoalsDescription')
}, {
  icon: 'i-lucide-line-chart',
  title: t('landingFeatureProjectionTitle'),
  description: t('landingFeatureProjectionDescription')
}])
</script>

<template>
  <div class="min-h-screen bg-default">
    <header class="sticky top-0 z-10 border-b border-default bg-default/80 backdrop-blur">
      <div class="mx-auto flex w-full max-w-7xl items-center justify-between gap-4 px-6 py-4">
        <NuxtLink to="/" aria-label="EHS Finances">
          <BrandLogo />
        </NuxtLink>

        <div class="flex items-center gap-2">
          <UButton v-if="!user" to="/auth/login" variant="ghost" color="neutral" :label="$t('login')" />
          <UButton
            :to="user ? '/dashboard' : '/auth/register'"
            :label="user ? $t('landingDashboard') : $t('createAccount')"
            :trailing-icon="user ? undefined : 'i-lucide-arrow-right'"
          />
        </div>
      </div>
    </header>

    <UPageHero
      :headline="$t('landingHeadline')"
      :title="$t('landingTitle')"
      :description="$t('landingDescription')"
      :links="links"
    />

    <UPageSection
      :title="$t('landingFeaturesTitle')"
      :description="$t('landingFeaturesDescription')"
      :features="features"
    />

    <UPageCTA
      :title="$t('landingCtaTitle')"
      :description="$t('landingCtaDescription')"
      :links="links"
      variant="subtle"
    />

    <footer class="border-t border-default">
      <div class="mx-auto flex w-full max-w-7xl flex-col items-center justify-between gap-2 px-6 py-6 text-sm text-muted sm:flex-row">
        <BrandLogo />
        <span>{{ new Date().getFullYear() }} · {{ $t('landingFooter') }}</span>
      </div>
    </footer>
  </div>
</template>
