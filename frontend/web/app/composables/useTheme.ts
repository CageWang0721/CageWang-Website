export function useTheme() {
  const isDark = useState('theme-dark', () => false)

  const apply = (dark: boolean) => {
    isDark.value = dark
    if (import.meta.client) {
      document.documentElement.dataset.theme = dark ? 'dark' : 'light'
      localStorage.setItem('blog-theme', dark ? 'dark' : 'light')
    }
  }

  onMounted(() => {
    const stored = localStorage.getItem('blog-theme')
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    apply(stored ? stored === 'dark' : prefersDark)
  })

  return {
    isDark: readonly(isDark),
    toggle: () => apply(!isDark.value)
  }
}
