import { execSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { expect, test, type Page } from '@playwright/test'

/**
 * Fluxo operacional REAL pelo painel: login → fila → iniciar atendimento → identificar →
 * confirmar retirada → histórico, contra backend Spring Boot + MySQL. Cada teste cria a sua
 * própria solicitação fictícia no banco (scripts/fixture-retirada.sql); nada é simulado.
 */
const API = 'http://localhost:8080'
const SQL_FIXTURE = readFileSync(path.resolve(import.meta.dirname, '../../scripts/fixture-retirada.sql'), 'utf8')

function criarSolicitacao(nome: string, { autorizacaoVencida = false } = {}): number {
  const saida = execSync(
    `docker exec -i ${process.env.MYSQL_CONTAINER ?? 'entradasegura-mysql'} mysql --default-character-set=utf8mb4 -N -uentradasegura -pentradasegura entradasegura`,
    { input: `SET @nome = '${nome}', @vencida = ${autorizacaoVencida ? 1 : 0};\n${SQL_FIXTURE}`, stdio: ['pipe', 'pipe', 'ignore'] },
  )
    .toString()
    .trim()
  return Number(saida.split(/\s+/).pop())
}

const sufixo = () => `${Date.now().toString(36).toUpperCase()}`

async function entrarEAbrir(page: Page, nomeAluno: string) {
  await page.goto('/')
  await page.getByLabel('E-mail').fill('portaria@demo.entradasegura.local')
  await page.getByLabel('Senha').fill('Portaria@2026')
  await page.getByRole('button', { name: 'Entrar' }).click()
  await page.getByLabel('Pesquisar por aluno, responsável ou protocolo').fill(nomeAluno)
  await page.getByTestId('fila').getByText(nomeAluno).click()
  return page.getByTestId('painel-detalhes')
}

const posted = (page: Page, acao: string) =>
  page.waitForResponse((r) => r.request().method() === 'POST' && r.url().startsWith(`${API}/api/portaria/retiradas/`) && r.url().endsWith(`/${acao}`))

test('funcionário executa a retirada de ponta a ponta e consulta o histórico', async ({ page }) => {
  const nome = `Aluno E2E ${sufixo()}`
  const id = criarSolicitacao(nome)
  const painel = await entrarEAbrir(page, nome)
  await expect(painel).toContainText(`P-${id}`)
  await expect(painel).toContainText('Aguardando')
  await expect(painel).toContainText('Mãe') // vínculo vindo do cadastro
  await expect(painel).toContainText('Responsável legal') // tipo da autorização vigente

  // 1) Iniciar atendimento
  let resp = posted(page, 'iniciar-atendimento')
  await painel.getByRole('button', { name: 'Iniciar atendimento' }).click()
  expect((await resp).status()).toBe(200)
  await expect(page.getByText(`Atendimento iniciado: ${nome}`)).toBeVisible()
  await expect(painel).toContainText('Em atendimento')
  await expect(painel.getByRole('button', { name: 'Ler QR Code' })).toHaveCount(0) // QR não é simulado no modo http

  // 2) Identificar o responsável (documento conferido; autorização revalidada no backend)
  await painel.getByRole('button', { name: 'Confirmar identificação' }).click()
  const dlg = page.getByRole('dialog')
  const confirmar = dlg.getByRole('button', { name: 'Confirmar identificação' })
  await expect(confirmar).toBeDisabled()
  await dlg.getByRole('checkbox').check()
  resp = posted(page, 'confirmar-identificacao')
  await confirmar.click()
  const identificacao = await resp
  expect(identificacao.status()).toBe(200)
  expect(identificacao.request().postDataJSON()).toMatchObject({ metodo: 'DOCUMENTO' })
  await expect(painel).toContainText('Identificado')
  await expect(painel).toContainText('Documento ·')
  await expect(painel).toContainText('Marina Lopes') // atendido por

  // 3) Confirmar a saída
  await painel.getByRole('button', { name: 'Confirmar retirada do aluno' }).click()
  resp = posted(page, 'confirmar-retirada')
  await page.getByRole('dialog').getByRole('button', { name: 'Confirmar saída' }).click()
  expect((await resp).status()).toBe(200)
  await expect(page.getByText(`Retirada concluída: ${nome}`)).toBeVisible()
  await expect(painel).toContainText('Concluída')

  // 4) Histórico atualizado a partir do backend
  const linha = page.getByTestId('historico').locator('tbody tr').filter({ hasText: nome })
  await expect(linha).toHaveCount(1)
  await expect(linha).toContainText('Concluída')
  await expect(linha).toContainText('Marina Lopes')
})

test('autorização vencida: identificação bloqueada e rejeição exige justificativa', async ({ page }) => {
  const nome = `Aluno E2E Vencida ${sufixo()}`
  criarSolicitacao(nome, { autorizacaoVencida: true })
  const painel = await entrarEAbrir(page, nome)
  await expect(painel).toContainText('Autorização do responsável não está vigente')

  const resp = posted(page, 'iniciar-atendimento')
  await painel.getByRole('button', { name: 'Iniciar atendimento' }).click()
  expect((await resp).status()).toBe(200)
  await expect(painel).toContainText('Em atendimento')
  await expect(painel.getByRole('button', { name: 'Confirmar identificação' })).toHaveCount(0)

  await painel.getByRole('button', { name: /Identificação não confere/ }).click()
  const dlg = page.getByRole('dialog')
  await expect(dlg.getByLabel('Motivo')).toHaveValue('AUTORIZACAO_EXPIRADA')
  await dlg.getByRole('button', { name: 'Rejeitar solicitação' }).click()
  await expect(dlg.getByRole('alert')).toContainText('Informe a justificativa')

  await dlg.getByLabel(/Justificativa/).fill('Autorização temporária venceu ontem; responsável orientado a renovar.')
  const rejeicao = posted(page, 'rejeitar')
  await dlg.getByRole('button', { name: 'Rejeitar solicitação' }).click()
  expect((await rejeicao).status()).toBe(200)
  await expect(page.getByText(`Solicitação rejeitada: ${nome}`)).toBeVisible()
  await expect(painel).toContainText('Rejeitada: Autorização expirada ou revogada')

  const linha = page.getByTestId('historico').locator('tbody tr').filter({ hasText: nome })
  await expect(linha).toContainText('Rejeitada')
  await expect(linha).toContainText('Autorização temporária venceu ontem')
})

test('conflito com outra operação (409) é exibido e a tela é recarregada', async ({ page, playwright }) => {
  const nome = `Aluno E2E Conflito ${sufixo()}`
  const id = criarSolicitacao(nome)
  const painel = await entrarEAbrir(page, nome)
  await expect(painel.getByRole('button', { name: 'Iniciar atendimento' })).toBeVisible()

  // Outro posto inicia o mesmo atendimento direto na API (login real, CSRF e JWT)
  const outro = await playwright.request.newContext({ baseURL: API })
  await outro.get('/actuator/health')
  const csrf = (await outro.storageState()).cookies.find((c) => c.name === 'XSRF-TOKEN')!.value
  const login = await outro.post('/api/auth/login', {
    headers: { 'X-XSRF-TOKEN': csrf },
    data: { email: 'portaria@demo.entradasegura.local', password: 'Portaria@2026' },
  })
  const token = (await login.json()).accessToken as string
  const r = await outro.post(`/api/portaria/retiradas/${id}/iniciar-atendimento`, {
    headers: { 'X-XSRF-TOKEN': csrf, Authorization: `Bearer ${token}` },
    data: { versao: 0 },
  })
  expect(r.status()).toBe(200)
  await outro.dispose()

  // Este painel ainda mostra a versão antiga
  const resp = posted(page, 'iniciar-atendimento')
  await painel.getByRole('button', { name: 'Iniciar atendimento' }).click()
  const conflito = await resp
  expect(conflito.status()).toBe(409)
  expect((await conflito.json()).codigo).toBe('VERSAO_DESATUALIZADA')
  await expect(page.getByText('A solicitação foi alterada por outra operação')).toBeVisible()
  await expect(painel).toContainText('Em atendimento')
})
