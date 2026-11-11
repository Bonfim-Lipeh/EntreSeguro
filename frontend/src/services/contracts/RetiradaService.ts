import type {
  ConfirmarIdentificacaoRequest,
  EventoPortaria,
  FiltroFila,
  MovimentacaoDTO,
  RejeitarRequest,
  ResumoPortariaDTO,
  RetiradaDTO,
  SessaoPortariaDTO,
  ValidacaoIdentificacaoDTO,
} from '@/domain/retirada'

/**
 * Contrato entre o painel da portaria e o backend.
 *
 * Cada método corresponde a um endpoint do RetiradaController (Spring Boot).
 * Implementações:
 *  - HttpRetiradaService: consome a API Java via REST.
 *  - MockRetiradaService: simulação em memória para demonstração.
 * Os componentes visuais nunca dependem de uma implementação concreta.
 */
export interface RetiradaService {
  readonly modo: 'mock' | 'http'
  /** true quando a implementação envia eventos em tempo real. */
  readonly tempoReal: boolean

  /** GET /api/portaria/sessao */
  obterSessao(): Promise<SessaoPortariaDTO>
  /** GET /api/portaria/retiradas?status=...&busca=... (ordem FIFO) */
  listarFila(filtro: FiltroFila): Promise<RetiradaDTO[]>
  /** GET /api/portaria/retiradas/{id} */
  obterRetirada(id: number): Promise<RetiradaDTO>
  /** GET /api/portaria/resumo */
  obterResumo(): Promise<ResumoPortariaDTO>
  /** GET /api/portaria/movimentacoes?limite=n */
  listarHistorico(limite: number): Promise<MovimentacaoDTO[]>

  /** POST /api/portaria/retiradas/{id}/iniciar-atendimento */
  iniciarAtendimento(id: number, versao: number): Promise<RetiradaDTO>
  /** POST /api/portaria/retiradas/{id}/validar-identificacao — não altera o status */
  validarIdentificacao(id: number, codigo: string): Promise<ValidacaoIdentificacaoDTO>
  /** POST /api/portaria/retiradas/{id}/confirmar-identificacao */
  confirmarIdentificacao(id: number, req: ConfirmarIdentificacaoRequest): Promise<RetiradaDTO>
  /** POST /api/portaria/retiradas/{id}/confirmar-retirada */
  confirmarRetirada(id: number, versao: number): Promise<RetiradaDTO>
  /** POST /api/portaria/retiradas/{id}/rejeitar */
  rejeitar(id: number, req: RejeitarRequest): Promise<RetiradaDTO>

  /** Assina eventos da portaria. Retorna a função para cancelar a assinatura. */
  assinarEventos(callback: (evento: EventoPortaria) => void): () => void
}

/**
 * Recurso exclusivo da demonstração: lista QR Codes fictícios para simular a
 * leitura sem câmera. Não faz parte da API real.
 */
export interface QrCodeDemonstracao {
  codigo: string
  rotulo: string
}

export interface SimuladorDemonstracao {
  listarQrCodes(retiradaId: number): QrCodeDemonstracao[]
  simularNovaSolicitacao(): void
  simularCancelamentoPeloResponsavel(): string | null
  definirConexao(online: boolean): void
  estaOnline(): boolean
}
