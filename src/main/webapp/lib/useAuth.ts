'use client';

import { useCallback, useEffect } from 'react';
import { useAppDispatch, useAppSelector } from './store';
import { isAdmin as isAdminUser } from './auth';
import { logoutUser } from '../slices/authSlice';
import { getLocation, replaceLocation } from './window';
import { getLocalePrefix, isPublicRoute } from './routes';
import type { StoredUser } from './types';

function buildLoginUrl(currentPath: string, queryString: string): string {
  const localePrefix = getLocalePrefix(currentPath);
  const redirectTarget = queryString ? `${currentPath}?${queryString}` : currentPath;
  const encodedRedirect = encodeURIComponent(redirectTarget);
  return `${localePrefix}/login?redirect=${encodedRedirect}`;
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
  const user = useAppSelector((state) => state.auth.user);
  const loading = false;

  useEffect(() => {
    if (!redirectOnFail) return;
    if (user) return;

    const location = getLocation();
    const path = location?.pathname || '';
    const queryString = location?.search?.replace(/^\?/, '') || '';

    if (isPublicRoute(path)) return;

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
