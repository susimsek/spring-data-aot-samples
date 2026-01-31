'use client';

import type { ReactElement } from 'react';
import React from 'react';
import { configureStore } from '@reduxjs/toolkit';
import { Provider } from 'react-redux';
import { render, type RenderOptions } from '@testing-library/react';
import { combineReducers } from 'redux';
import { I18nextProvider } from 'react-i18next';
import authReducer from '../slices/authSlice';
import themeReducer from '../slices/themeSlice';
import ThemeProvider from '../components/ThemeProvider';
import i18n from './test-i18n';
import type { RootState } from '../lib/store';

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
      <I18nextProvider i18n={i18n}>
        <Provider store={store}>
          <ThemeProvider>{children}</ThemeProvider>
        </Provider>
      </I18nextProvider>
    );
  }

  return {
    store,
    ...render(ui, { wrapper: Wrapper, ...options }),
  };
}
