import { defineConfig, globalIgnores } from 'eslint/config';
import nextVitals from 'eslint-config-next/core-web-vitals';
import nextTs from 'eslint-config-next/typescript';
import prettier from 'eslint-config-prettier/flat';

export default defineConfig([
  ...nextVitals,
  ...nextTs,
  // Override default ignores of eslint-config-next and keep lint fast.
  globalIgnores([
    '**/node_modules/**',
    '**/.next/**',
    '**/out/**',
    '**/build/**',
    '**/node/**',
    '**/target/**',
    '**/next-env.d.ts',
    'jest.config.js',
    'jest.setup.ts',
    'postcss.config.js',
  ]),
  // Next.js app lives under src/main/webapp (monorepo-style root).
  { settings: { next: { rootDir: ['src/main/webapp/'] } } },
  {
    rules: {
      'react-hooks/incompatible-library': 'off',
      '@typescript-eslint/no-explicit-any': 'error',
    },
  },
  {
    files: ['src/main/webapp/**/__tests__/**/*.{ts,tsx}', 'src/main/webapp/**/*.{test,spec}.{ts,tsx}'],
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
    },
  },
  // Disable formatting-related rules (Prettier is the formatter).
  prettier,
]);
