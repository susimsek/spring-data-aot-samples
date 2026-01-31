'use client';

import { configureStore } from '@reduxjs/toolkit';
import { TypedUseSelectorHook, useDispatch, useSelector } from 'react-redux';
import authReducer from '../slices/authSlice';
import themeReducer from '../slices/themeSlice';
import { getMatchMedia } from './window';
import type { Theme } from './types';

const THEME_KEY = 'theme';

function getInitialTheme(): Theme {
  try {
    const stored = localStorage.getItem(THEME_KEY);
    if (stored === 'dark' || stored === 'light') return stored;
    const matchMedia = getMatchMedia();
    return matchMedia?.('(prefers-color-scheme: dark)')?.matches ? 'dark' : 'light';
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

export const useAppDispatch = useDispatch.withTypes<AppDispatch>();
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;

export default store;
