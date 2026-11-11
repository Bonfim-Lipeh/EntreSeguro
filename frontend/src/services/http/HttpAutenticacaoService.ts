import type { AutenticacaoService } from '../contracts/AutenticacaoService'
import type { SessaoAuth } from './sessaoAuth'

export class HttpAutenticacaoService implements AutenticacaoService {
  private readonly sessao: SessaoAuth

  constructor(sessao: SessaoAuth) {
    this.sessao = sessao
  }

  entrar(email: string, senha: string) {
    return this.sessao.entrar(email, senha)
  }

  restaurarSessao() {
    return this.sessao.renovar()
  }

  sair() {
    return this.sessao.sair()
  }

  aoExpirarSessao(callback: () => void) {
    return this.sessao.aoExpirar(callback)
  }
}
