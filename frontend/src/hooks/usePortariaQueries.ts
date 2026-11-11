import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { FiltroFila } from '@/domain/retirada'
import { NetworkError } from '@/services'
import { useServicos } from './ServicosContext'
import { portariaKeys } from './queryKeys'

/** Sem tempo real (API HTTP antes da Etapa 9), a fila é atualizada por polling. */
function useIntervaloAtualizacao() {
  const { retiradas } = useServicos()
  return retiradas.tempoReal ? 30_000 : 10_000
}

export function useSessao() {
  const { retiradas } = useServicos()
  return useQuery({ queryKey: portariaKeys.sessao(), queryFn: () => retiradas.obterSessao(), staleTime: Infinity })
}

export function useFila(filtro: FiltroFila) {
  const { retiradas } = useServicos()
  const intervalo = useIntervaloAtualizacao()
  return useQuery({
    queryKey: portariaKeys.fila(filtro),
    queryFn: () => retiradas.listarFila(filtro),
    placeholderData: keepPreviousData,
    refetchInterval: (q) => (q.state.error instanceof NetworkError ? 8_000 : intervalo),
  })
}

export function useRetirada(id: number | null) {
  const { retiradas } = useServicos()
  return useQuery({
    queryKey: portariaKeys.retirada(id ?? -1),
    queryFn: () => retiradas.obterRetirada(id!),
    enabled: id !== null,
  })
}

export function useResumo() {
  const { retiradas } = useServicos()
  const intervalo = useIntervaloAtualizacao()
  return useQuery({ queryKey: portariaKeys.resumo(), queryFn: () => retiradas.obterResumo(), refetchInterval: intervalo })
}

export function useHistorico(limite = 10) {
  const { retiradas } = useServicos()
  return useQuery({ queryKey: portariaKeys.historico(limite), queryFn: () => retiradas.listarHistorico(limite) })
}
