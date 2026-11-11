import type { ProblemDetail } from '@/domain/retirada'

/** Erro devolvido pela API (ou pela simulação) no formato ProblemDetail. */
export class ApiError extends Error {
  readonly status: number
  readonly codigo: string | undefined
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title)
    this.name = 'ApiError'
    this.status = problem.status
    this.codigo = problem.codigo
    this.problem = problem
  }
}

/** Falha de rede: servidor inacessível ou conexão perdida. */
export class NetworkError extends Error {
  constructor(message = 'Sem conexão com o servidor. Verifique a rede e tente novamente.') {
    super(message)
    this.name = 'NetworkError'
  }
}

export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof ApiError || erro instanceof NetworkError) return erro.message
  return 'Ocorreu um erro inesperado. Tente novamente.'
}
