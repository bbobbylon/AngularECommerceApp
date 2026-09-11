import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright E2E for the Luv2Shop storefront.
 *
 * The specs are **hermetic**: they stub the backend API at the network boundary (see
 * `e2e/support/mock-backend.ts`), so the suite needs neither MySQL/Docker nor the Spring Boot
 * backend running — just the Angular dev server, which Playwright starts via `webServer` below.
 * That keeps it fast and CI-friendly. Run with `npm run e2e`.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  timeout: 30_000,
  expect: { timeout: 10_000 },

  use: {
    baseURL: 'http://localhost:4250',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],

  // Start the Angular dev server before the tests; reuse a running one locally.
  webServer: {
    command: 'npm start',
    url: 'http://localhost:4250',
    reuseExistingServer: !process.env.CI,
    timeout: 180_000,
  },
});
