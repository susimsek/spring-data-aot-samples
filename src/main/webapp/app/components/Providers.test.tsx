'use client';

import React from 'react';
import { Provider } from 'react-redux';
import { render, screen, waitFor } from '@testing-library/react';
import ThemeProvider from './ThemeProvider';
import ToastProvider from './ToastProvider';
import AuthGuard from './AuthGuard';
import { createTestStore } from '../__tests__/test-utils';

jest.mock('../lib/window', () => ({
  __esModule: true,
  replaceLocation: jest.fn(),
  reloadPage: jest.fn(),
}));

const setPath = (pathname: string) => {
  window.history.pushState({}, '', pathname);
};

describe('Providers', () => {
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
          <ToastProvider>
            <AuthGuard>
              <div>child</div>
            </AuthGuard>
          </ToastProvider>
        </ThemeProvider>
      </Provider>,
    );

    await waitFor(() => {
      expect(screen.getByText('child')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(document.documentElement.dataset.bsTheme).toBeTruthy();
    });
  });
});
