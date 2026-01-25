'use client';

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ThemeToggleButton from './ThemeToggleButton';
import { renderWithProviders } from '../__tests__/test-utils';

describe('ThemeToggleButton', () => {
  test('toggles between light and dark', async () => {
    const user = userEvent.setup();

    renderWithProviders(<ThemeToggleButton />, {
      preloadedState: {
        auth: { user: null, status: 'idle', error: null },
        theme: { theme: 'light' },
      },
    });

    // When theme is light, button shows "Dark" (switch to dark mode).
    expect(screen.getByRole('button', { name: /switch to dark mode/i })).toHaveTextContent('Dark');

    await user.click(screen.getByRole('button'));

    // When theme is dark, button shows "Light" (switch to light mode).
    expect(screen.getByRole('button', { name: /switch to light mode/i })).toHaveTextContent('Light');
  });
});
