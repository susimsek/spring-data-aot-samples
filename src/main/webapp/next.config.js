const path = require('path');

const isDev = process.env.NODE_ENV === 'development';
const options = {
  tls: false,
};
const sources = ['/api/:path*'];

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',
  trailingSlash: true,
  distDir: 'build',
  turbopack: {
    // This project lives under `src/main/webapp`, but deps are installed at repo root.
    // Pin Turbopack root to the repo root so `node_modules` is resolvable.
    root: path.join(__dirname, '..', '..', '..'),
  },
  images: {
    unoptimized: true,
  },
  reactStrictMode: true,
  ...(isDev && {
    async rewrites() {
      return sources.map(source => ({
        source,
        destination: `http${options.tls ? 's' : ''}://localhost:8080${source}`,
      }));
    },
  }),
};

module.exports = nextConfig;
