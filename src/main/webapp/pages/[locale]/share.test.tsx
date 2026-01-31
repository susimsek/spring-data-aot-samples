import { renderWithProviders } from '@tests/test-utils';
import { waitFor } from '@testing-library/react';
import SharePageClient from './share';

jest.mock('@lib/api', () => ({
  __esModule: true,
  default: { fetchNoteWithShareToken: jest.fn() },
  ApiError: class ApiError extends Error {
    status?: number;
  },
}));

import Api from '@lib/api';

describe('[locale]/share', () => {
  test('loads shared note by token', async () => {
    window.history.pushState({}, '', '/share?share_token=token123');

    (Api.fetchNoteWithShareToken as jest.Mock).mockResolvedValueOnce({
      id: 1,
      title: 'Shared note',
      content: 'Hello',
      tags: ['tag1'],
      createdDate: '2026-01-01T00:00:00.000Z',
      lastModifiedDate: '2026-01-02T00:00:00.000Z',
      owner: 'alice',
      createdBy: 'alice',
      lastModifiedBy: 'alice',
    });

    const { getByText } = renderWithProviders(<SharePageClient />);
    await waitFor(() => expect(Api.fetchNoteWithShareToken).toHaveBeenCalledWith('token123'));
    await waitFor(() => expect(getByText('Shared note')).toBeInTheDocument());
  });
});
