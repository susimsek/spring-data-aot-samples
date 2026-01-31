const nextJest = require('next/jest');

const createJestConfig = nextJest({
  dir: './src/main/webapp',
});

/** @type {import('jest').Config} */
const customJestConfig = {
  coverageProvider: 'v8',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  testMatch: ['<rootDir>/src/main/webapp/**/*.test.(ts|tsx)'],
  testPathIgnorePatterns: ['<rootDir>/node_modules/', '<rootDir>/src/main/webapp/.next/', '<rootDir>/src/main/webapp/build/'],
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/main/webapp/$1',
    '^@components/(.*)$': '<rootDir>/src/main/webapp/components/$1',
    '^@lib/(.*)$': '<rootDir>/src/main/webapp/lib/$1',
    '^@pages/(.*)$': '<rootDir>/src/main/webapp/pages/$1',
    '^@slices/(.*)$': '<rootDir>/src/main/webapp/slices/$1',
    '^@tests/(.*)$': '<rootDir>/src/main/webapp/__tests__/$1',
    '^@styles/(.*)$': '<rootDir>/src/main/webapp/styles/$1',
    '^@root/(.*)$': '<rootDir>/$1',
  },
  cacheDirectory: '<rootDir>/target/jest-cache',
  coverageDirectory: '<rootDir>/target/jest-coverage',
};

module.exports = createJestConfig(customJestConfig);
