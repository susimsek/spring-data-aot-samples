import { renderWithProviders } from '@tests/test-utils';
import { waitFor } from '@testing-library/react';
import SharedLinksPage from '../pages/[locale]/shared-links';

jest.mock('@lib/api', () => ({
  __esModule: true,
  default: { fetchMyShareLinks: jest.fn() },
  ApiError: class ApiError extends Error {
    status?: number;
  },
}));

import Api from '@lib/api';

describe('[locale]/shared-links', () => {
  test('loads links when authenticated', async () => {
    (Api.fetchMyShareLinks as jest.Mock).mockResolvedValueOnce({
      content: [
        {
          id: 1,
          token: 'tok',
          noteTitle: 'Note 1',
          revoked: false,
          expired: false,
        },
      ],
      page: {
        totalElements: 1,
        totalPages: 1,
        number: 0,
      },
    });

    const { getByText } = renderWithProviders(<SharedLinksPage />, {
      preloadedState: {
        auth: { user: { username: 'user' }, status: 'succeeded', sessionChecked: true, error: null },
      },
    });

    await waitFor(() => expect(Api.fetchMyShareLinks).toHaveBeenCalled());
    await waitFor(() => expect(getByText('Note 1')).toBeInTheDocument());
  });
});
