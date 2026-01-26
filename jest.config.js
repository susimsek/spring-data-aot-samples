const nextJest = require('next/jest');

const createJestConfig = nextJest({
  dir: './src/main/webapp',
});

/** @type {import('jest').Config} */
const customJestConfig = {
  coverageProvider: 'v8',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  testMatch: ['<rootDir>/src/main/webapp/app/**/*.test.(ts|tsx)'],
  testPathIgnorePatterns: [
    '<rootDir>/node_modules/',
    '<rootDir>/src/main/webapp/.next/',
    '<rootDir>/src/main/webapp/build/',
  ],
  cacheDirectory: '<rootDir>/target/jest-cache',
  coverageDirectory: '<rootDir>/target/jest-coverage',
};

module.exports = createJestConfig(customJestConfig);
