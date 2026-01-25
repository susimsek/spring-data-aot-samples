'use client';

import { createContext, useCallback, useContext, useEffect, useMemo, type ReactNode } from 'react';
import { useAppDispatch, useAppSelector } from '../hooks';
import { setTheme, toggleTheme as toggleThemeAction } from '../slices/themeSlice';
import type { Theme } from '../types';

const ThemeContext = createContext<{ theme: Theme; toggleTheme: () => void }>({
  theme: 'light',
  toggleTheme: () => {},
});

const STORAGE_KEY = 'theme';

function getSystemTheme(): Theme {
  if (typeof window === 'undefined') return 'light';
  return window.matchMedia?.('(prefers-color-scheme: dark)')?.matches ? 'dark' : 'light';
}

function getStoredTheme(): Theme | null {
  if (typeof window === 'undefined') return null;
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    if (value === 'dark' || value === 'light') return value;
    return null;
  } catch {
    return null;
  }
}

function setStoredTheme(value: Theme): void {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem(STORAGE_KEY, value);
  } catch {
    // ignore
  }
}

export function useTheme() {
  return useContext(ThemeContext);
}

export default function ThemeProvider({ children }: { children: ReactNode }) {
  const dispatch = useAppDispatch();
  const theme = useAppSelector(state => state.theme.theme);

  useEffect(() => {
    const stored = getStoredTheme();
    const initial = stored || getSystemTheme();
    dispatch(setTheme(initial));
  }, [dispatch]);

  useEffect(() => {
    document.documentElement.dataset.bsTheme = theme;
    setStoredTheme(theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    dispatch(toggleThemeAction());
  }, [dispatch]);

  const value = useMemo(() => ({ theme, toggleTheme }), [theme, toggleTheme]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}
