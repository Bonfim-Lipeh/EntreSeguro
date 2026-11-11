import { expect, test } from '@playwright/test'

test('tablet: detalhes abrem em painel lateral sobreposto e fecham', async ({ page }) => {
  await page.goto('/')
  await page.getByTestId('item-fila-RT-1408').click()
  const dlg = page.getByRole('dialog')
  await expect(dlg).toBeVisible()
  await expect(dlg).toContainText('Rafael Coutinho Silva')
  await expect(dlg.getByRole('button', { name: 'Confirmar identificação' })).toBeVisible()
  await dlg.getByRole('button', { name: 'Fechar painel' }).click()
  await expect(dlg).toBeHidden()
})
