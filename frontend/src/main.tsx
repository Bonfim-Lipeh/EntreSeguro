import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import '@fontsource-variable/inter'
import './index.css'
import App from './App'
import { Toaster } from '@/components/ui/sonner'
import { ServicosProvider } from '@/hooks/ServicosContext'
import { criarServicos, NetworkError } from '@/services'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5_000,
      retry: (falhas, erro) => erro instanceof NetworkError && falhas < 1,
      refetchOnWindowFocus: true,
    },
    mutations: { retry: false },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ServicosProvider servicos={criarServicos()}>
      <QueryClientProvider client={queryClient}>
        <App />
        <Toaster />
      </QueryClientProvider>
    </ServicosProvider>
  </StrictMode>,
)
