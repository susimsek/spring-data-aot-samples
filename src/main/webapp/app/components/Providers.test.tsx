'use client';

import { render, screen, waitFor } from '@testing-library/react';
import Providers from './Providers';

describe('Providers', () => {
  test('renders children and sets default theme attribute', async () => {
    render(
      <Providers>
        <div>child</div>
      </Providers>,
    );

    expect(screen.getByText('child')).toBeInTheDocument();

    await waitFor(() => {
      expect(document.documentElement.dataset.bsTheme).toBeTruthy();
    });
  });
});
