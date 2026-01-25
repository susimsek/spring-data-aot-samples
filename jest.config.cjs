const nextJest = require('next/jest');

const createJestConfig = nextJest({
  // Load next.config.js and .env from the Next.js app folder.
  dir: './src/main/webapp',
});

/** @type {import('jest').Config} */
const customJestConfig = {
  coverageProvider: 'v8',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  testMatch: ['<rootDir>/src/main/webapp/**/*.test.(ts|tsx)'],
  testPathIgnorePatterns: ['<rootDir>/node_modules/', '<rootDir>/src/main/webapp/.next/', '<rootDir>/src/main/webapp/build/'],
  collectCoverageFrom: [
    'src/main/webapp/app/components/**/*.{ts,tsx}',
    'src/main/webapp/app/lib/**/*.{ts,tsx}',
    'src/main/webapp/app/slices/**/*.{ts,tsx}',
    'src/main/webapp/app/hooks.ts',
    '!src/main/webapp/app/**/page.tsx',
    '!src/main/webapp/app/layout.tsx',
    '!src/main/webapp/app/not-found.tsx',
    '!src/main/webapp/app/store.ts',
    '!src/main/webapp/app/**/__tests__/**',
    '!src/main/webapp/**/test-utils.{ts,tsx}',
    '!src/main/webapp/app/lib/fontawesome.ts',
    '!src/main/webapp/app/lib/iconLoader.ts',
    '!src/main/webapp/app/lib/window.ts',
    '!src/main/webapp/app/types.ts',
    '!src/main/webapp/next-env.d.ts',
  ],
  coverageDirectory: '<rootDir>/target/jest-coverage',
  coverageReporters: ['lcov', 'text', 'text-summary'],
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80,
    },
  },
};

module.exports = createJestConfig(customJestConfig);
