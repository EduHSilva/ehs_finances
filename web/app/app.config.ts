import { designTokens } from './design-system/tokens'

export default defineAppConfig({
  ui: {
    colors: {
      primary: designTokens.colors.primary,
      neutral: designTokens.colors.neutral,
      success: designTokens.colors.success,
      warning: designTokens.colors.warning,
      error: designTokens.colors.error,
      info: designTokens.colors.info,
      secondary: designTokens.colors.secondary
    },
    /*
     * Redesign "papel quente": headings serifados (Fraunces) com tracking negativo.
     * O resto do sistema (cores, raios, sombras) vem do CSS em `assets/css/main.css`.
     */
    pageHero: {
      slots: {
        title: 'text-pretty font-display font-semibold tracking-[-0.025em] text-highlighted'
      }
    },
    pageSection: {
      slots: {
        title: 'text-pretty font-display font-semibold tracking-[-0.02em] text-highlighted'
      }
    },
    pageCta: {
      slots: {
        title: 'text-pretty font-display font-semibold tracking-[-0.02em]'
      }
    },
    authForm: {
      slots: {
        title: 'font-display tracking-[-0.02em]'
      }
    },
    pageCard: {
      slots: {
        root: 'shadow-(--brand-shadow)',
        title: 'text-pretty font-display font-semibold tracking-[-0.01em] text-highlighted'
      }
    },
    pageFeature: {
      slots: {
        title: 'text-pretty font-display font-semibold tracking-[-0.01em] text-highlighted'
      }
    },
    /*
     * Preenchimentos sólidos: no dark o texto do botão é tinta escura (regra de
     * contraste da spec, nunca branco sobre accent claro). No light, sólidos
     * semânticos usam branco sobre o tom forte (o `text-inverted` do tema escuro
     * é a tinta clara).
     */
    button: {
      compoundVariants: [
        {
          color: 'primary',
          variant: 'solid',
          class: { base: 'text-(--brand-on-primary)' }
        },
        {
          color: 'success',
          variant: 'solid',
          class: { base: 'dark:text-(--brand-on-primary)' }
        },
        {
          color: 'warning',
          variant: 'solid',
          class: { base: 'text-(--brand-on-primary)' }
        },
        {
          color: 'error',
          variant: 'solid',
          class: { base: 'dark:text-(--brand-on-primary)' }
        }
      ]
    },
    badge: {
      compoundVariants: [
        {
          color: 'primary',
          variant: 'solid',
          class: { base: 'text-(--brand-on-primary)' }
        }
      ]
    }
  }
})
