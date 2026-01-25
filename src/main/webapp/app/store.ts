'use client';

import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import themeReducer from './slices/themeSlice';
import type { Theme } from './types';

const THEME_KEY = 'theme';

function getInitialTheme(): Theme {
  if (typeof window === 'undefined') return 'light';
  try {
    const stored = localStorage.getItem(THEME_KEY);
    if (stored === 'dark' || stored === 'light') return stored;
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

export const store = configureStore({
  reducer: {
    auth: authReducer,
    theme: themeReducer,
  },
  preloadedState: {
    theme: {
      theme: getInitialTheme(),
    },
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

export default store;
