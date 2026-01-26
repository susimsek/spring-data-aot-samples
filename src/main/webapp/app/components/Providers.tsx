'use client';

import type { ReactNode } from 'react';
import { Provider } from 'react-redux';
import ThemeProvider from './ThemeProvider';
import ToastProvider from './ToastProvider';
import AuthGuard from './AuthGuard';
import store from '../store';

export default function Providers({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <Provider store={store}>
      <ThemeProvider>
        <ToastProvider>
          <AuthGuard>{children}</AuthGuard>
        </ToastProvider>
      </ThemeProvider>
    </Provider>
  );
}
