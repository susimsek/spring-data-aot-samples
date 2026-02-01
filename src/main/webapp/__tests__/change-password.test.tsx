import { renderWithProviders } from '@tests/test-utils';
import ChangePasswordPageClient from '../pages/[locale]/change-password';

describe('[locale]/change-password', () => {
  test('renders change password form', () => {
    const { getByText } = renderWithProviders(<ChangePasswordPageClient />);
    expect(getByText('Change password')).toBeInTheDocument();
  });
});
