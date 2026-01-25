'use client';

import type { ReactElement } from 'react';
import React from 'react';
import { configureStore } from '@reduxjs/toolkit';
import { Provider } from 'react-redux';
import { render, type RenderOptions } from '@testing-library/react';
import authReducer from '../slices/authSlice';
import themeReducer from '../slices/themeSlice';
import ThemeProvider from '../components/ThemeProvider';

export function createTestStore(preloadedState?: any) {
  return configureStore({
    reducer: {
      auth: authReducer,
      theme: themeReducer,
    },
    preloadedState,
  });
}

export function renderWithProviders(
  ui: ReactElement,
  options: { preloadedState?: any; store?: ReturnType<typeof createTestStore> } & Omit<RenderOptions, 'wrapper'> = {},
) {
  const store = options.store ?? createTestStore(options.preloadedState);

  function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <Provider store={store}>
        <ThemeProvider>{children}</ThemeProvider>
      </Provider>
    );
  }

  return {
    store,
    ...render(ui, { wrapper: Wrapper, ...options }),
  };
}
