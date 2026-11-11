/**
 * Simulação em memória do backend para DEMONSTRAÇÃO da interface.
 * Os dados se perdem ao recarregar a página. Não é um segundo backend:
 * reproduz apenas o comportamento necessário para exercitar a tela.
 */
import type { QrCodeDemonstracao, RetiradaService, SimuladorDemonstracao } from '../contracts/RetiradaService'
import type {
  ConfirmarIdentificacaoRequest,
  EventoPortaria,
  FiltroFila,
  MovimentacaoDTO,
  RejeitarRequest,
  ResultadoValidacaoQr,
  ResumoPortariaDTO,
  RetiradaDTO,
  SessaoPortariaDTO,
  ValidacaoIdentificacaoDTO,
} from '@/domain/retirada'
import { JUSTIFICATIVA_MIN } from '@/domain/retirada'
import { ApiError, NetworkError } from '../errors'
import {
  FUNCIONARIO_ATUAL,
  POOL_NOVAS_SOLICITACOES,
  SESSAO_DEMONSTRACAO,
  criarDadosIniciais,
  criarRegistro,
  type RegistroRetirada,
} from './seed'
import { TRANSICOES, autorizacaoVigente, calcularAcoesPermitidas, calcularAlertas } from './regrasSimuladas'

export interface OpcoesMock {
  /** Atraso artificial de rede, em ms [mín, máx]. */
  latencia?: [number, number]
  /** Intervalo de chegada automática de novas solicitações (ms); 0 desliga. */
  intervaloChegadas?: number
  agora?: () => number
}

const normalizar = (s: string) =>
  s
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()

const iso = (ms: number | null) => (ms === null ? null : new Date(ms).toISOString())

export class MockRetiradaService implements RetiradaService, SimuladorDemonstracao {
  readonly modo = 'mock' as const
  readonly tempoReal = true

  private readonly registros: RegistroRetirada[]
  private readonly movimentacoes: MovimentacaoDTO[] = []
  private readonly ouvintes = new Set<(e: EventoPortaria) => void>()
  private readonly latencia: [number, number]
  private readonly intervaloChegadas: number
  private readonly agora: () => number
  private online = true
  private poolIndex = 0
  private timer: ReturnType<typeof setInterval> | null = null
  private proximaMovimentacaoId = 1

  constructor(opcoes: OpcoesMock = {}) {
    this.latencia = opcoes.latencia ?? [180, 420]
    this.intervaloChegadas = opcoes.intervaloChegadas ?? 0
    this.agora = opcoes.agora ?? Date.now
    this.registros = criarDadosIniciais(this.agora())
    this.registros
      .filter((r) => r.status === 'CONCLUIDA' || r.status === 'REJEITADA' || r.status === 'CANCELADA')
      .sort((a, b) => (a.dtConclusao ?? 0) - (b.dtConclusao ?? 0))
      .forEach((r) => this.registrarMovimentacao(r))
  }

  // ---------------------------------------------------------------- consultas

  obterSessao(): Promise<SessaoPortariaDTO> {
    return this.executar(() => SESSAO_DEMONSTRACAO)
  }

  listarFila(filtro: FiltroFila): Promise<RetiradaDTO[]> {
    return this.executar(() => {
      const termo = normalizar(filtro.busca.trim())
      return this.registros
        .filter((r) => filtro.status.length === 0 || filtro.status.includes(r.status))
        .filter(
          (r) =>
            !termo ||
            normalizar(r.aluno.nome).includes(termo) ||
            normalizar(r.responsavel.nome).includes(termo) ||
            normalizar(r.protocolo).includes(termo),
        )
        .sort((a, b) => a.dtSolicitacao - b.dtSolicitacao)
        .map((r) => this.paraDTO(r))
    })
  }

  obterRetirada(id: number): Promise<RetiradaDTO> {
    return this.executar(() => this.paraDTO(this.buscar(id)))
  }

  obterResumo(): Promise<ResumoPortariaDTO> {
    return this.executar(() => {
      const conta = (s: RegistroRetirada['status']) => this.registros.filter((r) => r.status === s).length
      const aguardando = this.registros.filter((r) => r.status === 'AGUARDANDO')
      const maisAntiga = aguardando.length ? Math.min(...aguardando.map((r) => r.dtSolicitacao)) : null
      return {
        aguardando: aguardando.length,
        emAtendimento: conta('EM_ATENDIMENTO'),
        identificados: conta('IDENTIFICADO'),
        concluidasHoje: conta('CONCLUIDA'),
        rejeitadasHoje: conta('REJEITADA'),
        canceladasHoje: conta('CANCELADA'),
        aguardandoDesde: iso(maisAntiga),
      }
    })
  }

  listarHistorico(limite: number): Promise<MovimentacaoDTO[]> {
    return this.executar(() => [...this.movimentacoes].reverse().slice(0, limite))
  }

  // ------------------------------------------------------------------- ações

  iniciarAtendimento(id: number, versao: number): Promise<RetiradaDTO> {
    return this.executar(() => {
      const reg = this.transicionar(id, versao, 'INICIAR_ATENDIMENTO')
      reg.dtInicioAtendimento = this.agora()
      reg.atendidoPor = FUNCIONARIO_ATUAL
      return this.paraDTO(reg)
    })
  }

  validarIdentificacao(id: number, codigo: string): Promise<ValidacaoIdentificacaoDTO> {
    return this.executar(() => {
      const reg = this.buscar(id)
      if (reg.status !== 'EM_ATENDIMENTO') {
        throw this.conflito('TRANSICAO_INVALIDA', 'A identificação só pode ser validada durante o atendimento.')
      }
      const resultado = this.avaliarCodigo(reg, codigo.trim())
      if (resultado === 'CONFERE') {
        reg.qrCodeValidado = true
        reg.qrUtilizado = true
        reg.versao++
      }
      return {
        valido: resultado === 'CONFERE',
        resultado,
        mensagem: MENSAGENS_QR[resultado],
        retirada: this.paraDTO(reg),
      }
    })
  }

  confirmarIdentificacao(id: number, req: ConfirmarIdentificacaoRequest): Promise<RetiradaDTO> {
    return this.executar(() => {
      const atual = this.buscar(id)
      if (req.metodo === 'QR_CODE' && !atual.qrCodeValidado) {
        throw new ApiError({ title: 'Validação', status: 422, codigo: 'QR_NAO_VALIDADO', detail: 'Nenhum QR Code válido foi lido para esta solicitação.' })
      }
      this.exigirAutorizacaoVigente(atual)
      const reg = this.transicionar(id, req.versao, 'CONFIRMAR_IDENTIFICACAO')
      reg.dtIdentificacao = this.agora()
      reg.metodoIdentificacao = req.metodo
      return this.paraDTO(reg)
    })
  }

  confirmarRetirada(id: number, versao: number): Promise<RetiradaDTO> {
    return this.executar(() => {
      // Revalidação da autorização no momento da retirada (regra do backend).
      this.exigirAutorizacaoVigente(this.buscar(id))
      const reg = this.transicionar(id, versao, 'CONFIRMAR_RETIRADA')
      reg.dtConclusao = this.agora()
      this.registrarMovimentacao(reg)
      return this.paraDTO(reg)
    })
  }

  rejeitar(id: number, req: RejeitarRequest): Promise<RetiradaDTO> {
    return this.executar(() => {
      const justificativa = req.justificativa.trim()
      if (justificativa.length < JUSTIFICATIVA_MIN) {
        throw new ApiError({
          title: 'Validação',
          status: 422,
          codigo: 'JUSTIFICATIVA_OBRIGATORIA',
          detail: `A justificativa é obrigatória e deve ter ao menos ${JUSTIFICATIVA_MIN} caracteres.`,
        })
      }
      const reg = this.transicionar(id, req.versao, 'REJEITAR')
      reg.dtConclusao = this.agora()
      reg.motivoRejeicao = req.motivo
      reg.justificativa = justificativa
      reg.atendidoPor = FUNCIONARIO_ATUAL
      this.registrarMovimentacao(reg)
      return this.paraDTO(reg)
    })
  }

  // ------------------------------------------------------------ tempo real

  assinarEventos(callback: (evento: EventoPortaria) => void): () => void {
    this.ouvintes.add(callback)
    if (this.intervaloChegadas > 0 && !this.timer) {
      this.timer = setInterval(() => {
        if (this.online) this.simularNovaSolicitacao()
      }, this.intervaloChegadas)
    }
    return () => {
      this.ouvintes.delete(callback)
      if (this.ouvintes.size === 0 && this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    }
  }

  // ------------------------------------------- controles da demonstração

  listarQrCodes(retiradaId: number): QrCodeDemonstracao[] {
    const reg = this.registros.find((r) => r.id === retiradaId)
    if (!reg) return []
    const outra = this.registros.find((r) => r.id !== retiradaId && r.status === 'AGUARDANDO')
    const usada = this.registros.find((r) => r.status === 'CONCLUIDA' && r.qrUtilizado)
    const opcoes: QrCodeDemonstracao[] = [
      { codigo: reg.qrCodigo, rotulo: `Celular de ${reg.responsavel.nome} (QR atual)` },
      { codigo: reg.qrCodigoExpirado, rotulo: `QR antigo de ${primeiroNome(reg.responsavel.nome)} (expirado)` },
    ]
    if (outra) opcoes.push({ codigo: outra.qrCodigo, rotulo: `QR de outra solicitação (${outra.protocolo})` })
    if (usada) opcoes.push({ codigo: usada.qrCodigo, rotulo: `QR já utilizado (${usada.protocolo})` })
    opcoes.push({ codigo: 'ES1.codigo-nao-reconhecido', rotulo: 'Código ilegível ou não reconhecido' })
    return opcoes
  }

  simularNovaSolicitacao(): void {
    const disponiveis = POOL_NOVAS_SOLICITACOES.filter(
      (p) => !this.registros.some((r) => r.aluno.nome === p.aluno[0] && !this.ehFinal(r)),
    )
    if (!disponiveis.length) return
    const semente = disponiveis[this.poolIndex++ % disponiveis.length]
    const reg = criarRegistro({ ...semente, status: 'AGUARDANDO', solicitadoHaMin: 0, chegadaEmMin: 7 }, this.agora())
    this.registros.push(reg)
    this.emitir({ tipo: 'RETIRADA_CRIADA', retiradaId: reg.id, alunoNome: reg.aluno.nome })
  }

  simularCancelamentoPeloResponsavel(): string | null {
    const candidatas = this.registros.filter((r) => r.status === 'AGUARDANDO')
    const reg = candidatas[candidatas.length - 1]
    if (!reg) return null
    reg.status = TRANSICOES.CANCELAR.para
    reg.dtConclusao = this.agora()
    reg.versao++
    this.registrarMovimentacao(reg)
    this.emitir({ tipo: 'RETIRADA_CANCELADA', retiradaId: reg.id, alunoNome: reg.aluno.nome })
    return reg.aluno.nome
  }

  definirConexao(online: boolean): void {
    this.online = online
  }

  estaOnline(): boolean {
    return this.online
  }

  // ---------------------------------------------------------------- internos

  private async executar<T>(fn: () => T): Promise<T> {
    const [min, max] = this.latencia
    if (max > 0) await new Promise((r) => setTimeout(r, min + Math.random() * (max - min)))
    if (!this.online) throw new NetworkError()
    return structuredClone(fn())
  }

  private buscar(id: number): RegistroRetirada {
    const reg = this.registros.find((r) => r.id === id)
    if (!reg) {
      throw new ApiError({ title: 'Não encontrada', status: 404, codigo: 'RETIRADA_NAO_ENCONTRADA', detail: 'Solicitação não encontrada.' })
    }
    return reg
  }

  private transicionar(id: number, versao: number, acao: keyof typeof TRANSICOES): RegistroRetirada {
    const reg = this.buscar(id)
    if (reg.versao !== versao) {
      throw this.conflito(
        'VERSAO_DESATUALIZADA',
        reg.status === 'CANCELADA'
          ? 'Esta solicitação foi cancelada pelo responsável. A fila foi atualizada.'
          : 'A solicitação foi alterada por outra operação. Os dados foram atualizados; revise antes de continuar.',
      )
    }
    const t = TRANSICOES[acao]
    if (reg.status !== t.de) {
      throw this.conflito('TRANSICAO_INVALIDA', `Ação não permitida para uma solicitação com status ${reg.status}.`)
    }
    reg.status = t.para
    reg.versao++
    return reg
  }

  private exigirAutorizacaoVigente(reg: RegistroRetirada) {
    if (!autorizacaoVigente(reg, this.agora())) {
      throw this.conflito(
        'AUTORIZACAO_EXPIRADA',
        'A autorização deste responsável expirou. A retirada não pode prosseguir; registre a rejeição com justificativa.',
      )
    }
  }

  private avaliarCodigo(reg: RegistroRetirada, codigo: string): ResultadoValidacaoQr {
    if (codigo === reg.qrCodigoExpirado) return 'CODIGO_EXPIRADO'
    const dono = this.registros.find((r) => r.qrCodigo === codigo)
    if (!dono) return 'CODIGO_INVALIDO'
    if (dono.qrUtilizado) return 'CODIGO_JA_UTILIZADO'
    if (dono.id !== reg.id) return 'SOLICITACAO_DIVERGENTE'
    return 'CONFERE'
  }

  private conflito(codigo: string, detail: string) {
    return new ApiError({ title: 'Conflito', status: 409, codigo, detail })
  }

  private ehFinal(r: RegistroRetirada) {
    return r.status === 'CONCLUIDA' || r.status === 'REJEITADA' || r.status === 'CANCELADA'
  }

  private registrarMovimentacao(reg: RegistroRetirada) {
    this.movimentacoes.push({
      id: this.proximaMovimentacaoId++,
      retiradaId: reg.id,
      protocolo: reg.protocolo,
      alunoNome: reg.aluno.nome,
      turma: reg.aluno.turma,
      responsavelNome: reg.responsavel.nome,
      funcionarioNome: reg.status === 'CANCELADA' ? null : (reg.atendidoPor?.nome ?? null),
      dataHora: new Date(reg.dtConclusao ?? this.agora()).toISOString(),
      resultado: reg.status as MovimentacaoDTO['resultado'],
      justificativa: reg.justificativa,
    })
  }

  private emitir(evento: EventoPortaria) {
    this.ouvintes.forEach((cb) => cb(evento))
  }

  private paraDTO(reg: RegistroRetirada): RetiradaDTO {
    const agora = this.agora()
    return {
      id: reg.id,
      protocolo: reg.protocolo,
      aluno: { ...reg.aluno },
      responsavel: { ...reg.responsavel, autorizacaoValidaAte: iso(reg.responsavel.autorizacaoValidaAte) },
      status: reg.status,
      dtSolicitacao: iso(reg.dtSolicitacao)!,
      chegadaPrevista: iso(reg.chegadaPrevista),
      dtInicioAtendimento: iso(reg.dtInicioAtendimento),
      dtIdentificacao: iso(reg.dtIdentificacao),
      dtConclusao: iso(reg.dtConclusao),
      atendidoPor: reg.atendidoPor ? { ...reg.atendidoPor } : null,
      metodoIdentificacao: reg.metodoIdentificacao,
      qrCodeValidado: reg.qrCodeValidado,
      motivoRejeicao: reg.motivoRejeicao,
      justificativa: reg.justificativa,
      alertas: calcularAlertas(reg, agora),
      acoesPermitidas: calcularAcoesPermitidas(reg, agora),
      versao: reg.versao,
    }
  }
}

const primeiroNome = (nome: string) => nome.split(' ')[0]

const MENSAGENS_QR: Record<ResultadoValidacaoQr, string> = {
  CONFERE: 'QR Code válido e vinculado a esta solicitação. Confira o documento e confirme a identificação.',
  CODIGO_INVALIDO: 'Código não reconhecido. Solicite um novo QR Code ou confira o documento.',
  CODIGO_EXPIRADO: 'QR Code expirado. Peça ao responsável para gerar um novo código no aplicativo.',
  CODIGO_JA_UTILIZADO: 'Este QR Code já foi utilizado em outra retirada e não pode ser reutilizado.',
  SOLICITACAO_DIVERGENTE: 'O QR Code pertence a outra solicitação. Não libere o aluno com este código.',
}

