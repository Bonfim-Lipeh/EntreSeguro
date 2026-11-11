import { describe, expect, it, beforeEach } from 'vitest'
import { MockRetiradaService } from './MockRetiradaService'
import { ApiError, NetworkError } from '../errors'
import type { RetiradaDTO } from '@/domain/retirada'

const ATIVAS = ['AGUARDANDO', 'EM_ATENDIMENTO', 'IDENTIFICADO'] as const

async function primeira(svc: MockRetiradaService, status: RetiradaDTO['status']) {
  const fila = await svc.listarFila({ status: [status], busca: '' })
  return fila.find((r) => r.alertas.every((a) => a.nivel !== 'critico'))!
}

describe('MockRetiradaService — simulação da API da portaria', () => {
  let svc: MockRetiradaService
  beforeEach(() => {
    svc = new MockRetiradaService({ latencia: [0, 0] })
  })

  it('lista a fila em ordem de chegada (FIFO)', async () => {
    const fila = await svc.listarFila({ status: [...ATIVAS], busca: '' })
    const tempos = fila.map((r) => Date.parse(r.dtSolicitacao))
    expect(tempos).toEqual([...tempos].sort((a, b) => a - b))
    expect(fila.every((r) => ATIVAS.includes(r.status as (typeof ATIVAS)[number]))).toBe(true)
  })

  it('pesquisa por aluno ou responsável ignorando acentos e maiúsculas', async () => {
    const porAluno = await svc.listarFila({ status: [], busca: 'gabriel tavares' })
    expect(porAluno).toHaveLength(1)
    const porResponsavel = await svc.listarFila({ status: [], busca: 'SONIA' })
    expect(porResponsavel[0].aluno.nome).toBe('Gabriel Tavares Nunes')
  })

  it('percorre o fluxo completo AGUARDANDO → EM_ATENDIMENTO → IDENTIFICADO → CONCLUIDA', async () => {
    let r = await primeira(svc, 'AGUARDANDO')
    expect(r.acoesPermitidas).toEqual(['INICIAR_ATENDIMENTO'])

    r = await svc.iniciarAtendimento(r.id, r.versao)
    expect(r.status).toBe('EM_ATENDIMENTO')
    expect(r.acoesPermitidas).toEqual(['VALIDAR_IDENTIFICACAO', 'CONFIRMAR_IDENTIFICACAO', 'REJEITAR'])

    const qr = svc.listarQrCodes(r.id)[0]
    const validacao = await svc.validarIdentificacao(r.id, qr.codigo)
    expect(validacao.valido).toBe(true)
    // QR válido NÃO altera o status: a identificação ainda precisa ser confirmada.
    expect(validacao.retirada.status).toBe('EM_ATENDIMENTO')

    r = await svc.confirmarIdentificacao(r.id, { metodo: 'QR_CODE', versao: validacao.retirada.versao })
    expect(r.status).toBe('IDENTIFICADO')
    expect(r.acoesPermitidas).toEqual(['CONFIRMAR_RETIRADA'])

    const antes = (await svc.obterResumo()).concluidasHoje
    r = await svc.confirmarRetirada(r.id, r.versao)
    expect(r.status).toBe('CONCLUIDA')
    expect(r.acoesPermitidas).toEqual([])
    expect((await svc.obterResumo()).concluidasHoje).toBe(antes + 1)

    const historico = await svc.listarHistorico(5)
    expect(historico[0]).toMatchObject({ retiradaId: r.id, resultado: 'CONCLUIDA', funcionarioNome: 'Marcos Vieira' })
  })

  it('não permite pular etapas nem rejeitar fora de EM_ATENDIMENTO', async () => {
    const aguardando = await primeira(svc, 'AGUARDANDO')
    await expect(svc.confirmarRetirada(aguardando.id, aguardando.versao)).rejects.toMatchObject({ status: 409, codigo: 'TRANSICAO_INVALIDA' })
    await expect(
      svc.rejeitar(aguardando.id, { motivo: 'OUTRO', justificativa: 'Justificativa suficiente', versao: aguardando.versao }),
    ).rejects.toMatchObject({ status: 409 })

    const identificado = await primeira(svc, 'IDENTIFICADO')
    expect(identificado.acoesPermitidas).not.toContain('REJEITAR')
    await expect(
      svc.rejeitar(identificado.id, { motivo: 'OUTRO', justificativa: 'Justificativa suficiente', versao: identificado.versao }),
    ).rejects.toMatchObject({ status: 409, codigo: 'TRANSICAO_INVALIDA' })
  })

  it('exige justificativa na rejeição', async () => {
    const r = await primeira(svc, 'EM_ATENDIMENTO')
    await expect(svc.rejeitar(r.id, { motivo: 'OUTRO', justificativa: '   curta ', versao: r.versao })).rejects.toMatchObject({
      status: 422,
      codigo: 'JUSTIFICATIVA_OBRIGATORIA',
    })
    const rejeitada = await svc.rejeitar(r.id, {
      motivo: 'IDENTIDADE_NAO_CONFERE',
      justificativa: 'Documento apresentado não corresponde ao cadastro.',
      versao: r.versao,
    })
    expect(rejeitada.status).toBe('REJEITADA')
    expect(rejeitada.justificativa).toBe('Documento apresentado não corresponde ao cadastro.')
  })

  it('classifica QR Codes inválidos sem alterar o status', async () => {
    let r = await primeira(svc, 'AGUARDANDO')
    r = await svc.iniciarAtendimento(r.id, r.versao)
    const opcoes = svc.listarQrCodes(r.id)
    const resultados = []
    for (const o of opcoes.slice(1)) resultados.push((await svc.validarIdentificacao(r.id, o.codigo)).resultado)
    expect(resultados).toEqual(['CODIGO_EXPIRADO', 'SOLICITACAO_DIVERGENTE', 'CODIGO_JA_UTILIZADO', 'CODIGO_INVALIDO'])
    expect((await svc.obterRetirada(r.id)).status).toBe('EM_ATENDIMENTO')
  })

  it('bloqueia identificação quando a autorização expirou e permite apenas rejeitar', async () => {
    const [expirada] = await svc.listarFila({ status: ['AGUARDANDO'], busca: 'Manuela' })
    expect(expirada.alertas.some((a) => a.nivel === 'critico')).toBe(true)
    const emAtendimento = await svc.iniciarAtendimento(expirada.id, expirada.versao)
    expect(emAtendimento.acoesPermitidas).not.toContain('CONFIRMAR_IDENTIFICACAO')
    expect(emAtendimento.acoesPermitidas).toContain('REJEITAR')
    await expect(
      svc.confirmarIdentificacao(expirada.id, { metodo: 'DOCUMENTO', versao: emAtendimento.versao }),
    ).rejects.toMatchObject({ status: 409, codigo: 'AUTORIZACAO_EXPIRADA' })
  })

  it('detecta versão desatualizada após cancelamento pelo responsável', async () => {
    const fila = await svc.listarFila({ status: ['AGUARDANDO'], busca: '' })
    const ultima = fila[fila.length - 1]
    svc.simularCancelamentoPeloResponsavel()
    const erro = await svc.iniciarAtendimento(ultima.id, ultima.versao).catch((e) => e)
    expect(erro).toBeInstanceOf(ApiError)
    expect(erro.message).toMatch(/cancelada pelo responsável/)
  })

  it('emite evento e inclui nova solicitação no fim da fila', async () => {
    const eventos: string[] = []
    const cancelar = svc.assinarEventos((e) => eventos.push(e.tipo))
    svc.simularNovaSolicitacao()
    cancelar()
    const fila = await svc.listarFila({ status: ['AGUARDANDO'], busca: '' })
    expect(eventos).toEqual(['RETIRADA_CRIADA'])
    expect(fila[fila.length - 1].aluno.nome).toBe('Valentina Cruz Moura')
  })

  it('simula perda de conexão com NetworkError', async () => {
    svc.definirConexao(false)
    await expect(svc.obterResumo()).rejects.toBeInstanceOf(NetworkError)
    svc.definirConexao(true)
    await expect(svc.obterResumo()).resolves.toBeDefined()
  })
})
