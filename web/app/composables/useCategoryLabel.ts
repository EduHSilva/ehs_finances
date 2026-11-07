export function useCategoryLabel() {
  const { t, te } = useI18n()

  return (title?: string | null, defaultKey?: string | null) => {
    if (defaultKey && te(defaultKey)) return t(defaultKey)
    return title || t('noCategory')
  }
}
