'use client';

import { useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { isAdmin as isAdminUser } from './auth.js';
import { fetchCurrentUser, logoutUser } from '../slices/authSlice.js';

export default function useAuth({ redirectOnFail = false, fetchUser = true } = {}) {
  const dispatch = useDispatch();
  const user = useSelector(state => state.auth.user);
  const status = useSelector(state => state.auth.status);
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
