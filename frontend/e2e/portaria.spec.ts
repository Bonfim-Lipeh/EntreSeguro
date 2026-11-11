import { expect, test, type Page } from '@playwright/test'

const item = (page: Page, protocolo: string) => page.getByTestId(`item-fila-${protocolo}`)
const painel = (page: Page) => page.getByTestId('painel-detalhes')
const indicador = (page: Page, id: string) => page.getByTestId(`indicador-${id}`)

test.beforeEach(async ({ page }) => {
  await page.goto('/')
  await expect(page.getByTestId('fila')).toBeVisible()
})

test('exibe cabeçalho, indicadores e fila FIFO de solicitações ativas', async ({ page }) => {
  await expect(page.getByText('Modo demonstração', { exact: true })).toBeVisible()
  await expect(page.getByText('Marcos Vieira').first()).toBeVisible()
  await expect(page.getByRole('status').filter({ hasText: 'Conectado' })).toBeVisible()
  await expect(indicador(page, 'AGUARDANDO')).toHaveText('6')
  await expect(indicador(page, 'EM_ANDAMENTO')).toHaveText('2')
  await expect(indicador(page, 'CONCLUIDAS')).toHaveText('4')
  await expect(indicador(page, 'ENCERRADAS_SEM_SAIDA')).toHaveText('2')

  const itens = page.getByTestId('fila').locator('li')
  await expect(itens).toHaveCount(8)
  await expect(itens.first()).toContainText('Sofia Mattos Azevedo')
  await expect(itens.last()).toContainText('Helena Barros Dias')
  await expect(page.getByTestId('historico').locator('tbody tr')).toHaveCount(6)
})

test('pesquisa por responsável e filtra por status', async ({ page }) => {
  await page.getByLabel('Pesquisar por aluno, responsável ou protocolo').fill('sonia')
  await expect(page.getByTestId('fila').locator('li')).toHaveCount(1)
  await expect(page.getByTestId('fila')).toContainText('Gabriel Tavares Nunes')

  await page.getByLabel('Limpar pesquisa').click()
  await page.getByRole('radio', { name: 'Concluídas' }).click()
  await expect(page.getByTestId('fila').locator('li')).toHaveCount(4)

  // Indicador também funciona como atalho de filtro.
  await page.getByRole('button', { name: /Rejeitadas ou canceladas/ }).click()
  await expect(page.getByRole('radio', { name: 'Rejeitadas e canceladas' })).toHaveAttribute('aria-checked', 'true')
  await expect(page.getByTestId('fila').locator('li')).toHaveCount(2)

  await page.getByLabel('Pesquisar por aluno, responsável ou protocolo').fill('inexistente')
  await expect(page.getByText('Nenhum resultado para a pesquisa')).toBeVisible()
})

test('fluxo completo: iniciar, validar QR, confirmar identificação e retirada', async ({ page }) => {
  await item(page, 'RT-1409').click()
  const p = painel(page)
  await expect(p).toContainText('Lucas Andrade Moreira')
  await expect(p.getByRole('button', { name: 'Confirmar retirada do aluno' })).toHaveCount(0)

  await p.getByRole('button', { name: 'Iniciar atendimento' }).click()
  await expect(page.getByText('Atendimento iniciado: Lucas Andrade Moreira')).toBeVisible()
  await expect(p.locator('[data-status]').first()).toHaveAttribute('data-status', 'EM_ATENDIMENTO')
  await expect(indicador(page, 'AGUARDANDO')).toHaveText('5')

  // QR válido: confere, mas NÃO conclui a retirada.
  await p.getByRole('button', { name: 'Ler QR Code' }).click()
  await expect(p.getByTestId('resultado-qr')).toHaveAttribute('data-valido', 'true')
  await expect(p.getByText('A leitura não libera o aluno')).toBeVisible()
  await expect(p.locator('[data-status]').first()).toHaveAttribute('data-status', 'EM_ATENDIMENTO')

  await p.getByRole('button', { name: 'Confirmar identificação' }).click()
  const dlg = page.getByRole('dialog')
  const confirmar = dlg.getByRole('button', { name: 'Confirmar identificação' })
  await expect(confirmar).toBeDisabled()
  await dlg.getByRole('checkbox').check()
  await confirmar.click()
  await expect(p.locator('[data-status]').first()).toHaveAttribute('data-status', 'IDENTIFICADO')
  await expect(p.getByRole('button', { name: /rejeitar/i })).toHaveCount(0)

  await p.getByRole('button', { name: 'Confirmar retirada do aluno' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Confirmar saída' }).click()
  await expect(page.getByText('Retirada concluída: Lucas Andrade Moreira')).toBeVisible()
  await expect(p.locator('[data-status]').first()).toHaveAttribute('data-status', 'CONCLUIDA')
  await expect(indicador(page, 'CONCLUIDAS')).toHaveText('5')
  // Sai da fila de ativas e entra no histórico.
  await expect(item(page, 'RT-1409')).toHaveCount(0)
  const primeiraLinha = page.getByTestId('historico').locator('tbody tr').first()
  await expect(primeiraLinha).toContainText('Lucas Andrade Moreira')
  await expect(primeiraLinha).toContainText('Marcos Vieira')
  await expect(primeiraLinha).toContainText('Concluída')
})

test('QR de outra solicitação não confere e rejeição exige justificativa', async ({ page }) => {
  await item(page, 'RT-1410').click()
  const p = painel(page)
  await p.getByRole('button', { name: 'Iniciar atendimento' }).click()
  await expect(p.locator('[data-status]').first()).toHaveAttribute('data-status', 'EM_ATENDIMENTO')

  await p.getByLabel('Código apresentado (fictício)').selectOption({ label: 'QR de outra solicitação (RT-1409)' })
  await p.getByRole('button', { name: 'Ler QR Code' }).click()
  await expect(p.getByTestId('resultado-qr')).toHaveAttribute('data-valido', 'false')
  await expect(p.getByTestId('resultado-qr')).toContainText('pertence a outra solicitação')

  await p.getByRole('button', { name: /Identificação não confere/ }).click()
  const dlg = page.getByRole('dialog')
  await dlg.getByRole('button', { name: 'Rejeitar solicitação' }).click()
  await expect(dlg.getByRole('alert')).toContainText('Informe a justificativa')
  await expect(dlg).toBeVisible()

  await dlg.getByLabel('Motivo').selectOption('IDENTIDADE_NAO_CONFERE')
  await dlg.getByLabel(/Justificativa/).fill('QR apresentado pertence a outra solicitação; responsável sem documento.')
  await dlg.getByRole('button', { name: 'Rejeitar solicitação' }).click()
  await expect(dlg).toBeHidden()
  await expect(page.getByText('Solicitação rejeitada: Beatriz Sampaio Leal')).toBeVisible()
  await expect(indicador(page, 'ENCERRADAS_SEM_SAIDA')).toHaveText('3')
  const linha = page.getByTestId('historico').locator('tbody tr').first()
  await expect(linha).toContainText('Rejeitada')
  await expect(linha).toContainText('QR apresentado pertence a outra solicitação')
})

test('autorização expirada bloqueia a identificação e permite apenas rejeitar', async ({ page }) => {
  await item(page, 'RT-1412').click()
  const p = painel(page)
  await expect(p.locator('[data-nivel=critico]')).toContainText('Autorização temporária expirou')
  await p.getByRole('button', { name: 'Iniciar atendimento' }).click()
  await expect(p.locator('[data-status]').first()).toHaveAttribute('data-status', 'EM_ATENDIMENTO')
  await expect(p.getByRole('button', { name: 'Confirmar identificação' })).toHaveCount(0)
  await expect(p.getByRole('button', { name: /Identificação não confere/ })).toBeVisible()
})

test('evento em tempo real: cancelamento pelo responsável atualiza painel e indicadores', async ({ page }) => {
  await item(page, 'RT-1414').click()
  await expect(painel(page).getByRole('button', { name: 'Iniciar atendimento' })).toBeVisible()

  await page.getByRole('button', { name: 'Controles da demonstração' }).click()
  await page.getByRole('menuitem', { name: 'Simular cancelamento pelo responsável' }).click()
  await expect(page.getByText('Solicitação cancelada pelo responsável: Helena Barros Dias')).toBeVisible()
  await expect(painel(page).locator('[data-status]').first()).toHaveAttribute('data-status', 'CANCELADA')
  await expect(painel(page).getByRole('button', { name: 'Iniciar atendimento' })).toHaveCount(0)
  await expect(indicador(page, 'AGUARDANDO')).toHaveText('5')

  await page.getByRole('button', { name: 'Controles da demonstração' }).click()
  await page.getByRole('menuitem', { name: 'Simular nova solicitação' }).click()
  await expect(page.getByText('Nova solicitação na fila: Valentina Cruz Moura')).toBeVisible()
  await expect(page.getByTestId('fila').locator('li').last()).toContainText('Valentina Cruz Moura')
  await expect(indicador(page, 'AGUARDANDO')).toHaveText('6')
})

test('perda de conexão exibe aviso e erro nas ações; reconexão normaliza', async ({ page }) => {
  await item(page, 'RT-1411').click()
  await page.getByRole('button', { name: 'Controles da demonstração' }).click()
  await page.getByRole('menuitem', { name: 'Simular perda de conexão' }).click()

  await painel(page).getByRole('button', { name: 'Iniciar atendimento' }).click()
  await expect(page.getByText('Sem conexão com o servidor. Verifique a rede')).toBeVisible()
  await expect(page.getByRole('status').filter({ hasText: /Sem conexão|Reconectando/ })).toBeVisible({ timeout: 15_000 })
  await expect(page.getByRole('alert').filter({ hasText: 'não confirme retiradas' })).toBeVisible()

  await page.getByRole('button', { name: 'Controles da demonstração' }).click()
  await page.getByRole('menuitem', { name: 'Restabelecer conexão' }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Conectado' })).toBeVisible({ timeout: 15_000 })
  await expect(page.getByRole('alert').filter({ hasText: 'não confirme retiradas' })).toHaveCount(0)
  await painel(page).getByRole('button', { name: 'Iniciar atendimento' }).click()
  await expect(painel(page).locator('[data-status]').first()).toHaveAttribute('data-status', 'EM_ATENDIMENTO')
})
