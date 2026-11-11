import { createContext, useContext, type ReactNode } from 'react'
import type { Servicos } from '@/services'

const ServicosContext = createContext<Servicos | null>(null)

export function ServicosProvider({ servicos, children }: { servicos: Servicos; children: ReactNode }) {
  return <ServicosContext.Provider value={servicos}>{children}</ServicosContext.Provider>
}

export function useServicos(): Servicos {
  const ctx = useContext(ServicosContext)
  if (!ctx) throw new Error('useServicos deve ser usado dentro de <ServicosProvider>')
  return ctx
}
