import { renderWithProviders } from '@tests/test-utils';
import RegisterPageClient from '../pages/[locale]/register';

describe('[locale]/register', () => {
  test('renders registration form', () => {
    const { getByText } = renderWithProviders(<RegisterPageClient />);
    expect(getByText('Create your account')).toBeInTheDocument();
  });
});
