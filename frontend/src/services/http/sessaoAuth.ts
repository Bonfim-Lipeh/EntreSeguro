import type { ProblemDetail } from '@/domain/retirada'
import { ApiError, NetworkError } from '../errors'

interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
}

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

function lerCookie(nome: string): string | null {
  const par = document.cookie.split('; ').find((c) => c.startsWith(`${nome}=`))
  return par ? decodeURIComponent(par.slice(nome.length + 1)) : null
}

/** Mensagens para respostas de segurança que chegam sem `detail` (filtros do Spring Security). */
const DETALHE_PADRAO: Record<number, string> = {
  401: 'Sessão não autenticada. Entre novamente.',
  403: 'Seu perfil não tem permissão para acessar o painel da portaria.',
}

export async function problemDe(resposta: Response): Promise<ApiError> {
  let problem: ProblemDetail = { title: resposta.statusText || 'Erro', status: resposta.status }
  try {
    problem = { ...problem, ...((await resposta.json()) as Partial<ProblemDetail>) }
  } catch {
    /* corpo não-JSON: mantém o título padrão */
  }
  problem.detail ??= DETALHE_PADRAO[resposta.status]
  return new ApiError(problem)
}

/**
 * Estado da sessão do cliente HTTP.
 * - Access token somente em memória (não usa localStorage).
 * - Refresh token em cookie HttpOnly enviado com `credentials: 'include'`.
 * - CSRF: o backend emite o cookie XSRF-TOKEN; o valor é repetido no cabeçalho X-XSRF-TOKEN.
 * - Renovações simultâneas compartilham uma única requisição, pois o backend
 *   rotaciona o refresh token e recusa o reuso do anterior.
 */
export function criarSessaoAuth(baseUrl: string) {
  let accessToken: string | null = null
  let renovacao: Promise<boolean> | null = null
  const ouvintes = new Set<() => void>()

  async function csrf(): Promise<string> {
    const atual = lerCookie(CSRF_COOKIE)
    if (atual) return atual
    // Qualquer resposta do backend emite o cookie; o health check é público.
    try {
      await fetch(`${baseUrl}/actuator/health`, { credentials: 'include' })
    } catch {
      throw new NetworkError()
    }
    const emitido = lerCookie(CSRF_COOKIE)
    if (!emitido) {
      throw new ApiError({
        title: 'Proteção CSRF indisponível',
        status: 0,
        detail: 'O navegador não recebeu o cookie de segurança. Acesse o painel por http://localhost:5173.',
      })
    }
    return emitido
  }

  async function postAuth(path: string, body?: unknown): Promise<Response> {
    const token = await csrf()
    try {
      return await fetch(`${baseUrl}${path}`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          Accept: 'application/json, application/problem+json',
          [CSRF_HEADER]: token,
          ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        },
        body: body !== undefined ? JSON.stringify(body) : undefined,
      })
    } catch {
      throw new NetworkError()
    }
  }

  return {
    token: () => accessToken,
    csrf,

    async entrar(email: string, senha: string) {
      const resposta = await postAuth('/api/auth/login', { email, password: senha })
      if (!resposta.ok) throw await problemDe(resposta)
      accessToken = ((await resposta.json()) as AuthResponse).accessToken
    },

    /** true = novo access token obtido; false = sessão recusada pelo backend. Falha de rede lança NetworkError. */
    renovar(): Promise<boolean> {
      renovacao ??= (async () => {
        try {
          const resposta = await postAuth('/api/auth/refresh')
          if (!resposta.ok) {
            accessToken = null
            return false
          }
          accessToken = ((await resposta.json()) as AuthResponse).accessToken
          return true
        } finally {
          renovacao = null
        }
      })()
      return renovacao
    },

    async sair() {
      accessToken = null
      try {
        await postAuth('/api/auth/logout')
      } catch {
        /* sem rede: o token em memória já foi descartado */
      }
    },

    expirar() {
      accessToken = null
      ouvintes.forEach((cb) => cb())
    },

    aoExpirar(cb: () => void) {
      ouvintes.add(cb)
      return () => void ouvintes.delete(cb)
    },
  }
}

export type SessaoAuth = ReturnType<typeof criarSessaoAuth>
