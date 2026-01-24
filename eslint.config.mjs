import { defineConfig, globalIgnores } from 'eslint/config';
import nextVitals from 'eslint-config-next/core-web-vitals';
import prettier from 'eslint-config-prettier';

export default defineConfig([
  ...nextVitals,
  // Override default ignores of eslint-config-next and keep lint fast.
  globalIgnores(['**/node_modules/**', '**/.next/**', '**/out/**', '**/build/**', '**/node/**', '**/target/**', '**/next-env.d.ts']),
  // Next.js app lives under src/main/webapp (monorepo-style root).
  { settings: { next: { rootDir: ['src/main/webapp/'] } } },
  // We don't use React Compiler in this project; this rule is noisy with react-hook-form.
  { rules: { 'react-hooks/incompatible-library': 'off' } },
  // Disable formatting-related rules (Prettier is the formatter).
  prettier,
]);
