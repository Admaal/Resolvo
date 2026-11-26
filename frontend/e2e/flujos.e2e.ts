import { expect, test, type Page } from '@playwright/test';

async function entrarComo(page: Page, rol: 'Cliente' | 'Agente' | 'Admin') {
  await page.goto('/');
  await page.getByRole('button', { name: new RegExp(`Entrar como ${rol}`) }).click();
  await expect(page).toHaveURL(/\/tickets/);
  await expect(page.locator('a[href^="/tickets/RS-"]').first()).toBeVisible();
}

test('el agente entra con la demo y ve la bandeja con sus vistas', async ({ page }) => {
  await entrarComo(page, 'Agente');

  await expect(page.getByRole('heading', { name: /Todos los tickets/ })).toBeVisible();
  const vistas = page.getByRole('navigation', { name: 'Vistas' });
  await expect(vistas.getByRole('link', { name: /Sin asignar/ })).toBeVisible();
  await vistas.getByRole('link', { name: /SLA en riesgo/ }).click();
  await expect(page).toHaveURL(/vista=riesgo/);
  await expect(page.getByRole('heading', { name: /SLA en riesgo/ })).toBeVisible();
});

test('el agente abre un ticket y publica un comentario', async ({ page }) => {
  await entrarComo(page, 'Agente');
  await page.locator('a[href="/tickets/RS-3"]').first().click();
  await expect(page).toHaveURL(/\/tickets\/RS-3$/);

  const texto = `Comentario E2E ${Date.now()}`;
  await page.getByPlaceholder(/Escribe una respuesta/).fill(texto);
  await page.getByRole('button', { name: /^Enviar/ }).click();

  await expect(page.getByText(texto)).toBeVisible();
  await page.reload();
  await expect(page.getByText(texto)).toBeVisible();
});

test('el cliente solo ve sus vistas y no el asistente de IA', async ({ page }) => {
  await entrarComo(page, 'Cliente');
  const vistas = page.getByRole('navigation', { name: 'Vistas' });

  await expect(vistas.getByRole('link')).toHaveText([/Mis tickets/, /Todos/, /Resueltos/]);
  await page.locator('a[href^="/tickets/RS-"]').first().click();
  await expect(page.getByText(/Asistente/)).toHaveCount(0);
});

test('en móvil las vistas van abajo y se esconden dentro del ticket', async ({ page }, info) => {
  test.skip(info.project.name !== 'movil', 'solo en el proyecto móvil');
  await entrarComo(page, 'Agente');
  const vistas = page.getByRole('navigation', { name: 'Vistas' });

  const caja = await vistas.boundingBox();
  const alto = page.viewportSize()!.height;
  expect(caja).not.toBeNull();
  expect(Math.round(caja!.y + caja!.height)).toBe(alto);

  await page.locator('a[href^="/tickets/RS-"]').first().click();
  await expect(vistas).toBeHidden();
  await page.getByRole('link', { name: /Bandeja/ }).click();
  await expect(vistas).toBeVisible();
});
