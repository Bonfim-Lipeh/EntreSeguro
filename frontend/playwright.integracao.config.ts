import { defineConfig, devices } from '@playwright/test'

/**
 * Testes de INTEGRAÇÃO REAL: painel (VITE_API_MODE=http) + backend Spring Boot + MySQL.
 * Pré-requisitos: MySQL e backend (perfil dev, com dados de demonstração) rodando em
 * http://localhost:8080. O frontend é iniciado em http://localhost:5173 (modo http).
 * Nada é simulado: as requisições vão ao backend real.
 */
export default defineConfig({
  testDir: './e2e-integracao',
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:5173',
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    launchOptions: process.env.PW_CHROME_PATH ? { executablePath: process.env.PW_CHROME_PATH } : {},
  },
  projects: [{ name: 'integracao', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } }],
  webServer: {
    command: 'npx vite --mode http --port 5173 --strictPort',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
  },
})
