'use client';

import { render, screen } from '@testing-library/react';
import Footer from './Footer';

describe('Footer', () => {
  test('renders footer content', () => {
    render(<Footer />);
    expect(screen.getByText(/notes/i)).toBeInTheDocument();
    expect(screen.getByText(/spring boot/i)).toBeInTheDocument();
  });
});
