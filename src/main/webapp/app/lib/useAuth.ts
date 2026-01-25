'use client';

import { useCallback, useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../hooks';
import { isAdmin as isAdminUser } from './auth';
import { fetchCurrentUser, logoutUser } from '../slices/authSlice';
import type { StoredUser } from '../types';

export default function useAuth(
  options: {
    redirectOnFail?: boolean;
    fetchUser?: boolean;
  } = {},
): {
  user: StoredUser | null;
  loading: boolean;
  isAdmin: boolean;
  isAuthenticated: boolean;
  logout: () => Promise<void>;
} {
  const { redirectOnFail = false, fetchUser = true } = options;
  const dispatch = useAppDispatch();
  const user = useAppSelector(state => state.auth.user);
  const status = useAppSelector(state => state.auth.status);
  const loading = status === 'loading';

  useEffect(() => {
    if (!fetchUser) return;
    if (!user && status === 'idle') {
      dispatch(fetchCurrentUser())
        .unwrap()
        .catch(() => {
          if (redirectOnFail && typeof window !== 'undefined') {
            window.location.replace('/login');
          }
        });
    }
  }, [dispatch, fetchUser, redirectOnFail, status, user]);

  const logout = useCallback(async () => {
    try {
      await dispatch(logoutUser()).unwrap();
    } finally {
      if (typeof window !== 'undefined') {
        window.location.reload();
      }
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
