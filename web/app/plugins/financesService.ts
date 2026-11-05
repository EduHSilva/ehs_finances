import financesService from '~/services/financesService'

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()

  const service = financesService({
    apiUrl: `${config.public.apiUrl || apiOrigin()}/finances/`
  })

  return {
    provide: {
      financesService: service
    }
  }
})
