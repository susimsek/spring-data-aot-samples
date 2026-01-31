import { screen } from '@testing-library/react';
import Footer from './Footer';
import { renderWithProviders } from '../__tests__/test-utils';

describe('Footer', () => {
  test('renders footer content', () => {
    renderWithProviders(<Footer />);
    expect(screen.getByText(/notes/i)).toBeInTheDocument();
    expect(screen.getByText(/spring boot/i)).toBeInTheDocument();
  });
});
