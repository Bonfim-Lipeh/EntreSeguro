/**
 * Dados 100% FICTÍCIOS para demonstração. Nenhum nome corresponde a alunos
 * ou responsáveis reais. Horários são relativos ao momento em que a
 * demonstração é iniciada.
 */
import type {
  FuncionarioResumoDTO,
  MetodoIdentificacao,
  MotivoRejeicao,
  SessaoPortariaDTO,
  StatusRetirada,
  TipoAutorizacao,
} from '@/domain/retirada'

export interface RegistroRetirada {
  id: number
  protocolo: string
  aluno: { id: number; nome: string; turma: string }
  responsavel: {
    id: number
    nome: string
    vinculo: string
    tipoAutorizacao: TipoAutorizacao
    autorizacaoValidaAte: number | null
    documentoReferencia: string
  }
  status: StatusRetirada
  dtSolicitacao: number
  chegadaPrevista: number | null
  dtInicioAtendimento: number | null
  dtIdentificacao: number | null
  dtConclusao: number | null
  atendidoPor: FuncionarioResumoDTO | null
  metodoIdentificacao: MetodoIdentificacao | null
  qrCodeValidado: boolean
  motivoRejeicao: MotivoRejeicao | null
  justificativa: string | null
  primeiraRetirada: boolean
  versao: number
  /** Código opaco atual do QR (nunca contém dados pessoais). */
  qrCodigo: string
  /** Código anterior, já expirado — usado para demonstrar a expiração. */
  qrCodigoExpirado: string
  qrUtilizado: boolean
}

export const FUNCIONARIO_ATUAL: FuncionarioResumoDTO = { id: 1, nome: 'Marcos Vieira', funcao: 'Portaria' }
const FUNCIONARIA_ALINE: FuncionarioResumoDTO = { id: 2, nome: 'Aline Ferreira', funcao: 'Portaria' }

export const SESSAO_DEMONSTRACAO: SessaoPortariaDTO = {
  escola: { id: 1, nome: 'Colégio Horizonte' },
  funcionario: FUNCIONARIO_ATUAL,
  posto: 'Portão principal',
}

const MIN = 60_000

export function gerarCodigoQr(): string {
  const bytes = new Uint8Array(12)
  crypto.getRandomValues(bytes)
  return 'ES1.' + Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
}

interface Semente {
  aluno: [string, string]
  responsavel: [string, string, TipoAutorizacao, string]
  status: StatusRetirada
  solicitadoHaMin: number
  chegadaEmMin?: number
  atendimentoHaMin?: number
  identificadoHaMin?: number
  concluidoHaMin?: number
  por?: FuncionarioResumoDTO
  metodo?: MetodoIdentificacao
  rejeicao?: [MotivoRejeicao, string]
  autorizacaoExpiraHaMin?: number
  primeiraRetirada?: boolean
}

const SEMENTES: Semente[] = [
  // Finalizadas no dia
  { aluno: ['Laura Fontes Ribeiro', '3º Ano A'], responsavel: ['Marina Fontes Ribeiro', 'Mãe', 'RESPONSAVEL_LEGAL', 'RG final 31-4'], status: 'CONCLUIDA', solicitadoHaMin: 52, atendimentoHaMin: 47, identificadoHaMin: 46, concluidoHaMin: 45, por: FUNCIONARIO_ATUAL, metodo: 'QR_CODE' },
  { aluno: ['Thiago Monteiro Reis', '1º Ano B'], responsavel: ['Paulo Monteiro Reis', 'Pai', 'RESPONSAVEL_LEGAL', 'CNH final 0-9'], status: 'CONCLUIDA', solicitadoHaMin: 44, atendimentoHaMin: 40, identificadoHaMin: 39, concluidoHaMin: 38, por: FUNCIONARIA_ALINE, metodo: 'DOCUMENTO' },
  { aluno: ['Miguel Santana Vaz', '4º Ano B'], responsavel: ['Rogério Santana Vaz', 'Pai', 'RESPONSAVEL_LEGAL', 'RG final 77-2'], status: 'CANCELADA', solicitadoHaMin: 41, concluidoHaMin: 35 },
  { aluno: ['Alice Guimarães Prado', '2º Ano A'], responsavel: ['Renata Guimarães Prado', 'Mãe', 'RESPONSAVEL_LEGAL', 'RG final 12-0'], status: 'CONCLUIDA', solicitadoHaMin: 37, atendimentoHaMin: 33, identificadoHaMin: 32, concluidoHaMin: 31, por: FUNCIONARIO_ATUAL, metodo: 'QR_CODE' },
  { aluno: ['Isabela Nogueira Pires', '5º Ano A'], responsavel: ['Vanessa Nogueira Pires', 'Mãe', 'RESPONSAVEL_LEGAL', 'RG final 58-6'], status: 'REJEITADA', solicitadoHaMin: 34, atendimentoHaMin: 30, concluidoHaMin: 28, por: FUNCIONARIO_ATUAL, rejeicao: ['IDENTIDADE_NAO_CONFERE', 'Pessoa presente apresentou documento de terceiro não cadastrado. Responsável legal contatado por telefone.'] },
  { aluno: ['Enzo Carvalho Brito', '3º Ano B'], responsavel: ['Sérgio Carvalho Brito', 'Avô', 'AUTORIZADO_PERMANENTE', 'RG final 40-1'], status: 'CONCLUIDA', solicitadoHaMin: 29, atendimentoHaMin: 26, identificadoHaMin: 25, concluidoHaMin: 24, por: FUNCIONARIA_ALINE, metodo: 'QR_CODE' },
  // Em andamento
  { aluno: ['Sofia Mattos Azevedo', '2º Ano C'], responsavel: ['André Mattos Azevedo', 'Pai', 'RESPONSAVEL_LEGAL', 'CNH final 3-3'], status: 'IDENTIFICADO', solicitadoHaMin: 20, atendimentoHaMin: 4, identificadoHaMin: 2, por: FUNCIONARIO_ATUAL, metodo: 'QR_CODE' },
  { aluno: ['Rafael Coutinho Silva', '5º Ano B'], responsavel: ['Fernanda Coutinho Silva', 'Mãe', 'RESPONSAVEL_LEGAL', 'RG final 26-8'], status: 'EM_ATENDIMENTO', solicitadoHaMin: 16, atendimentoHaMin: 3, por: FUNCIONARIO_ATUAL },
  // Aguardando (FIFO)
  { aluno: ['Lucas Andrade Moreira', '3º Ano A'], responsavel: ['Patrícia Andrade Moreira', 'Mãe', 'RESPONSAVEL_LEGAL', 'RG final 90-5'], status: 'AGUARDANDO', solicitadoHaMin: 14, chegadaEmMin: -2 },
  { aluno: ['Beatriz Sampaio Leal', '1º Ano A'], responsavel: ['Ricardo Sampaio Leal', 'Pai', 'RESPONSAVEL_LEGAL', 'CNH final 6-1'], status: 'AGUARDANDO', solicitadoHaMin: 11, chegadaEmMin: 1 },
  { aluno: ['Gabriel Tavares Nunes', '2º Ano B'], responsavel: ['Sônia Tavares Nunes', 'Avó', 'AUTORIZADO_PERMANENTE', 'RG final 03-7'], status: 'AGUARDANDO', solicitadoHaMin: 9, chegadaEmMin: 3 },
  { aluno: ['Manuela Prado Costa', '4º Ano A'], responsavel: ['Eduardo Ramos Prado', 'Tio', 'AUTORIZADO_TEMPORARIO', 'RG final 81-2'], status: 'AGUARDANDO', solicitadoHaMin: 8, chegadaEmMin: 2, autorizacaoExpiraHaMin: 3 },
  { aluno: ['Davi Rezende Lopes', '1º Ano B'], responsavel: ['Juliana Rezende Lopes', 'Mãe', 'RESPONSAVEL_LEGAL', 'RG final 47-3'], status: 'AGUARDANDO', solicitadoHaMin: 5, chegadaEmMin: 6 },
  { aluno: ['Helena Barros Dias', '3º Ano B'], responsavel: ['Cláudio Mendes Rocha', 'Motorista escolar', 'AUTORIZADO_PERMANENTE', 'CNH final 2-8'], status: 'AGUARDANDO', solicitadoHaMin: 2, chegadaEmMin: 8, primeiraRetirada: true },
]

/** Alunos e responsáveis usados para simular novas solicitações. */
export const POOL_NOVAS_SOLICITACOES: Array<Pick<Semente, 'aluno' | 'responsavel'>> = [
  { aluno: ['Valentina Cruz Moura', '2º Ano A'], responsavel: ['Daniela Cruz Moura', 'Mãe', 'RESPONSAVEL_LEGAL', 'RG final 65-0'] },
  { aluno: ['Arthur Bastos Freitas', '5º Ano A'], responsavel: ['Leonardo Bastos Freitas', 'Pai', 'RESPONSAVEL_LEGAL', 'CNH final 8-4'] },
  { aluno: ['Lívia Campos Teixeira', '4º Ano B'], responsavel: ['Teresa Campos Teixeira', 'Avó', 'AUTORIZADO_PERMANENTE', 'RG final 19-9'] },
  { aluno: ['Bernardo Pacheco Lima', '1º Ano A'], responsavel: ['Camila Pacheco Lima', 'Mãe', 'RESPONSAVEL_LEGAL', 'RG final 72-6'] },
  { aluno: ['Clara Medeiros Antunes', '3º Ano A'], responsavel: ['Gustavo Medeiros Antunes', 'Pai', 'RESPONSAVEL_LEGAL', 'CNH final 5-5'] },
  { aluno: ['Heitor Siqueira Melo', '2º Ano C'], responsavel: ['Rosana Siqueira Melo', 'Tia', 'AUTORIZADO_TEMPORARIO', 'RG final 38-1'] },
]

let proximoId = 1
let proximoAlunoId = 100
let proximoResponsavelId = 500

export function criarRegistro(
  semente: Pick<Semente, 'aluno' | 'responsavel'> & Partial<Semente>,
  agora: number,
): RegistroRetirada {
  const id = proximoId++
  const ha = (min?: number) => (min === undefined ? null : agora - min * MIN)
  const tipo = semente.responsavel[2]
  const validaAte =
    semente.autorizacaoExpiraHaMin !== undefined
      ? agora - semente.autorizacaoExpiraHaMin * MIN
      : tipo === 'AUTORIZADO_TEMPORARIO'
        ? agora + 3 * 24 * 60 * MIN
        : null
  const status = semente.status ?? 'AGUARDANDO'
  const final = status === 'CONCLUIDA' || status === 'REJEITADA' || status === 'CANCELADA'

  return {
    id,
    protocolo: `RT-${String(1400 + id).padStart(4, '0')}`,
    aluno: { id: proximoAlunoId++, nome: semente.aluno[0], turma: semente.aluno[1] },
    responsavel: {
      id: proximoResponsavelId++,
      nome: semente.responsavel[0],
      vinculo: semente.responsavel[1],
      tipoAutorizacao: tipo,
      autorizacaoValidaAte: validaAte,
      documentoReferencia: semente.responsavel[3],
    },
    status,
    dtSolicitacao: agora - (semente.solicitadoHaMin ?? 0) * MIN,
    chegadaPrevista: semente.chegadaEmMin === undefined ? null : agora + semente.chegadaEmMin * MIN,
    dtInicioAtendimento: ha(semente.atendimentoHaMin),
    dtIdentificacao: ha(semente.identificadoHaMin),
    dtConclusao: ha(semente.concluidoHaMin),
    atendidoPor: semente.por ?? null,
    metodoIdentificacao: semente.metodo ?? null,
    qrCodeValidado: semente.metodo === 'QR_CODE',
    motivoRejeicao: semente.rejeicao?.[0] ?? null,
    justificativa: semente.rejeicao?.[1] ?? null,
    primeiraRetirada: semente.primeiraRetirada ?? false,
    versao: 1,
    qrCodigo: gerarCodigoQr(),
    qrCodigoExpirado: gerarCodigoQr(),
    qrUtilizado: final || semente.metodo === 'QR_CODE',
  }
}

export function criarDadosIniciais(agora: number): RegistroRetirada[] {
  return SEMENTES.map((s) => criarRegistro(s, agora))
}
