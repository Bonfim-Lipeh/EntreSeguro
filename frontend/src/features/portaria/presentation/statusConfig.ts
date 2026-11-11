/**
 * Regras de APRESENTAÇÃO do status (rótulos, cores, ícones e filtros).
 * Não define transições: quem decide o que pode ser feito é o backend,
 * por meio de `acoesPermitidas`.
 */
import { CheckCircle2, CircleDashed, Clock, ScanFace, UserRoundCheck, XCircle, type LucideIcon } from 'lucide-react'
import type { MotivoRejeicao, StatusRetirada, TipoAutorizacao } from '@/domain/retirada'

export interface StatusVisual {
  rotulo: string
  icone: LucideIcon
  badge: string
  ponto: string
}

export const STATUS_VISUAL: Record<StatusRetirada, StatusVisual> = {
  AGUARDANDO: { rotulo: 'Aguardando', icone: Clock, badge: 'bg-amber-50 text-amber-900 border-amber-300', ponto: 'bg-amber-500' },
  EM_ATENDIMENTO: { rotulo: 'Em atendimento', icone: ScanFace, badge: 'bg-sky-50 text-sky-900 border-sky-300', ponto: 'bg-sky-600' },
  IDENTIFICADO: { rotulo: 'Identificado', icone: UserRoundCheck, badge: 'bg-indigo-50 text-indigo-900 border-indigo-300', ponto: 'bg-indigo-600' },
  CONCLUIDA: { rotulo: 'Concluída', icone: CheckCircle2, badge: 'bg-emerald-50 text-emerald-900 border-emerald-300', ponto: 'bg-emerald-600' },
  REJEITADA: { rotulo: 'Rejeitada', icone: XCircle, badge: 'bg-red-50 text-red-900 border-red-300', ponto: 'bg-red-600' },
  CANCELADA: { rotulo: 'Cancelada', icone: CircleDashed, badge: 'bg-slate-100 text-slate-700 border-slate-300', ponto: 'bg-slate-500' },
}

export type FiltroId = 'ATIVAS' | 'AGUARDANDO' | 'EM_ANDAMENTO' | 'CONCLUIDAS' | 'ENCERRADAS_SEM_SAIDA' | 'TODAS'

export const FILTROS: Array<{ id: FiltroId; rotulo: string; status: StatusRetirada[] }> = [
  { id: 'ATIVAS', rotulo: 'Ativas', status: ['AGUARDANDO', 'EM_ATENDIMENTO', 'IDENTIFICADO'] },
  { id: 'AGUARDANDO', rotulo: 'Aguardando', status: ['AGUARDANDO'] },
  { id: 'EM_ANDAMENTO', rotulo: 'Em atendimento', status: ['EM_ATENDIMENTO', 'IDENTIFICADO'] },
  { id: 'CONCLUIDAS', rotulo: 'Concluídas', status: ['CONCLUIDA'] },
  { id: 'ENCERRADAS_SEM_SAIDA', rotulo: 'Rejeitadas e canceladas', status: ['REJEITADA', 'CANCELADA'] },
  { id: 'TODAS', rotulo: 'Todas', status: [] },
]

export const statusDoFiltro = (id: FiltroId) => FILTROS.find((f) => f.id === id)!.status

export const ehStatusAtivo = (s: StatusRetirada) => s === 'AGUARDANDO' || s === 'EM_ATENDIMENTO' || s === 'IDENTIFICADO'

export const TIPO_AUTORIZACAO_ROTULO: Record<TipoAutorizacao, string> = {
  RESPONSAVEL_LEGAL: 'Responsável legal',
  AUTORIZADO_PERMANENTE: 'Autorizado permanente',
  AUTORIZADO_TEMPORARIO: 'Autorização temporária',
}

export const MOTIVO_REJEICAO_ROTULO: Record<MotivoRejeicao, string> = {
  DOCUMENTACAO_INCOMPLETA: 'Documentação incompleta',
  IDENTIDADE_NAO_CONFERE: 'Identidade não confere',
  RESPONSAVEL_INDISPONIVEL: 'Responsável indisponível',
  AUTORIZACAO_EXPIRADA: 'Autorização expirada ou revogada',
  OUTRO: 'Outro motivo',
}

/** Faixas de espera para destacar solicitações pendentes há mais tempo. */
export function nivelEspera(minutos: number): 'normal' | 'atencao' | 'critico' {
  if (minutos >= 10) return 'critico'
  if (minutos >= 5) return 'atencao'
  return 'normal'
}
