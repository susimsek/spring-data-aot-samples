import { getLocalePrefix, isPublicRoute, isProtectedRoute, PROTECTED_ROUTES, PUBLIC_ROUTES, stripLocalePrefix } from './routes';

describe('routes', () => {
  describe('PUBLIC_ROUTES', () => {
    test('contains expected public routes', () => {
      expect(PUBLIC_ROUTES).toContain('/login');
      expect(PUBLIC_ROUTES).toContain('/register');
      expect(PUBLIC_ROUTES).toContain('/share');
      expect(PUBLIC_ROUTES).toContain('/403');
      expect(PUBLIC_ROUTES).toContain('/404');
    });
  });

  describe('PROTECTED_ROUTES', () => {
    test('contains expected protected routes', () => {
      expect(PROTECTED_ROUTES).toContain('/');
      expect(PROTECTED_ROUTES).toContain('/change-password');
      expect(PROTECTED_ROUTES).toContain('/shared-links');
    });
  });

  describe('isPublicRoute', () => {
    test('returns true for exact public routes', () => {
      expect(isPublicRoute('/login')).toBe(true);
      expect(isPublicRoute('/register')).toBe(true);
      expect(isPublicRoute('/share')).toBe(true);
      expect(isPublicRoute('/403')).toBe(true);
      expect(isPublicRoute('/404')).toBe(true);
    });

    test('returns true for nested public routes', () => {
      expect(isPublicRoute('/share/abc123')).toBe(true);
      expect(isPublicRoute('/login/callback')).toBe(true);
    });

    test('ignores query strings and hashes', () => {
      expect(isPublicRoute('/login?redirect=%2F')).toBe(true);
      expect(isPublicRoute('/login#section')).toBe(true);
    });

    test('returns false for protected routes', () => {
      expect(isPublicRoute('/')).toBe(false);
      expect(isPublicRoute('/change-password')).toBe(false);
      expect(isPublicRoute('/shared-links')).toBe(false);
    });
  });

  describe('isProtectedRoute', () => {
    test('returns true for exact protected routes', () => {
      expect(isProtectedRoute('/')).toBe(true);
      expect(isProtectedRoute('/change-password')).toBe(true);
      expect(isProtectedRoute('/shared-links')).toBe(true);
    });

    test('returns false for public routes', () => {
      expect(isProtectedRoute('/login')).toBe(false);
      expect(isProtectedRoute('/register')).toBe(false);
      expect(isProtectedRoute('/share')).toBe(false);
    });

    test('ignores query strings and hashes', () => {
      expect(isProtectedRoute('/shared-links?page=1')).toBe(true);
      expect(isProtectedRoute('/shared-links#top')).toBe(true);
    });
  });

  describe('stripLocalePrefix', () => {
    test('removes locale prefix and preserves non-empty path', () => {
      expect(stripLocalePrefix('/en/login')).toBe('/login');
      expect(stripLocalePrefix('/tr/shared-links')).toBe('/shared-links');
    });

    test('treats locale root as "/"', () => {
      expect(stripLocalePrefix('/en')).toBe('/');
      expect(stripLocalePrefix('/tr')).toBe('/');
    });

    test('ignores query strings and hashes', () => {
      expect(stripLocalePrefix('/en/login?x=1')).toBe('/login');
      expect(stripLocalePrefix('/en/login#x')).toBe('/login');
    });
  });

  describe('getLocalePrefix', () => {
    test('returns locale prefix when present', () => {
      expect(getLocalePrefix('/en/login')).toBe('/en');
      expect(getLocalePrefix('/tr/shared-links')).toBe('/tr');
    });

    test('ignores query strings and hashes', () => {
      expect(getLocalePrefix('/en/login?x=1')).toBe('/en');
      expect(getLocalePrefix('/tr/login#x')).toBe('/tr');
    });

    test('returns empty string when no locale prefix present', () => {
      expect(getLocalePrefix('/login')).toBe('');
    });
  });
});
