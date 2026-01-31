/// <reference types="@testing-library/jest-dom" />
'use client';

import React from 'react';
import { Provider } from 'react-redux';
import { render, screen, waitFor } from '@testing-library/react';

jest.mock('@lib/window', () => {
  const actual = jest.requireActual('@lib/window');
  return {
    __esModule: true,
    ...actual,
    replaceLocation: jest.fn(),
  };
});

import * as Window from '@lib/window';
import ThemeProvider from './ThemeProvider';
import { createTestStore } from '@tests/test-utils';
import AuthGuard from './AuthGuard';

const setPath = (pathname: string, search = '') => {
  window.history.pushState({}, '', pathname + search);
};

describe('AuthGuard', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setPath('/');
  });

  test('renders children when user is authenticated', async () => {
    const store = createTestStore({
      auth: { user: { username: 'admin', authorities: ['ROLE_USER'] }, status: 'succeeded', error: null },
      theme: { theme: 'light' },
    });

    render(
      <Provider store={store}>
        <ThemeProvider>
          <AuthGuard>
            <div data-testid="protected-content">Protected Content</div>
          </AuthGuard>
        </ThemeProvider>
      </Provider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    });
  });

  test('redirects to login when user is not authenticated on protected route', async () => {
    const store = createTestStore({
      auth: { user: null, status: 'idle', error: null },
      theme: { theme: 'light' },
    });

    setPath('/change-password');

    render(
      <Provider store={store}>
        <ThemeProvider>
          <AuthGuard>
            <div data-testid="protected-content">Protected Content</div>
          </AuthGuard>
        </ThemeProvider>
      </Provider>,
    );

    await waitFor(() => {
      expect(Window.replaceLocation).toHaveBeenCalledWith('/login?redirect=%2Fchange-password');
    });
  });

  test('renders children on public route without authentication', async () => {
    const store = createTestStore({
      auth: { user: null, status: 'idle', error: null },
      theme: { theme: 'light' },
    });

    setPath('/login');

    render(
      <Provider store={store}>
        <ThemeProvider>
          <AuthGuard>
            <div data-testid="public-content">Public Content</div>
          </AuthGuard>
        </ThemeProvider>
      </Provider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('public-content')).toBeInTheDocument();
    });
    expect(Window.replaceLocation).not.toHaveBeenCalled();
  });

  test('includes query string in redirect URL', async () => {
    const store = createTestStore({
      auth: { user: null, status: 'idle', error: null },
      theme: { theme: 'light' },
    });

    setPath('/shared-links', '?page=2');

    render(
      <Provider store={store}>
        <ThemeProvider>
          <AuthGuard>
            <div>Content</div>
          </AuthGuard>
        </ThemeProvider>
      </Provider>,
    );

    await waitFor(() => {
      expect(Window.replaceLocation).toHaveBeenCalledWith('/login?redirect=%2Fshared-links%3Fpage%3D2');
    });
  });
});
