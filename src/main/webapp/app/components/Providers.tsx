'use client';

import type { ReactNode } from 'react';
import { Provider } from 'react-redux';
import ThemeProvider from './ThemeProvider';
import ToastProvider from './ToastProvider';
import store from '../store';

export default function Providers({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <Provider store={store}>
      <ThemeProvider>
        <ToastProvider>{children}</ToastProvider>
      </ThemeProvider>
    </Provider>
  );
}
