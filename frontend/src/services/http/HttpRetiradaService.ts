import type { RetiradaService } from '../contracts/RetiradaService'
import type {
  ConfirmarIdentificacaoRequest,
  FiltroFila,
  MovimentacaoDTO,
  RejeitarRequest,
  ResumoPortariaDTO,
  RetiradaDTO,
  SessaoPortariaDTO,
  ValidacaoIdentificacaoDTO,
} from '@/domain/retirada'
import { criarApiClient } from './apiClient'
import type { SessaoAuth } from './sessaoAuth'

/**
 * Implementação REST do contrato, autenticada com JWT (ver sessaoAuth.ts).
 * Consultas (sessão, fila, detalhe, resumo, histórico) validadas contra o backend Java.
 * As ações de status (POST) ainda não possuem endpoint no backend.
 */
export class HttpRetiradaService implements RetiradaService {
  readonly modo = 'http' as const
  /** WebSocket/STOMP entra na Etapa 9; até lá a fila é atualizada por polling. */
  readonly tempoReal = false
  private readonly api: ReturnType<typeof criarApiClient>

  constructor(baseUrl: string, sessao?: SessaoAuth) {
    this.api = criarApiClient(baseUrl.replace(/\/$/, ''), sessao)
  }

  obterSessao() {
    return this.api.get<SessaoPortariaDTO>('/api/portaria/sessao')
  }

  listarFila(filtro: FiltroFila) {
    const params = new URLSearchParams()
    if (filtro.status.length) params.set('status', filtro.status.join(','))
    if (filtro.busca.trim()) params.set('busca', filtro.busca.trim())
    return this.api.get<RetiradaDTO[]>(`/api/portaria/retiradas?${params}`)
  }

  obterRetirada(id: number) {
    return this.api.get<RetiradaDTO>(`/api/portaria/retiradas/${id}`)
  }

  obterResumo() {
    return this.api.get<ResumoPortariaDTO>('/api/portaria/resumo')
  }

  listarHistorico(limite: number) {
    return this.api.get<MovimentacaoDTO[]>(`/api/portaria/movimentacoes?limite=${limite}`)
  }

  iniciarAtendimento(id: number, versao: number) {
    return this.api.post<RetiradaDTO>(`/api/portaria/retiradas/${id}/iniciar-atendimento`, { versao })
  }

  validarIdentificacao(id: number, codigo: string) {
    return this.api.post<ValidacaoIdentificacaoDTO>(`/api/portaria/retiradas/${id}/validar-identificacao`, {
      codigo,
    })
  }

  confirmarIdentificacao(id: number, req: ConfirmarIdentificacaoRequest) {
    return this.api.post<RetiradaDTO>(`/api/portaria/retiradas/${id}/confirmar-identificacao`, req)
  }

  confirmarRetirada(id: number, versao: number) {
    return this.api.post<RetiradaDTO>(`/api/portaria/retiradas/${id}/confirmar-retirada`, { versao })
  }

  rejeitar(id: number, req: RejeitarRequest) {
    return this.api.post<RetiradaDTO>(`/api/portaria/retiradas/${id}/rejeitar`, req)
  }

  assinarEventos() {
    return () => {}
  }
}
