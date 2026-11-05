import { designTokens } from './design-system/tokens'

export default defineAppConfig({
  ui: {
    colors: {
      primary: designTokens.colors.primary,
      neutral: designTokens.colors.neutral
    }
  }
})
