import { renderWithProviders } from '@tests/test-utils';
import NotFoundRoutePage from './404';

describe('[locale]/404', () => {
  test('renders not found message', () => {
    const { getByText } = renderWithProviders(<NotFoundRoutePage />);
    expect(getByText('Page not found')).toBeInTheDocument();
  });
});
