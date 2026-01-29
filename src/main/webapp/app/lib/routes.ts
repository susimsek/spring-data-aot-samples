export const PUBLIC_ROUTES = ['/login', '/register', '/share', '/403', '/404'];

export const PROTECTED_ROUTES = ['/', '/change-password', '/shared-links'];

export function isPublicRoute(path: string): boolean {
  return PUBLIC_ROUTES.some((route) => path === route || path.startsWith(`${route}/`));
}

export function isProtectedRoute(path: string): boolean {
  return PROTECTED_ROUTES.some((route) => path === route || path.startsWith(`${route}/`));
}
