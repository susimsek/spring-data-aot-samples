'use client';

import { useCallback, useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../hooks';
import { isAdmin as isAdminUser } from './auth';
import { logoutUser } from '../slices/authSlice';
import { reloadPage, replaceLocation } from './window';
import type { StoredUser } from '../types';

// Public routes that do not require authentication
const PUBLIC_ROUTES = ['/login', '/register', '/share', '/403', '/404'];

/**
 * Checks if a given path is a public route.
 */
function isPublicRoute(path: string): boolean {
  return PUBLIC_ROUTES.some(route => path === route || path.startsWith(`${route}/`));
}

/**
 * Builds the login URL with an optional redirect parameter.
 */
function buildLoginUrl(currentPath: string, queryString: string): string {
  const redirectTarget = queryString ? `${currentPath}?${queryString}` : currentPath;
  const encodedRedirect = encodeURIComponent(redirectTarget);
  return `/login?redirect=${encodedRedirect}`;
}

export default function useAuth(
  options: {
    redirectOnFail?: boolean;
  } = {},
): {
  user: StoredUser | null;
  loading: boolean;
  isAdmin: boolean;
  isAuthenticated: boolean;
  logout: () => Promise<void>;
} {
  const { redirectOnFail = false } = options;
  const dispatch = useAppDispatch();
  const user = useAppSelector(state => state.auth.user);
  const loading = false;

  useEffect(() => {
    if (!redirectOnFail) return;
    if (user) return;

    const location = (globalThis as any).location as Location | undefined;
    const path = location?.pathname || '';
    const queryString = location?.search?.replace(/^\?/, '') || '';

    // Skip redirect for public routes
    if (isPublicRoute(path)) return;

    // Redirect to login with the current path as redirect parameter
    const loginUrl = buildLoginUrl(path, queryString);
    replaceLocation(loginUrl);
  }, [redirectOnFail, user]);

  const logout = useCallback(async () => {
    try {
      await dispatch(logoutUser()).unwrap();
    } finally {
      replaceLocation('/');
    }
  }, [dispatch]);

  return {
    user,
    loading,
    isAdmin: isAdminUser(user),
    isAuthenticated: !!user?.username,
    logout,
  };
}
