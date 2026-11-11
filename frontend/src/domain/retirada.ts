/**
 * Tipos de dados do painel da portaria.
 *
 * Estes tipos espelham os DTOs que a API Java (Spring Boot) deverá expor.
 * Eles NÃO contêm regras de negócio: a máquina de estados definitiva
 * pertence à classe de domínio `Retirada` no backend.
 * Contrato documentado em docs/API-PORTARIA.md.
 */

export const STATUS_RETIRADA = [
  'AGUARDANDO',
  'EM_ATENDIMENTO',
  'IDENTIFICADO',
  'CONCLUIDA',
  'REJEITADA',
  'CANCELADA',
] as const

export type StatusRetirada = (typeof STATUS_RETIRADA)[number]

/** Ações que o backend informa como permitidas para o usuário atual. */
export type AcaoRetirada =
  | 'INICIAR_ATENDIMENTO'
  | 'VALIDAR_IDENTIFICACAO'
  | 'CONFIRMAR_IDENTIFICACAO'
  | 'REJEITAR'
  | 'CONFIRMAR_RETIRADA'

export type MetodoIdentificacao = 'QR_CODE' | 'DOCUMENTO'

export type TipoAutorizacao = 'RESPONSAVEL_LEGAL' | 'AUTORIZADO_PERMANENTE' | 'AUTORIZADO_TEMPORARIO'

/** Espelha o enum MotivoExcecaoRetirada do backend (fonte da verdade). */
export type MotivoRejeicao =
  | 'DOCUMENTACAO_INCOMPLETA'
  | 'IDENTIDADE_NAO_CONFERE'
  | 'RESPONSAVEL_INDISPONIVEL'
  | 'AUTORIZACAO_EXPIRADA'
  | 'OUTRO'

export interface EscolaResumoDTO {
  id: number
  nome: string
}

export interface FuncionarioResumoDTO {
  id: number
  nome: string
  funcao: string
}

export interface SessaoPortariaDTO {
  escola: EscolaResumoDTO
  funcionario: FuncionarioResumoDTO
  posto: string
}

export interface AlunoResumoDTO {
  id: number
  nome: string
  turma: string
}

export interface ResponsavelSolicitanteDTO {
  id: number
  nome: string
  /** Ex.: "Mãe", "Avô", "Motorista escolar". */
  /** Null quando não há vínculo cadastrado. */
  vinculo: string | null
  /** Null quando não há autorização de retirada cadastrada. */
  tipoAutorizacao: TipoAutorizacao | null
  /** Data/hora ISO-8601 de fim da autorização; null quando não expira. */
  autorizacaoValidaAte: string | null
  /** Referência mascarada do documento cadastrado, para conferência visual. */
  documentoReferencia: string
}

export interface AlertaConferenciaDTO {
  nivel: 'info' | 'atencao' | 'critico'
  mensagem: string
}

export interface RetiradaDTO {
  id: number
  /** Protocolo legível exibido ao funcionário e ao responsável. */
  protocolo: string
  aluno: AlunoResumoDTO
  responsavel: ResponsavelSolicitanteDTO
  status: StatusRetirada
  dtSolicitacao: string
  chegadaPrevista: string | null
  dtInicioAtendimento: string | null
  dtIdentificacao: string | null
  dtConclusao: string | null
  atendidoPor: FuncionarioResumoDTO | null
  metodoIdentificacao: MetodoIdentificacao | null
  /** true quando um QR Code válido já foi lido para esta solicitação. */
  qrCodeValidado: boolean
  motivoRejeicao: MotivoRejeicao | null
  justificativa: string | null
  /** Informações calculadas pelo backend para apoiar a conferência. */
  alertas: AlertaConferenciaDTO[]
  /** Ações que o backend permite neste momento. O frontend só exibe estas. */
  acoesPermitidas: AcaoRetirada[]
  /** Controle de concorrência otimista (@Version no JPA). */
  versao: number
}

export interface FiltroFila {
  status: StatusRetirada[]
  busca: string
}

export interface ResumoPortariaDTO {
  aguardando: number
  emAtendimento: number
  identificados: number
  concluidasHoje: number
  rejeitadasHoje: number
  canceladasHoje: number
  /** Horário da solicitação mais antiga ainda aguardando, ou null. */
  aguardandoDesde: string | null
}

export type ResultadoMovimentacao = 'CONCLUIDA' | 'REJEITADA' | 'CANCELADA'

export interface MovimentacaoDTO {
  id: number
  retiradaId: number
  protocolo: string
  alunoNome: string
  turma: string
  responsavelNome: string
  funcionarioNome: string | null
  dataHora: string
  resultado: ResultadoMovimentacao
  justificativa: string | null
}

export type ResultadoValidacaoQr =
  | 'CONFERE'
  | 'CODIGO_INVALIDO'
  | 'CODIGO_EXPIRADO'
  | 'CODIGO_JA_UTILIZADO'
  | 'SOLICITACAO_DIVERGENTE'

export interface ValidacaoIdentificacaoDTO {
  valido: boolean
  resultado: ResultadoValidacaoQr
  mensagem: string
  retirada: RetiradaDTO
}

export interface ConfirmarIdentificacaoRequest {
  metodo: MetodoIdentificacao
  versao: number
}

export interface RejeitarRequest {
  motivo: MotivoRejeicao
  justificativa: string
  versao: number
}

/** Eventos de tempo real (futuro: STOMP /topic/escolas/{id}/portaria). */
export type EventoPortaria =
  | { tipo: 'RETIRADA_CRIADA'; retiradaId: number; alunoNome: string }
  | { tipo: 'RETIRADA_ATUALIZADA'; retiradaId: number }
  | { tipo: 'RETIRADA_CANCELADA'; retiradaId: number; alunoNome: string }

/** Corpo de erro padrão do Spring Boot 3 (RFC 7807 ProblemDetail). */
export interface ProblemDetail {
  type?: string
  title: string
  status: number
  detail?: string
  instance?: string
  /** Código de negócio estável, ex.: TRANSICAO_INVALIDA. */
  codigo?: string
}

export const JUSTIFICATIVA_MIN = 10
