'use client';

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AuthDropdown from './AuthDropdown';
import { renderWithProviders } from '@tests/test-utils';

describe('AuthDropdown', () => {
  test('renders nothing when not authenticated', () => {
    const { container } = renderWithProviders(<AuthDropdown />, {
      preloadedState: { auth: { user: null, status: 'idle', sessionChecked: true, error: null }, theme: { theme: 'light' } },
    });
    expect(container).toBeEmptyDOMElement();
  });

  test('renders username and change-password link when authenticated', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AuthDropdown />, {
      preloadedState: {
        auth: { user: { username: 'alice', authorities: ['ROLE_USER'] }, status: 'succeeded', sessionChecked: true, error: null },
        theme: { theme: 'light' },
      },
    });

    const toggle = screen.getByRole('button', { name: /alice/i });
    expect(toggle).toBeInTheDocument();

    await user.click(toggle);

    expect(screen.getByText(/signed in as alice/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /change password/i })).toHaveAttribute('href', '/change-password');
  });
});
