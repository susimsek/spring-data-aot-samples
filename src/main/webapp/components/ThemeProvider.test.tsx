'use client';

import React from 'react';
import { Provider } from 'react-redux';
import { render, screen, waitFor } from '@testing-library/react';
import ThemeProvider from './ThemeProvider';
import { createTestStore } from '@tests/test-utils';

describe('ThemeProvider', () => {
  beforeEach(() => {
    localStorage.clear();
    delete (globalThis as any).matchMedia;
    document.documentElement.dataset.bsTheme = '';
  });

  test('uses stored theme when present', async () => {
    localStorage.setItem('theme', 'dark');
    const store = createTestStore({ auth: { user: null, status: 'idle', sessionChecked: true, error: null }, theme: { theme: 'light' } });

    render(
      <Provider store={store}>
        <ThemeProvider>
          <div>child</div>
        </ThemeProvider>
      </Provider>,
    );

    expect(screen.getByText('child')).toBeInTheDocument();

    await waitFor(() => {
      expect(store.getState().theme.theme).toBe('dark');
      expect(document.documentElement.dataset.bsTheme).toBe('dark');
    });
  });

  test('falls back to system theme when no stored theme', async () => {
    (globalThis as any).matchMedia = jest.fn(() => ({ matches: true }));
    const store = createTestStore({ auth: { user: null, status: 'idle', sessionChecked: true, error: null }, theme: { theme: 'light' } });

    render(
      <Provider store={store}>
        <ThemeProvider>
          <div>child</div>
        </ThemeProvider>
      </Provider>,
    );

    await waitFor(() => {
      expect(store.getState().theme.theme).toBe('dark');
      expect(document.documentElement.dataset.bsTheme).toBe('dark');
    });
  });

  test('ignores invalid stored theme values', async () => {
    localStorage.setItem('theme', 'blue');
    (globalThis as any).matchMedia = jest.fn(() => ({ matches: true }));
    const store = createTestStore({ auth: { user: null, status: 'idle', sessionChecked: true, error: null }, theme: { theme: 'light' } });

    render(
      <Provider store={store}>
        <ThemeProvider>
          <div>child</div>
        </ThemeProvider>
      </Provider>,
    );

    await waitFor(() => {
      expect(store.getState().theme.theme).toBe('dark');
    });
  });

  test('handles storage failures gracefully', async () => {
    const getItemSpy = jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('boom');
    });

    (globalThis as any).matchMedia = jest.fn(() => ({ matches: false }));
    const store = createTestStore({ auth: { user: null, status: 'idle', sessionChecked: true, error: null }, theme: { theme: 'dark' } });

    render(
      <Provider store={store}>
        <ThemeProvider>
          <div>child</div>
        </ThemeProvider>
      </Provider>,
    );

    await waitFor(() => {
      expect(store.getState().theme.theme).toBe('light');
    });

    getItemSpy.mockRestore();
  });

  test('ignores storage failures when persisting theme', async () => {
    const setItemSpy = jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('boom');
    });

    (globalThis as any).matchMedia = jest.fn(() => ({ matches: true }));
    const store = createTestStore({ auth: { user: null, status: 'idle', sessionChecked: true, error: null }, theme: { theme: 'light' } });

    render(
      <Provider store={store}>
        <ThemeProvider>
          <div>child</div>
        </ThemeProvider>
      </Provider>,
    );

    await waitFor(() => {
      expect(store.getState().theme.theme).toBe('dark');
    });

    setItemSpy.mockRestore();
  });
});
