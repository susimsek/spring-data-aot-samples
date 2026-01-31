import rootConfig from '@root/next-i18next.config';

export const PUBLIC_ROUTES = ['/login', '/register', '/share', '/403', '/404'];

export const PROTECTED_ROUTES = ['/', '/change-password', '/shared-links'];

export const LOCALE_ROUTES = rootConfig.i18n.locales;

function normalizePathname(path: string): string {
  const normalized = path.startsWith('/') ? path : `/${path}`;
  const cutIndex = normalized.search(/[?#]/);
  return cutIndex === -1 ? normalized : normalized.slice(0, cutIndex) || '/';
}

export function stripLocalePrefix(path: string): string {
  if (!path) return '/';
  const normalized = normalizePathname(path);
  for (const locale of LOCALE_ROUTES) {
    const prefix = `/${locale}`;
    if (normalized === prefix) return '/';
    if (normalized.startsWith(`${prefix}/`)) {
      const rest = normalized.slice(prefix.length);
      return rest || '/';
    }
  }
  return normalized;
}

export function getLocalePrefix(path: string): string {
  if (!path) return '';
  const normalized = normalizePathname(path);
  for (const locale of LOCALE_ROUTES) {
    const prefix = `/${locale}`;
    if (normalized === prefix || normalized.startsWith(`${prefix}/`)) {
      return prefix;
    }
  }
  return '';
}

export function isPublicRoute(path: string): boolean {
  const normalized = stripLocalePrefix(path);
  return PUBLIC_ROUTES.some((route) => normalized === route || normalized.startsWith(`${route}/`));
}

export function isProtectedRoute(path: string): boolean {
  const normalized = stripLocalePrefix(path);
  return PROTECTED_ROUTES.some((route) => normalized === route || normalized.startsWith(`${route}/`));
}
