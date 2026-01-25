'use client';

import { useCallback, useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../hooks';
import { isAdmin as isAdminUser } from './auth';
import { logoutUser } from '../slices/authSlice';
import { reloadPage, replaceLocation } from './window';
import type { StoredUser } from '../types';

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
    if (path.includes('/login') || path.includes('/register')) return;

    replaceLocation('/login');
  }, [redirectOnFail, user]);

  const logout = useCallback(async () => {
    try {
      await dispatch(logoutUser()).unwrap();
    } finally {
      reloadPage();
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
