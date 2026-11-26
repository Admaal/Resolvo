import { defineConfig, devices } from '@playwright/test';

// E2E contra la pila real: Postgres, la API en :8080 y la SPA en :4200 (ng serve con proxy a /api).
// La API tiene que estar arrancada antes; la SPA la levanta Playwright si no está ya.
// En local se puede usar el Edge instalado con PW_CHANNEL=msedge, sin descargar Chromium.
const canal = process.env['PW_CHANNEL'];

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  fullyParallel: false,
  workers: 1,
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'retain-on-failure',
    ...(canal ? { channel: canal } : {}),
  },
  projects: [
    {
      name: 'escritorio',
      use: { ...devices['Desktop Chrome'], ...(canal ? { channel: canal } : {}) },
    },
    {
      name: 'movil',
      use: {
        ...devices['Pixel 7'],
        viewport: { width: 390, height: 844 },
        ...(canal ? { channel: canal } : {}),
      },
    },
  ],
  webServer: {
    command: 'npm start',
    url: 'http://localhost:4200',
    reuseExistingServer: true,
    timeout: 180_000,
  },
});
