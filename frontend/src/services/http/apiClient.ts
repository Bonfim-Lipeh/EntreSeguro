import { NetworkError } from '../errors'
import { problemDe, type SessaoAuth } from './sessaoAuth'

/**
 * Cliente HTTP mínimo. Centraliza URL base, cabeçalhos, JWT e conversão de
 * erros ProblemDetail em ApiError.
 *
 * Com `sessao`: envia `Authorization: Bearer`, repete o token CSRF em POSTs e,
 * ao receber 401, tenta renovar o access token uma vez antes de encerrar a sessão.
 */
export function criarApiClient(baseUrl: string, sessao?: SessaoAuth) {
  async function enviar(method: 'GET' | 'POST', path: string, body?: unknown): Promise<Response> {
    const token = sessao?.token()
    const csrf = method === 'POST' && sessao ? await sessao.csrf() : null
    try {
      return await fetch(`${baseUrl}${path}`, {
        method,
        credentials: sessao ? 'include' : 'same-origin',
        headers: {
          Accept: 'application/json, application/problem+json',
          ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
          ...(csrf ? { 'X-XSRF-TOKEN': csrf } : {}),
        },
        body: body !== undefined ? JSON.stringify(body) : undefined,
      })
    } catch {
      throw new NetworkError()
    }
  }

  async function request<T>(method: 'GET' | 'POST', path: string, body?: unknown): Promise<T> {
    let resposta = await enviar(method, path, body)

    if (resposta.status === 401 && sessao) {
      if (await sessao.renovar()) {
        resposta = await enviar(method, path, body)
      }
      if (resposta.status === 401) sessao.expirar()
    }

    if (!resposta.ok) throw await problemDe(resposta)

    if (resposta.status === 204) return undefined as T
    return (await resposta.json()) as T
  }

  return {
    get: <T>(path: string) => request<T>('GET', path),
    post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  }
}
