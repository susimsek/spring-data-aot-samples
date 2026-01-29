'use client';

import type { ReactElement } from 'react';
import React from 'react';
import { configureStore } from '@reduxjs/toolkit';
import { Provider } from 'react-redux';
import { render, type RenderOptions } from '@testing-library/react';
import { combineReducers } from 'redux';
import authReducer from '../slices/authSlice';
import themeReducer from '../slices/themeSlice';
import ThemeProvider from '../components/ThemeProvider';
import type { RootState } from '../store';

const rootReducer = combineReducers({
  auth: authReducer,
  theme: themeReducer,
});

export function createTestStore(preloadedState?: Partial<RootState>) {
  return configureStore({
    reducer: rootReducer,
    preloadedState: preloadedState as RootState | undefined,
  });
}

export function renderWithProviders(
  ui: ReactElement,
  options: { preloadedState?: Partial<RootState>; store?: ReturnType<typeof createTestStore> } & Omit<RenderOptions, 'wrapper'> = {},
) {
  const store = options.store ?? createTestStore(options.preloadedState);

  function Wrapper({ children }: Readonly<{ children: React.ReactNode }>) {
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
