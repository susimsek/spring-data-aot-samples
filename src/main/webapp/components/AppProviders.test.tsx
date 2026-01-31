import { render, screen } from '@testing-library/react';
import AppProviders from './AppProviders';

jest.mock('./AuthGuard', () => ({
  __esModule: true,
  default: function AuthGuard({ children }: Readonly<{ children: React.ReactNode }>) {
    return <>{children}</>;
  },
}));

describe('AppProviders', () => {
  test('renders children', () => {
    render(
      <AppProviders>
        <div data-testid="child" />
      </AppProviders>,
    );

    expect(screen.getByTestId('child')).toBeInTheDocument();
  });
});
