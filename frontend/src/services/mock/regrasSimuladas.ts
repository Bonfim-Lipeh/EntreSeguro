/**
 * ATENÇÃO — REPRODUÇÃO PROVISÓRIA PARA DEMONSTRAÇÃO.
 *
 * A máquina de estados definitiva pertence ao backend Java (classe de domínio
 * `Retirada`). Este arquivo existe apenas para que a simulação se comporte como
 * a API se comportará, e será removido quando o frontend consumir a API real.
 * Nenhum componente visual importa este arquivo.
 *
 * Tabela (CLAUDE.md §3):
 *   AGUARDANDO     --iniciar atendimento-->            EM_ATENDIMENTO
 *   EM_ATENDIMENTO --confirmar identificação-->        IDENTIFICADO
 *   EM_ATENDIMENTO --rejeitar (justificativa)-->       REJEITADA
 *   IDENTIFICADO   --confirmar retirada-->             CONCLUIDA
 *   AGUARDANDO     --cancelar (responsável)-->         CANCELADA
 */
import type { AcaoRetirada, AlertaConferenciaDTO, StatusRetirada } from '@/domain/retirada'
import type { RegistroRetirada } from './seed'

type AcaoComTransicao = Exclude<AcaoRetirada, 'VALIDAR_IDENTIFICACAO'> | 'CANCELAR'

export const TRANSICOES: Record<AcaoComTransicao, { de: StatusRetirada; para: StatusRetirada }> = {
  INICIAR_ATENDIMENTO: { de: 'AGUARDANDO', para: 'EM_ATENDIMENTO' },
  CONFIRMAR_IDENTIFICACAO: { de: 'EM_ATENDIMENTO', para: 'IDENTIFICADO' },
  REJEITAR: { de: 'EM_ATENDIMENTO', para: 'REJEITADA' },
  CONFIRMAR_RETIRADA: { de: 'IDENTIFICADO', para: 'CONCLUIDA' },
  CANCELAR: { de: 'AGUARDANDO', para: 'CANCELADA' },
}

export function autorizacaoVigente(reg: RegistroRetirada, agora: number): boolean {
  const ate = reg.responsavel.autorizacaoValidaAte
  return ate === null || ate > agora
}

export function calcularAcoesPermitidas(reg: RegistroRetirada, agora: number): AcaoRetirada[] {
  const vigente = autorizacaoVigente(reg, agora)
  switch (reg.status) {
    case 'AGUARDANDO':
      return ['INICIAR_ATENDIMENTO']
    case 'EM_ATENDIMENTO': {
      const acoes: AcaoRetirada[] = []
      if (!reg.qrCodeValidado) acoes.push('VALIDAR_IDENTIFICACAO')
      if (vigente) acoes.push('CONFIRMAR_IDENTIFICACAO')
      acoes.push('REJEITAR')
      return acoes
    }
    case 'IDENTIFICADO':
      return vigente ? ['CONFIRMAR_RETIRADA'] : []
    default:
      return []
  }
}

const hora = (ms: number) => new Date(ms).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
const data = (ms: number) => new Date(ms).toLocaleDateString('pt-BR')

export function calcularAlertas(reg: RegistroRetirada, agora: number): AlertaConferenciaDTO[] {
  const alertas: AlertaConferenciaDTO[] = []
  const ate = reg.responsavel.autorizacaoValidaAte
  const final = reg.status === 'CONCLUIDA' || reg.status === 'REJEITADA' || reg.status === 'CANCELADA'

  if (!final && ate !== null && ate <= agora) {
    alertas.push({
      nivel: 'critico',
      mensagem: `Autorização temporária expirou às ${hora(ate)}. A retirada não pode ser concluída por este responsável.`,
    })
  } else if (!final && ate !== null) {
    alertas.push({ nivel: 'atencao', mensagem: `Autorização temporária válida até ${data(ate)} às ${hora(ate)}.` })
  }
  if (!final && reg.primeiraRetirada) {
    alertas.push({ nivel: 'atencao', mensagem: 'Primeira retirada realizada por este responsável. Confira o documento com atenção.' })
  }
  if (!final && reg.responsavel.tipoAutorizacao !== 'RESPONSAVEL_LEGAL' && ate === null) {
    alertas.push({ nivel: 'info', mensagem: 'Pessoa autorizada pelo responsável legal (autorização permanente).' })
  }
  return alertas
}
