'use client';

import React from 'react';
import { Provider } from 'react-redux';
import { act, renderHook } from '@testing-library/react';

jest.mock('./api', () => ({
  __esModule: true,
  default: {
    logout: jest.fn().mockResolvedValue({}),
    currentUser: jest.fn().mockResolvedValue({}),
    login: jest.fn().mockResolvedValue({}),
  },
}));

jest.mock('./window', () => {
  const actual = jest.requireActual('./window');
  return {
    __esModule: true,
    ...actual,
    reloadPage: jest.fn(),
    replaceLocation: jest.fn(),
  };
});

import Api from './api';
import * as Window from './window';
import ThemeProvider from '@components/ThemeProvider';
import { createTestStore } from '@tests/test-utils';
import useAuth from './useAuth';

describe('useAuth', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('returns auth flags based on store state', () => {
    const store = createTestStore({
      auth: { user: { username: 'admin', authorities: ['ROLE_ADMIN'] }, status: 'succeeded', error: null },
      theme: { theme: 'light' },
    });

    const wrapper = ({ children }: Readonly<{ children: React.ReactNode }>) => (
      <Provider store={store}>
        <ThemeProvider>{children}</ThemeProvider>
      </Provider>
    );

    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.loading).toBe(false);
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.isAdmin).toBe(true);
    expect(result.current.user?.username).toBe('admin');
  });

  test('logout dispatches and navigates to home page', async () => {
    const store = createTestStore({
      auth: { user: { username: 'alice', authorities: ['ROLE_USER'] }, status: 'succeeded', error: null },
      theme: { theme: 'light' },
    });

    const wrapper = ({ children }: Readonly<{ children: React.ReactNode }>) => (
      <Provider store={store}>
        <ThemeProvider>{children}</ThemeProvider>
      </Provider>
    );

    const { result } = renderHook(() => useAuth(), { wrapper });
    await act(async () => {
      await result.current.logout();
    });

    expect((Api as any).logout).toHaveBeenCalledTimes(1);
    expect(Window.replaceLocation).toHaveBeenCalledWith('/');
  });

  test('redirectOnFail triggers navigation when fetching user fails', async () => {
    const store = createTestStore({
      auth: { user: null, status: 'idle', error: null },
      theme: { theme: 'light' },
    });

    const wrapper = ({ children }: Readonly<{ children: React.ReactNode }>) => (
      <Provider store={store}>
        <ThemeProvider>{children}</ThemeProvider>
      </Provider>
    );

    renderHook(() => useAuth({ redirectOnFail: true }), { wrapper });

    // Should redirect with encoded path as redirect param
    expect(Window.replaceLocation).toHaveBeenCalledWith(expect.stringContaining('/login?redirect='));
  });
});
