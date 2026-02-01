import { renderWithProviders } from '@tests/test-utils';
import AccessDeniedPage from '../pages/[locale]/403';

describe('[locale]/403', () => {
  test('renders access denied message', () => {
    const { getByText } = renderWithProviders(<AccessDeniedPage />);
    expect(getByText('Access denied')).toBeInTheDocument();
  });
});
