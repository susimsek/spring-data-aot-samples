'use client';

import { configureStore } from '@reduxjs/toolkit';

jest.mock('../lib/api', () => ({
  __esModule: true,
  default: {
    currentUser: jest.fn(),
    login: jest.fn(),
    logout: jest.fn(),
  },
}));

jest.mock('../lib/auth', () => ({
  __esModule: true,
  loadStoredUser: jest.fn(() => null),
  persistUser: jest.fn(),
  clearStoredUser: jest.fn(),
}));

import Api from '../lib/api';
import { clearStoredUser, persistUser } from '../lib/auth';
import reducer, { type AuthState, clearUser, loginUser, logoutUser } from './authSlice';

describe('authSlice', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('clearUser clears state and storage', () => {
    const state = reducer({ user: { username: 'alice' } as any, status: 'succeeded', error: 'x' }, clearUser());
    expect(state).toEqual({ user: null, status: 'idle', error: null });
    expect(clearStoredUser).toHaveBeenCalledTimes(1);
  });

  test('loginUser success stores user', async () => {
    (Api.login as jest.Mock).mockResolvedValueOnce({});
    (Api.currentUser as jest.Mock).mockResolvedValueOnce({ username: 'alice', authorities: ['ROLE_USER'] });

    const store = configureStore({
      reducer: { auth: reducer },
    });

    const res = await store.dispatch(loginUser({ username: 'alice', password: 'pw' }));
    expect(loginUser.fulfilled.match(res)).toBe(true);
    expect(store.getState().auth.user?.username).toBe('alice');
    expect(persistUser).toHaveBeenCalledWith({ username: 'alice', authorities: ['ROLE_USER'] });
  });

  test('loginUser failure sets error', async () => {
    (Api.login as jest.Mock).mockRejectedValueOnce(new Error('Invalid username or password.'));

    const store = configureStore({
      reducer: { auth: reducer },
    });

    const res = await store.dispatch(loginUser({ username: 'alice', password: 'pw' }));
    expect(loginUser.rejected.match(res)).toBe(true);
    expect(store.getState().auth.status).toBe('failed');
    expect(store.getState().auth.error).toBe('Invalid username or password.');
  });

  test('logoutUser always clears stored user', async () => {
    (Api.logout as jest.Mock).mockResolvedValueOnce({});

    const store = configureStore({
      reducer: { auth: reducer },
      preloadedState: { auth: { user: { username: 'alice' }, status: 'succeeded', error: null } satisfies AuthState },
    });

    const res = await store.dispatch(logoutUser());
    expect(logoutUser.fulfilled.match(res)).toBe(true);
    expect(clearStoredUser).toHaveBeenCalledTimes(1);
    expect(store.getState().auth.user).toBeNull();
    expect(store.getState().auth.status).toBe('idle');
  });
});
