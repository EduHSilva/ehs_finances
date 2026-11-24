/**
 * Design system do Finanças — "papel quente + headings serifados" com accent esmeralda.
 *
 * Aqui ficam os hex (única fonte de cor crua). Os consumidores usam tokens semânticos:
 * - Nuxt UI (`ui.colors.*`) lê as escalas deste arquivo via `app.config.ts`.
 * - O CSS (`app/assets/css/main.css`) recebe as mesmas escalas/semânticas via
 *   `nuxt.config.ts` (`app.head.style`), que injeta variáveis `--brand-*` e `--ui-*`.
 *
 * Ao mudar um tom, atualize `accents`/`neutrals`/`semantics` e as escalas juntos.
 */

export type ScaleStep = 50 | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900 | 950
export type ColorScale = Required<Record<ScaleStep, string>>
export type ThemeMode = 'light' | 'dark'

export const accents = {
  light: {
    primary: '#1E7A57',
    primaryStrong: '#155F43',
    primarySoft: '#E1F0E8',
    onPrimary: '#FFFFFF'
  },
  dark: {
    primary: '#7FD0A9',
    primaryStrong: '#A5E3C4',
    primarySoft: '#1B332A',
    onPrimary: '#0C1F17'
  }
} as const

/** Neutros "papel quente" (light/dark). */
export const neutrals = {
  light: {
    bg: '#F5F2EA',
    surface: '#FFFDF8',
    surface2: '#EFE9DC',
    text: '#26221C',
    muted: '#6B6459',
    subtle: '#9A9284',
    border: '#E7E0D3',
    borderStrong: '#D8CFBF'
  },
  dark: {
    bg: '#16170F',
    surface: '#1E1F18',
    surface2: '#262720',
    text: '#F1ECDF',
    muted: '#B6AFA0',
    subtle: '#8C8576',
    border: '#2F3027',
    borderStrong: '#43443A'
  }
} as const

/** Sidebar escura nos dois temas (gradiente vertical + accent legível). */
export const sidebar = {
  light: {
    base: '#1A2621',
    top: '#22332B',
    text: '#E9E5D9',
    muted: '#9DABA2',
    accent: '#86D6B1'
  },
  dark: {
    base: '#0D120F',
    top: '#131B16',
    text: '#ECE8DC',
    muted: '#93A096',
    accent: '#86D6B1'
  }
} as const

export const semantics = {
  light: {
    success: '#2F7D5B',
    successSoft: '#E5F2EA',
    danger: '#B34A33',
    dangerSoft: '#FAE8E1',
    warning: '#A9761C',
    warningSoft: '#F8EFD8'
  },
  dark: {
    success: '#8AD3A6',
    successSoft: '#1D3326',
    danger: '#EB9C84',
    dangerSoft: '#3B241C',
    warning: '#E3B96B',
    warningSoft: '#3A2F1A'
  }
} as const

export const radii = {
  sm: '8px',
  md: '10px',
  lg: '14px',
  xl: '20px'
} as const

export const shadows = {
  card: '0 10px 30px rgba(38, 34, 28, .07)',
  pop: '0 24px 60px -28px rgba(32, 28, 18, .45)',
  cardDark: '0 12px 32px rgba(0, 0, 0, .35)',
  popDark: '0 24px 60px -24px rgba(0, 0, 0, .65)'
} as const

/**
 * Escalas exigidas por bibliotecas (Nuxt UI/Tailwind), derivadas do tom base sem
 * trocar de matiz: o shade 600 (light) e o 400 (dark) batem com `accents`.
 */
export const scales = {
  /** Esmeralda — 600 = #1E7A57 (primary light). */
  emerald: {
    50: '#EEF7F2',
    100: '#D8EDE3',
    200: '#B3DBC8',
    300: '#86C4A9',
    400: '#4FA783',
    500: '#2C8D66',
    600: '#1E7A57',
    700: '#155F43',
    800: '#124F38',
    900: '#0F4130',
    950: '#08251B'
  },
  /** Esmeralda clara — 400 = #7FD0A9 (primary dark). */
  mint: {
    50: '#F0FAF4',
    100: '#DCF5E7',
    200: '#BCEACF',
    300: '#9ADDB6',
    400: '#7FD0A9',
    500: '#57BB8D',
    600: '#33A072',
    700: '#1E7A57',
    800: '#155F43',
    900: '#124F38',
    950: '#08251B'
  },
  /** "Papel quente" — 50 = surface light, 950 = bg dark. */
  paper: {
    50: '#FFFDF8',
    100: '#F5F2EA',
    200: '#EFE9DC',
    300: '#E7E0D3',
    400: '#D8CFBF',
    500: '#B6AFA0',
    600: '#9A9284',
    700: '#6B6459',
    800: '#3A352C',
    900: '#26221C',
    950: '#16170F'
  },
  success: {
    50: '#E5F2EA',
    100: '#D3EADF',
    200: '#A9D6C1',
    300: '#7CC0A3',
    400: '#8AD3A6',
    500: '#4FA07A',
    600: '#2F7D5B',
    700: '#27664B',
    800: '#1F523C',
    900: '#1D3326',
    950: '#12241A'
  },
  warning: {
    50: '#F8EFD8',
    100: '#F3E4BE',
    200: '#E9D092',
    300: '#DCB866',
    400: '#E3B96B',
    500: '#C08C28',
    600: '#A9761C',
    700: '#8C6117',
    800: '#6F4E14',
    900: '#3A2F1A',
    950: '#241D10'
  },
  danger: {
    50: '#FAE8E1',
    100: '#F6D6C8',
    200: '#EFB090',
    300: '#E27F62',
    400: '#EB9C84',
    500: '#C4614A',
    600: '#B34A33',
    700: '#903B29',
    800: '#733125',
    900: '#3B241C',
    950: '#24150F'
  }
} satisfies Record<string, ColorScale>

const scaleVars = (name: string, scale: ColorScale) => Object
  .entries(scale)
  .map(([step, value]) => `  --brand-${name}-${step}: ${value};`)
  .join('\n')

/** Bloco CSS com as escalas e as variáveis semânticas de um tema. */
export function themeStyle(mode: ThemeMode) {
  const n = neutrals[mode]
  const a = accents[mode]
  const s = sidebar[mode]
  const se = semantics[mode]
  const isLight = mode === 'light'

  return [
    '/* Gerado de app/design-system/tokens.ts — não edite aqui. */',
    isLight ? ':root, .light {' : '.dark {',
    '  --font-sans: Inter, system-ui, sans-serif;',
    '  --font-display: Fraunces, Georgia, serif;',
    scaleVars('primary', scales.emerald),
    scaleVars('neutral', scales.paper),
    scaleVars('success', scales.success),
    scaleVars('warning', scales.warning),
    scaleVars('error', scales.danger),
    scaleVars('info', scales.emerald),
    scaleVars('secondary', scales.emerald),
    `  --ui-radius: ${radii.md};`,
    `  --ui-container: 80rem;`,
    `  --brand-radius: ${radii.md};`,
    '  --brand-radius-card: 16px;',
    `  --brand-shadow: ${isLight ? shadows.card : shadows.cardDark};`,
    `  --brand-shadow-pop: ${isLight ? shadows.pop : shadows.popDark};`,
    `  --brand-primary: ${a.primary};`,
    `  --brand-primary-hover: ${a.primaryStrong};`,
    `  --brand-primary-soft: ${a.primarySoft};`,
    `  --brand-on-primary: ${a.onPrimary};`,
    `  --brand-accent: ${a.primary};`,
    `  --brand-success: ${se.success};`,
    `  --brand-success-soft: ${se.successSoft};`,
    `  --brand-warning: ${se.warning};`,
    `  --brand-warning-soft: ${se.warningSoft};`,
    `  --brand-danger: ${se.danger};`,
    `  --brand-danger-soft: ${se.dangerSoft};`,
    `  --brand-sidebar: ${s.base};`,
    `  --brand-sidebar-top: ${s.top};`,
    `  --brand-sidebar-text: ${s.text};`,
    `  --brand-sidebar-muted: ${s.muted};`,
    `  --brand-sidebar-accent: ${s.accent};`,
    '  /* Cores semânticas próprias (podem ser usadas fora do Tailwind). */',
    `  --color-bg: ${n.bg};`,
    `  --color-surface: ${n.surface};`,
    `  --color-surface-2: ${n.surface2};`,
    `  --color-text: ${n.text};`,
    `  --color-muted: ${n.muted};`,
    `  --color-subtle: ${n.subtle};`,
    `  --color-border: ${n.border};`,
    `  --color-border-strong: ${n.borderStrong};`,
    '}',
    isLight ? '.light, :host {' : '.dark {',
    `  --ui-bg: ${n.bg};`,
    `  --ui-bg-muted: ${n.surface};`,
    `  --ui-bg-elevated: ${n.surface2};`,
    `  --ui-bg-accented: ${n.border};`,
    `  --ui-bg-inverted: ${n.text};`,
    `  --ui-text: ${n.text};`,
    `  --ui-text-muted: ${n.muted};`,
    `  --ui-text-toned: ${n.muted};`,
    `  --ui-text-dimmed: ${n.subtle};`,
    `  --ui-text-highlighted: ${n.text};`,
    `  --ui-text-inverted: ${n.surface};`,
    `  --ui-border: ${n.border};`,
    `  --ui-border-muted: ${n.border};`,
    `  --ui-border-accented: ${n.borderStrong};`,
    `  --ui-border-inverted: ${n.borderStrong};`,
    `  --ui-primary: ${a.primary};`,
    '}'
  ].join('\n')
}

export const designTokens = {
  brand: {
    name: 'Finanças',
    logoIcon: 'i-lucide-wallet-cards'
  },
  typography: {
    sans: 'Inter, system-ui, sans-serif',
    display: 'Fraunces, Georgia, serif'
  },
  colors: {
    /** Escalas consumidas pelo Nuxt UI em `app.config.ts`. */
    primary: 'emerald',
    neutral: 'paper',
    success: 'success',
    warning: 'warning',
    danger: 'danger',
    error: 'danger',
    info: 'emerald',
    secondary: 'emerald'
  },
  accents,
  neutrals,
  sidebar,
  semantics,
  radii,
  shadows,
  scales
}
