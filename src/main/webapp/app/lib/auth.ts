import type { StoredUser } from '../types';

const USER_STORAGE_KEY = 'currentUser';

export function loadStoredUser(): StoredUser | null {
  if (typeof localStorage === 'undefined') return null;
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredUser) : null;
  } catch {
    return null;
  }
}

export function persistUser(user: StoredUser | null): void {
  if (typeof localStorage === 'undefined') return;
  try {
    if (user) {
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(USER_STORAGE_KEY);
    }
  } catch {
    // ignore
  }
}

export function clearStoredUser(): void {
  persistUser(null);
}

export function isAdmin(user: StoredUser | null): boolean {
  const authorities = user?.authorities;
  return Array.isArray(authorities) && authorities.includes('ROLE_ADMIN');
}

export function buildRedirectQuery(): string {
  const location = (globalThis as any).location as Location | undefined;
  if (!location) return '';
  const path = location.pathname || '/';
  const search = location.search || '';
  const hash = location.hash || '';
  return encodeURIComponent(`${path}${search}${hash}`);
}
