'use client';

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AppNavbar from './AppNavbar';
import { renderWithProviders } from '@tests/test-utils';

describe('AppNavbar', () => {
  test('renders search and calls callbacks', async () => {
    const user = userEvent.setup();
    const onSearchChange = jest.fn();
    const onSearchClear = jest.fn();

    renderWithProviders(
      <AppNavbar
        search=""
        showSearch
        onSearchChange={onSearchChange}
        onSearchClear={onSearchClear}
        showHomeButton
        showAuthDropdown
        badgeLabel="Admin"
      />,
      {
        preloadedState: {
          auth: { user: { username: 'alice', authorities: ['ROLE_USER'] }, status: 'succeeded', sessionChecked: true, error: null },
          theme: { theme: 'light' },
        },
      },
    );

    expect(screen.getByText('Admin')).toBeInTheDocument();

    // Expand the navbar so content inside Navbar.Collapse is mounted.
    const toggle = document.querySelector('button[aria-controls="appNavbar"]') as HTMLButtonElement | null;
    expect(toggle).toBeTruthy();
    if (toggle) {
      await user.click(toggle);
    }

    const search = screen.getByPlaceholderText('Search notes');
    await user.type(search, 'abc');
    expect(onSearchChange).toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: /clear search/i }));
    expect(onSearchClear).toHaveBeenCalledTimes(1);

    const home = screen.getByRole('link', { name: /home/i });
    expect(home).toHaveAttribute('href', '/');
  });

  test('hides auth dropdown when requireAuthForActions is true and user is not authenticated', () => {
    renderWithProviders(<AppNavbar showAuthDropdown requireAuthForActions />, {
      preloadedState: { auth: { user: null, status: 'idle', sessionChecked: true, error: null }, theme: { theme: 'light' } },
    });

    expect(screen.queryByText(/signed in as/i)).not.toBeInTheDocument();
  });

  test('keeps search visible while hiding home/auth actions when unauthenticated and requireAuthForActions is true', () => {
    renderWithProviders(
      <AppNavbar
        showSearch
        showHomeButton
        showAuthDropdown
        requireAuthForActions
        search=""
        onSearchChange={() => {}}
        onSearchClear={() => {}}
      />,
      {
        preloadedState: { auth: { user: null, status: 'idle', sessionChecked: true, error: null }, theme: { theme: 'light' } },
      },
    );

    expect(screen.getByPlaceholderText('Search notes')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /home/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /alice/i })).not.toBeInTheDocument();
  });
});
