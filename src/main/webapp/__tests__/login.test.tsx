import { renderWithProviders } from '@tests/test-utils';
import LoginPageClient from '../pages/[locale]/login';

describe('[locale]/login', () => {
  test('renders login form', () => {
    const { getByText } = renderWithProviders(<LoginPageClient />);
    expect(getByText('Sign in to Notes')).toBeInTheDocument();
  });
});
