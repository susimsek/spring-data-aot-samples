import type { StoredUser } from '../types';

const USER_STORAGE_KEY = 'currentUser';

export function loadStoredUser(): StoredUser | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredUser) : null;
  } catch {
    return null;
  }
}

export function persistUser(user: StoredUser | null): void {
  if (typeof window === 'undefined') return;
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
  if (typeof window === 'undefined') return '';
  const path = window.location.pathname || '/';
  const search = window.location.search || '';
  const hash = window.location.hash || '';
  return encodeURIComponent(`${path}${search}${hash}`);
}
