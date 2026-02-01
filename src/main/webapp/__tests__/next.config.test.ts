import type { NextConfig } from 'next';

describe('next.config', () => {
  let originalEnv: string | undefined;

  beforeEach(() => {
    originalEnv = process.env.NODE_ENV;
  });

  afterEach(() => {
    if (originalEnv !== undefined) {
      Object.defineProperty(process.env, 'NODE_ENV', {
        value: originalEnv,
        writable: true,
        configurable: true,
      });
    } else {
      delete (process.env as { NODE_ENV?: string }).NODE_ENV;
    }
    jest.resetModules();
  });

  test('should export static site with trailingSlash enabled', async () => {
    Object.defineProperty(process.env, 'NODE_ENV', {
      value: 'production',
      writable: true,
      configurable: true,
    });
    const config = (await import('../next.config')).default as NextConfig;

    expect(config.output).toBe('export');
    expect(config.trailingSlash).toBe(true);
  });

  test('should disable optimized images', async () => {
    const config = (await import('../next.config')).default as NextConfig;

    expect(config.images?.unoptimized).toBe(true);
  });

  test('should disable poweredByHeader', async () => {
    const config = (await import('../next.config')).default as NextConfig;

    expect(config.poweredByHeader).toBe(false);
  });

  test('should enable reactStrictMode', async () => {
    const config = (await import('../next.config')).default as NextConfig;

    expect(config.reactStrictMode).toBe(true);
  });

  test('should configure custom outputFileTracingRoot', async () => {
    const config = (await import('../next.config')).default as NextConfig;

    expect(config.outputFileTracingRoot).toBeDefined();
    expect(typeof config.outputFileTracingRoot).toBe('string');
    expect(config.outputFileTracingRoot).toBeTruthy();
  });

  test('should configure typescript with custom tsconfig path', async () => {
    const config = (await import('../next.config')).default as NextConfig;

    expect(config.typescript?.tsconfigPath).toBe('../../../tsconfig.json');
  });

  test('should configure turbopack root', async () => {
    const config = (await import('../next.config')).default as NextConfig;

    expect(config.turbopack?.root).toBeDefined();
    expect(typeof config.turbopack?.root).toBe('string');
    expect(config.turbopack?.root).toBeTruthy();
  });

  test('should add rewrites in development mode', async () => {
    Object.defineProperty(process.env, 'NODE_ENV', {
      value: 'development',
      writable: true,
      configurable: true,
    });
    jest.resetModules();
    const config = (await import('../next.config')).default as NextConfig;

    expect(config.rewrites).toBeDefined();

    if (config.rewrites) {
      const rewrites = await config.rewrites();
      if (Array.isArray(rewrites)) {
        expect(rewrites.length).toBeGreaterThan(0);
        expect(rewrites[0]).toMatchObject({
          source: '/api/:path*',
          destination: 'http://localhost:8080/api/:path*',
        });
      }
    }
  });

  test('should not add rewrites in production mode', async () => {
    Object.defineProperty(process.env, 'NODE_ENV', {
      value: 'production',
      writable: true,
      configurable: true,
    });
    jest.resetModules();
    const config = (await import('../next.config')).default as NextConfig;

    expect(config.rewrites).toBeUndefined();
  });

  test('should use http protocol when tls is false in development', async () => {
    Object.defineProperty(process.env, 'NODE_ENV', {
      value: 'development',
      writable: true,
      configurable: true,
    });
    jest.resetModules();
    const config = (await import('../next.config')).default as NextConfig;

    if (config.rewrites) {
      const rewrites = await config.rewrites();
      if (Array.isArray(rewrites) && rewrites.length > 0) {
        expect(rewrites[0].destination).toMatch(/^http:\/\//);
      }
    }
  });

  test('should handle multiple source patterns in development', async () => {
    Object.defineProperty(process.env, 'NODE_ENV', {
      value: 'development',
      writable: true,
      configurable: true,
    });
    jest.resetModules();
    const config = (await import('../next.config')).default as NextConfig;

    if (config.rewrites) {
      const rewrites = await config.rewrites();
      if (Array.isArray(rewrites)) {
        expect(rewrites).toEqual(
          expect.arrayContaining([
            expect.objectContaining({
              source: '/api/:path*',
            }),
          ]),
        );
      }
    }
  });

  test('should properly configure file tracing for monorepo structure', async () => {
    const config = (await import('../next.config')).default as NextConfig;

    expect(config.outputFileTracingRoot).toBeTruthy();
    expect(typeof config.outputFileTracingRoot).toBe('string');
  });

  test('should configure turbopack for monorepo structure', async () => {
    const config = (await import('../next.config')).default as NextConfig;

    expect(config.turbopack?.root).toBeTruthy();
    expect(typeof config.turbopack?.root).toBe('string');
  });
});
