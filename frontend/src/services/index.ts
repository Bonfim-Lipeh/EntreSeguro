import type { AutenticacaoService } from './contracts/AutenticacaoService'
import type { RetiradaService, SimuladorDemonstracao } from './contracts/RetiradaService'
import { HttpAutenticacaoService } from './http/HttpAutenticacaoService'
import { HttpRetiradaService } from './http/HttpRetiradaService'
import { criarSessaoAuth } from './http/sessaoAuth'
import { MockRetiradaService } from './mock/MockRetiradaService'

export interface Servicos {
  retiradas: RetiradaService
  /** Presente somente no modo HTTP (API Java com JWT). */
  autenticacao: AutenticacaoService | null
  /** Presente somente no modo demonstração. */
  simulador: SimuladorDemonstracao | null
}

/**
 * Ponto único de escolha da fonte de dados.
 * VITE_API_MODE=http → API Java; qualquer outro valor → simulação.
 */
export function criarServicos(env: ImportMetaEnv = import.meta.env): Servicos {
  if (env.VITE_API_MODE === 'http') {
    const baseUrl = (env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '')
    const sessao = criarSessaoAuth(baseUrl)
    return {
      retiradas: new HttpRetiradaService(baseUrl, sessao),
      autenticacao: new HttpAutenticacaoService(sessao),
      simulador: null,
    }
  }
  const mock = new MockRetiradaService({ intervaloChegadas: 90_000 })
  return { retiradas: mock, autenticacao: null, simulador: mock }
}

export type { AutenticacaoService } from './contracts/AutenticacaoService'
export type { RetiradaService, SimuladorDemonstracao, QrCodeDemonstracao } from './contracts/RetiradaService'
export { ApiError, NetworkError, mensagemDeErro } from './errors'
