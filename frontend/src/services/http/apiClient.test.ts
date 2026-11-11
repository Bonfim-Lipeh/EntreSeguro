import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { criarApiClient } from './apiClient'
import { criarSessaoAuth } from './sessaoAuth'
import { ApiError } from '../errors'

const BASE = 'http://api.test'
const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

describe('cliente HTTP autenticado (JWT + refresh + CSRF)', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=csrf-123; path=/'
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
  })

  it('login envia CSRF e cookies; consultas seguintes levam o Bearer', async () => {
    fetchMock.mockResolvedValueOnce(json(200, { accessToken: 'jwt-1' })).mockResolvedValueOnce(json(200, { ok: true }))
    const sessao = criarSessaoAuth(BASE)
    await sessao.entrar('a@b.c', 'x')
    await criarApiClient(BASE, sessao).get('/api/portaria/sessao')

    const [urlLogin, optLogin] = fetchMock.mock.calls[0]
    expect(urlLogin).toBe(`${BASE}/api/auth/login`)
    expect(optLogin.credentials).toBe('include')
    expect(optLogin.headers['X-XSRF-TOKEN']).toBe('csrf-123')
    expect(fetchMock.mock.calls[1][1].headers.Authorization).toBe('Bearer jwt-1')
  })

  it('401 dispara UMA renovação compartilhada e repete as requisições com o novo token', async () => {
    const sessao = criarSessaoAuth(BASE)
    let refreshes = 0
    fetchMock.mockImplementation(async (url: string, opt: RequestInit) => {
      if (url.endsWith('/api/auth/refresh')) {
        refreshes++
        return json(200, { accessToken: 'jwt-novo' })
      }
      const auth = (opt.headers as Record<string, string>).Authorization
      return auth === 'Bearer jwt-novo' ? json(200, { ok: true }) : json(401, { error: 'Não autenticado' })
    })
    const api = criarApiClient(BASE, sessao)
    const r = await Promise.all([api.get('/a'), api.get('/b'), api.get('/c')])
    expect(r).toEqual([{ ok: true }, { ok: true }, { ok: true }])
    expect(refreshes).toBe(1)
  })

  it('refresh recusado encerra a sessão e notifica os ouvintes', async () => {
    const sessao = criarSessaoAuth(BASE)
    const expirou = vi.fn()
    sessao.aoExpirar(expirou)
    fetchMock.mockImplementation(async () => json(401, {}))
    await expect(criarApiClient(BASE, sessao).get('/x')).rejects.toMatchObject({ status: 401 })
    expect(expirou).toHaveBeenCalledOnce()
  })

  it('403 sem detail recebe mensagem em português; ProblemDetail do backend é preservado', async () => {
    const api = criarApiClient(BASE, criarSessaoAuth(BASE))
    fetchMock.mockResolvedValueOnce(json(403, { status: 403, error: 'Forbidden' }))
    await expect(api.get('/x')).rejects.toThrow('Seu perfil não tem permissão para acessar o painel da portaria.')
    fetchMock.mockResolvedValueOnce(json(404, { title: 'Not Found', status: 404, detail: 'Solicitação não encontrada.' }))
    const erro = await api.get('/y').catch((e: unknown) => e)
    expect(erro).toBeInstanceOf(ApiError)
    expect((erro as ApiError).message).toBe('Solicitação não encontrada.')
  })
})
