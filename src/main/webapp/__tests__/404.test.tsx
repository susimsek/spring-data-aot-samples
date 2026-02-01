import { renderWithProviders } from '@tests/test-utils';
import NotFoundRoutePage from '../pages/[locale]/404';

describe('[locale]/404', () => {
  test('renders not found message', () => {
    const { getByText } = renderWithProviders(<NotFoundRoutePage />);
    expect(getByText('Page not found')).toBeInTheDocument();
  });
});
