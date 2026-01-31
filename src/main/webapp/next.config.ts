import type { NextConfig } from 'next';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const filename = fileURLToPath(import.meta.url);
const dirname = path.dirname(filename);

const isDev = process.env.NODE_ENV === 'development';
const options = {
  tls: false,
};
const sources = ['/api/:path*'];

const nextConfig: NextConfig = {
  output: 'export',
  trailingSlash: true,
  distDir: 'build',
  // This app is nested under src/main/webapp while node_modules lives at repo root (npm workspaces).
  outputFileTracingRoot: path.join(dirname, '..', '..', '..'),
  typescript: {
    tsconfigPath: '../../../tsconfig.json',
  },
  turbopack: {
    // This project lives under `src/main/webapp`, but deps are installed at repo root.
    // Pin Turbopack root to the repo root so `node_modules` is resolvable.
    root: path.join(dirname, '..', '..', '..'),
  },
  images: {
    unoptimized: true,
  },
  poweredByHeader: false,
  reactStrictMode: true,
  ...(isDev
    ? {
        async rewrites() {
          return sources.map((source) => ({
            source,
            destination: `http${options.tls ? 's' : ''}://localhost:8080${source}`,
          }));
        },
      }
    : {}),
};

export default nextConfig;
