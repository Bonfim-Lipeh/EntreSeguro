import { expect, test, type Page, type Request } from '@playwright/test'

/**
 * Integração real painel ↔ API Java ↔ MySQL, com os dados de demonstração do perfil dev
 * (backend/src/main/resources/db/seed-dev). Nenhuma resposta é simulada, exceto no teste
 * de token expirado, que força UM 401 para exercitar a renovação contra o backend real.
 */
const API = 'http://localhost:8080'
const FUNCIONARIO = { email: 'portaria@demo.entradasegura.local', senha: 'Portaria@2026' }

async function entrar(page: Page, email = FUNCIONARIO.email, senha = FUNCIONARIO.senha) {
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('Senha').fill(senha)
  await page.getByRole('button', { name: 'Entrar' }).click()
}

test.beforeEach(async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible()
})

test('credenciais inválidas exibem a mensagem do backend', async ({ page }) => {
  await entrar(page, FUNCIONARIO.email, 'senha-errada')
  await expect(page.getByRole('alert')).toHaveText('Credenciais inválidas.')
})

test('login real carrega sessão, fila, resumo e histórico do backend com JWT', async ({ page }) => {
  const chamadasApi: Request[] = []
  page.on('request', (r) => r.url().startsWith(`${API}/api/portaria/`) && chamadasApi.push(r))

  // O banco muda com as operações reais; a tela é comparada com as respostas do backend.
  const login = page.waitForResponse((r) => r.url() === `${API}/api/auth/login`)
  const respFila = page.waitForResponse((r) => r.url().startsWith(`${API}/api/portaria/retiradas?`) && r.status() === 200)
  const respResumo = page.waitForResponse((r) => r.url() === `${API}/api/portaria/resumo` && r.status() === 200)
  const respHistorico = page.waitForResponse((r) => r.url().startsWith(`${API}/api/portaria/movimentacoes`) && r.status() === 200)
  await entrar(page)
  const respLogin = await login
  expect(respLogin.status()).toBe(200)
  expect((await respLogin.json()).accessToken).toBeTruthy()

  // Sessão (escola e funcionário vindos do MySQL)
  await expect(page.getByText('Colégio Horizonte (demonstração) · Posto principal')).toBeVisible()
  await expect(page.getByRole('banner').getByText('Marina Lopes')).toBeVisible()
  await expect(page.getByText('Modo demonstração', { exact: true })).toHaveCount(0)

  // Fila FIFO das ativas (escola 1) = resposta do backend; a solicitação da escola 2 não aparece
  const fila = (await (await respFila).json()) as Array<{ aluno: { nome: string }; dtSolicitacao: string }>
  const tempos = fila.map((r) => Date.parse(r.dtSolicitacao))
  expect(tempos).toEqual([...tempos].sort((a, b) => a - b))
  const itens = page.getByTestId('fila').locator('li')
  await expect(itens).toHaveCount(fila.length)
  await expect(itens.first()).toContainText(fila[0].aluno.nome)
  await expect(itens.last()).toContainText(fila[fila.length - 1].aluno.nome)
  await expect(page.getByTestId('fila')).not.toContainText('Yuri Okada Lima')

  // Resumo
  const resumo = (await (await respResumo).json()) as { aguardando: number }
  await expect(page.getByTestId('indicador-AGUARDANDO')).toHaveText(String(resumo.aguardando))

  // Histórico: somente encerramentos, com o aluno correto de cada solicitação
  const movs = (await (await respHistorico).json()) as Array<{ alunoNome: string; resultado: string }>
  expect(movs.length).toBeGreaterThan(0)
  for (const m of movs) expect(['CONCLUIDA', 'REJEITADA', 'CANCELADA']).toContain(m.resultado)
  const historico = page.getByTestId('historico').locator('tbody tr')
  await expect(historico).toHaveCount(movs.length)
  await expect(historico.first()).toContainText(movs[0].alunoNome)

  // Todas as chamadas ao painel levaram o JWT
  expect(chamadasApi.length).toBeGreaterThanOrEqual(4)
  for (const r of chamadasApi) expect(r.headers()['authorization']).toMatch(/^Bearer ey/)
})

test('detalhes de uma retirada vêm de GET /api/portaria/retiradas/{id}', async ({ page }) => {
  await entrar(page)
  const detalhe = page.waitForResponse((r) => /\/api\/portaria\/retiradas\/\d+$/.test(r.url()))
  await page.getByTestId('fila').locator('li').first().click()
  const resp = await detalhe
  expect(resp.status()).toBe(200)
  const r = (await resp.json()) as { aluno: { nome: string; turma: string }; responsavel: { nome: string } }
  const painel = page.getByTestId('painel-detalhes')
  await expect(painel).toContainText(r.aluno.nome)
  await expect(painel).toContainText(r.responsavel.nome)
  await expect(painel).toContainText(r.aluno.turma)
})

test('recarregar a página mantém a sessão via cookie de refresh', async ({ page }) => {
  await entrar(page)
  await expect(page.getByRole('banner').getByText('Marina Lopes')).toBeVisible()
  const refresh = page.waitForResponse((r) => r.url() === `${API}/api/auth/refresh`)
  await page.reload()
  expect((await refresh).status()).toBe(200)
  await expect(page.getByTestId('fila').locator('li').first()).toBeVisible()
})

test('token expirado é renovado automaticamente pelo backend', async ({ page }) => {
  await entrar(page)
  await expect(page.getByTestId('fila').locator('li').first()).toBeVisible()

  // Força UM 401 (como o backend responde a um JWT expirado) na próxima consulta do resumo
  let forcado = false
  await page.route(`${API}/api/portaria/resumo`, async (route) => {
    if (forcado) return route.continue()
    forcado = true
    await route.fulfill({ status: 401, contentType: 'application/json', body: '{"error":"Não autenticado"}' })
  })
  const refresh = page.waitForResponse((r) => r.url() === `${API}/api/auth/refresh`)
  await page.getByRole('button', { name: 'Atualizar fila' }).click()
  expect((await refresh).status()).toBe(200)
  await expect(page.getByText('Fila atualizada')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Entrar' })).toHaveCount(0)
})

test('sessão inválida (refresh revogado) volta ao login com aviso', async ({ page, context }) => {
  await entrar(page)
  await expect(page.getByRole('banner').getByText('Marina Lopes')).toBeVisible()
  // Remove o cookie de refresh: o access token "expira" e a renovação é recusada pelo backend
  const cookies = (await context.cookies()).filter((c) => c.name !== 'refresh_token')
  await context.clearCookies()
  await context.addCookies(cookies)
  await page.route(`${API}/api/portaria/resumo`, (route) =>
    route.fulfill({ status: 401, contentType: 'application/json', body: '{"error":"Não autenticado"}' }),
  )
  const refresh = page.waitForResponse((r) => r.url() === `${API}/api/auth/refresh`)
  await page.getByRole('button', { name: 'Atualizar fila' }).click()
  expect((await refresh).status()).toBe(401)
  await expect(page.getByText('Sua sessão expirou. Entre novamente para continuar.')).toBeVisible()
})

test('sair revoga a sessão e o recarregamento não a restaura', async ({ page }) => {
  await entrar(page)
  await expect(page.getByRole('banner').getByText('Marina Lopes')).toBeVisible()
  const logout = page.waitForResponse((r) => r.url() === `${API}/api/auth/logout`)
  await page.getByRole('button', { name: 'Sair' }).click()
  expect((await logout).status()).toBe(200)
  await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible()
  await page.reload()
  await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible()
})

test('perfil GESTOR não acessa o painel da portaria (403)', async ({ page }) => {
  const sessao = page.waitForResponse((r) => r.url() === `${API}/api/portaria/sessao`)
  await entrar(page, 'gestor@demo.entradasegura.local')
  expect((await sessao).status()).toBe(403)
  await expect(page.getByText('Seu perfil não tem permissão para acessar o painel da portaria.').first()).toBeVisible()
})
