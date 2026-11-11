import type { FiltroFila } from '@/domain/retirada'

export const portariaKeys = {
  raiz: ['portaria'] as const,
  sessao: () => ['portaria', 'sessao'] as const,
  fila: (filtro: FiltroFila) => ['portaria', 'fila', filtro] as const,
  retirada: (id: number) => ['portaria', 'retirada', id] as const,
  resumo: () => ['portaria', 'resumo'] as const,
  historico: (limite: number) => ['portaria', 'historico', limite] as const,
}
