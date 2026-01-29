'use client';

type MockAxiosInstance = jest.Mock & {
  get: jest.Mock;
  post: jest.Mock;
  put: jest.Mock;
  patch: jest.Mock;
  delete: jest.Mock;
  interceptors: { response: { use: jest.Mock } };
};

function createAxiosInstance(): MockAxiosInstance {
  const instance: any = jest.fn();
  instance.get = jest.fn();
  instance.post = jest.fn();
  instance.put = jest.fn();
  instance.patch = jest.fn();
  instance.delete = jest.fn();
  instance.interceptors = { response: { use: jest.fn() } };
  return instance as MockAxiosInstance;
}

const apiInstance = createAxiosInstance();
const publicApiInstance = createAxiosInstance();

const axiosDefault = {
  create: jest.fn(),
  isAxiosError: jest.fn(),
};

jest.mock('axios', () => ({
  __esModule: true,
  default: axiosDefault,
}));

const auth = {
  buildRedirectQuery: jest.fn(() => encodeURIComponent('/')),
  clearStoredUser: jest.fn(),
  isAdmin: jest.fn(() => false),
  loadStoredUser: jest.fn(() => null),
};

jest.mock('./auth', () => ({
  __esModule: true,
  ...auth,
}));

const win = {
  replaceLocation: jest.fn(),
  reloadPage: jest.fn(),
};

jest.mock('./window', () => ({
  __esModule: true,
  ...win,
}));

describe('Api client', () => {
  afterEach(() => {
    jest.useRealTimers();
  });

  beforeEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
    jest.resetModules();

    axiosDefault.create.mockReset();
    axiosDefault.isAxiosError.mockReset();
    axiosDefault.create.mockReturnValueOnce(apiInstance).mockReturnValueOnce(publicApiInstance);
    axiosDefault.isAxiosError.mockImplementation((err: any) => !!err?.isAxiosError);

    apiInstance.get.mockReset();
    apiInstance.post.mockReset();
    apiInstance.put.mockReset();
    apiInstance.patch.mockReset();
    apiInstance.delete.mockReset();
    apiInstance.mockReset();
    apiInstance.interceptors.response.use.mockReset();

    publicApiInstance.get.mockReset();
    publicApiInstance.post.mockReset();
    publicApiInstance.put.mockReset();
    publicApiInstance.patch.mockReset();
    publicApiInstance.delete.mockReset();
    publicApiInstance.mockReset();
    publicApiInstance.interceptors.response.use.mockReset();

    auth.buildRedirectQuery.mockClear();
    auth.clearStoredUser.mockClear();
    auth.isAdmin.mockClear();
    auth.loadStoredUser.mockClear();

    win.replaceLocation.mockClear();
    win.reloadPage.mockClear();
  });

  test('builds note endpoints based on admin flag', async () => {
    auth.isAdmin.mockReturnValueOnce(true);
    auth.loadStoredUser.mockReturnValueOnce({ username: 'admin', authorities: ['ROLE_ADMIN'] });

    apiInstance.post.mockResolvedValueOnce({ data: { id: 1 } });

    const { default: Api } = await import('./api');
    await Api.createNote({ title: 'x' });

    expect(apiInstance.post).toHaveBeenCalledWith('/api/admin/notes', { title: 'x' });
  });

  test('fetchNotes builds a query string', async () => {
    apiInstance.get.mockResolvedValueOnce({ data: { content: [] } });

    const { default: Api } = await import('./api');
    void Api;
    await Api.fetchNotes({
      view: 'active',
      page: 1,
      size: 10,
      sort: 'createdDate,desc',
      query: 'hello',
      tags: ['t1', 't2'],
      color: 'red',
      pinned: true,
    });

    const url = apiInstance.get.mock.calls[0][0] as string;
    expect(url.startsWith('/api/notes?')).toBe(true);

    const qs = url.split('?')[1] ?? '';
    const params = new URLSearchParams(qs);
    expect(params.get('page')).toBe('1');
    expect(params.get('size')).toBe('10');
    expect(params.get('sort')).toBe('createdDate,desc');
    expect(params.get('q')).toBe('hello');
    expect(params.get('color')).toBe('red');
    expect(params.get('pinned')).toBe('true');
    expect(params.getAll('tags')).toEqual(['t1', 't2']);
  });

  test('covers most API methods and branches', async () => {
    apiInstance.get.mockResolvedValue({ data: {} });
    apiInstance.post.mockResolvedValue({ data: {} });
    apiInstance.put.mockResolvedValue({ data: {} });
    apiInstance.patch.mockResolvedValue({ data: {} });
    apiInstance.delete.mockResolvedValue({ data: {} });
    publicApiInstance.get.mockResolvedValue({ data: {} });
    publicApiInstance.post.mockResolvedValue({ data: {} });

    const { default: Api } = await import('./api');
    void Api;

    await Api.login({ username: 'u', password: 'p' });
    await Api.register({ username: 'u' });
    await Api.changePassword({ currentPassword: 'a', newPassword: 'b' });
    await Api.currentUser();
    await Api.logout();

    await Api.fetchNotes({ page: 0, size: 10 });
    await Api.createNote({ title: 't' });
    await Api.updateNote(1, { title: 't' });
    await Api.patchNote(1, { title: 't' });
    await Api.softDelete(1);
    await Api.restore(1);
    await Api.deletePermanent(1);
    await Api.emptyTrash();
    await Api.fetchNote(1);
    await Api.bulkAction({ ids: [1] });
    await Api.fetchRevisions(1, undefined);
    await Api.fetchRevision(1, 1);
    await Api.restoreRevision(1, 1);

    // fetchTags: array response
    apiInstance.get.mockResolvedValueOnce({ data: ['a', 1, 'b'] });
    await expect(Api.fetchTags('q')).resolves.toEqual(['a', 'b']);

    // fetchTags: pageable response
    apiInstance.get.mockResolvedValueOnce({ data: { content: [{ name: 'x' }, { label: 'y' }, 'z'] } });
    await expect(Api.fetchTags(undefined)).resolves.toEqual(['x', 'y', 'z']);

    // fetchTags: unknown response
    apiInstance.get.mockResolvedValueOnce({ data: { nope: true } });
    await expect(Api.fetchTags('q')).resolves.toEqual([]);

    await Api.searchUsers('q');
    await Api.searchUsers(undefined);
    await Api.changeOwner(1, { owner: 'u' });
    await Api.fetchShareLinks(1, 0, 10);
    await Api.createShareLink(1, { expiresAt: 'x' });
    await Api.revokeShareLink(1);
    await Api.fetchMyShareLinks('createdDate,desc', 'q', 'all', '', '', 0, 10);
    await Api.fetchMyShareLinks('createdDate,desc', 'q', 'active', 'a', 'b', 0, 10);
    await Api.fetchNoteWithShareToken('token');
  });

  test('response interceptor retries once on 401 by refreshing', async () => {
    apiInstance.get.mockResolvedValueOnce({ data: { ok: true } });
    publicApiInstance.post.mockResolvedValueOnce({ data: { refreshed: true } });
    apiInstance.mockResolvedValueOnce({ data: { ok: true } });

    const { default: Api } = await import('./api');
    void Api;
    // Ensure interceptors are registered.
    expect(apiInstance.interceptors.response.use).toHaveBeenCalledTimes(1);

    const errorHandler = apiInstance.interceptors.response.use.mock.calls[0][1] as (err: unknown) => Promise<unknown>;

    const config: any = { url: '/api/notes', _retry: false };
    const err: any = {
      isAxiosError: true,
      message: 'Unauthorized',
      response: { status: 401, data: { title: 'Unauthorized' } },
      config,
    };

    await errorHandler(err);

    expect(publicApiInstance.post).toHaveBeenCalledWith('/api/auth/refresh');
    expect(apiInstance).toHaveBeenCalledWith(expect.objectContaining({ url: '/api/notes', _retry: true }));
  });

  test('does not attempt refresh for excluded auth endpoints', async () => {
    apiInstance.get.mockRejectedValueOnce(new Error('should not be used'));

    await import('./api');
    expect(apiInstance.interceptors.response.use).toHaveBeenCalledTimes(1);
    const errorHandler = apiInstance.interceptors.response.use.mock.calls[0][1] as (err: unknown) => Promise<unknown>;

    const config: any = { url: '/api/auth/login' };
    const err: any = {
      isAxiosError: true,
      message: 'Unauthorized',
      response: { status: 401, data: { title: 'Unauthorized' } },
      config,
    };

    await expect(errorHandler(err)).rejects.toMatchObject({ status: 401, title: 'Unauthorized' });
    expect(publicApiInstance.post).not.toHaveBeenCalled();
  });

  test('does not attempt refresh when skipAuthRedirect is set', async () => {
    await import('./api');
    const errorHandler = apiInstance.interceptors.response.use.mock.calls[0][1] as (err: unknown) => Promise<unknown>;

    const err: any = {
      isAxiosError: true,
      message: 'Unauthorized',
      response: { status: 401, data: { title: 'Unauthorized' } },
      config: { url: '/api/notes', skipAuthRedirect: true },
    };

    await expect(errorHandler(err)).rejects.toMatchObject({ status: 401, title: 'Unauthorized' });
    expect(publicApiInstance.post).not.toHaveBeenCalled();
  });

  test('does not retry when request is already marked as retried', async () => {
    await import('./api');
    const errorHandler = apiInstance.interceptors.response.use.mock.calls[0][1] as (err: unknown) => Promise<unknown>;

    const err: any = {
      isAxiosError: true,
      message: 'Unauthorized',
      response: { status: 401, data: { title: 'Unauthorized' } },
      config: { url: '/api/notes', _retry: true },
    };

    await expect(errorHandler(err)).rejects.toMatchObject({ status: 401, title: 'Unauthorized' });
    expect(publicApiInstance.post).not.toHaveBeenCalled();
  });

  test('does not retry for non-401 errors', async () => {
    await import('./api');
    const errorHandler = apiInstance.interceptors.response.use.mock.calls[0][1] as (err: unknown) => Promise<unknown>;

    const err: any = {
      isAxiosError: true,
      message: 'Forbidden',
      response: { status: 403, data: { title: 'Forbidden' } },
      config: { url: '/api/notes' },
    };

    await expect(errorHandler(err)).rejects.toMatchObject({ status: 403, title: 'Forbidden' });
    expect(publicApiInstance.post).not.toHaveBeenCalled();
  });

  test('normalizes non-axios errors', async () => {
    await import('./api');
    expect(apiInstance.interceptors.response.use).toHaveBeenCalledTimes(1);
    const errorHandler = apiInstance.interceptors.response.use.mock.calls[0][1] as (err: unknown) => Promise<unknown>;

    await expect(errorHandler(new Error('boom'))).rejects.toMatchObject({ message: 'boom' });
  });

  test('uses axios error message when title/detail are missing', async () => {
    await import('./api');
    expect(apiInstance.interceptors.response.use).toHaveBeenCalledTimes(1);
    const errorHandler = apiInstance.interceptors.response.use.mock.calls[0][1] as (err: unknown) => Promise<unknown>;

    const err: any = {
      isAxiosError: true,
      message: 'Network error',
      response: { status: 500, data: {} },
      config: { url: '/api/notes' },
    };

    await expect(errorHandler(err)).rejects.toMatchObject({ status: 500, message: 'Network error' });
  });

  test('prefers detail over title for error message', async () => {
    await import('./api');
    const errorHandler = apiInstance.interceptors.response.use.mock.calls[0][1] as (err: unknown) => Promise<unknown>;

    const err: any = {
      isAxiosError: true,
      message: 'ignored',
      response: { status: 400, data: { title: 'Bad Request', detail: 'Validation failed' } },
      config: { url: '/api/notes' },
    };

    await expect(errorHandler(err)).rejects.toMatchObject({ status: 400, title: 'Bad Request', message: 'Validation failed' });
  });

  test('uses default message when axios error message is missing', async () => {
    await import('./api');
    expect(apiInstance.interceptors.response.use).toHaveBeenCalledTimes(1);
    const errorHandler = apiInstance.interceptors.response.use.mock.calls[0][1] as (err: unknown) => Promise<unknown>;

    const err: any = {
      isAxiosError: true,
      response: { status: 500, data: {} },
      config: { url: '/api/notes' },
    };

    await expect(errorHandler(err)).rejects.toMatchObject({ status: 500, message: 'Request failed' });
  });

  test('refresh failure triggers redirect and rejects', async () => {
    jest.useFakeTimers();
    publicApiInstance.post.mockRejectedValueOnce({
      isAxiosError: true,
      message: 'Refresh failed',
      response: { status: 401, data: { title: 'Unauthorized' } },
    });

    globalThis.history.replaceState({}, '', '/notes');

    // Some tests render without a body; ensure it exists.
    document.body.innerHTML = '<div></div>';

    const { default: Api } = await import('./api');
    void Api;
    // Call refresh indirectly by invoking the error handler with a retry-able request.
    const errorHandler = apiInstance.interceptors.response.use.mock.calls[0][1] as (err: unknown) => Promise<unknown>;
    const err: any = {
      isAxiosError: true,
      message: 'Unauthorized',
      response: { status: 401, data: { title: 'Unauthorized' } },
      config: { url: '/api/notes' },
    };

    await expect(errorHandler(err)).rejects.toBeInstanceOf(Error);
    expect(auth.clearStoredUser).toHaveBeenCalled();

    jest.advanceTimersByTime(140);
    expect(win.replaceLocation).toHaveBeenCalledWith(`/login?redirect=${encodeURIComponent('/')}`);
    jest.useRealTimers();
  });
});
